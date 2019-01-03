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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

class CMainWindow extends JFrame
{
	private static final long serialVersionUID = 1;

	private CSettings settings;

	private SMSServer SmsServer;

	private CMainThread service;

	private CAboutDialog aboutDialog = null;

	private JMenuBar menuBar;

	private JMenu fileMenu, aboutMenu;

	private JMenuItem mniConnect, mniDisconnect, mniExit;

	private JMenuItem mniAbout;

	private JLabel lblManuf, lblModel, lblSerialNo, lblIMSI, lblSwVersion, lblBattery, lblSignal, lblStatus;

	private JTextField txtManuf, txtModel, txtSerialNo, txtIMSI, txtSwVersion, txtStatus;

	private JProgressBar pgbBattery, pgbSignal;

	private JLabel lblInFrom, lblInDate, lblInText;

	private JTextField txtInFrom, txtInDate;

	private JTextArea txtInText;

	private JLabel lblOutTo, lblOutDate, lblOutText;

	private JTextField txtOutTo, txtOutDate;

	private JTextArea txtOutText;

	private JLabel lblUpSince, lblTraffic, lblTrafficIn, lblTrafficOut;

	private JTextField txtUpSince, txtTrafficIn, txtTrafficOut;

	private JLabel lblInterfaces, lblInterfaceDB, lblInterfaceXML, lblInterfaceRMI;

	private JLabel lblRawLogs, lblRawInLog, lblRawOutLog;

