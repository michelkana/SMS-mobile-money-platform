// SMSLib for Java
// An open-source API Library for sending and receiving SMS via a GSM modem.
// Copyright (C) 2002-2007, Thanasis Delenikas, Athens/GREECE
// Web Site: http://www.smslib.org
//
// SMSLib is distributed under the LGPL license.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA

package org.smslib;

import javax.comm.*;
// import gnu.io.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class CSerialDriver implements SerialPortEventListener
{
	private static final int DELAY = 500;

	private static final int DELAY_AFTER_WRITE = 100;

	private static final int RECV_TIMEOUT = 30 * 1000;

	private static final int BUFFER_SIZE = 16384;

	private String port;

	private int baud;

	private CommPortIdentifier portId;

	private SerialPort serialPort;

	private InputStream inStream;

	private OutputStream outStream;

	private CNewMsgMonitor newMsgMonitor;

	private volatile boolean stopFlag;

	private Logger log;

	private CService srv;

	public CSerialDriver(String port, int baud, CService srv)
	{
		this.port = port;
		this.baud = baud;
		this.srv = srv;
		this.log = srv.getLogger();
		newMsgMonitor = null;
		stopFlag = false;

		inStream = null;
		outStream = null;
		serialPort = null;
	}

	public void setPort(String port)
	{
		this.port = port;
	}

	public String getPort()
	{
		return port;
	}

	public int getBaud()
	{
		return baud;
	}

	public void setNewMsgMonitor(CNewMsgMonitor monitor)
	{
		this.newMsgMonitor = monitor;
	}

	public void killMe()
	{
		stopFlag = true;
	}

	public void open() throws Exception
	{
		if (log != null) log.info("Connecting to serial port: " + port + " @ " + baud);

		portId = CommPortIdentifier.getPortIdentifier(getPort());
		serialPort = (SerialPort) portId.open("SMSLib", 1971);
		inStream = serialPort.getInputStream();
		outStream = serialPort.getOutputStream();
		serialPort.notifyOnDataAvailable(true);
		serialPort.notifyOnOutputEmpty(true);
		serialPort.notifyOnBreakInterrupt(true);
		serialPort.notifyOnFramingError(true);
		serialPort.notifyOnOverrunError(true);
		serialPort.notifyOnParityError(true);
		serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN);
		serialPort.addEventListener(this);
		serialPort.setSerialPortParams(getBaud(), SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		serialPort.setInputBufferSize(BUFFER_SIZE);
		serialPort.setOutputBufferSize(BUFFER_SIZE);
		serialPort.enableReceiveTimeout(RECV_TIMEOUT);
	}

	public void close()
	{
		if (log != null) log.info("Disconnecting from serial port: " + port);
		try
		{
			serialPort.close();
		}
		catch (Exception e)
		{
		}
	}

	public void serialEvent(SerialPortEvent event)
	{
		switch (event.getEventType())
		{
			case SerialPortEvent.BI:
				break;
			case SerialPortEvent.OE:
				if (log != null) log.error("COMM-ERROR: Overrun Error!");
				break;
			case SerialPortEvent.FE:
				if (log != null) log.error("COMM-ERROR: Framing Error!");
				break;
			case SerialPortEvent.PE:
				if (log != null) log.error("COMM-ERROR: Parity Error!");
				break;
			case SerialPortEvent.CD:
				break;
			case SerialPortEvent.CTS:
				break;
			case SerialPortEvent.DSR:
				break;
			case SerialPortEvent.RI:
				break;
			case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
				break;
			case SerialPortEvent.DATA_AVAILABLE:
				if (newMsgMonitor != null) newMsgMonitor.raise(CNewMsgMonitor.DATA);
				break;
		}
	}

	public void clearBufferCheckCMTI() throws Exception
	{
		StringBuffer buffer = new StringBuffer(BUFFER_SIZE);

		if (log != null) log.debug("SerialDriver(): clearBufferCheckCMTI() called");
		try
		{
			while (dataAvailable())
			{
				int c = inStream.read();
				if (c == -1) break;
				buffer.append((char) c);
			}
			if (log != null) log.debug("ME(CL): " + formatLog(buffer));
			if (newMsgMonitor != null && newMsgMonitor.getState() != CNewMsgMonitor.CMTI)
			{
				final String txt = buffer.toString();
				newMsgMonitor.raise((txt.indexOf("+CMTI:") >= 0 || txt.indexOf("+CDSI:") >= 0) ? CNewMsgMonitor.CMTI : CNewMsgMonitor.IDLE);
			}
		}
		catch (Exception e)
		{
		}
	}

	public void emptyBuffer() throws Exception
	{
		if (log != null) log.debug("SerialDriver(): emptyBuffer() called");
		Thread.sleep(DELAY);
		while (dataAvailable())
			inStream.read();
	}

	public void clearBuffer() throws Exception
	{
		Thread.sleep(DELAY);
		clearBufferCheckCMTI();
	}

	public void send(String s) throws Exception
	{
		if (log != null) log.debug("TE: " + formatLog(new StringBuffer(s)));
		for (int i = 0; i < s.length(); i++)
		{
			outStream.write((byte) s.charAt(i));
		}
		outStream.flush();
		Thread.sleep(DELAY_AFTER_WRITE);
	}

	public void send(char c) throws Exception
	{
		outStream.write((byte) c);
		outStream.flush();
		Thread.sleep(DELAY_AFTER_WRITE);
	}

	public void send(byte c) throws Exception
	{
		outStream.write(c);
		outStream.flush();
		Thread.sleep(DELAY_AFTER_WRITE);
	}

	public void skipBytes(int numOfBytes) throws Exception
	{
		int count, c;

		count = 0;
		while (count < numOfBytes)
		{
			c = inStream.read();
			if (c != -1) count++;
		}
	}

	public boolean dataAvailable() throws Exception
	{
		return (!stopFlag && inStream.available() > 0 ? true : false);
	}

	public String getResponse() throws Exception
	{
		final int RETRIES = 3;
		StringBuffer buffer;
		String response;
		int c, retry;

		retry = 0;
		buffer = new StringBuffer(BUFFER_SIZE);

		while (retry < RETRIES)
		{
			try
			{
				while (true)
				{
					while (true)
					{
						if (stopFlag) return "+ERROR:\r\n";
						c = inStream.read();
						if (c == -1)
						{
							buffer.delete(0, buffer.length());
							break;
						}
						buffer.append((char) c);
						if ((c == 0x0a) || (c == 0x0d)) break;
					}

					response = buffer.toString();

					if (response.matches("\\s*[\\p{ASCII}]*\\s+OK\\s")) break;
					// if (response.matches("\\s*[\\p{ASCII}]*\\s+READY\\s+OK\\s")) break;
					if (response.matches("\\s*[\\p{ASCII}]*\\s+READY\\s+")) break;
					if (response.matches("\\s*[\\p{ASCII}]*\\s+ERROR\\s")) break;
					if (response.matches("\\s*[\\p{ASCII}]*\\s+ERROR: \\d+\\s")) break;
					if (response.matches("\\s*[\\p{ASCII}]*\\s+SIM PIN\\s")) break;
					if (response.matches("\\s*[+]((CMTI)|(CDSI))[:][^\r\n]*[\r\n]"))
					{
						if (log != null) log.debug("ME: " + formatLog(buffer));
						buffer.delete(0, buffer.length());
						if (newMsgMonitor != null) newMsgMonitor.raise(CNewMsgMonitor.CMTI);
						continue;
					}
				}
				retry = RETRIES;
			}
			catch (Exception e)
			{
				if (retry < RETRIES)
				{
					Thread.sleep(DELAY);
					retry++;
				}
				else throw e;
			}
		}
		if (log != null) log.debug("ME: " + formatLog(buffer));
		clearBufferCheckCMTI();

		if (buffer.indexOf("RING") > 0)
		{
			if (srv.getConnected())
			{
				Pattern p = Pattern.compile("\\+?\\d+");
				Matcher m = p.matcher(buffer.toString());
				m.find();
				String phone = buffer.toString().substring(m.start(), m.end());

				if (srv.getCallHandler() != null) srv.getCallHandler().received(srv, new CIncomingCall(phone, new java.util.Date()));

				response = buffer.toString();
				response = response.replaceAll("\\s*RING\\s+[\\p{ASCII}]CLIP[[\\p{Alnum}][\\p{Punct}] ]+\\s\\s", "");
				return response;
			}
			else return buffer.toString();
		}
		else return buffer.toString();
	}

	private String formatLog(StringBuffer s)
	{
		StringBuffer response = new StringBuffer();
		int i;

		for (i = 0; i < s.length(); i++)
		{
			switch (s.charAt(i))
			{
				case 13:
					response.append("(cr)");
					break;
				case 10:
					response.append("(lf)");
					break;
				case 9:
					response.append("(tab)");
					break;
				default:
					response.append("(" + (int) s.charAt(i) + ")");
					break;
			}
		}
		response.append("  Text:[");
		for (i = 0; i < s.length(); i++)
		{
			switch (s.charAt(i))
			{
				case 13:
					response.append("(cr)");
					break;
				case 10:
					response.append("(lf)");
					break;
				case 9:
					response.append("(tab)");
					break;
				default:
					response.append(s.charAt(i));
					break;
			}
		}
		response.append("]");
		return response.toString();
	}
}
