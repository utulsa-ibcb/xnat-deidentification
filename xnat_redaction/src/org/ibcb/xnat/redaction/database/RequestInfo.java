package org.ibcb.xnat.redaction.database;

public class RequestInfo {
	
	private String requestid;
	private String userid;
	private String date;
	private int[] checkoutinfo;
	private String adminid;
	
	public RequestInfo(String requestid,String userid,String date,String adminid,int[] checkoutinfo)
	{
		this.requestid=requestid;
		this.userid=userid;
		this.date=date;
		this.checkoutinfo=checkoutinfo;
		this.adminid=adminid;
	}
	public RequestInfo(String requestid,String userid,String date,String adminid,String checkoutinfo)
	{
		this.requestid=requestid;
		this.userid=userid;
		this.date=date;
		this.checkoutinfo=ArrayParse(checkoutinfo);
		this.adminid=adminid;
	}
	public String getRequestid() {
		return requestid;
	}
	public void setRequestid(String requestid) {
		this.requestid = requestid;
	}
	public String getUserid() {
		return userid;
	}
	public void setUserid(String userid) {
		this.userid = userid;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public int[] getCheckoutinfo() {
		return checkoutinfo;
	}
	public void setCheckoutinfo(int[] checkoutinfo) {
		this.checkoutinfo = checkoutinfo;
	}
	public String getAdminid() {
		return adminid;
	}
	public void setAdminid(String adminid) {
		this.adminid = adminid;
	}
	protected int[] ArrayParse(String input)
	{
		input.replace('{', ' ');
		input.replace('}', ' ');
		String[] lines=input.split(",");
		int[] returnArray=new int[lines.length];
		for (int i=0;i<lines.length;i++)
		{
			returnArray[i]=Integer.parseInt(lines[i]);			
		}
		return returnArray;
		
	}

	

}
