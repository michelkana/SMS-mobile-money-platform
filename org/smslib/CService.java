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

import java.io.*;
import java.util.*;
import org.apache.log4j.*;
import org.apache.log4j.xml.*;

/**
 * This is the main SMSLib service class.
 */
public class CService
{
	/**
	 * Dummy synchronization object.
	 */
	private Object _SYNC_ = null;

	/**
	 * Product name.
	 */
	public static final String _name = "SMSLib for Java";

	/**
	 * Product version.
	 */
	public static final String _version = "v2.1.4";

	/**
	 * Holds values representing the modem protocol used.
	 */
	public static class Protocol
	{
		/**
		 * PDU protocol.
		 */
		public static final int PDU = 0;

		/**
		 * TEXT protocol.
		 */
		public static final int TEXT = 1;
	}

	/**
	 * Holds values representing receive mode.
	 * 
	 * @see CService#setReceiveMode(int)
	 * @see CService#getReceiveMode()
	 */
	public static class ReceiveMode
	{
		/**
		 * Synchronous reading.
		 */
		public static final int Sync = 0;

		/**
		 * Asynchronous reading - CMTI indications.
		 */
		public static final int AsyncCnmi = 1;

		/**
		 * Asynchronous reading - polling.
		 */
		public static final int AsyncPoll = 2;
	}

	private static final String LOG_CONF = "smslib-log.conf";

	private static final String LOG_CONF_XML = LOG_CONF + ".xml";

	private static final int DISCONNECT_TIMEOUT = 10 * 1000;

	private int keepAliveInterval = 30 * 1000;

	private int asyncPollInterval = 10 * 1000;

	private int asyncRecvClass = CIncomingMessage.MessageClass.All;

	private int retriesNoResponse = 5;

	private int delayNoResponse = 5000;

	private int retriesCmsErrors = 5;

	private int delayCmsErrors = 5000;

	private Logger log;

	private static final String VALUE_NOT_REPORTED = "* N/A *";

	private String smscNumber;

	private String simPin, simPin2;

	private int receiveMode;

	private int protocol;

	private AbstractATHandler atHandler;

	private CNewMsgMonitor newMsgMonitor;

	private CSerialDriver serialDriver;

	private volatile boolean connected;

	private CDeviceInfo deviceInfo;

	private CKeepAliveThread keepAliveThread;

	private CReceiveThread receiveThread;

	private ISmsMessageListener messageHandler;

	private ICallListener callHandler;

	private int outMpRefNo;

	private LinkedList mpMsgList;

	/**
	 * CService constructor.
	 * 
	 * @param port
	 *            The comm port to use (i.e. COM1, /dev/ttyS1 etc).
	 * @param baud
	 *            The baud rate. 57600 is a good number to start with.
	 * @param gsmDeviceManufacturer
	 *            The manufacturer of the modem (i.e. Wavecom, Nokia, Siemens, etc).
	 * @param gsmDeviceModel
	 *            The model (i.e. M1306B, 6310i, etc).
	 */
	public CService(String port, int baud, String gsmDeviceManufacturer, String gsmDeviceModel)
	{
		if (_SYNC_ == null) _SYNC_ = new Object();

		log = Logger.getLogger("org.smslib");
		if (new File(LOG_CONF).exists()) PropertyConfigurator.configure(LOG_CONF);
		else if (new File(LOG_CONF_XML).exists()) DOMConfigurator.configure(LOG_CONF_XML);
		else
		{
			BasicConfigurator.configure();
			log.setLevel(Level.WARN);
			if (System.getProperty("smslib.debug") != null) log.setLevel(Level.ALL);
		}

		smscNumber = "";
		simPin = null;
		simPin2 = null;

		connected = false;
		serialDriver = new CSerialDriver(port, baud, this);
		deviceInfo = new CDeviceInfo();
		newMsgMonitor = new CNewMsgMonitor();

		log.info(_name + " / " + _version);
		log.info("Using port: " + port + " @ " + baud + " baud.");
		log.info("JRE Version: " + System.getProperty("java.version"));
		log.info("JRE Impl Version: " + System.getProperty("java.vm.version"));
		log.info("O/S: " + System.getProperty("os.name") + " / " + System.getProperty("os.arch") + " / " + System.getProperty("os.version"));

		try
		{
			atHandler = AbstractATHandler.load(serialDriver, log, this, gsmDeviceManufacturer, gsmDeviceModel);
			log.info("Using " + atHandler.getDescription() + " AT handler.");
		}
		catch (Exception ex)
		{
			log.fatal("CANNOT INITIALIZE HANDLER (" + ex.getMessage() + ")");
		}

		protocol = Protocol.PDU;

		messageHandler = null;
		callHandler = null;

		receiveMode = ReceiveMode.Sync;
		receiveThread = null;

		outMpRefNo = new Random().nextInt();
		if (outMpRefNo < 0) outMpRefNo *= -1;
		outMpRefNo %= 65536;

		mpMsgList = new LinkedList();
	}

	/**
	 * Return the status of the connection.
	 * <p>
	 * <strong>Warning</strong>: The method return the "theoretical" status of the connection, without testing the actual connection at the time of the call.
	 * 
	 * @return True if the GSM device is connected.
	 * @see CService#connect()
	 * @see CService#disconnect()
	 */
	public boolean getConnected()
	{
		return connected;
	}

	/**
	 * Returns the DeviceInfo class.
	 * 
	 * @see CDeviceInfo
	 */
	public CDeviceInfo getDeviceInfo()
	{
		return deviceInfo;
	}

	/**
	 * Sets the SMSC number. Needed in rare cases - normally, you should <strong>not</strong> set the SMSC number yourself and let the GSM device get it from its SIM.
	 * 
	 * @param smscNumber
	 *            The SMSC number (international format).
	 * @see CService#getSmscNumber()
	 */
	public void setSmscNumber(String smscNumber)
	{
		this.smscNumber = smscNumber;
	}

	/**
	 * Returns the SMSC number previously set with setSmscNumber() call.
	 * 
	 * @return The SMSC number.
	 * @see CService#setSmscNumber(String)
	 */
	public String getSmscNumber()
	{
		return smscNumber;
	}

