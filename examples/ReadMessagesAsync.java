// ReadMessagesAsync.java - Sample application.
//
// This application shows you the basic procedure needed for reading
// SMS messages from your GSM modem.
// This example is about synchronous reading, utilizing a virtual message
// handler. Asynchronous reading is performed with two methods: Polling and
// CNMI.
//
// The Polling method uses a user-specified time interval, and reads messages
// every such interval. The message class defines which class of messages
// would be read.
//
// The CMNI method is the same as the polling method but now, the polling
// interval specifies the maximum wait time for performing a forced read. During
// this interval, SMSLib will intercept new message notifications and will
// process these messages immediately, no matter how long the polling
// interval is.
//
// Asynchronous reading is based on the phones ability to enable & disable new
// messages' indications. If this functionality is not supported by your phone,
// then the CNMI method will work exactly like the Polling method.

package examples;

import org.smslib.*;

// This is the new proposed method of using the ASYNC service.
class ReadMessagesAsync
{
	CService srv;

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
			System.out.println("*** Msg: " + message.getText());

			// and send a "thank you!" reply!
			try
			{
				// service.sendMessage(new
				// COutgoingMessage(message.getOriginator(), "Thank you!"));
			}
			catch (Exception e)
			{
				System.out.println("Could not send reply message!");
				e.printStackTrace();
			}

			// Return false to leave the message in memory - otherwise return
			// true to delete it.
			return true;
		}
	}

	public ReadMessagesAsync()
	{
	}

	public void doIt()
	{
		// Define the CService object. The parameters show the Comm Port used,
		// the Baudrate, the Manufacturer and Model strings. Manufacturer and
		// Model strings define which of the available AT Handlers will be used.
		srv = new CService("COM7", 57600, "Nokia", "");

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
			srv.setReceiveMode(CService.ReceiveMode.AsyncPoll);
			// Or do you want to switch to CNMI mode???
			// srv.setReceiveMode(CService.ReceiveMode.AsyncCnmi);

			// Go to sleep - simulate the asynchronous concept...
			System.out.println();
			System.out.println("I will wait for a period of 60 secs for incoming messages...");
			try
			{
				Thread.sleep(60000);
			}
			catch (Exception e)
			{
			}
			System.out.println("Timeout period expired, exiting...");

			// Disconnect - Don't forget to disconnect!
			srv.disconnect();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void main(String[] args)
	{
		ReadMessagesAsync example = new ReadMessagesAsync();
		example.doIt();
	}
}
