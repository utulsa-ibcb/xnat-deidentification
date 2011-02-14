package org.ibcb.xnat.redaction.database;

import java.util.HashMap;
import java.util.Set;


public class SubjectInfo {
	
	private String subjectid;
	private String phidata;
	private String[] requestids;
	private String projectid;
	
	
	public SubjectInfo(String sub,String phi,String[] req,String proj)
	{
		this.subjectid=sub;
		this.requestids=req;
		this.projectid=proj;
		this.phidata=phi;
	}
	
	public SubjectInfo(String sub,String phi,String req,String proj)
	{
		this.subjectid=sub;
		this.requestids=requestidParser(req);
		this.projectid=proj;
		this.phidata=phi;
	}
	public String getSubjectid() {
		return subjectid;
	}
	public void setSubjectid(String subjectid) {
		this.subjectid = subjectid;
	}
	public String getphidata() {
		return phidata;
	}
	public void setphidata(String phi) {
		this.phidata = phi;
		
	}
	public void setphidata(HashMap<String,String> phimap) {
			this.phidata=transphiData(phimap);
	}
	public static String transphiData(HashMap<String,String> phimap)
	{
		String phidata;
		if (phimap.isEmpty()) phidata="";
		else
		{
			phidata="";
			Set<String> keyset=phimap.keySet();
			for (String key :keyset)
			{				
				//encode the hashmap into a key,value; string
				phidata=phidata+key+","+phimap.get(key)+";";				
			}
		}
		return phidata;
	}
	public HashMap<String,String> getphiMap()
	{
		return transphiMap(this.phidata);
	}
	
	public static HashMap<String,String> transphiMap(String phidata)
	{
		HashMap<String,String> phimap=new HashMap<String,String>();
		if (phidata.length()<1) return null;
		if (!phidata.contains(";")) return null;
		String[] phipairs=phidata.split(";");
		for (String pairs : phipairs)
		{
			String[] pair=pairs.split(",");
			if (pair.length!=2) {
				//error				
			}
			else
			{
				phimap.put(pair[0].replace(" ", ""), pair[1].replace(" ", ""));				
			}
		}
			
		return phimap;
	}
	public String[] getRequestid() {
		return requestids;
	}
	public void setRequestids(String[] requestid) {
		this.requestids = requestid;
	}
	public void setRequestids(String requestids)
	{
		this.requestids=requestidParser(requestids);		
	}
	public String getProjectid() {
		return projectid;
	}
	public void setProjectid(String projectid) {
		this.projectid = projectid;
	}
	public String[] requestidParser(String requestid)
	{
		//parse all the requestids
		if (requestid.length()<1) return null;
		if (!requestid.contains(";")) return null;
		String[] requestids=requestid.split(";");
		return requestids;		
	}
	public String getRequestidText()
	{
		String requestids=null;
		for (String id:this.requestids)
		{
			requestids+=id;			
		}
		return requestids;
	}
}
