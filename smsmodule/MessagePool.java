//


package smsmodule;

import java.io.*;

// This is the new proposed method of using the ASYNC service.
public class MessagePool
{

	private String directoryPath;
	private String fileFilter;
	private boolean encrypt;
	
	public static final int PUT = 0;
	public static final int GET = 1;
	
	public MessagePool(String path, String fFilter, boolean crypto){
		directoryPath = path;
		fileFilter = fFilter;
		encrypt = crypto;
		File dir = new File(directoryPath);
		if ( ! dir.exists() ){
			try{
				dir.mkdirs(); 
			}catch (Exception e)
			{
				System.out.println("Could not create pool directory " + directoryPath);
				e.printStackTrace();
			}			
		}
	}
	
	public synchronized MessageFile accessIncomingPool(int opType, MessageFile msgFile){
		MessageFile res = null;
		if ( opType == PUT ){ // put operation
			try{
				//System.out.println("[MessagePool] PUT " + msgFile.getFileContent() + " TO " + directoryPath + msgFile.getFilename());
				/*File smsFile = new File(directoryPath + msgFile.getFilename());
				BufferedWriter out = new BufferedWriter(
	                           new FileWriter( smsFile ) );
	      String fileContent = msgFile.getFileContent();*/
	      Cryptograph cryptograph = new Cryptograph(encrypt);
    		/*fileContent = cryptograph.encrypt(fileContent);
				out.write(fileContent, 0, fileContent.length());
				out.close();*/
				cryptograph.encryptFile(directoryPath + msgFile.getFilename(), msgFile.getFileContent());
			}
			catch (Exception e)
			{
				System.out.println("Could not write SMS to incoming pool!");
				e.printStackTrace();
			}
		}
		if ( opType == GET ){ // read operation
			File dir = new File(directoryPath);
			FilenameFilter filter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.endsWith(fileFilter);
        }
	    };
	    String[] smsList = dir.list(filter);
      if ( smsList.length > 0 )
				try {
					String smsFilename = smsList[0];		
					File smsFile = new File(directoryPath,smsFilename);			
		      /*BufferedReader in = new BufferedReader(
		                          new FileReader( smsFile ) );
		      String s = null;
		      StringBuffer smsStr = new StringBuffer();
		      while( null != (s = in.readLine()) ) {
		        smsStr.append(s);
		        smsStr.append(System.getProperty("line.separator"));
		      }
		      in.close();*/		      
	      	Cryptograph cryptograph = new Cryptograph(encrypt); 
		      //res = new MessageFile(smsFilename, cryptograph.decrypt(smsStr.toString()));  
		      res = new MessageFile(smsFilename, cryptograph.decryptFile(smsFile.getPath()));
					// Remove sms file from incoming pool
					
					if ( ! smsFile.delete() ){
						System.out.println("could not delete " + smsFile.getPath());
						System.exit(0);
					}
				}
				catch (Exception e)
				{
					System.out.println("Could not read SMS from incoming pool!");
					e.printStackTrace();
				}
		}
		return res;
	}
	
	public synchronized MessageFile accessOutgoingPool(int opType, MessageFile msgFile){
		MessageFile res = null;
		if ( opType == PUT ){ // put operation
			try{
				File smsFile = new File(directoryPath + msgFile.getFilename());
				BufferedWriter out = new BufferedWriter(
	                           new FileWriter( smsFile ) );
				String fileContent = msgFile.getFileContent();
	      Cryptograph cryptograph = new Cryptograph(encrypt);
    		fileContent = cryptograph.encrypt(fileContent);
				out.write(fileContent, 0, fileContent.length());
				out.close();
			}
			catch (Exception e)
			{
				System.out.println("Could not write SMS to outgoing pool!");
				e.printStackTrace();
			}
		}
		if ( opType == GET ){ // read operation
			File dir = new File(directoryPath);
			FilenameFilter filter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.endsWith(fileFilter);
        }
	    };
	    String[] smsList = dir.list(filter);
	    
      if ( smsList.length > 0 ){      	
				try {
					
					String smsFilename = smsList[0];		
					File smsFile = new File(directoryPath + smsFilename);			
		      /*BufferedReader in = new BufferedReader(
		                          new FileReader( smsFile ) );
		      String s = null;
		      StringBuffer smsStr = new StringBuffer();
		      while( null != (s = in.readLine()) ) {
		        smsStr.append(s);
		        smsStr.append(System.getProperty("line.separator"));
		      }
		      in.close();*/
		      Cryptograph cryptograph = new Cryptograph(encrypt); 
		      //res = new MessageFile(smsFilename, cryptograph.decrypt(smsStr.toString())); 
					res = new MessageFile(smsFilename, cryptograph.decryptFile(smsFile.getPath()));
					// Remove sms file from outgoing pool
					
					if ( ! smsFile.delete() ){
						System.out.println("could not delete " + smsFile.getPath());
						System.exit(0);
					}
				}
				catch (Exception e)
				{
					System.out.println("Could not read SMS from outgoing pool!");
					e.printStackTrace();
				}
			}
		}
		return res;
	}
	
	public boolean exists(String filename){
		try{
				File f = new File(directoryPath+filename);
				return f.exists();
			}
			catch (Exception e)
			{
				System.out.println("Could not check the existence of " + directoryPath+filename);
				e.printStackTrace();
			}
		return false;
	}

}
