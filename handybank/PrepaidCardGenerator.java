
package handybank;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class PrepaidCardGenerator {
  
  public static void main (String [] args) {
  	try{
	  	SecureRandom rand = new SecureRandom();
	    byte bytes[] = new byte[20];
	    BufferedWriter out = new BufferedWriter(
		                           new FileWriter( "codes.sql" ) );
		  SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
			String datetime = df.format(new Date());
			String code;
			String sql;
			int value=100;
	    for ( int i=0; i<100; i++){
	    	rand.nextBytes(bytes);
	    	value = (int) (i/20 + 1) * 1000;
	    	code = String.valueOf(Math.abs(rand.nextLong()));   
	    	sql = "INSERT INTO prepaidcard(code, value, mandate, used, expdate) VALUES('" + code + "', " + value + ", '" + datetime + "', 0, '2007-12-31 23:59:59');\n"; 	
				out.write(sql, 0, sql.length());			
	    	System.out.println (code + "\n");
	 		} 			   	
	 		out.close();
	 	}catch (Exception e)
			{
				e.printStackTrace();
			}   
 	}
}