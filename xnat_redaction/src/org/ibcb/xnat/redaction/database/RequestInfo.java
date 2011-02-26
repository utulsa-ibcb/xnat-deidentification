package org.ibcb.xnat.redaction.database;

public class RequestInfo {
	
	private String requestid;
	private String userid;
	private String date;
	private String adminid;
	private String[] affectedsubjects;
	private String checkoutinfo;
	
	public RequestInfo(String requestid,String userid,String date,String adminid,String[] subjectids)
	{
		this.requestid=requestid;
		this.userid=userid;
		this.date=date;
		this.affectedsubjects=subjectids;
		this.adminid=adminid;
	}
	public RequestInfo(String requestid,String userid,String date,String adminid,String subjectids,String cinfo)
	{
		this.requestid=requestid;
		this.userid=userid;
		this.date=date;
		this.adminid=adminid;
		this.affectedsubjects=subjectidParser(subjectids);
		this.checkoutinfo=cinfo;
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
	public String getaffectedsubjectstext()
	{
		String subjects = "";
		for (String subject:this.affectedsubjects)
		{
			subjects=subjects+subject+";";			
		}
		return subjects;
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
	public void setcheckoutinfo(String cinfo)
	{
		this.checkoutinfo=cinfo;
	}
	public String getcheckoutinfo()
	{
		return this.checkoutinfo;		
	}
	public String[] getcheckoutinfolist()
	{
		return this.checkoutinfoParser(this.checkoutinfo);		
	}
	
	
	public static String[] checkoutinfoParser(String cinfo)
	{
		//parse all the requestids
		if (cinfo.length()<1) return null;
		if (!cinfo.contains(";")) return null;
		String[] Checkinfo=cinfo.split(";");
		return Checkinfo;		
	}

}
