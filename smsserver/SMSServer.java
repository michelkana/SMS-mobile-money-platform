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

import javax.swing.*;

class SMSServer extends Thread
{
	private CSettings settings;
	private CMainWindow mainWindow;
	protected CMainThread service;

	public void initialize() throws Exception
	{
		settings = new CSettings();

		settings.loadConfiguration();

		if (settings.getGeneralSettings().getGui())
		{
			mainWindow = new CMainWindow(this, settings);
			mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			mainWindow.setVisible(true);

			mainWindow.setRawInLog(settings.getGeneralSettings().isRawInLogEnabled());
			mainWindow.setRawOutLog(settings.getGeneralSettings().isRawOutLogEnabled());
			mainWindow.setInterfaceXML((settings.getPhoneSettings().getXmlInQueue() != null) || (settings.getPhoneSettings().getXmlOutQueue() != null));
			mainWindow.setInterfaceDB(settings.getDatabaseSettings().getEnabled());
		}
		else
		{
			mainWindow = null;

			System.out.println(stripHtml(CConstants.ABOUT_VERSION));
			System.out.println(stripHtml(CConstants.ABOUT_BY));
			System.out.println(stripHtml(CConstants.ABOUT_WEBPAGE));
			System.out.println(stripHtml(CConstants.ABOUT_OTHER));
			System.out.println("");
			System.out.println(CConstants.TEXT_CONSOLE);
			System.out.println("");

			service = new CUserThread(this, null, settings);
			service.initialize();
			service.connect(true);
		}

		Runtime.getRuntime().addShutdownHook(new CShutdown());
	}

	public void run()
	{
		while (true)
			try
			{
				sleep(5000);
			}
			catch (Exception e)
			{
			}
	}

	public static class CShutdown extends Thread
	{
		public void run()
		{
		}
	}

	public String stripHtml(String s)
	{
		String o;


		o = s.replaceAll("<html>", "");
		o = o.replaceAll("</html>", "");
		o = o.replaceAll("<b>", "");
		o = o.replaceAll("</b>", "");
		o = o.replaceAll("<h1>", "");
		o = o.replaceAll("</h1>", "");
		o = o.replaceAll("<br>", "");
		return o;
	}

	public static void main(String[] args)
	{
		try
		{
			SMSServer app = new SMSServer();
			app.initialize();
			app.setPriority(MIN_PRIORITY);
			app.start();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
