package database;

import java.io.*;
import java.sql.*;

public class SQLWrapper
{
	private String dbDriver;
	private String dbURL;
	private String dbUser;
	private String dbPwd;
	private Connection dbConnection;
  private Statement  dbStatement;
  private ResultSet  dbResultSet;
  
	
	public SQLWrapper(String dbUrl, String dbLogin, String dbPass, String dbDrv){
		dbURL = dbUrl;
		dbUser = dbLogin;
		dbPwd = dbPass;
		dbDriver = dbDrv;
		try{
			Class.forName( dbDriver );
			dbConnection = DriverManager.getConnection( dbURL, dbUser, dbPwd );		
		}
		catch ( Exception e){
			e.printStackTrace();
		}
	}
	
	public void runQuery(String sql){
		try {
			dbStatement = dbConnection.createStatement();
			boolean res = dbStatement.execute(sql);
			dbResultSet	= dbStatement.getResultSet();
		}
		catch (SQLException e) {
	  	e.printStackTrace();
	  }
	}
	
	public int getSizeResultSet(){
		int res = 0;
	  try {
       dbResultSet.last();
       res = dbResultSet.getRow();
	  }
	  catch (SQLException e) {
	  	e.printStackTrace();
	  }
	  return res;
	}
	
	public String getFieldValueResultSet(int rowNum, String colName){
		String res = "";
		try{
			if (dbResultSet.absolute(rowNum)){
				res = dbResultSet.getString(colName);
			}
		}
		catch (SQLException e) {
	  	e.printStackTrace();
	  }		
	  return res;
	}
	
	public String getInsertedValueResultSet(String colName){
		String res = "";
		try{
				dbResultSet.moveToInsertRow();
				res = dbResultSet.getString(colName);
		}
		catch (SQLException e) {
	  	e.printStackTrace();
	  }		
	  return res;
	}
	
}