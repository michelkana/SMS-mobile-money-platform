// SendMessage.java - Sample application.
//
// This application shows you the basic procedure needed for sending
// an WAP PUSH SI SMS message from your GSM modem.
//

package examples;

import org.smslib.*;
import java.net.*;

class SendWapSIMessage
{
	public static void main(String[] args)
	{
		// Define the CService object. The parameters show the Comm Port used,
		// the Baudrate, the Manufacturer and Model strings. Manufacturer and
		// Model strings define which of the available AT Handlers will be used.
		CService srv = new CService("COM1", 57600, "Nokia", "");

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

			// Lets create a Wap SI message for dispatch.
			// Recipient's number should always be defined in international format.
			CWapSIMessage msg = new CWapSIMessage("+306948494037", new URL("https://mail.google.com/"), "Visit GMail now!");

			// Do we require a Delivery Status Report?
			msg.setStatusReport(false);

			// We can also define the validity period.
			// Validity period is always defined in hours.
			// The following statement sets the validity period to 8 hours.
			msg.setValidityPeriod(8);

			// Ok, finished with the message parameters, now send it!
			// If we have many messages to send, we could also construct a
			// LinkedList with many COutgoingMessage objects and pass it to
			// srv.sendMessage().
			srv.sendMessage(msg);

			// Disconnect - Don't forget to disconnect!
			srv.disconnect();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		System.exit(0);
	}
}
