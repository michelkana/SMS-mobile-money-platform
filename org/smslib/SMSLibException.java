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

/**
 * Generic Exception class (parent class) used for SMSLib specific exceptions.
 * 
 * @see NoPduSupportException
 * @see AlreadyConnectedException
 * @see InvalidPinException
 * @see NoPinException
 * @see NoResponseException
 * @see NotConnectedException
 * @see OopsException
 */

public class SMSLibException extends Exception
{
	private static final long serialVersionUID = 1L;

	public SMSLibException()
	{
		super();
	}

	public SMSLibException(String s)
	{
		super(s);
	}
}
