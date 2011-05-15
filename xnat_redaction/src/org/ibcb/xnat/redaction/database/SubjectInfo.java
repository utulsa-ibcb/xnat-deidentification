package org.ibcb.xnat.redaction.database;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;


public class SubjectInfo {
	
	private String subjectid;
	private String phidata;
	private String[] requestids;
	private String projectid;
	private String subjectname;
	private String dateofbirth;
	
	public SubjectInfo(String sub,String phi,String proj,String[] req)
	{
		this.subjectid=sub;
		this.requestids=req;
		this.projectid=proj;
		this.phidata=phi;
	}
	
	public SubjectInfo(String sub,String phi,String proj,String req)
	{
		this.subjectid=sub;
		this.requestids=requestidParser(req);
		this.projectid=proj;
		this.phidata=phi;
	}
	public SubjectInfo(String sub,String phi,String proj,String[] req,String name, String dateofbirth)
	{
		this.subjectid=sub;
		this.requestids=req;
		this.projectid=proj;
		this.phidata=phi;
		this.subjectname=name;
		this.dateofbirth=dateofbirth;
	}
	
	public SubjectInfo(String sub,String phi,String proj,String req,String name, String dateofbirth)
	{
		this.subjectid=sub;
		this.requestids=requestidParser(req);
		this.projectid=proj;
		this.phidata=phi;
		this.subjectname=name;
		this.dateofbirth=dateofbirth;
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
	
	public String getSubjectname()
	{return subjectname;}
	public String getDateofbirth()
	{return dateofbirth;}
	public void setSubjectname(String name)
	{this.subjectname=name;}
	public void setDateofbirth(String Dateofbirth)
	{this.dateofbirth=Dateofbirth;}
	
	
	
	
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
		String requestids="";
		for (String id:this.requestids)
		{
			requestids+=id+";";			
		}
		return requestids;
	}
	public void merge(SubjectInfo oldsinfo)
	{
		if (this.subjectid==oldsinfo.subjectid)
		{
			//merge the phidata
			HashMap<String,String> ownHashMap=this.getphiMap();
			HashMap<String,String> oldHashMap=oldsinfo.getphiMap();
			for (String key : oldHashMap.keySet())
			{
				//Assume the phidata wont change
				if (!ownHashMap.containsKey(key))
					ownHashMap.put(key, oldHashMap.get(key));				
			}
			this.phidata=transphiData(ownHashMap);
			//merge the requestids
			String[] ownRequestIds=this.getRequestid();
			String[] oldRequestIds=oldsinfo.getRequestid();
			LinkedList<String> tmp=new LinkedList<String>(); 
			for (String requestid: ownRequestIds)
			{				
				tmp.add(requestid);
			}
			for (String requestid : oldRequestIds)
			{
				if (!tmp.contains(requestid))
					tmp.add(requestid);
			}
			String[] newrequestIds=new String[tmp.size()];
			int i=0;
			for (String requestid:tmp)
			{
				newrequestIds[i]=tmp.get(i);
				i++;
			}
			this.requestids=newrequestIds;			
		}
		else
		return;
		
	}
}
