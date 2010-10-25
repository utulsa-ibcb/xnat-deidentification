package org.ibcb.xnat.redaction;

public class RequestInfo {
	
	private String requestid;
	private String userid;
	private String date;
	private String checkoutinfo;
	private String adminid;
	
	public RequestInfo(String requestid,String userid,String date,String adminid,String checkoutinfo)
	{
		this.requestid=requestid;
		this.userid=userid;
		this.date=date;
		this.checkoutinfo=checkoutinfo;
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
	public String getCheckoutinfo() {
		return checkoutinfo;
	}
	public void setCheckoutinfo(String checkoutinfo) {
		this.checkoutinfo = checkoutinfo;
	}
	public String getAdminid() {
		return adminid;
	}
	public void setAdminid(String adminid) {
		this.adminid = adminid;
	}
	
	

}
