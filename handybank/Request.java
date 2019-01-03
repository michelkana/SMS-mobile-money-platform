
//

package handybank;

import java.io.*;
import org.smslib.*;
import java.text.SimpleDateFormat;
import java.util.*;
import smsmodule.*;
import database.*;
import java.util.HashMap;

public class Request extends Thread
{
  public static final int PUT = 0;
	public static final int GET = 1;
	private MessageFile msgFile;
	private CIncomingMessage message;
	private String messageText;
	//private String regEx = "(\\d{4}) ((TRAN \\d{8} \\d+)|(CHAR \\d+)|(CHAN \\d{4})|(QUES)|(LOCK))";
	private String regEx = "(REGISTER [12])|((\\d{4}) ((TRAN \\d+ \\d+)|(CHAR \\d+)|(CHAN \\d{4})|(QUES)|(LOCK)))";
	private static final int TRAN = 1;
	private static final int CHAR = 2;
	private static final int CHAN = 3;
	private static final int QUES = 4;
	private static final int LOCK = 5;
	private static final int REGISTER = 6;
	private int requestType;
	private String pin;
	private String destination = null;
	private String origine;
	private int amount;
	private String prepaidcardnumber;
	private String newpin;
	private String logFilename;
	private String toUser;
	private String toDestination;
	private String toSystem;
	private String outPoolDirectory;
 	private	String smsFileextension;
 	private String dbDriver = "com.mysql.jdbc.Driver";
	private String dbURL = "jdbc:mysql://localhost:3306/handybank";
	private String dbUser = "root";
	private String dbPwd = "";
	private SQLWrapper sqlWrapper;
	private String datetime;
	SimpleDateFormat df; 
	String sql;
	String IDTransaction;
	String dbBalance;
	String dbPin;
	String dbNbWrongPin;
	String IDCategory;
	String name;
	String birthdate = "0000-00-00 00:00:00";
	String birthplace = "N/A";
	String idcnumber = "N/A";
	String adress = "N/A";
	String city = "N/A";
	boolean crypto;
	boolean noinstance = false;
	String encryptedPin;
	HashMap requestParams;
	
	public Request(MessageFile msgF, String logDir, String outPool, String smsFileext, boolean encrypt, String dDriver, String dURL, String dUser, String dPwd){
		try{
			msgFile = msgF;
			message = new CIncomingMessage();
			message.fromString(msgF.getFileContent());
			messageText = message.getText();
			origine = message.getOriginator().replace("+","00");
			String[] str = msgFile.getFilename().split("\\\\");
			logFilename = logDir + str[str.length-1];
			outPoolDirectory = outPool;
			smsFileextension = smsFileext;
			crypto = encrypt;						
			dbDriver = dDriver;
			dbURL = dURL;
			dbUser = dUser;
			dbPwd = dPwd;
			File dir = new File(logDir);
			if ( ! dir.exists() ){
				try{
					dir.mkdirs(); 
				}catch (Exception e)
				{
					System.out.println("Could not create log directory " + logDir);
					e.printStackTrace();
				}			
			}
			dir = new File(outPool);
			if ( ! dir.exists() ){
				try{
					dir.mkdirs(); 
				}catch (Exception e)
				{
					System.out.println("Could not create outgoing pool directory " + outPool);
					e.printStackTrace();
				}			
			}
	  }
		catch(Exception e){	
			logerror();		
		}
	}
	
	public Request(int reqType, HashMap reqParams , String sessionID, String logDir, String outPool, String smsFileext, boolean encrypt){
		try{
			requestType = reqType;
			requestParams = reqParams;
			origine = (String) requestParams.get("origine");
			pin = (String) requestParams.get("pin");
			SimpleDateFormat df = new SimpleDateFormat( "yyyyMMdd_HHmmss" );
			logFilename = logDir + df.format(new Date()) + "_" + sessionID;
			outPoolDirectory = outPool;
			smsFileextension = smsFileext;
			crypto = encrypt;						
	  }
		catch(Exception e){	
			logerror();		
		}
	}
	
