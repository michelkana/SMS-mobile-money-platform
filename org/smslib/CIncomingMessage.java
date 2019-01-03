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

public class CIncomingMessage extends CMessage
{
	private static final long serialVersionUID = 1L;

	/**
	 * Holds values representing the message class of the message to be read from the GSM device.
	 */
	public static class MessageClass
	{
		/**
		 * Read all messages.
		 */
		public static final int All = 1;

		/*
		 * Read unread messages. After reading, all returned messages will be marked as read.
		 */
		public static final int Unread = 2;

		/**
		 * Read already-read messages.
		 */
		public static final int Read = 3;
	}

	public static final int PUT = 0;
	public static final int GET = 1;
	
	private int memIndex;

	private String memLocation;

	private int mpRefNo;

	private int mpMaxNo;

	private int mpSeqNo;

	private String mpMemIndex;

	private String pduUserData;

	public CIncomingMessage()
	{
		super(CMessage.MessageType.Incoming, null, null, null, null);
	}
	
	protected CIncomingMessage(Date date, String originator, String text, int memIndex, String memLocation)
	{
		super(MessageType.Incoming, date, originator, null, text);

		this.memIndex = memIndex;
		this.memLocation = memLocation;

		mpRefNo = 0;
		mpMaxNo = 0;
		mpSeqNo = 0;
		mpMemIndex = "";
	}

	protected CIncomingMessage(int messageType, int memIndex, String memLocation)
	{
		super(messageType, null, null, null, null);

		this.memIndex = memIndex;
		this.memLocation = memLocation;

		mpRefNo = 0;
		mpMaxNo = 0;
		mpSeqNo = 0;
		mpMemIndex = "";
	}

