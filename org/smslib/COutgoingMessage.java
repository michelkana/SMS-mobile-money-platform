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
import java.text.SimpleDateFormat;

/**
 * This class represents a normal (text) outgoing / outbound message.
 * 
 * @see CWapSIMessage
 */
public class COutgoingMessage extends CMessage
{
	private static final long serialVersionUID = 1L;

	protected Date dispatchDate;

	protected int validityPeriod;

	protected boolean statusReport;

	protected boolean flashSms;

	protected int srcPort;

	protected int dstPort;
	
	public static final int PUT = 0;
	public static final int GET = 1;

	public COutgoingMessage()
	{
		super(CMessage.MessageType.Outgoing, null, null, null, null);

		validityPeriod = -1;
		statusReport = false;
		flashSms = false;
		srcPort = -1;
		dstPort = -1;
		pid = 0;
		dcs = 0;
		dispatchDate = null;
		setDate(new Date());
		setMessageEncoding(MessageEncoding.Enc7Bit);
	}

	/**
	 * General constructor for an outgoing message. Only the text and the recipient's number is required. The message encoding is set to 7bit by default.
	 * 
	 * @param recipient
	 *            The recipient's number - should be in international format.
	 * @param text
	 *            The message text.
	 * @see #setMessageEncoding(int)
	 * @see #setValidityPeriod(int)
	 * @see #setStatusReport(boolean)
	 * @see #setFlashSms(boolean)
	 * @see #setSourcePort(int)
	 * @see #setDestinationPort(int)
	 */
	public COutgoingMessage(String recipient, String text)
	{
		super(CMessage.MessageType.Outgoing, new Date(), null, recipient, text);

		validityPeriod = -1;
		statusReport = false;
		flashSms = false;
		srcPort = -1;
		dstPort = -1;
		pid = 0;
		dcs = 0;
		dispatchDate = null;
		setDate(new Date());
		setMessageEncoding(MessageEncoding.Enc7Bit);
	}