	private void logerror(){
		try{	 
			noinstance = true;
	    toSystem = "!!!! Error during Request Creation !!!! \n";
	    logFilename = logFilename + ".reqErr";
			File logFile = new File(logFilename);
			BufferedWriter out = new BufferedWriter(
	                           new FileWriter( logFile ) );		
	    Cryptograph cryptograph = new Cryptograph(crypto);
			cryptograph.encryptFile(logFilename, toSystem + msgFile.getFileContent());
			System.out.println(toSystem);
			System.out.println("Log to " + logFilename + "\n");
		}
		catch(Exception f){	
			System.out.println("\n!!! Error during Request Creation. Logging not possible !!!\n Log to " + logFilename);
		}	
	}
	
	public boolean parse(){
		if ( ! messageText.matches(regEx) )
			return false;
		else{
			String[] fields = messageText.split("[ \t]");
			if ( messageText.contains("REGISTER") ){
				requestType = REGISTER;
				IDCategory = fields[1];
			}else{						
				pin = fields[0];
				if ( messageText.contains("TRAN")){
					requestType = TRAN;				
					destination = fields[2];
					amount = Integer.valueOf(fields[3]).intValue();
				} else if ( messageText.contains("CHAR")){
						requestType = CHAR;
						prepaidcardnumber = fields[2];
				}else if ( messageText.contains("CHAN")){
						requestType = CHAN;
						newpin = fields[2];
				} else if ( messageText.contains("QUES")){
						requestType = QUES;
				} else if ( messageText.contains("LOCK")){
						requestType = LOCK;
				}		
			}	
		}
		return true;
	}
	
