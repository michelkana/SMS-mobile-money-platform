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

//
// SMSServer for Java GUI Application.
// Please read _README.txt for further information.
//

package smsserver;

import java.io.*;
import java.util.*;
import javax.swing.*;

import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import org.smslib.*;

class CMainThread extends Thread
{
	protected SMSServer SmsServer;

	protected CSettings settings;

	protected CDatabase database;

	protected CService service;

	protected CMainWindow mainWindow;

	private boolean connectRequest;

	private boolean exitRequest;

	private boolean exitFinished;

	public CMainThread(SMSServer SmsServer, CMainWindow mainWindow, CSettings settings)
	{
		this.SmsServer = SmsServer;
		this.mainWindow = mainWindow;
		this.settings = settings;
	}

	public void initialize()
	{
		database = new CDatabase(settings, this);
		if (mainWindow != null) mainWindow.setConnected(false);
		exitRequest = false;
		exitFinished = false;
		connectRequest = false;
		start();
	}

	public boolean connect(boolean calledFromMenu)
	{
		try
		{
			if (mainWindow == null) System.out.println(CConstants.TEXT_CONNECTING);
			service = new CService(settings.getSerialDriverSettings().getPort(), settings.getSerialDriverSettings().getBaud(), settings.getPhoneSettings().getManufacturer(), settings
			        .getPhoneSettings().getModel());
			service.setSimPin(settings.getPhoneSettings().getSimPin());
			service.setProtocol(settings.getPhoneSettings().getProtocol());
			service.connect();
			service.setSmscNumber(settings.getPhoneSettings().getSmscNumber());
			if (mainWindow != null)
			{
				mainWindow.setManufText(service.getDeviceInfo().getManufacturer());
				mainWindow.setModelText(service.getDeviceInfo().getModel());
				mainWindow.setSerialNoText(service.getDeviceInfo().getSerialNo());
				mainWindow.setIMSIText(service.getDeviceInfo().getImsi());
				mainWindow.setSwVersionText(service.getDeviceInfo().getSwVersion());
				mainWindow.setBatteryIndicator(service.getDeviceInfo().getBatteryLevel());
				mainWindow.setSignalIndicator(service.getDeviceInfo().getSignalLevel());
				mainWindow.setStatusText(CConstants.STATUS_CONNECTED);
			}
			else
			{
				System.out.println(CConstants.TEXT_INFO);
				System.out.println("\t" + SmsServer.stripHtml(CConstants.LABEL_MANUFACTURER) + " " + service.getDeviceInfo().getManufacturer());
				System.out.println("\t" + SmsServer.stripHtml(CConstants.LABEL_MODEL) + " " + service.getDeviceInfo().getModel());
				System.out.println("\t" + SmsServer.stripHtml(CConstants.LABEL_SERIALNO) + " " + service.getDeviceInfo().getSerialNo());
				System.out.println("\t" + SmsServer.stripHtml(CConstants.LABEL_IMSI) + " " + service.getDeviceInfo().getImsi());
				System.out.println("\t" + SmsServer.stripHtml(CConstants.LABEL_SWVERSION) + " " + service.getDeviceInfo().getSwVersion());
				System.out.println("\t" + SmsServer.stripHtml(CConstants.LABEL_BATTERY) + " " + service.getDeviceInfo().getBatteryLevel() + "%");
				System.out.println("\t" + SmsServer.stripHtml(CConstants.LABEL_SIGNAL) + " " + service.getDeviceInfo().getSignalLevel() + "%");
			}
			if (settings.getDatabaseSettings().getEnabled()) try
			{
				database.open();
			}
			catch (Exception e)
			{
				if (mainWindow != null) JOptionPane.showMessageDialog(mainWindow, CConstants.ERROR_CANNOT_OPEN_DATABASE, CConstants.ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
				else System.out.println(CConstants.ERROR_TITLE + " " + CConstants.ERROR_CANNOT_OPEN_DATABASE);
				database.close();
			}
			if (mainWindow != null) mainWindow.setConnected(true);
			else System.out.println(CConstants.LABEL_STATUS + CConstants.STATUS_CONNECTED);
			if (calledFromMenu) connectRequest = true;
			return true;
		}
		catch (InvalidPinException e)
		{
			if (!connectRequest)
			{
				if (mainWindow != null) JOptionPane.showMessageDialog(mainWindow, CConstants.ERROR_INVALID_PIN, CConstants.ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
				else System.out.println(CConstants.ERROR_TITLE + " " + CConstants.ERROR_INVALID_PIN);
			}
			disconnect(false);
			return false;
		}
		catch (NoPinException e)
		{
			if (!connectRequest)
			{
				if (mainWindow != null) JOptionPane.showMessageDialog(mainWindow, CConstants.ERROR_NO_PIN, CConstants.ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
				else System.out.println(CConstants.ERROR_TITLE + " " + CConstants.ERROR_NO_PIN);
			}
			disconnect(false);
			return false;
		}
		catch (NoPduSupportException e)
		{
			if (!connectRequest)
			{
				if (mainWindow != null) JOptionPane.showMessageDialog(mainWindow, CConstants.ERROR_NO_PDU_SUPPORT, CConstants.ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
				else System.out.println(CConstants.ERROR_TITLE + " " + CConstants.ERROR_NO_PDU_SUPPORT);
			}
			disconnect(false);
			return false;
		}
		catch (NoTextSupportException e)
		{
			if (!connectRequest)
			{
				if (mainWindow != null) JOptionPane.showMessageDialog(mainWindow, CConstants.ERROR_NO_TEXT_SUPPORT, CConstants.ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
				else System.out.println(CConstants.ERROR_TITLE + " " + CConstants.ERROR_NO_TEXT_SUPPORT);
			}
			disconnect(false);
			return false;
		}
		catch (NotConnectedException e)
		{
			if (!connectRequest)
			{
				if (mainWindow != null) JOptionPane.showMessageDialog(mainWindow, CConstants.ERROR_CANNOT_CONNECT + "\n" + e.getMessage(), CConstants.ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
				else System.out.println(CConstants.ERROR_TITLE + " " + CConstants.ERROR_CANNOT_CONNECT + e.getMessage());
			}
			disconnect(false);
			return false;
		}
		catch (Exception e)
		{
			if (!connectRequest)
			{
				if (mainWindow != null) JOptionPane.showMessageDialog(mainWindow, CConstants.ERROR_CANNOT_CONNECT + "\n" + e.getMessage(), CConstants.ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
				else
				{
					System.out.println(CConstants.ERROR_TITLE + " " + CConstants.ERROR_CANNOT_CONNECT + e.getMessage());
					e.printStackTrace();
				}
			}
			disconnect(false);
			return false;
		}
	}

	public void disconnect(boolean calledFromMenu)
	{
		try { service.disconnect(); } catch (Exception e) {}
		if (settings.getDatabaseSettings().getEnabled()) database.close();
		if (mainWindow != null) mainWindow.setConnected(false);
		else System.out.println(CConstants.LABEL_STATUS + CConstants.STATUS_DISCONNECTED);
		if (calledFromMenu) connectRequest = false;
	}

	public boolean processMessage(CIncomingMessage message) throws Exception
	{
		if (mainWindow != null)
		{
			mainWindow.setInFrom(message.getOriginator());
			mainWindow.setInDate(message.getDate() != null ? message.getDate().toString() : "* N/A *");
			mainWindow.setInText(message.getText());
		}
		else
		{
			System.out.println(CConstants.TEXT_INMSG);
			System.out.println("\t" + CConstants.LABEL_INCOMING_FROM + message.getOriginator());
			System.out.println("\t" + CConstants.LABEL_INCOMING_DATE + message.getDate());
			System.out.println("\t" + CConstants.LABEL_INCOMING_TEXT + message.getText());
		}
		settings.getGeneralSettings().rawInLog(message);
		if (settings.getPhoneSettings().getXmlInQueue() != null) try
		{
			saveToXmlInQueue(message);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		if (settings.getPhoneSettings().getForwardNumber() != null) try
		{
			saveToXmlOutQueue(message);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		if (database.isOpen()) try
		{
			database.saveMessage(message);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return true;
	}

	public void run()
	{
		boolean proceed;

		while (!exitRequest)
		{
			try
			{
				sleep(settings.getPhoneSettings().getPeriodInterval());
			}
			catch (Exception e)
			{
			}
			proceed = false;
			if (!exitRequest)
			{
				if (service != null)
				{
					if (service.getConnected()) proceed = true;
					else if (connectRequest)
					{
						disconnect(false);
						proceed = connect(false);
					}
					if (proceed)
					{
						try
						{
							if (mainWindow != null) mainWindow.setStatusText(CConstants.STATUS_REFRESHING);
							else System.out.println(CConstants.LABEL_STATUS + CConstants.STATUS_REFRESHING);
							service.refreshDeviceInfo();
							if (mainWindow != null) mainWindow.setStatusText(CConstants.STATUS_PROCESS_IN);
							else System.out.println(CConstants.LABEL_STATUS + CConstants.STATUS_PROCESS_IN);
							processStoredMessages();
							if (mainWindow != null) mainWindow.setStatusText(CConstants.STATUS_PROCESS_OUT);
							else System.out.println(CConstants.LABEL_STATUS + CConstants.STATUS_PROCESS_OUT);
							if (settings.getPhoneSettings().getXmlOutQueue() != null) try
							{
								checkXmlOutQueue();
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
							if (database.isOpen()) try
							{
								database.checkForOutgoingMessages();
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
							if (mainWindow != null)
							{
								mainWindow.setTrafficIn(service.getDeviceInfo().getStatistics().getTotalIn());
								mainWindow.setTrafficOut(service.getDeviceInfo().getStatistics().getTotalOut());
							}
							else
							{
								System.out.println(CConstants.LABEL_TRAFFIC + " " + CConstants.LABEL_TRAFFIC_IN + service.getDeviceInfo().getStatistics().getTotalIn() + "   "
								        + CConstants.LABEL_TRAFFIC_OUT + service.getDeviceInfo().getStatistics().getTotalOut());
							}
							if (mainWindow != null) mainWindow.setStatusText(CConstants.STATUS_IDLE);
							else System.out.println(CConstants.LABEL_STATUS + CConstants.STATUS_IDLE);
						}
						catch (Exception e)
						{
							disconnect(false);
						}
					}
				}
			}
		}
		disconnect(false);
		exitFinished = true;
	}

	public void exitRequest()
	{
		exitRequest = true;
		interrupt();
	}

	public boolean exitFinished()
	{
		return exitFinished;
	}

	protected void sendMessage(COutgoingMessage message) throws Exception
	{
		service.sendMessage(message);
		settings.getGeneralSettings().rawOutLog(message);
		database.saveSentMessage(message);
		if (mainWindow != null)
		{
			mainWindow.setOutTo(message.getRecipient());
			mainWindow.setOutDate(message.getDispatchDate().toString());
			mainWindow.setOutText(message.getText());
		}
		else
		{
			System.out.println(CConstants.TEXT_OUTMSG);
			System.out.println("\t" + CConstants.LABEL_OUTGOING_TO + message.getRecipient());
			System.out.println("\t" + CConstants.LABEL_OUTGOING_DATE + message.getDate());
			System.out.println("\t" + CConstants.LABEL_OUTGOING_TEXT + message.getText());
		}
	}

	private synchronized void processStoredMessages() throws Exception
	{
		LinkedList messageList;
		int batchLimit;

		batchLimit = settings.getPhoneSettings().getBatchIncoming();
		messageList = new LinkedList();
		service.readMessages(messageList, CIncomingMessage.MessageClass.All);
		for (int i = 0; i < messageList.size(); i++)
		{
			if ((i + 1) > batchLimit) break;
			CIncomingMessage message = (CIncomingMessage) messageList.get(i);
			if ((processMessage(message)) && (settings.getPhoneSettings().getDeleteAfterProcessing())) service.deleteMessage(message);
		}
	}

	private void saveToXmlInQueue(CIncomingMessage message) throws Exception
	{
		File xmlFile;
		PrintWriter out;

		xmlFile = File.createTempFile("Recv", ".xml", new File(settings.getPhoneSettings().getXmlInQueue()));
		out = new PrintWriter(new FileWriter(xmlFile.getCanonicalPath()));
		out.println("<?xml version='1.0' encoding='iso-8859-7'?>");
		out.println("<message>");
		out.println("	<originator>" + message.getOriginator() + "</originator>");
		out.println("	<date>" + textDate(message.getDate(), true) + "</date>");
		out.println("	<text> <![CDATA[" + message.getText() + "]]> </text>");
		out.println("</message>");
		out.close();
	}

	private void saveToXmlOutQueue(CIncomingMessage message) throws Exception
	{
		File xmlFile;
		PrintWriter out;

		xmlFile = File.createTempFile("Fwd", ".xml", new File(settings.getPhoneSettings().getXmlOutQueue()));
		out = new PrintWriter(new FileWriter(xmlFile.getCanonicalPath()));
		out.println("<?xml version='1.0' encoding='iso-8859-7'?>");
		out.println("<message>");
		out.println("	<recipient>" + settings.getPhoneSettings().getForwardNumber() + "</recipient>");
		out.println("	<text> <![CDATA[FW:" + message.getOriginator() + ": " + message.getText() + "]]> </text>");
		out.println("</message>");
		out.close();
	}

	private String textDate(java.util.Date date, boolean includeTime)
	{
		String dateStr = "";
		Calendar calendar = Calendar.getInstance();
		String day, month, year, hour, min, sec;

		if (date == null) return "* N/A *";
		calendar.setTime(date);
		day = "" + calendar.get(Calendar.DAY_OF_MONTH);
		if (day.length() != 2) day = "0" + day;
		month = "" + (calendar.get(Calendar.MONTH) + 1);
		if (month.length() != 2) month = "0" + month;
		year = "" + calendar.get(Calendar.YEAR);
		hour = "" + calendar.get(Calendar.HOUR_OF_DAY);
		if (hour.length() != 2) hour = "0" + hour;
		min = "" + calendar.get(Calendar.MINUTE);
		if (min.length() != 2) min = "0" + min;
		sec = "" + calendar.get(Calendar.SECOND);
		if (sec.length() != 2) sec = "0" + sec;
		dateStr = year + "/" + month + "/" + day;
		if (includeTime) dateStr += " " + hour + ":" + min + ":" + sec;
		return dateStr;
	}

	//@SuppressWarnings("unchecked")
	private void checkXmlOutQueue() throws Exception
	{
		COutgoingMessage message;
		LinkedList messageList;
		File outDir;
		File[] files;
		int batchLimit;

		batchLimit = settings.getPhoneSettings().getBatchOutgoing();
		outDir = new File(settings.getPhoneSettings().getXmlOutQueue());
		files = outDir.listFiles(new CXmlInFilter());
		if (files.length > 0)
		{
			messageList = new LinkedList();
			for (int i = 0; i < files.length; i++)
			{
				if ((i + 1) > batchLimit) break;
				message = new COutgoingMessage();
				try
				{
					parseXmlMessageFile(message, files[i].toString());
				}
				catch (Exception e)
				{
					e.printStackTrace();
					message = null;
				}
				if (message != null)
				{
					message.setId(files[i].toString());
					if (settings.getPhoneSettings().getMessageEncoding().equalsIgnoreCase("7bit")) message.setMessageEncoding(CMessage.MessageEncoding.Enc7Bit);
					else if (settings.getPhoneSettings().getMessageEncoding().equalsIgnoreCase("8bit")) message.setMessageEncoding(CMessage.MessageEncoding.Enc8Bit);
					else if (settings.getPhoneSettings().getMessageEncoding().equalsIgnoreCase("unicode")) message.setMessageEncoding(CMessage.MessageEncoding.EncUcs2);
					else message.setMessageEncoding(CMessage.MessageEncoding.Enc7Bit);
					messageList.add(message);
				}
			}
			service.sendMessage(messageList);
			for (int i = 0; i < messageList.size(); i++)
			{
				message = (COutgoingMessage) messageList.get(i);
				if (message.getDispatchDate() != null)
				{
					settings.getGeneralSettings().rawOutLog(message);
					if (mainWindow != null)
					{
						mainWindow.setOutTo(message.getRecipient());
						mainWindow.setOutDate(message.getDispatchDate().toString());
						mainWindow.setOutText(message.getText());
					}
					else
					{
						System.out.println(CConstants.TEXT_OUTMSG);
						System.out.println("\t" + CConstants.LABEL_OUTGOING_TO + message.getRecipient());
						System.out.println("\t" + CConstants.LABEL_OUTGOING_DATE + message.getDate());
						System.out.println("\t" + CConstants.LABEL_OUTGOING_TEXT + message.getText());
					}
					File file = new File(message.getId());
					file.delete();
				}
			}
		}
	}

	private void parseXmlMessageFile(COutgoingMessage message, String file) throws Exception
	{
		SAXParserFactory factory;
		SAXParser parser;
		CXMLMessageParser myParser;

		factory = SAXParserFactory.newInstance();
		parser = factory.newSAXParser();
		myParser = new CXMLMessageParser();
		myParser.setMessage(message);
		parser.parse(new File(file), myParser);
	}

	static class CXmlInFilter implements FilenameFilter
	{
		public boolean accept(File dir, String name)
		{
			return name.endsWith(".xml");
		}
	}

	static class CXMLMessageParser extends DefaultHandler
	{
		String level = "";

		String info = "";

		COutgoingMessage message = null;

		public void setMessage(COutgoingMessage message)
		{
			this.message = message;
		}

		public void startElement(String uri, String lName, String qName, Attributes attrs) throws SAXException
		{
			level = level + "/" + qName;
			info = "";
		}

		public void endElement(String uri, String lName, String qName) throws SAXException
		{
			if (level.equalsIgnoreCase("/message/recipient")) message.setRecipient(info.trim());
			if (level.equalsIgnoreCase("/message/text")) message.setText(info.trim());
			if (level.equalsIgnoreCase("/message/validity")) message.setValidityPeriod(Integer.parseInt(info.trim()));
			if (level.equalsIgnoreCase("/message/source_port")) message.setSourcePort(Integer.parseInt(info.trim()));
			if (level.equalsIgnoreCase("/message/destination_port")) message.setDestinationPort(Integer.parseInt(info.trim()));
			if (level.equalsIgnoreCase("/message/flash_sms")) message.setFlashSms(true);
			level = level.substring(0, level.lastIndexOf("/"));
		}

		public void characters(char buf[], int offset, int len) throws SAXException
		{
			String token;

			token = new String(buf, offset, len).trim();
			if (token.length() == 0) return;
			info = info + token;
		}
	}
}
