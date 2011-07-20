package org.ibcb.xnat.redaction.helpers;

public class Message {
	public static final String[] TYPES = {"ERROR","WARNING","INFO"};
	public static final int TYPE_ERROR = 0;
	public static final int TYPE_WARNING = 1;
	public static final int TYPE_INFO = 2;
	
	public int type;
	public String message;
	public Exception e;
	
	public String logText(){
		String pstring = TYPES[type] + ": " + message;
		
		if(e!=null){
			for(StackTraceElement ste : e.getStackTrace()){
				pstring+=ste.toString()+"\n";
			}
		}
		
		return pstring;
	}
}
