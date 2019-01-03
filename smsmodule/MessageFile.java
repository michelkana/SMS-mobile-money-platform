//


package smsmodule;

import java.io.*;

// This is the new proposed method of using the ASYNC service.
public class MessageFile
{

	private String filename;
	private String fileContent;
	
	public MessageFile(){
	}
	
	public MessageFile(String fName, String content){
		filename = fName;
		fileContent = content;
	}	
  
  public String getFilename(){
  	return filename;
  }
  
  public String getFileContent(){
  	return fileContent;
  }
  
 
  public void setFilename(String fName){
  	filename = fName;
  }
  
  public void setFileContent(String fContent){
  	fileContent = fContent;
  }
  
}
