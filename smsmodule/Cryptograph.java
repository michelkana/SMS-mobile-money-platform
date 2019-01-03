package smsmodule;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class Cryptograph {

  final private static String password = "HandyBankPAYINGwithoutCaSh";
  final private byte [] salt = { (byte) 0xc9, (byte) 0xc9,(byte) 0xc9,(byte) 0xc9,(byte) 0xc9,(byte) 0xc9,(byte) 0xc9,(byte) 0xc9};
  final int iterations = 3;
  private Cipher encryptCipher;
  private Cipher decryptCipher;
  private String charset = "UTF16";
  private boolean activated;
  
  public Cryptograph(boolean active) {
     try {
     	activated = active; 
      final PBEParameterSpec ps = new PBEParameterSpec(salt, 20);
      final SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
      final SecretKey k = kf.generateSecret(new PBEKeySpec(password.toCharArray()));
      encryptCipher = Cipher.getInstance("PBEWithMD5AndDES/CBC/PKCS5Padding");
      encryptCipher.init (Cipher.ENCRYPT_MODE, k, ps);
      decryptCipher = Cipher.getInstance("PBEWithMD5AndDES/CBC/PKCS5Padding");
      decryptCipher.init (Cipher.DECRYPT_MODE, k, ps);
    }
    catch (Exception e) {
      System.out.println("Could not initialize CryptoLibrary: " +
                                  e.getMessage());
    }  
  }


  public synchronized String encrypt(String str) throws SecurityException {
  	if (activated){
	    try {
	      byte[] b = str.getBytes();
	      return new String(encryptCipher.doFinal(b));
	    }
	    catch (Exception e){
	      throw new SecurityException("Could not encrypt: " + str + "\nError: " + e.getMessage());
	    }
	   }else{
	   	return str;
	  }
  }
  
   public synchronized String encryptHEX(String str) throws SecurityException {
  	if (activated){
	    try {
	      byte[] b = str.getBytes();
	      byte[] a = encryptCipher.doFinal(b);
				StringBuffer sb = new StringBuffer(a.length * 2);
				for(int x = 0 ; x < a.length ; x++)
				{
				   sb.append(( "00" + Integer.toHexString(0xff & a[x])).substring(1, 3));
				}
				return sb.toString();
	    }
	    catch (Exception e){
	      throw new SecurityException("Could not encrypt: " + str + "\nError: " + e.getMessage());
	    }
	   }else{
	   	return str;
	  }
  }
  
   public synchronized void encryptFile(String filename, String fileContent) throws SecurityException {
   		if (activated){
		    try {
		      CipherOutputStream out = new CipherOutputStream(new FileOutputStream(filename), encryptCipher);
		      String[] lines = fileContent.split("\n");
		      for ( int i = 0; i < lines.length; i++ ){      	
		      	//out.write((lines[i]+"\n").getBytes(), 0, (lines[i]+"\n").getBytes().length);
		      	out.write((lines[i]+System.getProperty("line.separator")).getBytes(), 0, (lines[i]+System.getProperty("line.separator")).getBytes().length);
		      }
		      out.close();
		    }
		    catch (Exception e){
		      throw new SecurityException("Could not encrypt file: " + filename + "\nError: " + e.getMessage());
		    }
		   }else{
		   		try{
				   	BufferedWriter out = new BufferedWriter(
	                           new FileWriter( filename ) );
				   	out.write(fileContent, 0, fileContent.length());
				   	out.close();
			   }
			   catch (Exception e){
		      throw new SecurityException("Could not encrypt file: " + filename + "\nError: " + e.getMessage());
		    }
		  }
  }

  public synchronized String decrypt(String str) throws SecurityException  {
  	if ( activated ){
	    try {
	      byte[] dec = str.getBytes();
	      byte[] b = decryptCipher.doFinal(dec);
	      return new String(b);
	    }
	    catch (Exception e) {
	      throw new SecurityException("Could not decrypt: " + str + "\nError: " + e.getMessage());
	    }
	   }else{
	   	return str;
	  }
  }

  public synchronized String decryptHEX(String hex) throws SecurityException  {
  	if ( activated ){
	    try {
				byte[] dec = new byte[hex.length() / 2];
				for (int i = 0; i < dec.length; i++) {
				   dec[i] = (byte) Integer.parseInt(hex.substring(2*i, 2*i+2), 16);
				}
	      byte[] b = decryptCipher.doFinal(dec);
	      return new String(b);
	    }
	    catch (Exception e) {
	      throw new SecurityException("Could not decrypt: " + hex + "\nError: " + e.getMessage());
	    }
	   }else{
	   	return hex;
	  }
  }
  
  public synchronized String decryptFile(String filename) throws SecurityException  {
  	if ( activated ){
	    try {
	      String str = "";      
	      CipherInputStream in = new CipherInputStream(new FileInputStream(filename), decryptCipher);
	      byte[] buf = new byte[1024];
	      int numRead= in.read(buf);
	      str = new String(buf,0,numRead);
	      in.close();
	      return str;
	    }
	    catch (Exception e) {
	      throw new SecurityException("Could not decrypt file: " + filename + "\nError: " + e.getMessage());
	    }
	   }else{
		   	try {
		      BufferedReader in = new BufferedReader(
		                          new FileReader( filename ) );
		      String s = null;
		      StringBuffer str = new StringBuffer();
		      while( null != (s = in.readLine()) ) {
		        str.append(s);
		        str.append(System.getProperty("line.separator"));
		      }
		      in.close();
		      return str.toString();
		    }
		    catch (Exception e) {
		      throw new SecurityException("Could not decrypt file: " + filename + "\nError: " + e.getMessage());
		    }
	  }
  }
  
}