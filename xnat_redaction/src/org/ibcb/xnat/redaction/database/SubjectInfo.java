package org.ibcb.xnat.redaction.database;

import java.util.HashMap;
import java.util.Set;


public class SubjectInfo {
	
	private String subjectid;
	private String phidata;
	private String requestid;
	private String projectid;
	
	
	public SubjectInfo(String sub,String phi,String req,String proj)
	{
		this.subjectid=sub;
		this.requestid=req;
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
	public String getRequestid() {
		return requestid;
	}
	public void setRequestid(String requestid) {
		this.requestid = requestid;
	}
	public String getProjectid() {
		return projectid;
	}
	public void setProjectid(String projectid) {
		this.projectid = projectid;
	}

	
}
