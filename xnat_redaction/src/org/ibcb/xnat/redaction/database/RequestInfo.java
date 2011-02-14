package org.ibcb.xnat.redaction.database;

public class RequestInfo {
	
	private String requestid;
	private String userid;
	private String date;
	private String adminid;
	private String[] affectedsubjects;
	
	public RequestInfo(String requestid,String userid,String date,String adminid,String[] subjectids)
	{
		this.requestid=requestid;
		this.userid=userid;
		this.date=date;
		this.affectedsubjects=subjectids;
		this.adminid=adminid;
	}
	public RequestInfo(String requestid,String userid,String date,String adminid,String subjectids)
	{
		this.requestid=requestid;
		this.userid=userid;
		this.date=date;
		this.adminid=adminid;
		this.affectedsubjects=subjectidParser(subjectids);
	}
	public RequestInfo() {
		// TODO Auto-generated constructor stub
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
	public String getAdminid() {
		return adminid;
	}
	public void setAdminid(String adminid) {
		this.adminid = adminid;
	}
	
	public String[] getaffectedsubjects()
	{
		return this.affectedsubjects;		
	}
	public void setaffectedsubjects(String subjectids)
	{
		this.affectedsubjects=subjectidParser(subjectids);
	}
	public void setaffectedsubjects(String[] affectedsubjects)
	{
		this.affectedsubjects=affectedsubjects;
	}
	public String[] subjectidParser(String subjectid)
	{
		//parse all the requestids
		if (subjectid.length()<1) return null;
		if (!subjectid.contains(";")) return null;
		String[] subjectids=subjectid.split(";");
		return subjectids;		
	}
	

}
