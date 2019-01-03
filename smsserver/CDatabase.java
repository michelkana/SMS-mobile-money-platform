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

import java.util.*;
import java.sql.*;

import org.smslib.*;

class CDatabase
{
	private CSettings settings;

	private CMainThread mainThread;

	private Connection connection;

	public CDatabase(CSettings settings, CMainThread mainThread)
	{
		this.settings = settings;
		this.mainThread = mainThread;
		connection = null;
	}

	public Connection getConnection()
	{
		return connection;
	}

	public boolean isOpen() throws Exception
	{
		if (!settings.getDatabaseSettings().getEnabled()) return false;
		if ((connection == null) || connection.isClosed()) open();
		return (connection != null ? !connection.isClosed() : false);
	}

	public void open() throws Exception
	{
		Class.forName(settings.getDatabaseSettings().getDriver());
		connection = DriverManager.getConnection(settings.getDatabaseSettings().getUrl(), settings.getDatabaseSettings().getUsername(), settings.getDatabaseSettings().getPassword());
		connection.setAutoCommit(false);
	}

	public void close()
	{
		if (connection != null) try
		{
			connection.close();
		}
		catch (Exception e)
		{
		}
		connection = null;
	}

	public void saveMessage(CIncomingMessage message) throws Exception
	{
		Statement sqlCmd;

		sqlCmd = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		switch (message.getType())
		{
			case CIncomingMessage.MessageType.Incoming:
				sqlCmd.executeUpdate("insert into sms_in (type, originator, message_date, text) values ('I', '" + message.getOriginator() + "', " + escapeDate(message.getDate(), true) + ", '" + message.getText().replaceAll("'", "''") + "')");
				break;
			case CIncomingMessage.MessageType.StatusReport:
				CStatusReportMessage msg = (CStatusReportMessage) message;
				sqlCmd.executeUpdate("insert into sms_in (type, originator, message_date, text, ref_no, original_sent_date, date_received) values ('T', '" + msg.getOriginator() + "', " + escapeDate(new java.util.Date(), true) + ", '" + msg.getText().replaceAll("'", "''") + "', " + msg.getRefNo() + ", " + escapeDate(msg.getDateOriginal(), true) + ", " + escapeDate(msg.getDateReceived(), true) + ")");
				break;
		}
		connection.commit();
		sqlCmd.close();
	}

	public void saveSentMessage(COutgoingMessage message) throws Exception
	{
		Statement sqlCmd;

		if (connection != null)
		{
			sqlCmd = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			sqlCmd.executeUpdate("insert into sms_out (recipient, text, dispatch_date, flash_sms, status_report, src_port, dst_port, validity_period) values ('" + message.getRecipient() + "', '" + message.getText().replaceAll("'", "''") + "', " + escapeDate(message.getDate(), true) + ", " + (message.getFlashSms() ? 1 : 0) + ", " + (message.getStatusReport() ? 1 : 0) + ", " + message.getSourcePort() + ", " + message.getDestinationPort() + ", " + message.getValidityPeriod() + ")");
			connection.commit();
			sqlCmd.close();
		}
	}

