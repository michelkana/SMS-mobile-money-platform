
package smsmodule;

import org.smslib.*;
import java.io.*;
import java.text.SimpleDateFormat;

// This is the new proposed method of using the ASYNC service.
public class ReadMessage extends Thread
{
	public static final int PUT = 0;
	public static final int GET = 1;
	private static String inPoolDirectory;
 	private	static String smsFileextension;
 	private	static String RXCOM;
 	private	static int    RXBAUD;
 	private	static String RXPHONE;
	
	CService srv;
	
	private static MessagePool messagePool;

	// This is the incoming call callback class.
	// The "received" method of this class is called by SMSLib API for each
	// message received.
	private static class CCallListener implements ICallListener
	{
		public void received(CService service, CIncomingCall call)
		{
			System.out.println();
			System.out.println("<< INCOMING CALL >>");
			System.out.println(" From: " + call.getPhoneNumber() + " @ " + call.getTimeOfCall());
			System.out.println("<< INCOMING CALL >>");
			System.out.println();
		}
	}

	// This is the message callback class.
	// The "received" method of this class is called by SMSLib API for each
	// message received.
	private static class CMessageListener implements ISmsMessageListener
	{
		public boolean received(CService service, CIncomingMessage message)
		{
			// Display the message received...
			//System.out.println("*** Msg: " + message.getText());

			// Put the message in the pool ...
			
			SimpleDateFormat df = new SimpleDateFormat( "yyyyMMdd_HHmmssSSS" );
			String smsFilename = df.format(message.getDate()) + "." + smsFileextension;
			
			MessageFile msgFile = new MessageFile(smsFilename, message.toString());
			msgFile = messagePool.accessIncomingPool(PUT, msgFile);
	
			// Return false to leave the message in memory - otherwise return
			// true to delete it.
			return true;
		}
	}

	public ReadMessage(String RxCom, int RxBaud, String RxPhone, String inPool, String smsFileext, boolean crypto){
		RXCOM = RxCom;
		RXBAUD = RxBaud;
		RXPHONE = RxPhone;
		inPoolDirectory = inPool;
		smsFileextension = smsFileext;
		messagePool = new MessagePool(inPoolDirectory, smsFileextension, crypto);
	}

	public void run()
	{
		// Define the CService object. The parameters show the Comm Port used,
		// the Baudrate, the Manufacturer and Model strings. Manufacturer and
		// Model strings define which of the available AT Handlers will be used.
		srv = new CService(RXCOM, RXBAUD, RXPHONE, "");

		// This is the listener callback class. This class will be called for
		// each message received.
		CMessageListener smsMessageListener = new CMessageListener();

		// This is the incoming call callback class. This class will be called
		// when an incoming call is detected.
		CCallListener callListener = new CCallListener();

		System.out.println();
		System.out.println("ReadMessagesAsyncPoll: Asynchronous Reading.");
		System.out.println("  Using " + CService._name + " " + CService._version);
		System.out.println();
		try
		{
			// If the GSM device is PIN protected, enter the PIN here.
			// PIN information will be used only when the GSM device reports
			// that it needs a PIN in order to continue.
			srv.setSimPin("0000");

			// Normally, you would want to set the SMSC number to blank. GSM
			// devices get the SMSC number information from their SIM card.
			srv.setSmscNumber("");

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

			// Set the call callback class.
			srv.setCallHandler(callListener);

			// Set the message callback class.
			srv.setMessageHandler(smsMessageListener);

			// Set the polling interval in seconds.
			srv.setAsyncPollInterval(10);

			// Set the class of the messages to be read.
			srv.setAsyncRecvClass(CIncomingMessage.MessageClass.All);

			// Switch to asynchronous POLL mode.
			//srv.setReceiveMode(CService.ReceiveMode.AsyncPoll);
			// Or do you want to switch to CNMI mode???
			 srv.setReceiveMode(CService.ReceiveMode.AsyncCnmi);

			// Go to sleep - simulate the asynchronous concept...
			while (true){
				System.out.println();
				System.out.println("I will wait for a period of 1 day for incoming messages...");
				try
				{
					Thread.sleep(86400000);
				}
				catch (Exception e)
				{
				}
				System.out.println("Timeout period expired, exiting... Please restart Handy Bank System");
			}
			// Disconnect - Don't forget to disconnect!
			//srv.disconnect();
		}
		catch (Exception e)
		{
			System.out.println("Mobile connection problems ! Please fix it! No SMS can't be received from usres ...");
			e.printStackTrace();
		}
	}

}
