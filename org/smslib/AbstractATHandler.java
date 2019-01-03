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

import org.apache.log4j.*;

abstract public class AbstractATHandler
{
	protected CSerialDriver serialDriver;

	protected Logger log;

	protected String storageLocations = "";

	protected CService srv;

	protected static final int DELAY_AT = 200;

	protected static final int DELAY_RESET = 20000;
	
	protected static final int DELAY_PIN = 12000;

	protected static final int DELAY_CMD_MODE = 1000;

	protected static final int DELAY_CMGS = 300;

	public AbstractATHandler(CSerialDriver serialDriver, Logger log, CService srv)
	{
		super();
		this.serialDriver = serialDriver;
		this.log = log;
		this.srv = srv;
		storageLocations = "";
	}

	abstract protected void setStorageLocations(String loc);

	abstract protected boolean dataAvailable() throws Exception;

	abstract protected void sync() throws Exception;

	abstract protected void reset() throws Exception;

	abstract protected void echoOff() throws Exception;

	abstract protected void init() throws Exception;

	abstract protected boolean isAlive() throws Exception;

	abstract protected boolean waitingForPin() throws Exception;

	abstract protected boolean enterPin(String pin) throws Exception;

	abstract protected boolean setVerboseErrors() throws Exception;

	abstract protected boolean setPduMode() throws Exception;

	abstract protected boolean setTextMode() throws Exception;

	abstract protected boolean enableIndications() throws Exception;

	abstract protected boolean disableIndications() throws Exception;

	abstract protected String getManufacturer() throws Exception;

	abstract protected String getModel() throws Exception;

	abstract protected String getSerialNo() throws Exception;

	abstract protected String getImsi() throws Exception;

	abstract protected String getSwVersion() throws Exception;

	abstract protected String getBatteryLevel() throws Exception;

	abstract protected String getSignalLevel() throws Exception;

	abstract protected boolean setMemoryLocation(String mem) throws Exception;

	abstract protected void switchToCmdMode() throws Exception;

	abstract protected boolean keepGsmLinkOpen() throws Exception;

	abstract protected int sendMessage(int size, String pdu, String phone, String text) throws Exception;

	abstract protected String listMessages(int messageClass) throws Exception;

	abstract protected boolean deleteMessage(int memIndex, String memLocation) throws Exception;

	abstract protected String getGprsStatus() throws Exception;

	abstract protected String send(String s) throws Exception;

	abstract protected String getNetworkRegistration() throws Exception;

	abstract protected void getStorageLocations() throws Exception;

	private String description;

	String getDescription()
	{
		return description;
	}

	void setDescription(String description)
	{
		this.description = description;
	}

	static AbstractATHandler load(CSerialDriver serialDriver, Logger log, CService srv, String gsmDeviceManufacturer, String gsmDeviceModel) throws RuntimeException
	{
		String BASE_HANDLER = org.smslib.handler.CATHandler.class.getName();
		String[] handlerClassNames =
		{ null, null, BASE_HANDLER };
		String[] handlerDescriptions =
		{ null, null, "Generic" };

		StringBuffer handlerClassName = new StringBuffer(BASE_HANDLER);
		if (gsmDeviceManufacturer != null && !gsmDeviceManufacturer.equals(""))
		{
			handlerClassName.append("_").append(gsmDeviceManufacturer);
			handlerClassNames[1] = handlerClassName.toString();
			handlerDescriptions[1] = gsmDeviceManufacturer + " (Generic)";
			if (gsmDeviceModel != null && !gsmDeviceModel.equals(""))
			{
				handlerClassName.append("_").append(gsmDeviceModel);
				handlerClassNames[0] = handlerClassName.toString();
				handlerDescriptions[0] = gsmDeviceManufacturer + " " + gsmDeviceModel;
			}
		}

		AbstractATHandler atHandler = null;
		for (int i = 0; i < 3; ++i)
		{
			try
			{
				if (handlerClassNames[i] != null)
				{
					Class handlerClass = Class.forName(handlerClassNames[i]);

					java.lang.reflect.Constructor handlerConstructor = handlerClass.getConstructor(new Class[]
					{ CSerialDriver.class, Logger.class, CService.class });
					atHandler = (AbstractATHandler) handlerConstructor.newInstance(new Object[]
					{ serialDriver, log, srv });
					atHandler.setDescription(handlerDescriptions[i]);
					break;
				}
			}
			catch (Exception ex)
			{
				if (i == 2) throw new RuntimeException("Class AbstractATHandler: Cannot initialize handler!");
				// ex.printStackTrace();
				// else throw new RuntimeException("Class AbstractATHandler:
				// Unhandled error!");
			}
		}

		return atHandler;
	}
}
