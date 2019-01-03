SMSServer for Java

Welcome!

SMSServer for Java is a graphical user interface application, which uses SMSLib API, and may be used for sending and receiving SMS messages from your GSM modem. Although given as a sample app, it can be used right from the start as a quick way to start your SMS processing, if you don't want to mess around with the API. It is highly configurable, and have two interfaces by which you can communicate with it and send or receive messages without a single line of code. On the other hand, you may use it as a point of reference for your own implementations.


*** General Information ***
SMSServer will act as a standalone application which will periodically query your GSM device to get new incoming messages, and will dispatch messages outgoing messages given to it by you. To do something usefull with SMSServer, you must have a way to get and process the incoming messages that SMSServer reads from the GSM device, and to pass SMSServer with messages you need to be dispatched. There are two ways to do that:
1) SMSServer has a database (jdbc) link. Once enabled, SMSServer will put all received messages in a table called "sms_in" and will dispatch all messages found in table "sms_out".
2) SMSServer can read from and write to XML files. If the XML link is enabled, all incoming messages read by your GSM device will be put in a directory as XML files, whereas another directory will be scanned for XML files (put there by you) and will dispatch them.


*** Installation ***
The installation is straight-forward. Copy the SMSServer.jar and SMSServer.conf in a directory of its own. Since SMSServer is based on SMSLib API, you should have already installed SMSLib.jar file (in classpath or Java's ext directories) and the appropriate Java Comm package (JavaComm or RxTx).
SMSServer should run without trouble.


*** Configuration ***
The configuration file is "SMSServer.conf" (should be placed in the same directory). In this file, you may find a number of configuration parameters, which you may alter to suit your needs. These are:

general.gui: Either "yes" or "no". "yes" enabled the graphical interface of SMSServer. "no" forces it to run in console/text mode.

general.raw_in_log: A valid filename (incl. path) which will act as a raw log for incoming messages. May be blank.

general.raw_out_log: A valid filename (incl. path) which will act as a raw log for outgoing messages. May be blank.

phone.manufacturer: Your phone's manufacturer, e.g. Nokia, Siemens, etc.

phone.model: You phone modem, e.g. 6310i, C55, etc.

phone.interval: A number (representing seconds) which determines the time that SMSServer sleeps between two processing cycles. Each processing cycle will query the GSM device for incoming messages, and send any messages waiting to be dispatched. Do not set this value too low, recommended values are from 15 seconds and up.

phone.delete_after_processing: One of the values "yes" and "no". A "yes" means that incoming messages will be deleted from GSM device's memory after having been read from SMSServer. A "no" means that messages will be left in GSM device's memory, and will be re-processed on the next processing cycle.

phone.phone_book: The full path name of the XML file which contains the phonebook. May be blank.

phone.xml_in_queue: The directory which will serve as a queue for XML files representing the incoming messages received by SMSServer. May be blank.

phone.xml_out_queue: The directory which will serve as a queue for XML files, which will be created by you. This directory will be scanned from SMSServer, and the XML files will be sent through your GSM device.

phone.operation_mode: Should have one of the values "pdu" and "ascii". This denoted the operation mode in which SMSServer is working. It is recommended to leave it to "pdu" value, since all devices support the PDU operation (whereas ASCII is optional and implemented in newer devices), and because some features require the PDU operation (now and in future versions where Smart Messaging will be implemented).

phone.smsc_number: The SMSC (message center) number, in international format (for example, I use  +3097100000 here in Greece / COSMOTE). You may leave this setting blank. If you leave it blank, the SMSC number already stored in the device will be used. This option is used *only* when in PDU mode.

phone.message_encoding: The encoding for every outgoing messages. Each message dispatched by SMSServer (both from XML or database links) will be forced to have this encoding. You should define it with one of the values "7bit", "8bit" or "unicode". Default is 7bit. This indication is useful only when working in PDU mode.

phone.sim_pin: The SIM PIN number of your GSM device. Optional - if you want SMSServer to automatically log-in to your GSM device.

phone.forward_number: If set to a valid phone number, all incoming messages will be automatically forwarded to this phone.

phone.protocol: Should be set to "pdu" or "text" and determines the operation mode of SMSLib.

database.enabled: One of the values "yes" and "no". A value of "yes" means that the database link is enabled.

database.type: The type of the database. Currently one of the values "sql92" and "mysql". If you are going to use a different database than the ones above, it may be necessary to review the class CDatabase.java, because it may be possible that some sql statements need re-writing.

database.url: The url of the database.

database.driver: The driver class of the database.

database.username: The username of the database. Depends on actual RDBMS implementation.

database.password: The password of the database. Depends on actual RDBMS implementation.

serial.port: The PC serial port connected to the mobile phone eg. com1.

serial.baud: The baud rate of modem interface on the phone. 19200 is a good starting value.

You may also review the conf file given with SMSServer as an example.
SMSServer can be invoked with "-Dsmsserver.configdir=xxx" directive, where xxx should point to the directory where the configuration file resides.


*** Database Link ***
If you enable the database link (by setting all necessary database.xxx configuration values in SMSServer.conf), SMSServer will perform the following tasks on every processing cycle:
1) It will store every SMS message it receives from you GSM device to the table "sms_in".
2) It will query the table "sms_out" for new records, and will send the information from these records as SMS. It will also save messages sent through its main processing loop in this table.

