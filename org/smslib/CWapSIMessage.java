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
import java.net.*;

public class CWapSIMessage extends COutgoingMessage
{
	private static final long serialVersionUID = 1L;

	public static class Signal
	{
		public static final int None = 0;

		public static final int Low = 1;

		public static final int Medium = 2;

		public static final int High = 3;

		public static final int Delete = 4;
	}

	private static final String PDU_PATTERN = "25060803AE81EAAF82B48401056A0045C6{URL1}03{URL2}00080103{TEXT}000101";

	private String siPdu = "";

	private URL hRef;

	private Date createDate, expireDate;

	int signal;

	private static final String[][] protocolBytes =
	{
	{ "http://www.", "0D" },
	{ "https://www.", "0F" },
	{ "http://", "0C" },
	{ "https://", "0E" } };

	private static final String[][] domainBytes =
	{
	{ ".com/", "85" },
	{ ".edu/", "86" },
	{ ".net/", "87" },
	{ ".org/", "88" } };

	public CWapSIMessage(String recipient, URL hRef, Date createDate, Date expireDate, int signal, String text) throws Exception
	{
		super();

		this.hRef = hRef;
		this.createDate = null; // (Date) createDate.clone();
		this.expireDate = null; // (Date) expireDate.clone();
		this.signal = signal;
		this.text = text;
		setMessageEncoding(CMessage.MessageEncoding.Enc8Bit);
		setSourcePort(9200);
		setDestinationPort(2948);
		this.type = CMessage.MessageType.WapPushSI;
		this.recipient = recipient;

		fixPdu();
	}

	public CWapSIMessage(String recipient, URL hRef, String text) throws Exception
	{
		this(recipient, hRef, null, null, Signal.None, text);
	}

	protected String getPDUData()
	{
		return siPdu;
	}

	private void fixPdu() throws Exception
	{
		String s, url;
		int i;
		char c;
		byte[] utfBytes;
		byte cc;
		boolean foundProtocol;

		siPdu = PDU_PATTERN;
		if (createDate != null) siPdu = CUtils.replace(siPdu, "{DATECRT}", formatDate(createDate));
		if (expireDate != null) siPdu = CUtils.replace(siPdu, "{DATEEXP}", formatDate(expireDate));

		s = "";
		utfBytes = text.getBytes("UTF-8");
		for (i = 0; i < utfBytes.length; i++)
		{
			cc = utfBytes[i];
			s = s + ((Integer.toHexString(cc).length() < 2) ? "0" + Integer.toHexString(cc) : Integer.toHexString(cc));
		}
		siPdu = CUtils.replace(siPdu, "{TEXT}", s);

		foundProtocol = false;
		url = hRef.toString();
		for (i = 0; i < 4; i++)
		{
			if (url.indexOf(protocolBytes[i][0]) == 0)
			{
				foundProtocol = true;
				siPdu = CUtils.replace(siPdu, "{URL1}", protocolBytes[i][1]);
				url = CUtils.replace(url, protocolBytes[i][0], "");
				break;
			}
		}
		if (!foundProtocol) siPdu = CUtils.replace(siPdu, "{URL1}", "0B");

		s = "";
		for (i = 0; i < url.length(); i++)
		{
			String subUrl;
			boolean foundDomain = false;

			subUrl = url.substring(i);
			for (int j = 0; j < 4; j++)
			{
				if (subUrl.indexOf(domainBytes[j][0]) == 0)
				{
					foundDomain = true;
					i += 4;
					s += "00";
					s += domainBytes[j][1];
					s += "03";
					break;
				}
			}

			if (!foundDomain)
			{
				c = url.charAt(i);
				s += ((Integer.toHexString(c).length() < 2) ? "0" + Integer.toHexString(c) : Integer.toHexString(c));
			}
		}
		siPdu = CUtils.replace(siPdu, "{URL2}", s);
		encodedText = siPdu;
	}

	private String formatDate(Date d)
	{
		String strDate = "", tmp = "";
		Calendar cal = Calendar.getInstance();

		cal.setTime(d);

		strDate = String.valueOf(cal.get(Calendar.YEAR));
		tmp = String.valueOf(cal.get(Calendar.MONTH) + 1);
		if (tmp.length() != 2) tmp = "0" + tmp;
		strDate += tmp;
		tmp = String.valueOf(cal.get(Calendar.DATE));
		if (tmp.length() != 2) tmp = "0" + tmp;
		strDate += tmp;
		tmp = String.valueOf(cal.get(Calendar.HOUR_OF_DAY));
		if (tmp.length() != 2) tmp = "0" + tmp;
		strDate += tmp;
		tmp = String.valueOf(cal.get(Calendar.MINUTE));
		if (tmp.length() != 2) tmp = "0" + tmp;
		strDate += tmp;
		tmp = String.valueOf(cal.get(Calendar.SECOND));
		if (tmp.length() != 2) tmp = "0" + tmp;
		strDate += tmp;
		return strDate;
	}
}