	/**
	 * Sets the SIM PIN.
	 * 
	 * @param simPin
	 *            The SIM pin code.
	 * @see CService#getSimPin()
	 */
	public void setSimPin(String simPin)
	{
		this.simPin = simPin;
	}

	/**
	 * Sets the SIM Pin 2. Some GSM modems may require this to unlock full functionality.
	 * 
	 * @param simPin
	 *            The SIM PIN #2 code.
	 * @see CService#getSimPin2()
	 */
	public void setSimPin2(String simPin)
	{
		this.simPin2 = simPin;
	}

	/**
	 * Returns the SIM PIN previously set with setSimPin().
	 * 
	 * @return The SIM PIN code.
	 * @see CService#setSimPin(String)
	 */
	public String getSimPin()
	{
		return simPin;
	}

	/**
	 * Returns the SIM PIN #2 previously set with setSimPin2().
	 * 
	 * @return The SIM PIN #2 code.
	 * @see CService#setSimPin2(String)
	 */
	public String getSimPin2()
	{
		return simPin2;
	}

	/**
	 * Sets the message handler for ASYNC mode. The handler is called automatically from SMSLib when a message is received. This handler is valid only for Asynchronous operation mode - in other modes, it is not used.
	 * 
	 * @param messageHandler
	 *            The message handler routine - must comply with ISmsMessageListener interface.
	 * @see CService#getMessageHandler()
	 */
	public void setMessageHandler(ISmsMessageListener messageHandler)
	{
		this.messageHandler = messageHandler;
	}

	/**
	 * Returns the message handler (if any).
	 * 
	 * @return The message handler.
	 * @see CService#setMessageHandler(ISmsMessageListener)
	 */
	public ISmsMessageListener getMessageHandler()
	{
		return messageHandler;
	}

	/**
	 * Sets the call handler. Works in ALL modes. The handler is called automatically once SMSLib receives an incoming call.
	 * 
	 * @param callHandler
	 *            The call handler routine - must comply with ICallListener interface.
	 * @see CService#getCallHandler()
	 */
	public void setCallHandler(ICallListener callHandler)
	{
		this.callHandler = callHandler;
	}

	/**
	 * Returns the call handler (if any).
	 * 
	 * @return The call handler.
	 * @see CService#setCallHandler(ICallListener)
	 */
	public ICallListener getCallHandler()
	{
		return callHandler;
	}

	/**
	 * Sets the Async Poll Interval (in seconds) - is every how many seconds will SMSLib poll the GSM modem for new messages.
	 * 
	 * @param secs
	 *            The interval in seconds.
	 * @see CService#getAsyncPollInterval()
	 * @see CService#setAsyncRecvClass(int)
	 * @see CService#getAsyncRecvClass()
	 */
	public void setAsyncPollInterval(int secs)
	{
		this.asyncPollInterval = secs * 1000;
	}

	/**
	 * Returns the Async Poll Interval, in seconds.
	 * 
	 * @return The Poll Interval in seconds.
	 * @see CService#setAsyncPollInterval(int)
	 * @see CService#setAsyncRecvClass(int)
	 * @see CService#getAsyncRecvClass()
	 */
	public int getAsyncPollInterval()
	{
		return (asyncPollInterval / 1000);
	}

	public void setAsyncRecvClass(int msgClass)
	{
		asyncRecvClass = msgClass;
	}

	public int getAsyncRecvClass()
	{
		return asyncRecvClass;
	}

	/**
	 * Sets the Keep-Alive Interval - every how many seconds the Keep-Alive thread will run and send a dummy OK command to the GSM modem. This is used to keep the serial port alive and prevent it from timing out. The interval is, by default, set to 30 seconds which should be enough for all modems.
	 * 
	 * @param secs
	 *            The Keep-Alive Interval in seconds.
	 * @see CService#getKeepAliveInterval()
	 */
	public void setKeepAliveInterval(int secs)
	{
		this.keepAliveInterval = secs * 1000;
	}

	/**
	 * Returns the Keep-Alive Interval, in seconds.
	 * 
	 * @return The Keep-Alive Interval in seconds.
	 * @see CService#setKeepAliveInterval(int)
	 */
	public int getKeepAliveInterval()
	{
		return keepAliveInterval / 1000;
	}

	/**
	 * Sets the number of retries that SMSLib performs during dispatch of a message, if it fails to get a response from the modem within the timeout period.
	 * <p>
	 * After the number of retries complete and the message is not sent, SMSLib treats it as undeliverable.
	 * <p>
	 * The default values should be ok in most cases.
	 * 
	 * @param retries
	 *            The number of retries.
	 * @see CService#getRetriesNoResponse()
	 * @see CService#setDelayNoResponse(int)
	 * @see CService#getDelayNoResponse()
	 */
	public void setRetriesNoResponse(int retries)
	{
		this.retriesNoResponse = retries;
	}

	/**
	 * Returns the current number of retries.
	 * 
	 * @return The number of retries.
	 * @see CService#setRetriesNoResponse(int)
	 */
	public int getRetriesNoResponse()
	{
		return retriesNoResponse;
	}

	/**
	 * Sets the delay between consecutive attemps for dispatching a message.
	 * 
	 * @param delay
	 *            The delay in millisecs.
	 * @see CService#getDelayNoResponse()
	 * @see CService#setRetriesNoResponse(int)
	 * @see CService#getRetriesNoResponse()
	 */
	public void setDelayNoResponse(int delay)
	{
		this.delayNoResponse = delay * 1000;
	}

	/**
	 * Gets the delay between consecutive attemps for dispatching a message.
	 * 
	 * @return delay The delay in millisecs.
	 * @see CService#getDelayNoResponse()
	 * @see CService#setRetriesNoResponse(int)
	 * @see CService#getRetriesNoResponse()
	 */
	public int getDelayNoResponse()
	{
		return delayNoResponse;
	}

	public void setRetriesCmsErrors(int retries)
	{
		this.retriesCmsErrors = retries;
	}

	public int getRetriesCmsErrors()
	{
		return retriesCmsErrors;
	}

	public void setDelayCmsErrors(int delay)
	{
		this.delayCmsErrors = delay * 1000;
	}

	public int getDelayCmsErrors()
	{
		return delayCmsErrors;
	}

