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
		if (phimap.isEmpty()) this.phidata="";
		else
		{
			this.phidata="";
			Set<String> keyset=phimap.keySet();
			for (String key :keyset)
			{				
				//encode the hashmap into a key,value; string
				this.phidata=this.phidata+key+","+phimap.get(key)+";";				
			}
		}
	}
	
	public HashMap<String,String> getphiMap()
	{
		HashMap<String,String> phimap=new HashMap<String,String>();
		if (this.phidata.length()<1) return null;
		if (!this.phidata.contains(";")) return null;
		String[] phipairs=this.phidata.split(";");
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
