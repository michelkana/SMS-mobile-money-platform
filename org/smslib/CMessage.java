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

import java.util.*;

/**
 * This is the parent class defining a generic SMS message. In almost all cases, you will not work with this class, except for calling some methods for accessing info fields common for all types of messages.
 * 
 * @see CIncomingMessage
 * @see COutgoingMessage
 * @see CStatusReportMessage
 * @see CWapSIMessage
 */
public class CMessage implements java.io.Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Holds values representing message encodings.
	 */
	public static class MessageEncoding
	{
		/**
		 * 7-Bit (default GSM alphabet) encoding.
		 */
		public static final int Enc7Bit = 1;

		/**
		 * 8-Bit encoding.
		 */
		public static final int Enc8Bit = 2;

		/**
		 * UCS2 (Unicode) encoding. Use this for Far-East languages.
		 */
		public static final int EncUcs2 = 3;
		
		/**
		 * Custom encoding. When you set this value, you should also set the DCS (Data Coding Scheme) value yourself!
		 */
		public static final int EncCustom = 4;
	}

	/**
	 * Holds values representing message types.
	 */
	public static class MessageType
	{
		/**
		 * Incoming (inbound) message.
		 */
		public static final int Incoming = 1;

		/**
		 * Outgoing (outbound) message.
		 */
		public static final int Outgoing = 2;

		/**
		 * Delivery status report message.
		 */
		public static final int StatusReport = 3;

		/**
		 * WAP PUSH-SI message.
		 */
		public static final int WapPushSI = 4;
	}

	protected int type;

	protected int refNo;

	protected String id;

	protected Date date;

	protected String originator;

	protected String recipient;

	protected String text;
	
	protected String encodedText;

	protected int messageEncoding;

	protected int pid;

	protected int dcs;

	public CMessage(int type, int refNo, Date date, String originator, String recipient, String text, int messageEncoding)
	{               
		this.type = type;
		this.refNo = refNo;
		this.id = null;
		setDate(date);
		this.originator = originator;
		this.recipient = recipient;
		this.text = text;
		this.messageEncoding = messageEncoding;
	}               
                  
	public CMessage(int type, Date date, String originator, String recipient, String text)
	{               
		this.type = type;
		this.refNo = -1;
		this.id = null;
		setDate(date);
		this.originator = originator;
		this.recipient = recipient;
		this.text = text;
		this.messageEncoding = MessageEncoding.Enc7Bit;
	}
	

	/**
	 * Returns the message type.
	 * 
	 * @return The message type.
	 * @see MessageType
	 */
	public int getType()
	{
		return type;
	}

	/**
	 * Returns the message SMSC Reference Number.
	 * 
	 * @return The SMSC Ref Number.
	 */
	public int getRefNo()
	{
		return refNo;
	}

	/**
	 * Returns the message id.
	 * 
	 * @return The message id.
	 * @see #setId(String)
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * Returns the message date.
	 * 
	 * @return The message date.
	 */
	public Date getDate()
	{
		return date == null ? null : (Date) date.clone();
	}

	/**
	 * Returns the message text.
	 * 
	 * @return The message text.
	 */
	public String getText()
	{
		return text;
	}

	/**
	 * Returns the message encoding.
	 * 
	 * @return The message encoding.
	 * @see MessageEncoding
	 */
	public int getMessageEncoding()
	{
		return messageEncoding;
	}

	/**
	 * Sets the id of the message.
	 * <p>
	 * The message id is a user-defined id field. It is not used in actual sending or receiving of a message. So, even if you don't define it, SMSLib will work normally. Use it if you need to differentiate messages in some way.
	 * 
	 * @param id
	 *            The message id.
	 * @see #getId()
	 */
	public void setId(String id)
	{
		this.id = id;
	}

	/**
	 * Sets the message text.
	 * 
	 * @param text
	 *            The message text.
	 */
	public void setText(String text)
	{
		this.text = text;
	}

	/**
	 * Sets the message (create) date.
	 * 
	 * @param date
	 *            The message date.
	 */
	public void setDate(Date date)
	{
		this.date = (date != null ? (Date) date.clone() : new java.util.Date());
	}

	/**
	 * Sets the message encoding.
	 * 
	 * @param messageEncoding
	 *            The message encoding.
	 * @see MessageEncoding
	 */
	public void setMessageEncoding(int messageEncoding)
	{
		this.messageEncoding = messageEncoding;
		if (text == null) return;
		encodedText = "";
		switch (this.messageEncoding)
		{
			case MessageEncoding.Enc7Bit:
				encodedText = CGSMAlphabet.textToPDU(text);
				if (encodedText.charAt(encodedText.length() - 1) == '0' && encodedText.charAt(encodedText.length() - 2) == '0')
				{
					text += " ";
					setMessageEncoding(messageEncoding);
				}
				break;
			case MessageEncoding.Enc8Bit:
				for (int i = 0; i < text.length(); i++)
				{
					char c = text.charAt(i);;
					encodedText += ((Integer.toHexString(c).length() < 2) ? "0" + Integer.toHexString(c) : Integer.toHexString(c));
				}
				break;
			case MessageEncoding.EncUcs2:
				for (int i = 0; i < text.length(); i++)
				{
					char c = text.charAt(i);
					int high = (int)(c / 256);
					int low = c % 256;
					encodedText += ((Integer.toHexString(high).length() < 2) ? "0" + Integer.toHexString(high) : Integer.toHexString(high));
					encodedText += ((Integer.toHexString(low).length() < 2) ? "0" + Integer.toHexString(low) : Integer.toHexString(low));
				}
				break;
			case MessageEncoding.EncCustom:
				if ((dcs & 0x04) == 0)
				{
					encodedText = CGSMAlphabet.textToPDU(text);
					if (encodedText.charAt(encodedText.length() - 1) == '0' && encodedText.charAt(encodedText.length() - 2) == '0')
					{
						text += " ";
						setMessageEncoding(messageEncoding);
					}
				}
				else
				{
					for (int i = 0; i < text.length(); i++)
					{
						char c = text.charAt(i);;
						encodedText += ((Integer.toHexString(c).length() < 2) ? "0" + Integer.toHexString(c) : Integer.toHexString(c));
					}
				}
				break;
		}
	}

	protected void setRefNo(int refNo)
	{
		this.refNo = refNo;
	}

	protected void addText(String text)
	{
		this.text += text;
	}

	/**
	 * Sets the message Protocol Id. Normally, you should not care about this value - by default it is set to 0 which is appropriate for normal SMS messages.
	 * 
	 * @param pid
	 *            The Protocol Id.
	 *            
	 *  @see #getPid()
	 */
	public void setPid(int pid)
	{
		this.pid = pid;
	}

	/**
	 * Returns the message's Protocol id.
	 * 
	 * @return The Protocol id.
	 * 
	 * @see #setPid(int)
	 */
	public int getPid()
	{
		return pid;
	}

	/**
	 * Sets the message's DCS (Data Coding Scheme). SMSLib will use this value if you set the message encoding to EncCustom.
	 * 
	 * @param dcs
	 *            The DCS value.
	 * @see #setMessageEncoding(int)
	 * @see #getDcs()
	 */
	public void setDcs(int dcs)
	{
		this.dcs = dcs;
	}

	/**
	 * Returns the message's DCS (Data Coding Scheme).
	 * 
	 * @return The DCS value.
	 * 
	 * @see #setDcs(int)
	 */
	public int getDcs()
	{
		return dcs;
	}

	/**
	 * A message-to-string mapping function. Used for debugging and for easy viewing of of message object's info fields.
	 */
	public String toString()
	{
		String str = "";

		str += "===============================================================================";
		str += "\n";
		str += "<< MESSAGE DUMP >>";
		str += "\n";
		str += "-------------------------------------------------------------------------------";
		str += "\n";
		str += " Type: " + (type == MessageType.Incoming ? "Incoming" : (type == MessageType.Outgoing ? "Outgoing" : "Status Report"));
		str += "\n";
		str += " Encoding: " + (messageEncoding == MessageEncoding.Enc7Bit ? "7-bit" : (messageEncoding == MessageEncoding.Enc8Bit ? "8-bit" : "UCS2 (Unicode)"));
		str += "\n";
		str += " Date: " + date;
		str += "\n";
		str += " Originator: " + originator;
		str += "\n";
		str += " Recipient: " + recipient;
		str += "\n";
		str += " Text: " + text;
		str += "\n";
		str += " SMSC Ref No: " + refNo;
		str += "\n";
		if (type == MessageType.Incoming)
		{
			CIncomingMessage msg = (CIncomingMessage) this;
			str += " Memory Index: " + msg.getMemIndex();
			str += "\n";
			str += " Multi-part Memory Index: " + msg.getMpMemIndex();
			str += "\n";
			str += " Memory Location: " + msg.getMemLocation();
			str += "\n";
		}
		if (type == MessageType.Outgoing)
		{
			COutgoingMessage msg = (COutgoingMessage) this;
			str += " Dispatch Date: " + msg.getDispatchDate();
			str += "\n";
			str += " Validity Period (Hours): " + msg.getValidityPeriod();
			str += "\n";
			str += " Status Report: " + msg.getStatusReport();
			str += "\n";
			str += " Source / Destination Ports: " + msg.getSourcePort() + " / " + msg.getDestinationPort();
			str += "\n";
			str += " Flash SMS: " + msg.getFlashSms();
			str += "\n";
		}
		if (type == MessageType.StatusReport)
		{
			CStatusReportMessage msg = (CStatusReportMessage) this;
			str += " Date Sent: " + msg.getDateOriginal();
			str += "\n";
			str += " Date Received (by recipient): " + msg.getDateReceived();
			str += "\n";
			str += " Delivery Status: ";
			if (msg.getDeliveryStatus() == CStatusReportMessage.DeliveryStatus.Delivered) str += "Delivered";
			else if (msg.getDeliveryStatus() == CStatusReportMessage.DeliveryStatus.KeepTrying) str += "Not delivered yet - will keep trying.";
			else if (msg.getDeliveryStatus() == CStatusReportMessage.DeliveryStatus.Aborted) str += "Not delivered - aborted.";
			str += "\n";
		}

		str += "===============================================================================";
		str += "\n";
		return str;
	}
}