	/**
	 * Returns the Log4J logger object used by SMSLib.
	 * 
	 * @return The Log4J logger object.
	 */
	public Logger getLogger()
	{
		return log;
	}

	/**
	 * Sets the logger to a custom Log4J Logger object. You can also use this call to completely disable logging, by passing a null object.
	 * 
	 * @param log
	 *            A Log4J logger object.
	 */
	public void setLogger(Logger log)
	{
		this.log = log;
	}

	/**
	 * Sets the receive mode.
	 * 
	 * @param receiveMode
	 *            The receive mode.
	 * @see CService.ReceiveMode
	 */
	public void setReceiveMode(int receiveMode) throws Exception
	{
		synchronized (_SYNC_)
		{
			this.receiveMode = receiveMode;
			if (connected)
			{
				if (receiveMode == ReceiveMode.AsyncCnmi)
				{
					if (!atHandler.enableIndications())
					{
						if (log != null) log.warn("Could not enable CMTI indications, continuing without them...");
					}
				}
				else
				{
					if (!atHandler.disableIndications())
					{
						if (log != null) log.warn("Could not disable CMTI indications, continuing without them...");
					}
				}
			}
		}
	}

	/**
	 * Returns the Receive Mode.
	 * 
	 * @return The Receive Mode.
	 * @see CService.ReceiveMode
	 */
	public int getReceiveMode()
	{
		return receiveMode;
	}

	/**
	 * Sets the protocol to be used.
	 * <p>
	 * The default protocol is PDU. If you want to change it, you must call this method after constructing the CService object and before connecting. Otherwise, you will get an exception.
	 * 
	 * @param protocol
	 *            The protocol to be used.
	 * @see CService#getProtocol()
	 * @see CService.Protocol
	 */
	public void setProtocol(int protocol) throws Exception
	{
		if (getConnected()) throw new OopsException("Cannot change protocol while connected!");
		else this.protocol = protocol;
	}

	/**
	 * Returns the protocol in use.
	 * 
	 * @return The protocol use.
	 * @see CService.Protocol
	 */
	public int getProtocol()
	{
		return protocol;
	}

	/**
	 * Sets the storage locations to be read by SMSLib.
	 * <p>
	 * Normally, SMSLib tries to read the available storage locations reported by the modem itself. Sometimes, the modem does not report all storage locations, so you can use this method to define yours, without messing with main SMSLib code.
	 * 
	 * @param loc
	 *            The storage locations (i.e. a string similar to "SMME", which is specific to each modem)
	 */
	public void setStorageLocations(String loc)
	{
		atHandler.setStorageLocations(loc);
	}

	/**
	 * Connects to the GSM modem.
	 * <p>
	 * The connect() function should be called before any operations. Its purpose is to open the serial link, check for modem existence, initialize modem, start background threads and prepare for subsequent operations.
	 * 
	 * @see #disconnect()
	 * @throws NotConnectedException
	 *             Nobody is answering.
	 * @throws AlreadyConnectedException
	 *             Already connected.
	 * @throws NoPinException
	 *             If PIN is requested from the modem but no PIN is defined.
	 * @throws InvalidPinException
	 *             If the defined PIN is not accepted by the modem.
	 * @throws NoPduSupportException
	 *             The modem does not support PDU mode - fatal error!
	 */
	public void connect() throws Exception
	{
		synchronized (_SYNC_)
		{
			if (getConnected()) throw new AlreadyConnectedException();
			else try
			{
				serialDriver.open();
				connected = true;
				atHandler.sync();
				serialDriver.emptyBuffer();
				atHandler.reset();
				serialDriver.setNewMsgMonitor(newMsgMonitor);
				if (atHandler.isAlive())
				{
					if (atHandler.waitingForPin())
					{
						if (getSimPin() == null) throw new NoPinException();
						else if (!atHandler.enterPin(getSimPin())) throw new InvalidPinException();
						if (atHandler.waitingForPin())
						{
							if (getSimPin2() == null) throw new NoPin2Exception();
							else if (!atHandler.enterPin(getSimPin2())) throw new InvalidPin2Exception();
						}
					}
					atHandler.init();
					atHandler.echoOff();
					waitForNetworkRegistration();
					atHandler.setVerboseErrors();
					if (atHandler.storageLocations.length() == 0) atHandler.getStorageLocations();
					if (log != null) log.info("MEM: Storage Locations Found: " + atHandler.storageLocations);
					switch (protocol)
					{
						case Protocol.PDU:
							if (log != null) log.info("PROT: Using PDU protocol.");
							if (!atHandler.setPduMode()) throw new NoPduSupportException();
							break;
						case Protocol.TEXT:
							if (log != null) log.info("PROT: Using TEXT protocol.");
							if (!atHandler.setTextMode()) throw new NoTextSupportException();
							break;
						default:
							throw new OopsException("Invalid protocol! Should be PDU or TEXT.");
					}
					setReceiveMode(receiveMode);
					refreshDeviceInfo();

					receiveThread = new CReceiveThread();
					receiveThread.start();
					keepAliveThread = new CKeepAliveThread();
					keepAliveThread.start();
				}
				else throw new NotConnectedException("GSM device is not responding.");
			}
			catch (Exception e)
			{
				try
				{
					disconnect();
				}
				catch (Exception e2)
				{
				}
				throw e;
			}
		}
	}

	/**
	 * Disconnects from the GSM modem.
	 * <p>
	 * This should be the last function called. Closes serial connection, shuts down background threads and performs clean-up.
	 * <p>
	 * <strong>Notes</strong>
	 * <ul>
	 * <li>Do not connect and disconnect continously - at least if you can avoid it. It takes time and resources. Connect once and stay connected.</li>
	 * </ul>
	 * 
	 * @see CService#connect()
	 */
	public void disconnect() throws Exception
	{
		if (getConnected())
		{
			assert (receiveThread != null);
			assert (keepAliveThread != null);

			final int wait = 100;
			int timeout = DISCONNECT_TIMEOUT;
			receiveThread.killMe();
			keepAliveThread.killMe();
			while (timeout > 0 && !receiveThread.killed() && !keepAliveThread.killed())
			{
				Thread.sleep(wait);
				timeout -= wait;
			}

			try
			{
				serialDriver.killMe();
				if (!receiveThread.killed())
				{
					receiveThread.interrupt();
					receiveThread.join();
				}

				if (!keepAliveThread.killed())
				{
					keepAliveThread.interrupt();
					keepAliveThread.join();
				}

			}
			finally
			{
				receiveThread = null;
				keepAliveThread = null;
				serialDriver.close();
				connected = false;
			}
		}
		else throw new NotConnectedException();
	}

