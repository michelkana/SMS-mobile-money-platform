package misc;

//import gnu.io.*;
import javax.comm.*;
import java.util.*;
import java.io.*;
import org.smslib.*;

public class CommTest
{
	static CommPortIdentifier portId;

	static Enumeration portList;

	static int bauds[] = { 19200, 38400, 115200 };

	public static void main(String[] args)
	{
		portList = CommPortIdentifier.getPortIdentifiers();

		while (portList.hasMoreElements())
		{
			portId = (CommPortIdentifier) portList.nextElement();
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL)
			{
				System.out.println("Found port: " + portId.getName());
				for (int i = 0; i < bauds.length; i++)
				{
					System.out.print("	Trying at " + bauds[i] + "...");
					try
					{
						SerialPort serialPort;
						InputStream inStream;
						OutputStream outStream;
						int c;
						String response;

						serialPort = (SerialPort) portId.open("SMSLibCommTester", 1971);
						serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN);
						serialPort.setSerialPortParams(bauds[i], SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
						inStream = serialPort.getInputStream();
						outStream = serialPort.getOutputStream();
						serialPort.enableReceiveTimeout(2000);

						c = inStream.read();
						while (c != -1)
							c = inStream.read();
						outStream.write('A');
						outStream.write('T');
						outStream.write('\r');
						outStream.write('A');
						outStream.write('T');
						outStream.write('\r');
						outStream.write('A');
						outStream.write('T');
						outStream.write('\r');
						try { Thread.sleep(1000); } catch (Exception e) {}
						response = "";
						c = inStream.read();
						while (c != -1)
						{
							response += (char) c;
							c = inStream.read();
						}
						serialPort.close();
						if (response.indexOf("OK") >= 0)
						{
							System.out.print("  Getting Info...");
							CService srv = new CService(portId.getName(), bauds[i], "", "");
							try
							{
								srv.connect();
								System.out.println("  Found: " + srv.getDeviceInfo().getModel());
								srv.disconnect();
							}
							catch (Exception e)
							{
								System.out.println("  Nobody here!");
							}
							srv = null;
						}
						else System.out.println("  Nobody here!");
					}
					catch (Exception e)
					{
						System.out.println("  Nobody here!");
					}
				}
			}
		}
	}
}