	public void run()
	{
		if ( ! noinstance ){
			System.out.println("## TRANSACTION BEGIN ##\n");
			try
			{
				boolean res;
							
				if ( ! parse() ){
					logFilename = logFilename + ".parseErr";			
					toSystem = "\tParse Error\n";
					toUser = "ERROR, Check the syntaxe in \"" + messageText + "\" and try again";
				}else{
					sqlWrapper = new SQLWrapper(dbURL, dbUser, dbPwd, dbDriver);
					switch ( requestType ){						
						case TRAN: res = transfer(); break;
						case CHAR: res = charge(); break;
						case CHAN: res = changepin(); break;
						case QUES: res = queryaccount(); break;
						case LOCK: res = lockaccount(); break;
						case REGISTER: res = register(); break;
						default: {res = false; toSystem = "Unknown Request Type\n"; toUser = "ERROR: unknown request type";}
					}
					if ( ! res ){
						logFilename = logFilename + ".execErr";	
					}else{
						logFilename = logFilename + ".execOK";
					}
				}
				
				COutgoingMessage responseMsg = new COutgoingMessage(origine, toUser);
				SimpleDateFormat df = new SimpleDateFormat( "yyyyMMdd_HHmmssSSS" );
				String smsFilename = df.format(message.getDate()) + "." + smsFileextension;
				MessageFile responseMsgFile = new MessageFile(smsFilename, responseMsg.toString());
				MessagePool messagePool = new MessagePool(outPoolDirectory, smsFileextension, crypto);
				responseMsgFile = messagePool.accessOutgoingPool(PUT, responseMsgFile);		
				String smsFilenameDest = "";				
				if ( destination != null ){
					COutgoingMessage responseMsgDest = new COutgoingMessage(destination, toDestination);
					df = new SimpleDateFormat( "yyyyMMdd_HHmmssSSS" );
					smsFilenameDest = df.format(message.getDate()) + "." + smsFileextension;
					MessageFile responseMsgFileDest = new MessageFile(smsFilenameDest, responseMsgDest.toString());
					responseMsgFileDest = messagePool.accessOutgoingPool(PUT, responseMsgFileDest);
				}			
					
				Thread.sleep(10000);		
					
				if ( messagePool.exists(smsFilename+".txErr") ){
					toSystem = toSystem + "\tRespTx: sending response to user failed\n";
					logFilename = logFilename + ".txErr"; 
				}else if ( messagePool.exists(smsFilename+".txOK") ){
					toSystem = toSystem + "\tRespTx: sending response to user successfully\n";
					logFilename = logFilename + ".txOK"; 
				}else{
					toSystem = toSystem + "\tRespTx: no status returned from TX\n";
					logFilename = logFilename + ".txNA"; 
				}
				
				if ( destination != null ){
					if ( messagePool.exists(smsFilenameDest+".txErr") ){
						toSystem = toSystem + "\tRespTx: sending response to destinator failed\n";
						logFilename = logFilename + ".txErr"; 
					}else if ( messagePool.exists(smsFilenameDest+".txOK") ){
						toSystem = toSystem + "\tRespTx: sending response to destinator successfully\n";
						logFilename = logFilename + ".txOK"; 
					}else{
						toSystem = toSystem + "\tRespTx: no status returned from TX\n";
						logFilename = logFilename + ".txNA"; 
					}					
				}
				
				
				File logFile = new File(logFilename);
				BufferedWriter out = new BufferedWriter(
		                           new FileWriter( logFile ) );	 
		    toUser = toUser + "\n";
		    Cryptograph cryptograph = new Cryptograph(crypto);
				cryptograph.encryptFile(logFilename, toUser + toSystem + msgFile.getFileContent());
				//out.write(toUser, 0, toUser.length());     
				//out.write(toSystem, 0, toSystem.length());
				//out.write(msgFile.getFileContent(), 0, msgFile.getFileContent().length());
				//out.close();			
				
				System.out.println("USER REQUEST:  " + messageText + "\n");
				System.out.println("USER RESPONSE: " + toUser);
				System.out.println("SYSTEM LOG:\n" + toSystem);
				System.out.println("## TRANSACTION END ##\n");
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private void transactionInit(String reqType){
		df = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
		datetime = df.format(new Date());		
		sql = "INSERT INTO transactionhistory (IDAccount, RequestType, StartTime, Status) VALUES ('" + origine + "', '" + reqType + "', '" + datetime + "', 'starting');";
		sqlWrapper.runQuery(sql);
		sql = "SELECT IDTransaction FROM transactionhistory WHERE IDAccount ='" + origine + "' AND RequestType = '" + reqType + "' AND StartTime = '" + datetime + "' AND Status = 'starting';";
		sqlWrapper.runQuery(sql);		
		IDTransaction = sqlWrapper.getFieldValueResultSet(1, "IDTransaction");
	}
	
	private boolean accountPINCheck(){
		boolean res = false;
		sql = "SELECT * FROM account WHERE IDAccount = '" + origine + "' AND NOT Locked ;";		
		sqlWrapper.runQuery(sql);
		dbBalance = sqlWrapper.getFieldValueResultSet(1, "Balance");
		dbPin = sqlWrapper.getFieldValueResultSet(1, "Pin");		
		dbNbWrongPin = sqlWrapper.getFieldValueResultSet(1, "WrongPIN");	
		int sizeResultSet = sqlWrapper.getSizeResultSet();
		if ( sizeResultSet < 1 ){
			toSystem += "\tDB-Error: Account is locked or doesn't exist\n";
			toUser = "ERROR, your account is locked or doesn't exist in our system. Please register first.";
		}else
		if ( sizeResultSet > 1 ){
			toSystem += "\tDB-Error: More then 1 account with the same phone number exist\n";
			toUser = "ERROR, system failure. Please try again.";
		}else				
		if ( ! dbPin.equals(pin) ){
			if ( Integer.valueOf(dbNbWrongPin).intValue() >= 2 ){
				toSystem += "\tError: user entered a wrong PIN three times. Account is blocked.\n";
				toUser = "ERROR, you entered a wrong PIN three times. Account is blocked.";
				sql = "UPDATE account  SET Locked = 1, WrongPIN = 3 WHERE IDAccount = '" + origine + "';";
				sqlWrapper.runQuery(sql);
			}else{
				toSystem += "\tPIN-Error: wrong PIN\n";
				toUser = "ERROR, wrong pin. Please try again.";
				sql = "UPDATE account  SET WrongPIN = " + String.valueOf(Integer.valueOf(dbNbWrongPin).intValue()+1) + " WHERE IDAccount = '" + origine + "';";
				sqlWrapper.runQuery(sql);
			}
		}else{			
			res = true;
		}
		 
		return res;
	}
	
	private void transactionClose(boolean res){
		datetime = df.format(new Date());
		if ( res ){
			sql = "UPDATE transactionhistory SET EndTime = '" + datetime + "',  Status = 'success' WHERE IDTransaction = " + IDTransaction + ";";
			sqlWrapper.runQuery(sql);
		}else{
			sql = "UPDATE transactionhistory SET EndTime = '" + datetime + "',  Status = \"" + toUser + "\" WHERE IDTransaction = " + IDTransaction + ";";
			sqlWrapper.runQuery(sql);
		}
	}
	
	
	private boolean transfer(){
		
		transactionInit("TRAN");				
		sql = "INSERT INTO transactioninfos (IDTransaction, InfoType, Value) VALUES ('" + IDTransaction + "', 'origine', '" + origine + "');";
		sqlWrapper.runQuery(sql);
		sql = "INSERT INTO transactioninfos (IDTransaction, InfoType, Value) VALUES ('" + IDTransaction + "', 'destination', '" + destination + "');";
		sqlWrapper.runQuery(sql);
		sql = "INSERT INTO transactioninfos (IDTransaction, InfoType, Value) VALUES ('" + IDTransaction + "', 'amount', '" + amount + "');";
		sqlWrapper.runQuery(sql);
		sql = "INSERT INTO transactioninfos (IDTransaction, InfoType, Value) VALUES ('" + IDTransaction + "', 'pin', '" + pin + "');";
		sqlWrapper.runQuery(sql);			
		boolean trRes = false;		
		if ( accountPINCheck() ){						
			sql = "SELECT * FROM account WHERE IDAccount like '%" + destination + "%' AND NOT Locked ;";		
			sqlWrapper.runQuery(sql);			
			int sizeResultSet = sqlWrapper.getSizeResultSet();
			if ( sizeResultSet < 1 ){
				toSystem += "\tDB-Error: Destination account is locked or doesn't exist\n";
				toUser = "ERROR, destination account is locked or doesn't exist in our system.";
			}else
			if ( sizeResultSet > 1 ){
				toSystem += "\tDB-Error: More then 1 destination account with the same phone number exist\n";
				toUser = "ERROR, system failure. Please try again.";
			}else{				
				int newDestBalance = Integer.valueOf(sqlWrapper.getFieldValueResultSet(1, "Balance")).intValue() + amount;
				int newOrigBalance = Integer.valueOf(dbBalance).intValue() - amount;
				if ( newOrigBalance < 0 ){
					toSystem += "\tDB-Error: balance insufficient for the requested transfer\n";
					toUser = "ERROR, your balance insufficient for the requested transfer.";
				}else{
					sql = "UPDATE Account SET Balance = " + String.valueOf(newOrigBalance) + ", LastChange = '" + datetime + "' WHERE IDAccount like '%" + origine + "%';";
					sqlWrapper.runQuery(sql);		
					sql = "UPDATE Account SET Balance = " + String.valueOf(newDestBalance) + ", LastChange = '" + datetime + "' WHERE IDAccount like '%" + destination + "%';";
					sqlWrapper.runQuery(sql);			
					toSystem = "\tRequest: [Transfer From: " + origine + " To: " + destination + " Amount: " + amount + " Pin: " + pin + "]\n";
					toUser = "SUCCESS, " + amount + "FCFA were transfered to "  + destination + "; Your new balance is " + newOrigBalance + " FCFA";					
					toDestination = "You receive " + amount + "FCFA from "  + origine + "; Your new balance is " + newDestBalance + " FCFA";										
					trRes = true;		
				}
			}
		}		
		transactionClose(trRes);		
		return trRes;
	}
	
	private boolean charge(){
		
		transactionInit("CHAR");
				
		sql = "INSERT INTO transactioninfos (IDTransaction, InfoType, Value) VALUES ('" + IDTransaction + "', 'origine', '" + origine + "');";
		sqlWrapper.runQuery(sql);
		sql = "INSERT INTO transactioninfos (IDTransaction, InfoType, Value) VALUES ('" + IDTransaction + "', 'prepaidcardnumber', '" + prepaidcardnumber + "');";
		sqlWrapper.runQuery(sql);
		sql = "INSERT INTO transactioninfos (IDTransaction, InfoType, Value) VALUES ('" + IDTransaction + "', 'pin', '" + pin + "');";
		sqlWrapper.runQuery(sql);
		
		boolean trRes = false;
		
		if ( accountPINCheck() ){			
				sql = "SELECT * FROM prepaidcard WHERE Code LIKE '%" + prepaidcardnumber + "%' AND NOT Used AND ExpDate > '" + datetime + "';";
				sqlWrapper.runQuery(sql);
				int sizeResultSet = sqlWrapper.getSizeResultSet();
				if ( sizeResultSet < 1 ){
					toSystem += "\tDB-Error: Prepaidcard number doesn't exist, is used or is expired\n";
					toUser = "ERROR, prepaidcard number doesn't exist, is used or expired.";
				}else
				if ( sizeResultSet > 1 ){
					toSystem += "\tDB-Error: More then 1 prepaidcards with the same number exist\n";
					toUser = "ERROR, system failure. Please try again.";
				}else{
					String cardValue = sqlWrapper.getFieldValueResultSet(1, "Value");
					int newBalance = Integer.valueOf(dbBalance).intValue()+Integer.valueOf(cardValue).intValue();
					sql = "UPDATE Account SET Balance = " + String.valueOf(newBalance) + ", LastChange = '" + datetime + "' WHERE IDAccount like '%" + origine + "%';";
					sqlWrapper.runQuery(sql);
					sql = "UPDATE prepaidcard SET Used = 1, UsedDate = '" + datetime + "' WHERE Code = '" + prepaidcardnumber + "';";
					sqlWrapper.runQuery(sql);				
					toSystem = "\tRequest: [Charge Amount From: " + origine + " PrepaidCard: " + prepaidcardnumber + " Pin: " + pin + "]\n";
					toUser = "SUCCESS, account charged with " + cardValue + " FCFA; New balance is " + newBalance;  
					trRes = true;
				}
		}		
		
		transactionClose(trRes);
		
		return trRes;
	}
	
	private boolean changepin(){				
		transactionInit("CHAN");				
		sql = "INSERT INTO transactioninfos (IDTransaction, InfoType, Value) VALUES ('" + IDTransaction + "', 'origine', '" + origine + "');";
		sqlWrapper.runQuery(sql);
		sql = "INSERT INTO transactioninfos (IDTransaction, InfoType, Value) VALUES ('" + IDTransaction + "', 'oldpin', '" + pin + "');";
		sqlWrapper.runQuery(sql);
		sql = "INSERT INTO transactioninfos (IDTransaction, InfoType, Value) VALUES ('" + IDTransaction + "', 'newpin', '" + newpin + "');";
		sqlWrapper.runQuery(sql);		
		boolean trRes = false;		
		if ( accountPINCheck() ){			
			sql = "UPDATE account  SET Pin = '" + newpin + "', LastChange = '" + datetime + "' WHERE IDAccount = '" + origine + "';";
			sqlWrapper.runQuery(sql);	
			toSystem = "\tRequest: [Change PIN From: " + origine + " New Pin: " + newpin + " Old Pin: " + pin + "]\n";
			toUser = "SUCCESS, pin was changed to " + newpin;
			trRes = true;		
		}		
		transactionClose(trRes);		
		return trRes;
	}
	
	private boolean queryaccount(){		
  	transactionInit("QUES");				
		sql = "INSERT INTO transactioninfos (IDTransaction, InfoType, Value) VALUES ('" + IDTransaction + "', 'origine', '" + origine + "');";
		sqlWrapper.runQuery(sql);
		sql = "INSERT INTO transactioninfos (IDTransaction, InfoType, Value) VALUES ('" + IDTransaction + "', 'pin', '" + pin + "');";
		sqlWrapper.runQuery(sql);			
		boolean trRes = false;		
		if ( accountPINCheck() ){						
			toSystem = "\tRequest [Query Account From: " + origine + " Pin: " + pin + "]\n";
			toUser = "SUCCESS, actual balance is " + dbBalance + " FCFA";			
			trRes = true;		
		}		
		transactionClose(trRes);		
		return trRes;
	}
	
	private boolean lockaccount(){		
	  transactionInit("LOCK");				
		sql = "INSERT INTO transactioninfos (IDTransaction, InfoType, Value) VALUES ('" + IDTransaction + "', 'origine', '" + origine + "');";
		sqlWrapper.runQuery(sql);
		sql = "INSERT INTO transactioninfos (IDTransaction, InfoType, Value) VALUES ('" + IDTransaction + "', 'pin', '" + pin + "');";
		sqlWrapper.runQuery(sql);			
		boolean trRes = false;		
		if ( accountPINCheck() ){			
			sql = "UPDATE account  SET Locked = 1, LastChange = '" + datetime + "' WHERE IDAccount = '" + origine + "';";
			sqlWrapper.runQuery(sql);	
			toSystem = "\tRequest [Lock Account From: " + origine + " Pin: " + pin + "]\n";
			toUser = "SUCCESS, your account is now locked";
			trRes = true;		
		}		
		transactionClose(trRes);		
		return trRes;
	}

	private synchronized boolean register(){		
		df = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
		datetime = df.format(new Date());
	  sql = "SELECT * FROM account WHERE IDAccount = '" + origine + "';";		
		sqlWrapper.runQuery(sql);
		if ( sqlWrapper.getSizeResultSet() > 0 ){
			toSystem = "\tError: registering " + origine + " unsussfull. Account already registerd\n";
			toUser = "Error, you are already registered in the system.";
			return false;
		}
		sql = "INSERT INTO User(Name, BirthDate, BirthPlace, IDCNumber, Adress, City, DateCreation, DateLastChange) VALUES('" + name + "', '" + birthdate + "', '" + birthplace + "', '" + idcnumber + "', '" + adress + "', '" + city + "', '" + datetime + "', '" + datetime + "');";
		sqlWrapper.runQuery(sql);
		sql = "SELECT IDUser FROM User ORDER BY IDUser DESC LIMIT 1;";
		sqlWrapper.runQuery(sql);
		String lastIDUser = sqlWrapper.getFieldValueResultSet(1, "IDUser");
		sql = "INSERT INTO Account(IDAccount, IDCategory, IDUser, Pin, Balance, Locked, WrongPIN, LastChange) VALUES ('" + origine + "', " + IDCategory + ", " + lastIDUser + ", '0000', 0, 0, 0, '" + datetime + "');";
		sqlWrapper.runQuery(sql);		
		toSystem = "\tRequest [Register: " + origine + "]\n";
		toUser = "SUCCESS, you have been register with PIN 0000";
		return true;
	}

	public void webRun()
	{
		System.out.println("## WWW TRANSACTION BEGIN ##\n");
		try
		{
			boolean res;

			sqlWrapper = new SQLWrapper(dbURL, dbUser, dbPwd, dbDriver);
			switch ( requestType ){						
				case TRAN: destination = (String) requestParams.get("destination"); amount = Integer.valueOf((String) requestParams.get("amount")).intValue(); origine = (String) requestParams.get("origine"); res = transfer(); break;
				case CHAR: prepaidcardnumber = (String) requestParams.get("prepaidcardnumber"); res = charge(); break;
				case CHAN: newpin = (String) requestParams.get("newpin"); res = changepin(); break;
				case QUES: res = queryaccount(); break;
				case LOCK: res = lockaccount(); break;
				case REGISTER: name = (String) requestParams.get("name"); IDCategory = (String) requestParams.get("idcategory"); birthdate = (String) requestParams.get("birthdate"); birthplace = (String) requestParams.get("birthplace"); idcnumber = (String) requestParams.get("idcnumber"); adress = (String) requestParams.get("adress"); city = (String) requestParams.get("city"); res = register(); break;
				default: {res = false; toSystem = "Unknown Request Type\n"; toUser = "ERROR: unknown request type";}
			}
			if ( ! res ){
				logFilename = logFilename + ".execErr";	
			}else{
				logFilename = logFilename + ".execOK";
			}
			
			COutgoingMessage responseMsg = new COutgoingMessage(origine, toUser);
			SimpleDateFormat df = new SimpleDateFormat( "yyyyMMdd_HHmmssSSS" );
			String smsFilename = df.format(new Date()) + "." + smsFileextension;
			MessageFile responseMsgFile = new MessageFile(smsFilename, responseMsg.toString());
			MessagePool messagePool = new MessagePool(outPoolDirectory, smsFileextension, crypto);
			responseMsgFile = messagePool.accessOutgoingPool(PUT, responseMsgFile);		
			String smsFilenameDest = "";				
			if ( destination != null ){
				COutgoingMessage responseMsgDest = new COutgoingMessage(destination, toDestination);
				df = new SimpleDateFormat( "yyyyMMdd_HHmmssSSS" );
				smsFilenameDest = df.format(message.getDate()) + "." + smsFileextension;
				MessageFile responseMsgFileDest = new MessageFile(smsFilenameDest, responseMsgDest.toString());
				responseMsgFileDest = messagePool.accessOutgoingPool(PUT, responseMsgFileDest);
			}			
				
			Thread.sleep(10000);		
				
			if ( messagePool.exists(smsFilename+".txErr") ){
				toSystem = toSystem + "\tRespTx: sending response to user failed\n";
				logFilename = logFilename + ".txErr"; 
			}else if ( messagePool.exists(smsFilename+".txOK") ){
				toSystem = toSystem + "\tRespTx: sending response to user successfully\n";
				logFilename = logFilename + ".txOK"; 
			}else{
				toSystem = toSystem + "\tRespTx: no status returned from TX\n";
				logFilename = logFilename + ".txNA"; 
			}
			
			if ( destination != null ){
				if ( messagePool.exists(smsFilenameDest+".txErr") ){
					toSystem = toSystem + "\tRespTx: sending response to destinator failed\n";
					logFilename = logFilename + ".txErr"; 
				}else if ( messagePool.exists(smsFilenameDest+".txOK") ){
					toSystem = toSystem + "\tRespTx: sending response to destinator successfully\n";
					logFilename = logFilename + ".txOK"; 
				}else{
					toSystem = toSystem + "\tRespTx: no status returned from TX\n";
					logFilename = logFilename + ".txNA"; 
				}					
			}
			
			
			File logFile = new File(logFilename);
			BufferedWriter out = new BufferedWriter(
	                           new FileWriter( logFile ) );	 
	    toUser = toUser + "\n";
	    Cryptograph cryptograph = new Cryptograph(crypto);
			cryptograph.encryptFile(logFilename, toUser + toSystem);
			
			System.out.println("USER RESPONSE: " + toUser);
			System.out.println("SYSTEM LOG:\n" + toSystem);
			System.out.println("## WWW TRANSACTION END ##\n");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public String getToUser(){
		return toUser;
	}

	public String getToSystem(){
		return toSystem;
	}
	
}