	/**
	 * Reads all SMS messages from the GSM modem.
	 * 
	 * @param messageList
	 *            The list to be populated with messages.
	 * @param messageClass
	 *            The message class of the messages to read.
	 * @throws NotConnectedException
	 *             Either connect() is not called or modem has been disconnected.
	 * @see CService#readMessages(LinkedList, int, int)
	 * @see CIncomingMessage
	 * @see CIncomingMessage.MessageClass
	 * @see CService#sendMessage(COutgoingMessage)
	 */
	public void readMessages(LinkedList messageList, int messageClass) throws Exception
	{
		switch (protocol)
		{
			case Protocol.PDU:
				readMessages_PDU(messageList, messageClass, 0);
				break;
			case Protocol.TEXT:
				readMessages_TEXT(messageList, messageClass, 0);
		}
	}

	/**
	 * Reads up to a specific number of SMS messages from the GSM modem.
	 * 
	 * @param messageList
	 *            The list to be populated with messages.
	 * @param messageClass
	 *            The message class of the messages to read.
	 * @param limit
	 *            Read up to <limit> number of messages. If limit is set to 0, read all messages.
	 * @throws NotConnectedException
	 *             Either connect() is not called or modem has been disconnected.
	 * @see CService#readMessages(LinkedList, int)
	 * @see CIncomingMessage
	 * @see CIncomingMessage.MessageClass
	 * @see CService#sendMessage(COutgoingMessage)
	 */
	public void readMessages(LinkedList messageList, int messageClass, int limit) throws Exception
	{
		switch (protocol)
		{
			case Protocol.PDU:
				readMessages_PDU(messageList, messageClass, limit);
				break;
			case Protocol.TEXT:
				readMessages_TEXT(messageList, messageClass, limit);
				break;
		}
	}

	//@SuppressWarnings("unchecked")
	private void readMessages_PDU(LinkedList messageList, int messageClass, int limit) throws Exception
	{
		int i, j, memIndex;
		String response, line, pdu;
		BufferedReader reader;
		CIncomingMessage mpMsg;

		if (limit < 0) limit = 0;
		mpMsg = null;
		synchronized (_SYNC_)
		{
			if (getConnected())
			{
				atHandler.switchToCmdMode();
				for (int ml = 0; ml < (atHandler.storageLocations.length() / 2); ml++)
				{
					if (atHandler.setMemoryLocation(atHandler.storageLocations.substring((ml * 2), (ml * 2) + 2)))
					{
						response = atHandler.listMessages(messageClass);
						response = response.replaceAll("\\s+OK\\s+", "\nOK");
						reader = new BufferedReader(new StringReader(response));
						for (;;)
						{
							line = reader.readLine().trim();
							if (line == null) break;
							line = line.trim();
							if (line.length() > 0) break;
						}
						while (true)
						{
							if (line == null) break;
							line = line.trim();
							if (line.length() <= 0 || line.equalsIgnoreCase("OK")) break;
							i = line.indexOf(':');
							j = line.indexOf(',');
							memIndex = Integer.parseInt(line.substring(i + 1, j).trim());
							pdu = reader.readLine().trim();
							try
							{
								if (isIncomingMessage(pdu))
								{
									CIncomingMessage msg;

									msg = new CIncomingMessage(pdu, memIndex, atHandler.storageLocations.substring((ml * 2), (ml * 2) + 2));
									if (log != null) log.debug("IN-DTLS: MI:" + msg.getMemIndex() + " REF:" + msg.getMpRefNo() + " MAX:" + msg.getMpMaxNo() + " SEQ:" + msg.getMpSeqNo());
									if (msg.getMpRefNo() == 0)
									{
										if (mpMsg != null) mpMsg = null;
										messageList.add(msg);
										deviceInfo.getStatistics().incTotalIn();
									}
									else
									{
										int k, l;
										LinkedList tmpList;
										CIncomingMessage listMsg;
										boolean found, duplicate;

										found = false;
										for (k = 0; k < mpMsgList.size(); k++)
										{
											tmpList = (LinkedList) mpMsgList.get(k);
											listMsg = (CIncomingMessage) tmpList.get(0);
											if (listMsg.getMpRefNo() == msg.getMpRefNo())
											{
												duplicate = false;
												for (l = 0; l < tmpList.size(); l++)
												{
													listMsg = (CIncomingMessage) tmpList.get(l);
													if (listMsg.getMpSeqNo() == msg.getMpSeqNo())
													{
														duplicate = true;
														break;
													}
												}
												if (!duplicate) tmpList.add(msg);
												found = true;
												break;
											}
										}
										if (!found)
										{
											tmpList = new LinkedList();
											tmpList.add(msg);
											mpMsgList.add(tmpList);
										}
									}
								}
								else if (isStatusReportMessage(pdu))
								{
									messageList.add(new CStatusReportMessage(pdu, memIndex, atHandler.storageLocations.substring((ml * 2), (ml * 2) + 2)));
									deviceInfo.getStatistics().incTotalIn();
								}
							}
							catch (Exception e)
							{
								if (log != null)
								{
									log.error("*****");
									log.error("Unhandled SMS in inbox, skipping!");
									log.error("Err: " + e.getMessage());
									log.error("*****");
								}
							}
							line = reader.readLine().trim();
							while (line.length() == 0)
								line = reader.readLine().trim();
							if ((limit > 0) && (messageList.size() == limit)) break;
						}
						reader.close();
					}
				}
			}
			else throw new NotConnectedException();
		}
		checkMpMsgList(messageList);
	}