The structure of the "sms_in" table is:
	type			char(1)			"I" for incoming messages, "S" for status report messages.
	originator		char(32)		the originator's phone number.
	message_date	datetime		the date/time of the SMS message.
	text				char(nnn)		the actual text of the SMS message.
	ref_no			number		Only for status report messages. The reference number of the message that this status report message refers to.
	original_send_date	datetime	Only for status report messages. The sent date of the sent message that this status report message refers to.
	date_received	datetime		Only for status report messages. The receipt-date-by-recipient of the sent message that this status report message refers to.

The "incoming" table is a "write-only" table as far as SMSServer is concerned. You may wish to add a key field (autonumber, sequence field, etc. depending on your actual database). It is your responsibility to read this table, process its messages, and delete them if you don't need them.

The structure of the "sms_out" table is:
	id						number		the id of the SMS message.
	recipient				char(32)		the recipient's phone number.
	text						char(nnn)		the actual text of the SMS message.
	dispatch_date			datetime		the dispatch date.
	status_report			number		1 : request a status report, 0 : do not request status report.
	flash_sms				number		1 : set the SMS as a Flash SMS, 0 : normal SMS.
	src_port				number		The message source port (16bit). Set to -1 for no source port.
	dst_port				number		The message destination port (16bit). Set to -1 for no destination port.
	validity_period	number	The validity period in hours. Set to -1 for max validity period.

The following sample script can be used to create the two tables on a MySQL database server:
*** START OF SCRIPT ***
	CREATE DATABASE sms;
	USE sms;
	
	DROP TABLE IF EXISTS `sms_in`;
	CREATE TABLE `sms_in` (
	 `message_date` datetime default NULL,
	 `originator` varchar(45) NOT NULL default '',
	 `text` text NOT NULL,
	 PRIMARY KEY  (`message_date`)
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;
	
	DROP TABLE IF EXISTS `sms_out`;
	CREATE TABLE `sms_out` (
	 `id` int(10) unsigned NOT NULL auto_increment,
	 `recipient` varchar(45) NOT NULL default '',
	 `text` text NOT NULL,
	 `dispatch_date` datetime default NULL,
	 `status_report` int(10) unsigned NOT NULL default '0',
	 `flash_sms` int(10) unsigned NOT NULL default '0',
	 `src_port` int(11) NOT NULL default '0',
	 `dst_port` int(11) NOT NULL default '0',
	 `validity_period` int(11) NOT NULL default '0',
	 PRIMARY KEY  (`id`)
	) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
*** END OF SCRIPT ***

When you want to send an SMS, just insert a record and fill in the above fields. "id" is the key of the table - it is your responsibility to care for its uniqueness. Also, remember to set "dispatch_date" to null. During each processing cycle, SMSServer will query the "outgoing" table for rows having "dispatch_date" null, and will dispatch these messages. After sucessfull dispatch, SMSServer will update the "dispatch_date" with the date/time of the dispatch, otherwise it will leave it blank and will try to re-dispatch it during the next processing cycle.


*** XML Link ***
You can enable the XML link for incoming / outgoing messages by setting appropriate values in "phone.xml_in_queue" and "phone.xml_out_queue" configuration items respectively. If you do, SMSServer will perform the following tasks on every processing cycle:
1) It will store every SMS message it receives from your GSM device to the directory denoted by "phone.xml_in_queue" parameter. Each message will be stored as an XML file, with a unique filename.
2) It will scan the directory denoted by "phone.xml_out_queue" parameter. Each XML file found in this directory will be send through as an SMS message. After successful dispatch, the XML file will be deleted from the queue directory.

An incoming XML file has the following structure:
	<?xml version='1.0' encoding='iso-8859-7'?>
	<message>
		<originator>+3069...</originator>
		<date>2003/02/24 11:07:16</date>
		<text> <![CDATA[ Hello SMSEngine! ]]> </text>
	</message>

An outgoing XML file *should* have the following structure:
	<?xml version='1.0' encoding='iso-8859-7'?>
	<message>
		<recipient>+30697...</recipient>
		<validity>10</validity>
		<source_port>1000</source_port>
		<destination_port>2000</destination_port>
		<flash_sms/>
		<text> <![CDATA[ Hello from SMSEngine. ]]> </text>
	</message>
The actual XML filename does not matter. The "recipient" and "text" tags are the minimum required. The rest are used on demand.

It is your responsibility to read incoming XML files from the incoming directory queue and process them, and put XML files in the outgoing directory queue in order to be dispatched. Once processed, you may delete XML files from the incoming directory, in order to save space.