	public CMainWindow(SMSServer SmsServer, CSettings settings)
	{
		this.SmsServer = SmsServer;
		this.settings = settings;
		this.settings.setMainWindow(this);

		service = new CUserThread(SmsServer, this, settings);

		setTitle(CConstants.MAIN_WINDOW_TITLE);
		setSize(750, 580);
		setLocation(5, 5);
		getContentPane().setLayout(new GridBagLayout());

		menuBar = new JMenuBar();
		fileMenu = new JMenu(CConstants.MENU_FILE_MAIN);
		mniConnect = new JMenuItem(CConstants.MENU_FILE_OPTION_01);
		mniConnect.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				service.connect(true);
			}
		});
		mniDisconnect = new JMenuItem(CConstants.MENU_FILE_OPTION_02);
		mniDisconnect.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				service.disconnect(true);
			}
		});
		mniExit = new JMenuItem(CConstants.MENU_FILE_OPTION_99);
		mniExit.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				System.exit(0);
			}
		});
		fileMenu.add(mniConnect);
		fileMenu.add(mniDisconnect);
		fileMenu.addSeparator();
		fileMenu.add(mniExit);
		menuBar.add(fileMenu);
		aboutMenu = new JMenu(CConstants.MENU_ABOUT_MAIN);
		mniAbout = new JMenuItem(CConstants.MENU_ABOUT_OPTION_01);
		mniAbout.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				if (aboutDialog == null) aboutDialog = new CAboutDialog(CMainWindow.this);
				aboutDialog.setVisible(true);
			}
		});
		aboutMenu.add(mniAbout);
		menuBar.add(aboutMenu);
		setJMenuBar(menuBar);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.weightx = 100;
		gbc.weighty = 100;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.insets.left = 5;
		gbc.insets.top = 5;
		gbc.insets.right = 5;
		JPanel statusPanel = new JPanel();
		statusPanel.setLayout(new GridBagLayout());
		lblManuf = new JLabel(CConstants.LABEL_MANUFACTURER, SwingConstants.LEFT);
		txtManuf = new JTextField("", 16);
		txtManuf.setEditable(false);
		gbc.gridx = 0;
		gbc.gridy = 0;
		statusPanel.add(lblManuf, gbc);
		gbc.gridx = 1;
		gbc.gridy = 0;
		statusPanel.add(txtManuf, gbc);

		lblModel = new JLabel(CConstants.LABEL_MODEL, SwingConstants.LEFT);
		txtModel = new JTextField("", 16);
		txtModel.setEditable(false);
		gbc.gridx = 0;
		gbc.gridy = 1;
		statusPanel.add(lblModel, gbc);
		gbc.gridx = 1;
		gbc.gridy = 1;
		statusPanel.add(txtModel, gbc);

		lblSerialNo = new JLabel(CConstants.LABEL_SERIALNO, SwingConstants.LEFT);
		txtSerialNo = new JTextField("", 20);
		txtSerialNo.setEditable(false);
		gbc.gridx = 0;
		gbc.gridy = 2;
		statusPanel.add(lblSerialNo, gbc);
		gbc.gridx = 1;
		gbc.gridy = 2;
		statusPanel.add(txtSerialNo, gbc);

		lblIMSI = new JLabel(CConstants.LABEL_IMSI, SwingConstants.LEFT);
		txtIMSI = new JTextField("", 20);
		txtIMSI.setEditable(false);
		gbc.gridx = 0;
		gbc.gridy = 3;
		statusPanel.add(lblIMSI, gbc);
		gbc.gridx = 1;
		gbc.gridy = 3;
		statusPanel.add(txtIMSI, gbc);

		lblSwVersion = new JLabel(CConstants.LABEL_SWVERSION, SwingConstants.LEFT);
		txtSwVersion = new JTextField("", 20);
		txtSwVersion.setEditable(false);
		gbc.gridx = 0;
		gbc.gridy = 4;
		statusPanel.add(lblSwVersion, gbc);
		gbc.gridx = 1;
		gbc.gridy = 4;
		statusPanel.add(txtSwVersion, gbc);

		gbc.insets.top = 15;
		lblBattery = new JLabel(CConstants.LABEL_BATTERY, SwingConstants.LEFT);
		pgbBattery = new JProgressBar(0, 100);
		pgbBattery.setStringPainted(true);
		gbc.gridx = 0;
		gbc.gridy = 5;
		statusPanel.add(lblBattery, gbc);
		gbc.gridx = 1;
		gbc.gridy = 5;
		statusPanel.add(pgbBattery, gbc);

		gbc.insets.top = 5;
		lblSignal = new JLabel(CConstants.LABEL_SIGNAL, SwingConstants.LEFT);
		pgbSignal = new JProgressBar(0, 100);
		pgbSignal.setStringPainted(true);
		gbc.gridx = 0;
		gbc.gridy = 6;
		statusPanel.add(lblSignal, gbc);
		gbc.gridx = 1;
		gbc.gridy = 6;
		statusPanel.add(pgbSignal, gbc);

		gbc.insets.top = 15;
		gbc.insets.bottom = 8;
		lblStatus = new JLabel(CConstants.LABEL_STATUS, SwingConstants.LEFT);
		txtStatus = new JTextField(CConstants.STATUS_DISCONNECTED, 20);
		txtStatus.setEditable(false);
		txtStatus.setHorizontalAlignment(SwingConstants.CENTER);
		txtStatus.setFont(new Font("SansSerif", Font.BOLD, 12));
		gbc.gridx = 0;
		gbc.gridy = 7;
		statusPanel.add(lblStatus, gbc);
		gbc.gridx = 1;
		gbc.gridy = 7;
		statusPanel.add(txtStatus, gbc);
		statusPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), CConstants.BORDER_MOBILE_INFORMATION));
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.weightx = 100;
		gbc.weighty = 100;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.insets.left = 5;
		gbc.insets.top = 5;
		getContentPane().add(statusPanel, gbc);

		gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.weightx = 100;
		gbc.weighty = 100;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.insets.left = 5;
		gbc.insets.top = 5;
		gbc.insets.right = 5;
		JPanel incomingPanel = new JPanel();
		incomingPanel.setLayout(new GridBagLayout());
		lblInFrom = new JLabel(CConstants.LABEL_INCOMING_FROM, SwingConstants.LEFT);
		txtInFrom = new JTextField("", 16);
		txtInFrom.setEditable(false);
		gbc.gridx = 0;
		gbc.gridy = 0;
		incomingPanel.add(lblInFrom, gbc);
		gbc.gridx = 1;
		gbc.gridy = 0;
		incomingPanel.add(txtInFrom, gbc);

		lblInDate = new JLabel(CConstants.LABEL_INCOMING_DATE, SwingConstants.LEFT);
		txtInDate = new JTextField("", 20);
		txtInDate.setEditable(false);
		gbc.gridx = 0;
		gbc.gridy = 1;
		incomingPanel.add(lblInDate, gbc);
		gbc.gridx = 1;
		gbc.gridy = 1;
		incomingPanel.add(txtInDate, gbc);

		gbc.insets.bottom = 19;
		lblInText = new JLabel(CConstants.LABEL_INCOMING_TEXT, SwingConstants.LEFT);
		txtInText = new JTextArea(8, 20);
		txtInText.setEditable(false);
		txtInText.setLineWrap(true);
		gbc.gridx = 0;
		gbc.gridy = 2;
		incomingPanel.add(lblInText, gbc);
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.gridheight = 8;
		incomingPanel.add(txtInText, gbc);
		incomingPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), CConstants.BORDER_INCOMING_MESSAGES));
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.weightx = 100;
		gbc.weighty = 100;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.insets.left = 5;
		gbc.insets.top = 5;
		gbc.insets.bottom = 5;
		getContentPane().add(incomingPanel, gbc);

		gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.weightx = 100;
		gbc.weighty = 100;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.insets.left = 5;
		gbc.insets.top = 5;
		gbc.insets.right = 5;
		JPanel outgoingPanel = new JPanel();
		outgoingPanel.setLayout(new GridBagLayout());
		lblOutTo = new JLabel(CConstants.LABEL_OUTGOING_TO, SwingConstants.LEFT);
		txtOutTo = new JTextField("", 16);
		txtOutTo.setEditable(false);
		gbc.gridx = 0;
		gbc.gridy = 0;
		outgoingPanel.add(lblOutTo, gbc);
		gbc.gridx = 1;
		gbc.gridy = 0;
		outgoingPanel.add(txtOutTo, gbc);

		lblOutDate = new JLabel(CConstants.LABEL_OUTGOING_DATE, SwingConstants.LEFT);
		txtOutDate = new JTextField("", 20);
		txtOutDate.setEditable(false);
		gbc.gridx = 0;
		gbc.gridy = 1;
		outgoingPanel.add(lblOutDate, gbc);
		gbc.gridx = 1;
		gbc.gridy = 1;
		outgoingPanel.add(txtOutDate, gbc);

		gbc.insets.bottom = 19;
		lblOutText = new JLabel(CConstants.LABEL_OUTGOING_TEXT, SwingConstants.LEFT);
		txtOutText = new JTextArea(8, 20);
		txtOutText.setEditable(false);
		txtOutText.setLineWrap(true);
		gbc.gridx = 0;
		gbc.gridy = 2;
		outgoingPanel.add(lblOutText, gbc);
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.gridheight = 8;
		outgoingPanel.add(txtOutText, gbc);
		outgoingPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), CConstants.BORDER_OUTGOING_MESSAGES));
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.weightx = 150;
		gbc.weighty = 150;
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.insets.left = 5;
		gbc.insets.top = 5;
		gbc.insets.bottom = 5;
		getContentPane().add(outgoingPanel, gbc);

		gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.weightx = 100;
		gbc.weighty = 100;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.insets.left = 5;
		gbc.insets.top = 5;
		gbc.insets.right = 5;
		JPanel statsPanel = new JPanel();
		statsPanel.setLayout(new GridBagLayout());
		lblUpSince = new JLabel(CConstants.LABEL_UP_SINCE, SwingConstants.LEFT);
		txtUpSince = new JTextField("", 17);
		txtUpSince.setEditable(false);
		gbc.gridx = 0;
		gbc.gridy = 0;
		statsPanel.add(lblUpSince, gbc);
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.EAST;
		statsPanel.add(txtUpSince, gbc);
		gbc.anchor = GridBagConstraints.WEST;

		lblTraffic = new JLabel(CConstants.LABEL_TRAFFIC, SwingConstants.LEFT);
		lblTrafficIn = new JLabel(CConstants.LABEL_TRAFFIC_IN, SwingConstants.LEFT);
		lblTrafficOut = new JLabel(CConstants.LABEL_TRAFFIC_OUT, SwingConstants.LEFT);
		txtTrafficIn = new JTextField(CConstants.TEXT_ZERO, 5);
		txtTrafficIn.setEditable(false);
		txtTrafficIn.setHorizontalAlignment(SwingConstants.RIGHT);
		txtTrafficOut = new JTextField(CConstants.TEXT_ZERO, 5);
		txtTrafficOut.setEditable(false);
		txtTrafficOut.setHorizontalAlignment(SwingConstants.RIGHT);
		JPanel tempPanel = new JPanel();
		tempPanel.add(lblTrafficIn);
		tempPanel.add(txtTrafficIn);
		tempPanel.add(lblTrafficOut);
		tempPanel.add(txtTrafficOut);
		gbc.gridx = 0;
		gbc.gridy = 1;
		statsPanel.add(lblTraffic, gbc);
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.EAST;
		statsPanel.add(tempPanel, gbc);

		lblInterfaces = new JLabel(CConstants.LABEL_INTERFACES, SwingConstants.LEFT);
		lblInterfaceDB = new JLabel(CConstants.LABEL_INTERFACE_DB_OFF, SwingConstants.LEFT);
		lblInterfaceXML = new JLabel(CConstants.LABEL_INTERFACE_XML_OFF, SwingConstants.LEFT);
		lblInterfaceRMI = new JLabel(CConstants.LABEL_INTERFACE_RMI_OFF, SwingConstants.LEFT);
		JPanel tempPanel1 = new JPanel();
		tempPanel1.add(lblInterfaceDB);
		tempPanel1.add(lblInterfaceXML);
		tempPanel1.add(lblInterfaceRMI);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx = 0;
		gbc.gridy = 2;
		statsPanel.add(lblInterfaces, gbc);
		gbc.anchor = GridBagConstraints.EAST;
		gbc.gridx = 1;
		gbc.gridy = 2;
		statsPanel.add(tempPanel1, gbc);

		lblRawLogs = new JLabel(CConstants.LABEL_RAW_LOGS, SwingConstants.LEFT);
		lblRawInLog = new JLabel(CConstants.LABEL_IN_RAW_LOG_OFF, SwingConstants.LEFT);
		lblRawOutLog = new JLabel(CConstants.LABEL_OUT_RAW_LOG_OFF, SwingConstants.LEFT);
		JPanel tempPanel2 = new JPanel();
		tempPanel2.add(lblRawInLog);
		tempPanel2.add(lblRawOutLog);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx = 0;
		gbc.gridy = 3;
		statsPanel.add(lblRawLogs, gbc);
		gbc.anchor = GridBagConstraints.EAST;
		gbc.gridx = 1;
		gbc.gridy = 3;
		statsPanel.add(tempPanel2, gbc);
		statsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), CConstants.BORDER_STATISTICS));
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.weightx = 100;
		gbc.weighty = 100;
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.insets.left = 5;
		gbc.insets.top = 5;
		gbc.insets.bottom = 5;
		getContentPane().add(statsPanel, gbc);

		service.initialize();
	}

	public void setStatusText(String text)
	{
		this.txtStatus.setText(text);
		repaint();
	}

	public void setManufText(String text)
	{
		this.txtManuf.setText(text);
	}

	public void setModelText(String text)
	{
		this.txtModel.setText(text);
	}

	public void setSerialNoText(String text)
	{
		this.txtSerialNo.setText(text);
	}

	public void setIMSIText(String text)
	{
		this.txtIMSI.setText(text);
	}

	public void setSwVersionText(String text)
	{
		this.txtSwVersion.setText(text);
	}

	public void setBatteryIndicator(int battery)
	{
		this.pgbBattery.setValue(battery);
	}

	public void setSignalIndicator(int signal)
	{
		this.pgbSignal.setValue(signal);
	}

	public void setInFrom(String inFrom)
	{
		this.txtInFrom.setText(inFrom);
	}

	public void setInDate(String inDate)
	{
		this.txtInDate.setText(inDate);
	}

	public void setInText(String inText)
	{
		this.txtInText.setText(inText);
	}

	public void setOutTo(String outTo)
	{
		this.txtOutTo.setText(outTo);
	}

	public void setOutDate(String outDate)
	{
		this.txtOutDate.setText(outDate);
	}

	public void setOutText(String outText)
	{
		this.txtOutText.setText(outText);
	}

	public void setTrafficIn(int number)
	{
		txtTrafficIn.setText("" + number);
	}

	public void setTrafficOut(int number)
	{
		txtTrafficOut.setText("" + number);
	}

	public void setInterfaceDB(boolean on)
	{
		this.lblInterfaceDB.setText((on ? CConstants.LABEL_INTERFACE_DB_ON : CConstants.LABEL_INTERFACE_DB_OFF));
	}

	public void setInterfaceXML(boolean on)
	{
		this.lblInterfaceXML.setText((on ? CConstants.LABEL_INTERFACE_XML_ON : CConstants.LABEL_INTERFACE_XML_OFF));
	}

	public void setRawInLog(boolean on)
	{
		this.lblRawInLog.setText((on ? CConstants.LABEL_IN_RAW_LOG_ON : CConstants.LABEL_IN_RAW_LOG_OFF));
	}

	public void setRawOutLog(boolean on)
	{
		this.lblRawOutLog.setText((on ? CConstants.LABEL_OUT_RAW_LOG_ON : CConstants.LABEL_OUT_RAW_LOG_OFF));
	}

	public void setConnected(boolean connected)
	{
		if (connected)
		{
			mniConnect.setEnabled(false);
			mniDisconnect.setEnabled(true);
			setStatusText(CConstants.STATUS_CONNECTED);
			txtUpSince.setText(new java.util.Date().toString());
		}
		else
		{
			mniConnect.setEnabled(true);
			mniDisconnect.setEnabled(false);
			setStatusText(CConstants.STATUS_DISCONNECTED);

			setTrafficIn(0);
			setTrafficOut(0);
			txtUpSince.setText("");

			setManufText("");
			setModelText("");
			setSerialNoText("");
			setIMSIText("");
			setSwVersionText("");
			setBatteryIndicator(0);
			setSignalIndicator(0);
			setInFrom("");
			setInDate("");
			setInText("");
			setOutTo("");
			setOutDate("");
			setOutText("");
		}
	}

	public CMainThread getService()
	{
		return service;
	}

	static class CAboutDialog extends JDialog
	{
		private static final long serialVersionUID = -6832700576136038240L;

		public CAboutDialog(JFrame owner)
		{
			super(owner, "About SMSEngine", true);

			JPanel buttonPanel;
			JPanel infoPanel1, infoPanel11, infoPanel2, infoPanel3, infoPanel4, infoPanel5;
			Box vBox;
			Container contentPane;
			JButton btnOk;

			contentPane = getContentPane();

			infoPanel1 = new JPanel();
			infoPanel1.setLayout(new FlowLayout());
			infoPanel1.add(new JLabel(CConstants.ABOUT_VERSION));
			infoPanel11 = new JPanel();
			infoPanel11.setLayout(new FlowLayout());
			infoPanel11.add(new JLabel(CConstants.ABOUT_BASED));
			infoPanel2 = new JPanel();
			infoPanel2.setLayout(new FlowLayout());
			infoPanel2.add(new JLabel(CConstants.ABOUT_BY));
			infoPanel3 = new JPanel();
			infoPanel3.setLayout(new FlowLayout());
			infoPanel3.add(new JLabel(CConstants.ABOUT_WEBPAGE));
			infoPanel4 = new JPanel();
			infoPanel4.setLayout(new FlowLayout());
			infoPanel4.add(new JLabel(CConstants.ABOUT_EMAIL));
			infoPanel5 = new JPanel();
			infoPanel5.setLayout(new FlowLayout());
			infoPanel5.add(new JLabel(CConstants.ABOUT_OTHER));

			vBox = Box.createVerticalBox();
			vBox.add(infoPanel1);
			vBox.add(infoPanel11);
			vBox.add(infoPanel2);
			vBox.add(Box.createVerticalStrut(5));
			vBox.add(infoPanel3);
			vBox.add(infoPanel4);
			vBox.add(Box.createVerticalStrut(10));
			vBox.add(infoPanel5);
			contentPane.add(vBox, BorderLayout.CENTER);

			buttonPanel = new JPanel();
			btnOk = new JButton("Ok");
			buttonPanel.add(btnOk);
			contentPane.add(buttonPanel, BorderLayout.SOUTH);

			btnOk.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent event)
				{
					setVisible(false);
				}
			});

			setSize(350, 300);
			setResizable(false);
			setLocation(owner.getLocation().x + 170, owner.getLocation().y + 30);
		}
	}
}