	public void fromString(String smsFileStr)
	{
			
		SimpleDateFormat df = new SimpleDateFormat("EEE MMM dd hh:mm:ss yyyy", Locale.US);	

		//String[] lines = smsFileStr.split("\n"); // if encrypt
		String[] lines = smsFileStr.split(System.getProperty("line.separator"));
		//System.out.println("\n the line 1 \n " + lines[0]);
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
				/*try
				{
					date = df.parse(fields[1].replace("CEST ", ""));
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}*/ 
			}
			if ( fields[0].equalsIgnoreCase(" Originator") ){
				originator = fields[1]; 
			}
			if ( fields[0].equalsIgnoreCase(" Recipient") ){
				recipient = fields[1];
			}	
			if ( fields[0].equalsIgnoreCase(" Text") ){
				text = fields[1];
			}
			if ( fields[0].equalsIgnoreCase(" SMSC Ref No") ){
				refNo = Integer.valueOf(fields[1]).intValue();
			}
			if ( fields[0].equalsIgnoreCase(" Memory Index") ){
				memIndex = Integer.valueOf(fields[1]).intValue();
			}
			if ( fields[0].equalsIgnoreCase(" Multi-part Memory Index") ){
				mpMemIndex = (fields.length>1)?fields[1]:null;
			}
			if ( fields[0].equalsIgnoreCase(" Memory Location") ){
				memLocation = (fields.length>1)?fields[1]:null;
			}	
		}
	}

	protected CIncomingMessage(String pdu, int memIndex, String memLocation)
	{
		super(MessageType.Incoming, null, null, null, null);

		Date date;
		String originator;
		String str1, str2;
		int index, i, j, k, protocol, addr, year, month, day, hour, min, sec, skipBytes;
		boolean hasUDH;
		int UDHLength;
		String UDHData;
		byte[] bytes;

		this.memIndex = memIndex;
		this.memLocation = memLocation;

		mpRefNo = 0;
		mpMaxNo = 0;
		mpSeqNo = 0;
		mpMemIndex = "";

		skipBytes = 0;

		i = Integer.parseInt(pdu.substring(0, 2), 16);
		index = (i + 1) * 2;

		hasUDH = ((Integer.parseInt(pdu.substring(index, index + 2), 16) & 0x40) != 0) ? true : false;

		index += 2;
		i = Integer.parseInt(pdu.substring(index, index + 2), 16);
		j = index + 4;
		originator = "";
		for (k = 0; k < i; k += 2)
			originator = originator + pdu.charAt(j + k + 1) + pdu.charAt(j + k);
		originator = "+" + originator;
		if (originator.charAt(originator.length() - 1) == 'F') originator = originator.substring(0, originator.length() - 1);

		addr = Integer.parseInt(pdu.substring(j - 2, j), 16);
		if ((addr & (1 << 6)) != 0 && (addr & (1 << 5)) == 0 && (addr & (1 << 4)) != 0)
		{
			str1 = pduToText(pdu.substring(j, j + k));
			bytes = new byte[str1.length()];
			for (i = 0; i < str1.length(); i++)
				bytes[i] = (byte) str1.charAt(i);
			originator = CGSMAlphabet.bytesToString(bytes);
		}
		// else if ( (addr & (1 << 6)) == 0 && (addr & (1 << 5)) == 0 && (addr &
		// (1 << 4)) != 0) originator = "+" + originator;

		index = j + k + 2;
		str1 = "" + pdu.charAt(index) + pdu.charAt(index + 1);
		protocol = Integer.parseInt(str1, 16);
		index += 2;
		year = Integer.parseInt("" + pdu.charAt(index + 1) + pdu.charAt(index));
		index += 2;
		month = Integer.parseInt("" + pdu.charAt(index + 1) + pdu.charAt(index));
		index += 2;
		day = Integer.parseInt("" + pdu.charAt(index + 1) + pdu.charAt(index));
		index += 2;
		hour = Integer.parseInt("" + pdu.charAt(index + 1) + pdu.charAt(index));
		index += 2;
		min = Integer.parseInt("" + pdu.charAt(index + 1) + pdu.charAt(index));
		index += 2;
		sec = Integer.parseInt("" + pdu.charAt(index + 1) + pdu.charAt(index));
		index += 4;
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, year + 2000);
		cal.set(Calendar.MONTH, month - 1);
		cal.set(Calendar.DAY_OF_MONTH, day);
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, min);
		cal.set(Calendar.SECOND, sec);
		date = cal.getTime();

		if (hasUDH)
		{
			UDHLength = Integer.parseInt(pdu.substring(index + 2, index + 2 + 2), 16);
			UDHData = pdu.substring(index + 2 + 2, index + 2 + 2 + (UDHLength * 2));

			if (UDHData.substring(0, 2).equalsIgnoreCase("00"))
			{
				mpRefNo = Integer.parseInt(UDHData.substring(4, 6), 16);
				mpMaxNo = Integer.parseInt(UDHData.substring(6, 8), 16);
				mpSeqNo = Integer.parseInt(UDHData.substring(8, 10), 16);
				skipBytes = 7;
			}
			else if (UDHData.substring(0, 2).equalsIgnoreCase("08"))
			{
				mpRefNo = Integer.parseInt(UDHData.substring(4, 8), 16);
				mpMaxNo = Integer.parseInt(UDHData.substring(8, 10), 16);
				mpSeqNo = Integer.parseInt(UDHData.substring(10, 12), 16);
				skipBytes = 8;
			}
		}
		else
		{
			UDHLength = 0;
			UDHData = "";
		}

		switch (protocol & 0x0C)
		{
			case 0:
				setMessageEncoding(MessageEncoding.Enc7Bit);
				str1 = pduToText(pdu.substring(index + 2));
				pduUserData = pdu.substring(index + 2 + UDHLength);
				bytes = new byte[str1.length()];
				for (i = 0; i < str1.length(); i++)
					bytes[i] = (byte) str1.charAt(i);
				str2 = CGSMAlphabet.bytesToString(bytes);
				if (hasUDH) str1 = str2.substring(UDHLength + 2);
				else str1 = str2;
				break;
			case 4:
				setMessageEncoding(MessageEncoding.Enc8Bit);
				index += 2;
				if (hasUDH) index += UDHLength + skipBytes;
				pduUserData = pdu.substring(index);
				str1 = "";
				while (index < pdu.length())
				{
					i = Integer.parseInt("" + pdu.charAt(index) + pdu.charAt(index + 1), 16);
					str1 = str1 + (char) i;
					index += 2;
				}
				break;
			case 8:
				setMessageEncoding(MessageEncoding.EncUcs2);
				index += 2;
				if (hasUDH) index += UDHLength + skipBytes;
				pduUserData = pdu.substring(index);
				str1 = "";
				while (index < pdu.length())
				{
					i = Integer.parseInt("" + pdu.charAt(index) + pdu.charAt(index + 1), 16);
					j = Integer.parseInt("" + pdu.charAt(index + 2) + pdu.charAt(index + 3), 16);
					str1 = str1 + (char) ((i * 256) + j);
					index += 4;
				}
				break;
		}

		this.originator = originator;
		this.date = date;
		this.text = str1;
	}

	private String pduToText(String pdu)
	{
		String text;
		byte oldBytes[], newBytes[];
		BitSet bitSet;
		int i, j, value1, value2;

		oldBytes = new byte[pdu.length() / 2];
		for (i = 0; i < pdu.length() / 2; i++)
		{
			oldBytes[i] = (byte) (Integer.parseInt(pdu.substring(i * 2, (i * 2) + 1), 16) * 16);
			oldBytes[i] += (byte) Integer.parseInt(pdu.substring((i * 2) + 1, (i * 2) + 2), 16);
		}

		bitSet = new BitSet(pdu.length() / 2 * 8);
		value1 = 0;
		for (i = 0; i < pdu.length() / 2; i++)
			for (j = 0; j < 8; j++)
			{
				value1 = (i * 8) + j;
				if ((oldBytes[i] & (1 << j)) != 0) bitSet.set(value1);
			}
		value1++;

		value2 = value1 / 7;
		if (value2 == 0) value2++;

		newBytes = new byte[value2];
		for (i = 0; i < value2; i++)
			for (j = 0; j < 7; j++)
				if ((value1 + 1) > (i * 7 + j)) if (bitSet.get(i * 7 + j)) newBytes[i] |= (byte) (1 << j);

		if (newBytes[value2 - 1] == 0) text = new String(newBytes, 0, value2 - 1);
		else text = new String(newBytes);
		return text;
	}

	protected void setMpRefNo(int mpRefNo)
	{
		this.mpRefNo = mpRefNo;
	}

	protected void setMpSeqNo(int mpSeqNo)
	{
		this.mpSeqNo = mpSeqNo;
	}

	protected void setMemIndex(int memIndex)
	{
		this.memIndex = memIndex;
	}

	protected void setMpMemIndex(int memIndex)
	{
		this.mpMemIndex += (mpMemIndex.length() == 0 ? "" : ",") + memIndex;
	}

	/**
	 * Returns the Originator's number. Number is in international format or in text format.
	 * 
	 * @return The Originator's number.
	 */
	public String getOriginator()
	{
		return originator;
	}

	/**
	 * Returns the memory index of the message.
	 * 
	 * @return The memory index of the message.
	 * @see #getMemLocation()
	 */
	public int getMemIndex()
	{
		return memIndex;
	}

	/**
	 * Returns the memory location of the message.
	 * <p>
	 * Memory location is a two-char identifier (i.e. SM, SR, etc) which denotes the memory storage of the phone.
	 * 
	 * @return The memory location.
	 * @see #getMemIndex()
	 */
	public String getMemLocation()
	{
		return memLocation;
	}

	/**
	 * Return the raw PDU data (excluding the pdu user data header, if available)
	 * <p>
	 * @return The PDU user data.
	 */
	public String getPDUUserData()
	{
		return pduUserData;
	}

	protected int getMpRefNo()
	{
		return mpRefNo;
	}

	protected int getMpMaxNo()
	{
		return mpMaxNo;
	}

	protected int getMpSeqNo()
	{
		return mpSeqNo;
	}

	protected String getMpMemIndex()
	{
		return mpMemIndex;
	}
}
