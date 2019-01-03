
//

package handybank;
import java.io.FileInputStream;
import java.util.Properties;
import smsmodule.*;

public class EBanking
{
	public static final int PUT = 0;
	public static final int GET = 1;
	
	public static void main(String[] args)
	{

		System.out.println();
		System.out.println("Handy Bank System");
		System.out.println();
		
		String logDirectory = "C:\\Documents and Settings\\Michel\\My Documents\\Vision\\Handy Bank\\Code\\SMSLib-Java-v2.1.4\\dist\\classes\\handybank\\log\\";
  	String inPoolDirectory = "C:\\Documents and Settings\\Michel\\My Documents\\Vision\\Handy Bank\\Code\\SMSLib-Java-v2.1.4\\dist\\classes\\handybank\\pool\\incoming\\";
  	String outPoolDirectory = "C:\\Documents and Settings\\Michel\\My Documents\\Vision\\Handy Bank\\Code\\SMSLib-Java-v2.1.4\\dist\\classes\\handybank\\pool\\outgoing\\";
 		String smsFileextension = "sms";
 		String RXCOM = "COM7";
 		int    RXBAUD = 57600;
 		String RXPHONE = "Nokia"; 
  	String TXCOM = "COM5";
 		int    TXBAUD = 57600;
 		String TXPHONE = "Samsung";  	
  	MessageFile msgFile;
  	boolean crypto = false;
  	String dbDriver = "com.mysql.jdbc.Driver";
  	String dbURL = "jdbc:mysql://localhost:3306/handybank";
		String dbUser = "root";
		String dbPwd = ""; 
  	
  	if ( args.length > 0 ){
  		try{
	  		Properties properties = new Properties();
				properties.load(new FileInputStream(args[0]));
				logDirectory = properties.getProperty("logDirectory");
				inPoolDirectory = properties.getProperty("inPoolDirectory");
				outPoolDirectory = properties.getProperty("outPoolDirectory");
				smsFileextension = properties.getProperty("smsFileextension");     
				RXCOM = properties.getProperty("RXCOM");               
				RXBAUD = Integer.valueOf(properties.getProperty("RXBAUD")).intValue();              
				RXPHONE = properties.getProperty("RXPHONE");            
				TXCOM = properties.getProperty("TXCOM");               
				TXBAUD = Integer.valueOf(properties.getProperty("TXBAUD")).intValue();              
				TXPHONE = properties.getProperty("TXPHONE");  	       
				crypto = Boolean.valueOf(properties.getProperty("crypto")).booleanValue();              
				dbDriver = properties.getProperty("dbDriver");
				dbURL = properties.getProperty("dbURL");
				dbUser = properties.getProperty("dbUser");              
				dbPwd = properties.getProperty("dbPwd");  
			}catch (Exception e)
			{
				e.printStackTrace();
			}            
  	} 
  	  
  	ReadMessage messageReader = new ReadMessage(RXCOM, RXBAUD, RXPHONE, inPoolDirectory, smsFileextension, crypto);
  	SendMessage messageSender = new SendMessage(TXCOM, TXBAUD, TXPHONE, outPoolDirectory, smsFileextension, crypto);
  	  
  	messageReader.start();
  	messageSender.start();
  	  
  	MessagePool inMessagePool = new MessagePool(inPoolDirectory, smsFileextension, crypto);
	    
		try
		{ 
      
			while (true){				
				if ( ( msgFile = inMessagePool.accessIncomingPool(GET, null) )!= null ){
					//System.out.println("EBanking: found message: " + msgFile.getFilename() + " " + msgFile.getFileContent());
					Request newReq = new Request(msgFile, logDirectory, outPoolDirectory, smsFileextension, crypto, dbDriver, dbURL, dbUser, dbPwd);
					newReq.start();						
				}
				Thread.sleep(1000);
			}
			// Disconnect - Don't forget to disconnect!
			//srv.disconnect();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		System.exit(0);
	}
}
