// SendMessage.java - Sample application.
//
// This application shows you the basic procedure needed for sending
// an SMS message from your GSM modem.
//

package examples;

import org.smslib.*;

class SendMessage
{
	public static void main(String[] args)
	{
		// Define the CService object. The parameters show the Comm Port used,
		// the Baudrate, the Manufacturer and Model strings. Manufacturer and
		// Model strings define which of the available AT Handlers will be used.
		CService srv = new CService("COM7", 57600, "Nokia", "");

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
			srv.setSmscNumber("");

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

			// Lets create a message for dispatch.
			// A message needs the recipient's number and the text. Recipient's
			// number should always be defined in international format.
			COutgoingMessage msg = new COutgoingMessage("+4915157133325", "Message from SMSLib for Java.");

			// Set the message encoding.
			// We can use 7bit, 8bit and Unicode. 7bit should be enough for most
			// cases. Unicode is necessary for Far-East countries.
			msg.setMessageEncoding(CMessage.MessageEncoding.Enc7Bit);

			// Do we require a Delivery Status Report?
			msg.setStatusReport(true);

			// We can also define the validity period.
			// Validity period is always defined in hours.
			// The following statement sets the validity period to 8 hours.
			msg.setValidityPeriod(8);

			// Do we require a flash SMS? A flash SMS appears immediately on
			// recipient's phone.
			// Sometimes its called a forced SMS. Its kind of rude, so be
			// careful!
			// Keep in mind that flash messages are not supported by all
			// handsets.
			// msg.setFlashSms(true);

			// Some special applications are "listening" for messages on
			// specific ports.
			// The following statements set the Source and Destination port.
			// They should always be used in pairs!!!
			// Source and Destination ports are defined as 16bit ints in the
			// message header.
			// msg.setSourcePort(10000);
			// msg.setDestinationPort(11000);

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