	public void fromString(String smsFileStr)
	{

		SimpleDateFormat df = new SimpleDateFormat("EEE MMM dd hh:mm:ss z yyyy");
		
		String[] lines = smsFileStr.split(System.getProperty("line.separator"));
		for (int i=0; i<lines.length; i++){
			String[] fields = lines[i].split(": ");
			if ( fields[0].equalsIgnoreCase(" Type") ){
				if ( fields[1].equalsIgnoreCase("Incoming") ){
					type = MessageType.Incoming;
				}
				if ( fields[1].equalsIgnoreCase("Outgoing") ){
					type = MessageType.Outgoing;
				}
				if ( fields[1].equalsIgnoreCase("Status Report") ){
					type = MessageType.StatusReport;
				}
			}
			if ( fields[0].equalsIgnoreCase(" Encoding") ){
				if ( fields[1].equalsIgnoreCase("7-bit") ){
					messageEncoding = MessageEncoding.Enc7Bit;
				}
				if ( fields[1].equalsIgnoreCase("8-bit") ){
					messageEncoding = MessageEncoding.Enc8Bit;
				}
				if ( fields[1].equalsIgnoreCase("UCS2 (Unicode)") ){
					messageEncoding = MessageEncoding.EncUcs2;
				}
			}
			if ( fields[0].equalsIgnoreCase(" Date") ){
				try
				{
					//date = df.parse(fields[1]);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			if ( fields[0].equalsIgnoreCase(" Originator") ){
				this.originator = fields[1];
			}
			if ( fields[0].equalsIgnoreCase(" Recipient") ){
				this.recipient = fields[1];
			}	
			if ( fields[0].equalsIgnoreCase(" Text") ){
				this.text = fields[1];
			}
			if ( fields[0].equalsIgnoreCase(" SMSC Ref No") ){
				refNo = Integer.valueOf(fields[1]).intValue();
			}
			if ( fields[0].equalsIgnoreCase(" Dispatch Date") ){
				/*try{
					dispatchDate = df.parse(fields[1]);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}*/
			}
			if ( fields[0].equalsIgnoreCase(" Validity Period (Hours)") ){
				validityPeriod = Integer.valueOf(fields[1]).intValue();
			}
			if ( fields[0].equalsIgnoreCase(" Source / Destination Ports") ){
				String[] s = fields[1].split(" / ");
				srcPort = Integer.valueOf(s[0]).intValue();
				dstPort = Integer.valueOf(s[1]).intValue();
			}
			if ( fields[0].equalsIgnoreCase(" Flash SMS") ){
				flashSms = Boolean.valueOf(fields[1]).booleanValue();
			}		
		}				
	}


	protected boolean isBig() throws Exception
	{
		int messageLength;

		messageLength = encodedText.length() / 2;
		return (messageLength > maxSize() ? true : false);
	}

	protected int getNoOfParts() throws Exception
	{
		int noOfParts = 0;
		int partSize;
		int messageLength;

		partSize = maxSize() - 8;
		messageLength = encodedText.length() / 2;
		noOfParts = messageLength / partSize;
		if ((noOfParts * partSize) < (messageLength)) noOfParts++;
		return noOfParts;
	}

	private int maxSize() { return 140; }

	private String getPart(int partNo, int udhLength) throws Exception
	{
		int partSize;

		if (partNo != 0)
		{
			partSize = maxSize() - udhLength;
			partSize *= 2;
			if (((partSize * (partNo - 1)) + partSize) > encodedText.length()) return encodedText.substring(partSize * (partNo - 1));
			else return encodedText.substring(partSize * (partNo - 1), (partSize * (partNo - 1)) + partSize);
		}
		else return encodedText;
	}

	protected String getPDU(String smscNumber, int mpRefNo, int partNo) throws Exception
	{
		String pdu, udh, ud, dataLen;
		String str1, str2;

		pdu = "";
		udh = "";
		if ((smscNumber != null) && (smscNumber.length() != 0))
		{
			str1 = "91" + toBCDFormat(smscNumber.substring(1));
			str2 = Integer.toHexString(str1.length() / 2);
			if (str2.length() != 2) str2 = "0" + str2;
			pdu = pdu + str2 + str1;
		}
		else if ((smscNumber != null) && (smscNumber.length() == 0)) pdu = pdu + "00";
		if (((srcPort != -1) && (dstPort != -1)) || (isBig()))
		{
			if (statusReport) pdu = pdu + "71";
			else pdu = pdu + "51";
		}
		else
		{
			if (statusReport) pdu = pdu + "31";
			else pdu = pdu + "11";
		}
		pdu = pdu + "00";
		str1 = getRecipient();
		if (str1.charAt(0) == '+')
		{
			str1 = toBCDFormat(str1.substring(1));
			str2 = Integer.toHexString(getRecipient().length() - 1);
			str1 = "91" + str1;
		}
		else
		{
			str1 = toBCDFormat(str1);
			str2 = Integer.toHexString(getRecipient().length());
			str1 = "81" + str1;
		}
		if (str2.length() != 2) str2 = "0" + str2;
		pdu = pdu + str2 + str1;

		{
			String s;

			s = Integer.toHexString(pid);
			while (s.length() < 2)
				s = "0" + s;
			pdu = pdu + s;
		}

		switch (getMessageEncoding())
		{
			case CMessage.MessageEncoding.Enc7Bit:
				if (flashSms) pdu = pdu + "10";
				else pdu = pdu + "00";
				break;
			case CMessage.MessageEncoding.Enc8Bit:
				if (flashSms) pdu = pdu + "14";
				else pdu = pdu + "04";
				break;
			case CMessage.MessageEncoding.EncUcs2:
				if (flashSms) pdu = pdu + "18";
				else pdu = pdu + "08";
				break;
			case CMessage.MessageEncoding.EncCustom:
			{
				String s;

				if (dcs == 0) throw new OopsException();
				s = Integer.toHexString(dcs);
				while (s.length() < 2)
					s = "0" + s;
				pdu = pdu + s;
			}
				break;
			default:
				throw new OopsException();
		}

		pdu = pdu + getValidityPeriodBits();

		if ((srcPort != -1) && (dstPort != -1))
		{
			String s;

			udh += "060504";
			s = Integer.toHexString(dstPort);
			while (s.length() < 4)
				s = "0" + s;
			udh += s;
			s = Integer.toHexString(srcPort);
			while (s.length() < 4)
				s = "0" + s;
			udh += s;
		}

		if (isBig())
		{
			String s;

			if ((srcPort != -1) && (dstPort != -1)) udh = "0C" + udh.substring(2) + "0804";
			else udh += "060804";
			s = Integer.toHexString(mpRefNo);
			while (s.length() < 4)
				s = "0" + s;
			udh += s;
			s = Integer.toHexString(getNoOfParts());
			while (s.length() < 2)
				s = "0" + s;
			udh += s;
			s = Integer.toHexString(partNo);
			while (s.length() < 2)
				s = "0" + s;
			udh += s;
		}

		switch (messageEncoding)
		{
			case MessageEncoding.Enc7Bit:
				ud = getPart(partNo, udh.length());
				dataLen = Integer.toHexString(((ud.length() + udh.length()) * 8 / 7) / 2);
				break;
			case MessageEncoding.Enc8Bit:
				ud = getPart(partNo, udh.length());
				dataLen = Integer.toHexString((ud.length() + udh.length()) / 2);
				break;
			case MessageEncoding.EncUcs2:
				ud = getPart(partNo, udh.length());
				dataLen = Integer.toHexString((ud.length() + udh.length()) / 2);
				break;
			case MessageEncoding.EncCustom:
				if ((dcs & 0x04) == 0)
				{
					ud = getPart(partNo, udh.length());
					dataLen = Integer.toHexString(((ud.length() + udh.length()) * 8 / 7) / 2);
				}
				else
				{
					ud = getPart(partNo, udh.length());
					dataLen = Integer.toHexString((ud.length() + udh.length()) / 2);
				}
				break;
			default: throw new OopsException();
		}
		if (dataLen.length() != 2) dataLen = "0" + dataLen;
		if (udh.length() != 0) pdu = pdu + dataLen + udh + ud;
		else pdu = pdu + dataLen + ud;
		return pdu.toUpperCase();
	}

	private String getValidityPeriodBits()
	{
		String bits;
		int value;

		if (validityPeriod == -1) bits = "FF";
		else
		{
			if (validityPeriod <= 12) value = (validityPeriod * 12) - 1;
			else if (validityPeriod <= 24) value = (((validityPeriod - 12) * 2) + 143);
			else if (validityPeriod <= 720) value = (validityPeriod / 24) + 166;
			else value = (validityPeriod / 168) + 192;
			bits = Integer.toHexString(value);
			if (bits.length() != 2) bits = "0" + bits;
			if (bits.length() > 2) bits = "FF";
		}
		return bits;
	}

	private String toBCDFormat(String s)
	{
		String bcd;
		int i;

		if ((s.length() % 2) != 0) s = s + "F";
		bcd = "";
		for (i = 0; i < s.length(); i += 2)
			bcd = bcd + s.charAt(i + 1) + s.charAt(i);
		return bcd;
	}

	/**
	 * Sets the Recipient's number. The number should be in international format.
	 * 
	 * @param recipient
	 *            The Recipient's number.
	 */
	public void setRecipient(String recipient)
	{
		this.recipient = recipient;
	}

	/**
	 * Returns the message recipient number. Number is in international format.
	 * 
	 * @return The Recipient's number.
	 */
	public String getRecipient()
	{
		return recipient;
	}

	/**
	 * Sets the validity period. By default, an outgoing message has the maximum allowed validity period.
	 * 
	 * @param hours
	 *            The validity period in hours.
	 */
	public void setValidityPeriod(int hours)
	{
		this.validityPeriod = hours;
	}

	/**
	 * Returns the defined validity period in hours.
	 * 
	 * @return The validity period (hours).
	 */
	public int getValidityPeriod()
	{
		return validityPeriod;
	}

	/**
	 * Sets the delivery status report functionality. Set this to true if you want to enable delivery status report for this specific message.
	 * 
	 * @param statusReport
	 *            True if you want to enable delivery status reports.
	 */
	public void setStatusReport(boolean statusReport)
	{
		this.statusReport = statusReport;
	}

	/**
	 * Returns the state of the delivery status report request.
	 * 
	 * @return True if delivery status report request is enabled.
	 * @see #setValidityPeriod(int)
	 */
	public boolean getStatusReport()
	{
		return statusReport;
	}

	/**
	 * Set the Flash SMS indication.
	 * <p>
	 * Flash SMS appear directly on recipient's screen. This functionality may not be supported on all headsets.
	 * 
	 * @param flashSms
	 *            True if you want to send a Flash SMS.
	 */
	public void setFlashSms(boolean flashSms)
	{
		this.flashSms = flashSms;
	}

	/**
	 * Returns true if the SMS is a flash SMS.
	 * 
	 * @return True if the SMS is a flash SMS.
	 * @see #setFlashSms(boolean)
	 */
	public boolean getFlashSms()
	{
		return flashSms;
	}

	/**
	 * Sets the Source Port information field. This settings affects PDU header creation.
	 * 
	 * @param port
	 *            The Source Port.
	 */
	public void setSourcePort(int port)
	{
		this.srcPort = port;
	}

	/**
	 * Return the message source-port information. Returns -1 if the source-port is undefined.
	 * 
	 * @return The message source-port information.
	 * @see #setSourcePort(int)
	 * @see #getDestinationPort()
	 */
	public int getSourcePort()
	{
		return srcPort;
	}

	/**
	 * Sets the DestinationPort information field. This settings affects PDU header creation.
	 * 
	 * @param port
	 *            The Destination Port.
	 */
	public void setDestinationPort(int port)
	{
		this.dstPort = port;
	}

	/**
	 * Return the message destination-port information. Returns -1 if the destination-port is undefined.
	 * 
	 * @return The message destination-port information.
	 * @see #setDestinationPort(int)
	 * @see #getSourcePort()
	 */
	public int getDestinationPort()
	{
		return dstPort;
	}

	/**
	 * Returns the date of dispatch - the date when this message was send from SMSLib. Returns NULL if the message has not been sent yet.
	 * 
	 * @return The dispatch date.
	 */
	public Date getDispatchDate()
	{
		if (dispatchDate != null) return (Date) dispatchDate.clone();
		else return null;
	}
}
