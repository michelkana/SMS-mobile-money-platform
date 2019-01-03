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

public class CNewMsgMonitor
{
	public static final int IDLE = 0;

	public static final int DATA = 1;

	public static final int CMTI = 2;

	private int state = IDLE;

	public synchronized int getState()
	{
		return state;
	}

	public synchronized void reset()
	{
		state = IDLE;
	}

	public synchronized void raise(int state)
	{
		if (state > this.state)
		{
			this.state = state;
			try
			{
				notify();
			}
			catch (Exception e)
			{
			}
		}
	}

	public synchronized int waitEvent(long timeout)
	{
		if (state == IDLE)
		{
			try
			{
				wait(timeout);
			}
			catch (Exception e)
			{
			}
		}
		int prevState = state;
		state = IDLE;
		return prevState;
	}
}
