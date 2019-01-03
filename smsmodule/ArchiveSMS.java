
package smsmodule;

import org.smslib.*;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class ArchiveSMS
{

	
	public static void main(String[] args)
	{
		LinkedList msgList = new LinkedList();
 		String filename = "sms.txt";
 		BufferedWriter smsout;
 		SimpleDateFormat df ;
	
		// Define the CService object. The parameters show the Comm Port used,
		// the Baudrate, the Manufacturer and Model strings. Manufacturer and
		// Model strings define which of the available AT Handlers will be used.
		CService srv = new CService("COM3", 230400, "Samsung", "");

		System.out.println();
		System.out.println("ReadMessages: Synchronous Reading.");
		System.out.println("  Using " + CService._name + " " + CService._version);
		System.out.println();
		

		
		try
		{
			
			smsout = new BufferedWriter(
	                           new FileWriter( filename ) );
			df = new SimpleDateFormat( "yyyy/MM/dd HH:mm:ss" );
			// If the GSM device is PIN protected, enter the PIN here.
			// PIN information will be used only when the GSM device reports
			// that it needs
			// a PIN in order to continue.
			srv.setSimPin("0000");

			// Some modems may require a SIM PIN 2 to unlock their full functionality.
			// Like the Vodafone 3G/GPRS PCMCIA card.
			// If you have such a modem, you should also define the SIM PIN 2.
			srv.setSimPin2("0000");

			//	If you would like to change the protocol to TEXT, do it here!
			// srv.setProtocol(CService.Protocol.TEXT);

			// OK, let connect and see what happens... Exceptions may be thrown
			// here!
			System.out.println("here");
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
			System.out.println("	GPRS Status   : " + (srv.getDeviceInfo().getGprsStatus() ? "Enabled" : "Disabled"));
			System.out.println("");

			// Get the messages in a LinkedList.
			// The Class defines which messages should be read. Here we request
			// the readout of all messages, whether read or unread.
			srv.readMessages(msgList, CIncomingMessage.MessageClass.All);
			// Iterate and display.
			// The CMessage parent object has a toString() method which displays
			// all of its contents. Useful for debugging, but for a real world
			// application you should use the necessary getXXX methods.			
			String sms;
			for (int i = 0; i < msgList.size(); i++)
			{
				CIncomingMessage msg = (CIncomingMessage) msgList.get(i);
				sms = "From: " + msg.getOriginator() + "\n" + "Date: " + df.format(msg.getDate()) + "\n" + msg.getText() + "\n\n";
				System.out.println(sms);
				try{
					smsout.write(sms, 0, sms.length());
				}catch(Exception e){
					System.out.println("Could not archive sms\n");
				}
			}

			// Disconnect - Don't forget to disconnect!
			srv.disconnect();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		System.exit(0);
	}
	
}
