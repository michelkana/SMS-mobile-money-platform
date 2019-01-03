package onlinebanking;
import handybank.*;
import java.util.HashMap;

public class WebRequest{

	private static final int TRAN = 1;
	private static final int CHAR = 2;
	private static final int CHAN = 3;
	private static final int QUES = 4;
	private static final int LOCK = 5;
	private static final int REGISTER = 6;
	private String p_phone;
	private String p_idcategory;
	private String p_name;
	private String p_birthdate;
	private String p_birthplace;
	private String p_idcnumber;
	private String p_adress;
	private String p_city;
	private String p_requestType;
	private String p_sessionID;
	private String p_destination;
	private String p_amount;
	private String p_prepaidcardnumber;
	
	private HashMap reqParams = new HashMap();
	private Request request;
	String logDirectory = "C:\\Documents and Settings\\Michel\\My Documents\\Vision\\Handy Bank\\Code\\SMSLib-Java-v2.1.4\\dist\\classes\\handybank\\log\\";
  String outPoolDirectory = "C:\\Documents and Settings\\Michel\\My Documents\\Vision\\Handy Bank\\Code\\SMSLib-Java-v2.1.4\\dist\\classes\\handybank\\pool\\outgoing\\";
 	String smsFileextension = "sms";
 	boolean crypto = false;
 	String response;
	
	public WebRequest(){
	}
	
	public void setRequestType(String requestType){
		p_requestType = requestType;
	}
	
	public void setSessionID(String sessionID){
		p_sessionID = sessionID;
	}	
	
	public void setPhone(String phone){
		p_phone = phone;
		reqParams.put("origine", phone);
	}
	
	public void setIDCategory(String idcategory){
		p_idcategory = idcategory;
		reqParams.put("idcategory", p_idcategory);
	}
	
	public void setName(String name){
		p_name = name;
		reqParams.put("name", name);
	}
	
	public void setBirthdate(String birthdate){
		p_birthdate = birthdate;
		reqParams.put("birthdate", birthdate);
	}
	
	public void setBirthplace(String birthplace){
		p_birthplace = birthplace;
		reqParams.put("birthplace", birthplace);
	}
	
	public void setIdcnumber(String idcnumber){
		p_idcnumber = idcnumber;
		reqParams.put("idcnumber", idcnumber);
	}
	
	public void setAdress(String adress){
		p_adress = adress;
		reqParams.put("adress", adress);
	}
	
	public void setCity(String city){
		p_city = city;
		reqParams.put("city", city);
	}
	
	public void setDestination(String destination){
		p_destination = destination;
		reqParams.put("destination", destination);
	}

	public void setAmount(String amount){
		p_amount = amount;
		reqParams.put("amount", amount);
	}
	
	public void setPrepaidcardnumber(String prepaidcardnumber){
		p_prepaidcardnumber = prepaidcardnumber;
		reqParams.put("prepaidcardnumber", prepaidcardnumber);
	}
	
	public String getResponse(){
		return response;
	}
	
	
	public void run(){
		request = new Request(Integer.valueOf(p_requestType).intValue(), reqParams, p_sessionID, logDirectory, outPoolDirectory, smsFileextension, crypto);
		request.webRun();
		response = request.getToUser();
	}

}