	//@SuppressWarnings("unchecked")
	private void readMessages_TEXT(LinkedList messageList, int messageClass, int limit) throws Exception
	{
		int i, j, memIndex;
		byte[] bytes;
		String response, line, msgText, originator, dateStr, refNo;
		BufferedReader reader;
		StringTokenizer tokens;
		CIncomingMessage msg;
		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();

		if (limit < 0) limit = 0;
		synchronized (_SYNC_)
		{
			if (getConnected())
			{
				atHandler.switchToCmdMode();
				for (int ml = 0; ml < (atHandler.storageLocations.length() / 2); ml++)
				{
					if (atHandler.setMemoryLocation(atHandler.storageLocations.substring((ml * 2), (ml * 2) + 2)))
					{
						response = atHandler.listMessages(messageClass);
						response = response.replaceAll("\\s+OK\\s+", "\nOK");
						reader = new BufferedReader(new StringReader(response));
						for (;;)
						{
							line = reader.readLine().trim();
							if (line == null) break;
							line = line.trim();
							if (line.length() > 0) break;
						}
						while (true)
						{
							if (line == null) break;
							line = line.trim();
							if (line.length() <= 0 || line.equalsIgnoreCase("OK")) break;
							i = line.indexOf(':');
							j = line.indexOf(',');
							memIndex = Integer.parseInt(line.substring(i + 1, j).trim());
							tokens = new StringTokenizer(line, ",");
							tokens.nextToken();
							tokens.nextToken();
							if (Character.isDigit(tokens.nextToken().trim().charAt(0)))
							{
								line = line.replaceAll(",,", ", ,");
								tokens = new StringTokenizer(line, ",");
								tokens.nextToken();
								tokens.nextToken();
								tokens.nextToken();
								refNo = tokens.nextToken();
								tokens.nextToken();
								dateStr = tokens.nextToken().replaceAll("\"", "");
								cal1.set(Calendar.YEAR, 2000 + Integer.parseInt(dateStr.substring(0, 2)));
								cal1.set(Calendar.MONTH, Integer.parseInt(dateStr.substring(3, 5)) - 1);
								cal1.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateStr.substring(6, 8)));
								dateStr = tokens.nextToken().replaceAll("\"", "");
								cal1.set(Calendar.HOUR_OF_DAY, Integer.parseInt(dateStr.substring(0, 2)));
								cal1.set(Calendar.MINUTE, Integer.parseInt(dateStr.substring(3, 5)));
								cal1.set(Calendar.SECOND, Integer.parseInt(dateStr.substring(6, 8)));
								dateStr = tokens.nextToken().replaceAll("\"", "");
								cal2.set(Calendar.YEAR, 2000 + Integer.parseInt(dateStr.substring(0, 2)));
								cal2.set(Calendar.MONTH, Integer.parseInt(dateStr.substring(3, 5)) - 1);
								cal2.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateStr.substring(6, 8)));
								dateStr = tokens.nextToken().replaceAll("\"", "");
								cal2.set(Calendar.HOUR_OF_DAY, Integer.parseInt(dateStr.substring(0, 2)));
								cal2.set(Calendar.MINUTE, Integer.parseInt(dateStr.substring(3, 5)));
								cal2.set(Calendar.SECOND, Integer.parseInt(dateStr.substring(6, 8)));

								msg = new CStatusReportMessage(Integer.parseInt(refNo), memIndex, atHandler.storageLocations.substring((ml * 2), (ml * 2) + 2), cal1.getTime(), cal2.getTime());
								if (log != null) log.debug("IN-DTLS: MI:" + msg.getMemIndex());
								messageList.add(msg);
								deviceInfo.getStatistics().incTotalIn();
							}
							else
							{
								line = line.replaceAll(",,", ", ,");
								tokens = new StringTokenizer(line, ",");
								tokens.nextToken();
								tokens.nextToken();
								originator = tokens.nextToken().replaceAll("\"", "");
								tokens.nextToken();
								dateStr = tokens.nextToken().replaceAll("\"", "");
								cal1.set(Calendar.YEAR, 2000 + Integer.parseInt(dateStr.substring(0, 2)));
								cal1.set(Calendar.MONTH, Integer.parseInt(dateStr.substring(3, 5)) - 1);
								cal1.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateStr.substring(6, 8)));
								dateStr = tokens.nextToken().replaceAll("\"", "");
								cal1.set(Calendar.HOUR_OF_DAY, Integer.parseInt(dateStr.substring(0, 2)));
								cal1.set(Calendar.MINUTE, Integer.parseInt(dateStr.substring(3, 5)));
								cal1.set(Calendar.SECOND, Integer.parseInt(dateStr.substring(6, 8)));
								msgText = reader.readLine().trim();
								bytes = new byte[msgText.length() / 2];
								j = 0;
								for (i = 0; i < msgText.length(); i += 2)
								{
									bytes[j] = Byte.parseByte(msgText.substring(i, i + 2), 16);
									j++;
								}
								msgText = CGSMAlphabet.bytesToString(bytes);
								msg = new CIncomingMessage(cal1.getTime(), originator, msgText, memIndex, atHandler.storageLocations.substring((ml * 2), (ml * 2) + 2));
								if (log != null) log.debug("IN-DTLS: MI:" + msg.getMemIndex());
								messageList.add(msg);
								deviceInfo.getStatistics().incTotalIn();
							}
							line = reader.readLine().trim();
							while (line.length() == 0)
								line = reader.readLine().trim();
						}
						reader.close();
					}
				}
			}
			else throw new NotConnectedException();
		}
	}

	//@SuppressWarnings("unchecked")
	private void checkMpMsgList(LinkedList messageList)
	{
		int k, l, m;
		LinkedList tmpList;
		CIncomingMessage listMsg, mpMsg;
		boolean found;

		mpMsg = null;
		if (log != null) log.debug("CheckMpMsgList(): MAINLIST: " + mpMsgList.size());
		for (k = 0; k < mpMsgList.size(); k++)
		{
			tmpList = (LinkedList) mpMsgList.get(k);
			if (log != null) log.debug("CheckMpMsgList(): SUBLIST[" + k + "]: " + tmpList.size());
			listMsg = (CIncomingMessage) tmpList.get(0);
			found = false;
			if (listMsg.getMpMaxNo() == tmpList.size())
			{
				found = true;
				for (l = 0; l < tmpList.size(); l++)
					for (m = 0; m < tmpList.size(); m++)
					{
						listMsg = (CIncomingMessage) tmpList.get(m);
						if (listMsg.getMpSeqNo() == (l + 1))
						{
							if (listMsg.getMpSeqNo() == 1)
							{
								mpMsg = listMsg;
								mpMsg.setMpMemIndex(mpMsg.getMemIndex());
							}
							else
							{
								if (mpMsg != null)
								{
									mpMsg.addText(listMsg.getText());
									mpMsg.setMpSeqNo(listMsg.getMpSeqNo());
									mpMsg.setMpMemIndex(listMsg.getMemIndex());
									if (listMsg.getMpSeqNo() == listMsg.getMpMaxNo())
									{
										mpMsg.setMemIndex(-1);
										messageList.add(mpMsg);
										mpMsg = null;
									}
								}
							}
							break;
						}
					}
				tmpList.clear();
				tmpList = null;
			}
			if (found)
			{
				mpMsgList.remove(k);
				k--;
			}
		}
	}

	/**
	 * Sends an SMS message from the GSM modem.
	 * <p>
	 * This method actually wraps the message in a list and calls #sendMessage(List) that does the job.
	 * 
	 * @param message
	 *            The message to be sent.
	 * @throws NotConnectedException
	 *             Either connect() is not called or modem has been disconnected.
	 * @see CService#sendMessage(LinkedList)
	 * @see CService#readMessages(LinkedList, int)
	 */
	//@SuppressWarnings("unchecked")
	public void sendMessage(COutgoingMessage message) throws Exception
	{
		LinkedList messageList;

		messageList = new LinkedList();
		messageList.add(message);
		sendMessage(messageList);
	}

	/**
	 * Sends a list of messages from the GSM modem.
	 * <p>
	 * The function iterates through the supplied list of COutgoingMessage objects and tries to send them.
	 * <p>
	 * Upon succesful sending, each COutgoingMessage object should have its RefNo and DispatchDate fields set to specific values. Upon failure, the RefNo will be set to 0 and DispatchDate set to null.
	 * 
	 * @param messageList
	 *            A list of COutgoingMessage objects presenting messages that will be sent out.
	 * @throws NotConnectedException
	 *             Either connect() is not called or modem has been disconnected.
	 * @see CService#sendMessage(COutgoingMessage)
	 * @see COutgoingMessage
	 * @see CWapSIMessage
	 * @see CService#readMessages(LinkedList, int)
	 */
	public void sendMessage(LinkedList messageList) throws Exception
	{
		switch (protocol)
		{
			case Protocol.PDU:
				sendMessage_PDU(messageList);
				break;
			case Protocol.TEXT:
				sendMessage_TEXT(messageList);
				break;
		}
	}

	private void sendMessage_PDU(LinkedList messageList) throws Exception
	{
		COutgoingMessage message;
		int i, j, refNo;
		String pdu;

		if (getConnected())
		{
			synchronized (_SYNC_)
			{
				atHandler.keepGsmLinkOpen();
			}
			for (i = 0; i < messageList.size(); i++)
			{
				message = (COutgoingMessage) messageList.get(i);
				if (!message.isBig())
				{
					pdu = message.getPDU(smscNumber, 0, 0);
					j = pdu.length();
					j /= 2;
					if (smscNumber == null) ; // Do nothing on purpose!
					else if (smscNumber.length() == 0) j--;
					else
					{
						int smscNumberLen = smscNumber.length();
						if (smscNumber.charAt(0) == '+') smscNumberLen--;
						if (smscNumberLen % 2 != 0) smscNumberLen++;
						int smscLen = (2 + smscNumberLen) / 2;
						j = j - smscLen - 1;
					}
					synchronized (_SYNC_)
					{
						refNo = atHandler.sendMessage(j, pdu, null, null);
					}
					if (refNo >= 0)
					{
						message.setRefNo(refNo);
						message.dispatchDate = new Date();
						deviceInfo.getStatistics().incTotalOut();
					}
					else if (refNo == -2)
					{
						disconnect();
						break;
					}
					else message.dispatchDate = null;
				}
				else
				{
					for (int partNo = 1; partNo <= message.getNoOfParts(); partNo++)
					{
						pdu = message.getPDU(smscNumber, outMpRefNo, partNo);
						j = pdu.length();
						j /= 2;
						if (smscNumber == null) ; // Do nothing on purpose!
						else if (smscNumber.length() == 0) j--;
						else
						{
							int smscNumberLen = smscNumber.length();
							if (smscNumber.charAt(0) == '+') smscNumberLen--;
							if (smscNumberLen % 2 != 0) smscNumberLen++;
							int smscLen = (2 + smscNumberLen) / 2;
							j = j - smscLen - 1;
						}
						synchronized (_SYNC_)
						{
							refNo = atHandler.sendMessage(j, pdu, null, null);
						}
						if (refNo >= 0)
						{
							message.setRefNo(refNo);
							message.dispatchDate = new Date();
							deviceInfo.getStatistics().incTotalOut();
						}
						else if (refNo == -2)
						{
							disconnect();
							break;
						}
						else message.dispatchDate = null;
					}
					outMpRefNo = (outMpRefNo + 1) % 65536;
				}
			}
		}
		else throw new NotConnectedException();
	}

	private void sendMessage_TEXT(LinkedList messageList) throws Exception
	{
		COutgoingMessage message;
		byte[] bytes;
		StringBuffer hexText = new StringBuffer();
		int i, j, n, refNo;

		if (getConnected())
		{
			for (i = 0; i < messageList.size(); i++)
			{
				hexText.delete(0, hexText.length());
				message = (COutgoingMessage) messageList.get(i);
				bytes = new byte[400];
				n = CGSMAlphabet.stringToBytes(message.getText(), bytes);
				for (j = 0; j < n; j++)
					hexText.append(Integer.toHexString(bytes[j]).toUpperCase());
				synchronized (_SYNC_)
				{
					refNo = atHandler.sendMessage(0, null, message.getRecipient(), hexText.toString());
				}
				if (refNo >= 0)
				{
					message.setRefNo(refNo);
					message.dispatchDate = new Date();
					deviceInfo.getStatistics().incTotalOut();
				}
				else message.dispatchDate = null;
			}
		}
		else throw new NotConnectedException();
	}

	protected void deleteMessage(int memIndex, String memLocation) throws Exception
	{
		synchronized (_SYNC_)
		{
			if (getConnected()) atHandler.deleteMessage(memIndex, memLocation);
			else throw new NotConnectedException();
		}
	}

	/**
	 * Deletes a message from the modem's memory.
	 * <p>
	 * <strong>Warning</strong>: Do not pass invalid CIncomingMessage objects to this call - You may corrupt your modem's storage!
	 * <p>
	 * Delete operations are irreversible.
	 * 
	 * @param message
	 *            The CIncomingMessage object previously read with readMessages() call.
	 * @throws NotConnectedException
	 *             Either connect() is not called or modem has been disconnected.
	 * @see CIncomingMessage
	 * @see CService#readMessages(LinkedList, int)
	 */
	public void deleteMessage(CIncomingMessage message) throws Exception
	{
		synchronized (_SYNC_)
		{
			if (message.getMemIndex() >= 0) deleteMessage(message.getMemIndex(), message.getMemLocation());
			else if ((message.getMemIndex() == -1) && (message.getMpMemIndex().length() != 0))
			{
				StringTokenizer tokens = new StringTokenizer(message.getMpMemIndex(), ",");
				while (tokens.hasMoreTokens())
					deleteMessage(Integer.parseInt(tokens.nextToken()), message.getMemLocation());
			}
		}
	}

	/**
	 * Deletes ALL messages of the specified message class.
	 * <p>
	 * Delete operations are irreversible.
	 * 
	 * @param messageClass
	 *            The message class.
	 * @throws NotConnectedException
	 *             Either connect() is not called or modem has been disconnected.
	 * @see CIncomingMessage.MessageClass
	 */
	public void deleteMessages(int messageClass) throws Exception
	{
		LinkedList msgList;

		synchronized (_SYNC_)
		{
			msgList = new LinkedList();
			readMessages(msgList, messageClass);
			for (int i = 0; i < msgList.size(); i++)
				deleteMessage((CIncomingMessage) msgList.get(i));
		}
	}

	/**
	 * Reads (or refreshes) all GSM modem information (like manufacturer, signal level, etc).
	 * <p>
	 * This method is called automatically upon connection. Should you require fresh info, you should call it yourself when you need it.
	 * 
	 * @throws NotConnectedException
	 *             Either connect() is not called or modem has been disconnected.
	 * @see CDeviceInfo
	 */
	public void refreshDeviceInfo() throws Exception
	{
		synchronized (_SYNC_)
		{
			if (getConnected())
			{
				if (deviceInfo.manufacturer.length() == 0) deviceInfo.manufacturer = getManufacturer();
				if (deviceInfo.model.length() == 0) deviceInfo.model = getModel();
				if (deviceInfo.serialNo.length() == 0) deviceInfo.serialNo = getSerialNo();
				if (deviceInfo.imsi.length() == 0) deviceInfo.imsi = getImsi();
				if (deviceInfo.swVersion.length() == 0) deviceInfo.swVersion = getSwVersion();
				deviceInfo.gprsStatus = getGprsStatus();
				deviceInfo.batteryLevel = getBatteryLevel();
				deviceInfo.signalLevel = getSignalLevel();
			}
			else throw new NotConnectedException();
		}
	}

	public void catchAsyncException(Exception e)
	{
		if (log != null) log.debug("Unexpected exception", e);
		else e.printStackTrace();
	}

	protected boolean isAlive()
	{
		boolean alive;

		if (!connected) alive = false;
		else try
		{
			alive = atHandler.isAlive();
		}
		catch (Exception e)
		{
			alive = false;
		}
		return alive;
	}

	private boolean waitForNetworkRegistration() throws Exception
	{
		StringTokenizer tokens;
		String response;
		int answer;

		while (true)
		{
			response = atHandler.getNetworkRegistration();
			if (response.indexOf("ERROR") > 0) return false;
			response = response.replaceAll("\\s+OK\\s+", "");
			response = response.replaceAll("\\s+", "");
			response = response.replaceAll("\\+CREG:", "");
			tokens = new StringTokenizer(response, ",");
			tokens.nextToken();
			try
			{
				answer = Integer.parseInt(tokens.nextToken());
			}
			catch (Exception e)
			{
				answer = -1;
			}
			switch (answer)
			{
				case 0:
					if (log != null) log.error("GSM: Auto-registration disabled!");
					throw new OopsException("GSM Network Auto-Registration disabled!");
				case 1:
					if (log != null) log.info("GSM: Registered to home network.");
					return true;
				case 2:
					if (log != null) log.warn("GSM: Not registered, searching for network...");
					break;
				case 3:
					if (log != null) log.error("GSM: Network registration denied!");
					throw new OopsException("GSM Network Registration denied!");
				case 4:
					if (log != null) log.error("GSM: Unknown registration error!");
					throw new OopsException("GSM Network Registration error!");
				case 5:
					if (log != null) log.info("GSM: Registered to foreign network (roaming).");
					return true;
				case -1:
					if (log != null) log.info("GSM: Invalid CREG response.");
					throw new OopsException("GSM: Invalid CREG response.");
			}
			Thread.sleep(1000);
		}
	}

	/**
	 * Send a custom AT command to the modem and returns its response.
	 * 
	 * @param cmd
	 *            The custom AT command
	 * @return The modem response
	 */
	public String sendCustomCmd(String cmd) throws Exception
	{
		synchronized (_SYNC_)
		{
			return atHandler.send(cmd);
		}
	}

	private String getManufacturer() throws Exception
	{
		String response;

		response = atHandler.getManufacturer();
		if (response.matches("\\s*[\\p{ASCII}]*\\s+ERROR(?:: \\d+)?\\s+")) return VALUE_NOT_REPORTED;
		response = response.replaceAll("\\s+OK\\s+", "");
		response = response.replaceAll("\\s+", "");
		return response;
	}

	private String getModel() throws Exception
	{
		String response;

		response = atHandler.getModel();
		if (response.matches("\\s*[\\p{ASCII}]*\\s+ERROR(?:: \\d+)?\\s+")) return VALUE_NOT_REPORTED;
		response = response.replaceAll("\\s+OK\\s+", "");
		response = response.replaceAll("\\s+", "");
		return response;
	}

	private String getSerialNo() throws Exception
	{
		String response;

		response = atHandler.getSerialNo();
		if (response.matches("\\s*[\\p{ASCII}]*\\s+ERROR(?:: \\d+)?\\s+")) return VALUE_NOT_REPORTED;
		response = response.replaceAll("\\s+OK\\s+", "");
		response = response.replaceAll("\\s+", "");
		return response;
	}

	private String getImsi() throws Exception
	{
		return "** MASKED **";
		// IMSI is masked on purpose.
		// Uncomment following code for IMSI to be reported.
		//
		// String response; response = atHandler.getImsi(); if
		// (response.matches("\\s*[\\p{ASCII}]*\\s+ERROR(?:: \\d+)?\\s+"))
		// return VALUE_NOT_REPORTED; response =
		// response.replaceAll("\\s+OK\\s+", ""); response =
		// response.replaceAll("\\s+", ""); return response;
		//
	}

	private String getSwVersion() throws Exception
	{
		String response;

		response = atHandler.getSwVersion();
		if (response.matches("\\s*[\\p{ASCII}]*\\s+ERROR(?:: \\d+)?\\s+")) return VALUE_NOT_REPORTED;
		response = response.replaceAll("\\s+OK\\s+", "");
		response = response.replaceAll("\\s+", "");
		return response;
	}

	private boolean getGprsStatus() throws Exception
	{
		return (atHandler.getGprsStatus().matches("\\s*[\\p{ASCII}]CGATT[\\p{ASCII}]*1\\s*OK\\s*"));
	}

	private int getBatteryLevel() throws Exception
	{
		String response;
		StringTokenizer tokens;

		response = atHandler.getBatteryLevel();
		if (response.matches("\\s*[\\p{ASCII}]*\\s+ERROR(?:: \\d+)?\\s+")) return 0;
		response = response.replaceAll("\\s+OK\\s+", "");
		response = response.replaceAll("\\s+", "");
		tokens = new StringTokenizer(response, ":,");
		tokens.nextToken();
		tokens.nextToken();
		return Integer.parseInt(tokens.nextToken());
	}

	private int getSignalLevel() throws Exception
	{
		String response;
		StringTokenizer tokens;

		response = atHandler.getSignalLevel();
		if (response.matches("\\s*[\\p{ASCII}]*\\s+ERROR(?:: \\d+)?\\s+")) return 0;
		response = response.replaceAll("\\s+OK\\s+", "");
		response = response.replaceAll("\\s+", "");
		tokens = new StringTokenizer(response, ":,");
		tokens.nextToken();
		return (Integer.parseInt(tokens.nextToken().trim()) * 100 / 31);
	}

	private boolean isIncomingMessage(String pdu)
	{
		int index, i;

		i = Integer.parseInt(pdu.substring(0, 2), 16);
		index = (i + 1) * 2;

		i = Integer.parseInt(pdu.substring(index, index + 2), 16);
		if ((i & 0x03) == 0) return true;
		else return false;
	}

	private boolean isStatusReportMessage(String pdu)
	{
		int index, i;

		i = Integer.parseInt(pdu.substring(0, 2), 16);
		index = (i + 1) * 2;

		i = Integer.parseInt(pdu.substring(index, index + 2), 16);
		if ((i & 0x02) == 2) return true;
		else return false;
	}

	public boolean received(CIncomingMessage message)
	{
		return false;
	}

	private class CKeepAliveThread extends Thread
	{
		private volatile boolean stopFlag;

		private volatile boolean stopped;

		public CKeepAliveThread()
		{
			stopFlag = false;
			stopped = false;
		}

		public void killMe()
		{
			stopFlag = true;
			synchronized (_SYNC_)
			{
				this.interrupt();
			}
		}

		public boolean killed()
		{
			return stopped;
		}

		public void run()
		{
			try
			{
				while (!stopFlag)
				{
					try
					{
						sleep(keepAliveInterval);
					}
					catch (Exception e)
					{
					}
					if (stopFlag) break;
					synchronized (_SYNC_)
					{
						if (getConnected()) try
						{
							if (log != null) log.info("** Keep-Live **");
							atHandler.isAlive();
						}
						catch (Exception e)
						{
							if (!stopFlag && log != null) log.debug("Unexpected exception", e);
						}
					}
				}

			}
			finally
			{
				if (log != null) log.debug("KeepAlive Thread is teminated!");
				stopped = true;
			}
		}
	}

	private class CReceiveThread extends Thread
	{
		private volatile boolean stopFlag;

		private volatile boolean stopped;

		public CReceiveThread()
		{
			stopFlag = false;
			stopped = false;
		}

		public void killMe()
		{
			stopFlag = true;
			synchronized (newMsgMonitor)
			{
				newMsgMonitor.notify();
			}
		}

		public boolean killed()
		{
			return stopped;
		}

		public void run()
		{
			LinkedList messageList = new LinkedList();
			try
			{
				while (!stopFlag)
				{
					int state = newMsgMonitor.waitEvent(asyncPollInterval);
					if (stopFlag) break;
					if (getConnected() && (receiveMode == ReceiveMode.AsyncCnmi || receiveMode == ReceiveMode.AsyncPoll))
					{
						try
						{
							if (state == CNewMsgMonitor.DATA && !atHandler.dataAvailable() && newMsgMonitor.getState() != CNewMsgMonitor.CMTI) continue;

							newMsgMonitor.reset();
							messageList.clear();
							readMessages(messageList, asyncRecvClass);
							for (int i = 0; i < messageList.size(); i++)
							{
								CIncomingMessage message = (CIncomingMessage) messageList.get(i);
								if (getMessageHandler() == null)
								{
									if (received(message)) deleteMessage(message);
								}
								else
								{
									if (getMessageHandler() != null && getMessageHandler().received(CService.this, message)) deleteMessage(message);
								}

							}
						}
						catch (Exception e)
						{
							catchAsyncException(e);
						}
					}
				}

			}
			finally
			{
				if (log != null) log.debug("Receive Thread is terminated!");
				stopped = true;
			}
		}
	}

	public static void main(String[] args)
	{
		System.out.println(_name + " " + _version);
		System.out.println("	Java API for sending / receiving SMS messages via GSM modem.");
		System.out.println("	This software is distributed under the LGPL license.");
		System.out.println("");
		System.out.println("Copyright (C) 2002-2007, Thanasis Delenikas, Athens / GREECE.");
		System.out.println("Visit http://smslib.org for latest information.\n");
	}
}
