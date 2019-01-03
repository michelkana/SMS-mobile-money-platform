// SendMessage.java - Sample application.
//
// This application shows you the basic procedure needed for sending
// an SMS message from your GSM modem.
//

package smsmodule;

import org.smslib.*;

public class SendMessage extends Thread
{
	public static final int PUT = 0;
	public static final int GET = 1;
	private String outPoolDirectory;
 	private	String smsFileextension;
 	private	String TXCOM;
 	private	int    TXBAUD;
 	private	String TXPHONE;  
 	private boolean crypto;
	
	public SendMessage(String TxCom, int TxBaud, String TxPhone, String outPool, String smsFileext, boolean encrypt){
		TXCOM = TxCom;
		TXBAUD = TxBaud;
		TXPHONE = TxPhone;
		outPoolDirectory = outPool;
		smsFileextension = smsFileext;
		crypto = encrypt;
	}
	
	public void run()
	{
		// Define the CService object. The parameters show the Comm Port used,
		// the Baudrate, the Manufacturer and Model strings. Manufacturer and
		// Model strings define which of the available AT Handlers will be used.
		CService srv = new CService(TXCOM, TXBAUD, TXPHONE, "");

		System.out.println();
		System.out.println("SendMessage(): Send a message.");
		System.out.println("  Using " + CService._name + " " + CService._version);
		System.out.println();

		try
		{
			// If the GSM device is PIN protected, enter the PIN here.
			// PIN information will be used only when the GSM device reports
			// that it needs a PIN in order to continue.
			srv.setSimPin("0000");

			// Some modems may require a SIM PIN 2 to unlock their full functionality.
			// Like the Vodafone 3G/GPRS PCMCIA card.
			// If you have such a modem, you should also define the SIM PIN 2.
			srv.setSimPin2("0000");

			// Normally, you would want to set the SMSC number to blank. GSM
			// devices get the SMSC number information from their SIM card.
			srv.setSmscNumber("+491710760000");

			//	If you would like to change the protocol to TEXT, do it here!
			// srv.setProtocol(CService.Protocol.TEXT);

			// OK, let connect and see what happens... Exceptions may be thrown
			// here!
			srv.connect();

			// Lets get info about the GSM device...
			System.out.println("Mobile Device Information: ");
			System.out.println("	Manufacturer  : " + srv.getDeviceInfo().getManufacturer());
			System.out.println("	Model         : " + srv.getDeviceInfo().getModel());
			System.out.println("	Serial No     : " + srv.getDeviceInfo().getSerialNo());
			System.out.println("	IMSI          : " + srv.getDeviceInfo().getImsi());
			System.out.println("	S/W Version   : " + srv.getDeviceInfo().getSwVersion());
			System.out.println("	Battery Level : " + srv.getDeviceInfo().getBatteryLevel() + "%");
			System.out.println("	Signal Level  : " + srv.getDeviceInfo().getSignalLevel() + "%");
			
			MessagePool messagePool = new MessagePool(outPoolDirectory, smsFileextension, crypto);

			while (true){				
				MessageFile msgFile = messagePool.accessOutgoingPool(GET, null);			    
				if ( msgFile != null ){				
					COutgoingMessage msg = new COutgoingMessage();							
					msg.fromString(msgFile.getFileContent());
					msg.setMessageEncoding(CMessage.MessageEncoding.Enc7Bit);		
					try{
						srv.sendMessage(msg);			
						msgFile.setFileContent("Successfully Sent\n" + msgFile.getFileContent());
						msgFile.setFilename(msgFile.getFilename().concat(".txOK"));		
						msgFile = messagePool.accessOutgoingPool(PUT, msgFile);
					}
					catch (Exception e)
					{
						msgFile.setFilename(msgFile.getFilename().concat(".txErr"));
						msgFile.setFileContent("Error occured while sending \n" + e.getMessage() + "\n" + msgFile.getFileContent());
						msgFile = messagePool.accessOutgoingPool(PUT, msgFile);
					}			
				}			
				Thread.sleep(1000);
			}
			// Disconnect - Don't forget to disconnect!
			//srv.disconnect();
		}
		catch (Exception e)
		{
			System.out.println("Mobile connection problems! Please fix it! No SMS can't be sent to usres ...");
			e.printStackTrace();
		}
		//System.exit(0);
	}
}
