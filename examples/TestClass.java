//
// TestClass.java - Generic test template class.
//

package examples;

import org.smslib.*;

class TestClass
{
	public static void main(String[] args)
	{
		CService srv = new CService("COM1", 57600, "Nokia", "");

		System.out.println();
		System.out.println("ReadMessages: Synchronous Reading.");
		System.out.println("  Using " + CService._name + " " + CService._version);
		System.out.println();

		try
		{
			srv.setSimPin("0000");

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
			System.out.println("	GPRS Status   : " + (srv.getDeviceInfo().getGprsStatus() ? "Enabled" : "Disabled"));
			System.out.println("");

			// Write your test calls here.
			// ...
			// ...


			srv.disconnect();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		System.exit(0);
	}
}
