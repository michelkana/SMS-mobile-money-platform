package smsmodule;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class LogAnalyser {
  
  public static void main (String [] args) {
  	if ( args.length < 4 ){
  		System.out.println("Syntax: LogAnalyser passwordfile userpassword operationtype operationparameters\n");
  		System.exit(1);
  	}
  	
  	String pwdfile = args[0];
  	String pwd = args[1];  
  	String op = args[2];
  	String param = args[3];
  		  
  	Cryptograph cryptograph = new Cryptograph(true);
  	//cryptograph.encryptFile(pwdfile, "tfibtf10");
  
    if ( pwd.equalsIgnoreCase(cryptograph.decryptFile(pwdfile)) ){    
    	System.out.println ("Password Correct\n");
    	if ( op.equalsIgnoreCase("1") ){
    		System.out.println ("File decrypted\n");
    		System.out.println(cryptograph.decryptFile(param));
    	}
    	if ( op.equalsIgnoreCase("2") ){
    		System.out.println ("String encrypted\n");
    		System.out.println(cryptograph.encryptHEX(param));
    	}
    	if ( op.equalsIgnoreCase("3") ){
    		System.out.println ("String decrypted\n");
    		System.out.println(cryptograph.decryptHEX(param));
    	}
    }
    else{
      System.out.println ("Password incorrect\n");
     }
  }
  
}