	//@SuppressWarnings("unchecked")
	public void checkForOutgoingMessages() throws Exception
	{
		Statement sqlCmd1, sqlCmd2;
		ResultSet rs;
		LinkedList messageList = new LinkedList();
		COutgoingMessage message;
		int batchLimit;

		batchLimit = settings.getPhoneSettings().getBatchOutgoing();
		sqlCmd1 = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		rs = sqlCmd1.executeQuery("select count(*) as cnt from sms_out where dispatch_date is null");
		rs.next();
		if (rs.getInt("cnt") != 0)
		{
			rs.close();
			sqlCmd2 = connection.createStatement();
			rs = sqlCmd1.executeQuery("select * from sms_out where dispatch_date is null");
			while (rs.next())
			{
				if (messageList.size() > batchLimit) break;
				message = new COutgoingMessage(rs.getString("recipient").trim(), rs.getString("text").trim());
				message.setId("" + rs.getInt("id"));
				message.setFlashSms(rs.getInt("flash_sms") == 1);
				message.setStatusReport(rs.getInt("status_report") == 1);
				message.setSourcePort(rs.getInt("src_port"));
				message.setDestinationPort(rs.getInt("dst_port"));
				message.setValidityPeriod(rs.getInt("validity_period"));
				if (settings.getPhoneSettings().getMessageEncoding().equalsIgnoreCase("7bit")) message.setMessageEncoding(CMessage.MessageEncoding.Enc7Bit);
				else if (settings.getPhoneSettings().getMessageEncoding().equalsIgnoreCase("8bit")) message.setMessageEncoding(CMessage.MessageEncoding.Enc8Bit);
				else if (settings.getPhoneSettings().getMessageEncoding().equalsIgnoreCase("unicode")) message.setMessageEncoding(CMessage.MessageEncoding.EncUcs2);
				else message.setMessageEncoding(CMessage.MessageEncoding.Enc7Bit);
				messageList.add(message);
			}
			rs.close();
			mainThread.service.sendMessage(messageList);
			for (int i = 0; i < messageList.size(); i++)
			{
				message = (COutgoingMessage) messageList.get(i);
				if (message.getDispatchDate() != null)
				{
					settings.getGeneralSettings().rawOutLog(message);
					if (mainThread.mainWindow != null)
					{
						mainThread.mainWindow.setOutTo(message.getRecipient());
						mainThread.mainWindow.setOutDate(message.getDispatchDate().toString());
						mainThread.mainWindow.setOutText(message.getText());
					}
					else
					{
						System.out.println(CConstants.TEXT_OUTMSG);
						System.out.println("\t" + CConstants.LABEL_OUTGOING_TO + message.getRecipient());
						System.out.println("\t" + CConstants.LABEL_OUTGOING_DATE + message.getDate());
						System.out.println("\t" + CConstants.LABEL_OUTGOING_TEXT + message.getText());
					}
					sqlCmd2.executeUpdate("update sms_out set dispatch_date = " + escapeDate(message.getDispatchDate(), true) + " where id = " + message.getId());
				}
			}
			sqlCmd2.close();
			connection.commit();
		}
		else
		{
			rs.close();
			connection.rollback();
		}
		sqlCmd1.close();
	}

	protected String escapeDate(java.util.Date date, boolean includeTime)
	{
		String dateStr = "";
		Calendar calendar = Calendar.getInstance();

		calendar.setTime(date);
		switch (settings.getDatabaseSettings().getType())
		{
			case CSettings.CDatabaseSettings.DB_TYPE_SQL92:
				if (includeTime) dateStr = "{ts ?";
				else dateStr = "{d ?";
				dateStr += "" + calendar.get(Calendar.YEAR);
				dateStr += "-";
				dateStr += "" + (calendar.get(Calendar.MONTH) + 1);
				dateStr += "-";
				dateStr += "" + calendar.get(Calendar.DAY_OF_MONTH);
				if (includeTime)
				{
				}
				else dateStr += "?}";
				break;
			case CSettings.CDatabaseSettings.DB_TYPE_MSSQL:
				dateStr = "'";
				dateStr += calendar.get(Calendar.YEAR) + "-";
				dateStr += (calendar.get(Calendar.MONTH) + 1) + "-";
				dateStr += calendar.get(Calendar.DAY_OF_MONTH);
				if (includeTime)
				{
					dateStr += " ";
					dateStr += calendar.get(Calendar.HOUR_OF_DAY) + ":";
					dateStr += calendar.get(Calendar.MINUTE) + ":";
					dateStr += calendar.get(Calendar.SECOND);
					dateStr += "'";
				}
				else dateStr += "'";
				break;
			case CSettings.CDatabaseSettings.DB_TYPE_MYSQL:
				dateStr = "'";
				dateStr += calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.DAY_OF_MONTH);
				if (includeTime) dateStr += " " + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND);
				dateStr += "'";
				break;
		}
		return dateStr;
	}
}
