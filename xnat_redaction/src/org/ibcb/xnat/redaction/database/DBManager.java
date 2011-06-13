package org.ibcb.xnat.redaction.database;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import org.postgresql.jdbc3.Jdbc3PoolingDataSource;
//import org.postgresql.jdbc4.Jdbc4Connection;

//import java.sql.;

public class DBManager extends Thread{


	private static Jdbc3PoolingDataSource datasource;
	private static HashMap<String,String> PHImap;
	public static final int SINGLE_THREAD=0;
	public static final int INSERT_REQUESTINFO=1;
	public static final int INSERT_SUBJECTINFO=2;
	public static final int UPDATE_SUBJECTINFO=3;
	public int type_of_work=0;
	protected Statement stmt;
	String hostname="localhost";
	static 
	{
		datasource=new Jdbc3PoolingDataSource();   //Use pooling data source to provide connection pool
		datasource.setDataSourceName("A Pooling Source");
		datasource.setServerName("");
		datasource.setDatabaseName("privacydb");
		datasource.setUser("xnat_redaction");
		datasource.setPassword("xnat");
		datasource.setMaxConnections(20);
		
	}
	public DBManager(int type_of_work,String Hostname)
	{
		this.hostname=Hostname;
		datasource.setServerName(hostname);
		this.type_of_work=type_of_work;
		//Initializer();

	}
	public DBManager(int type_of_work)
	{
		datasource.setServerName(hostname);
		this.type_of_work=type_of_work;
		//Initializer();

	}
	public DBManager()
	{
		type_of_work=SINGLE_THREAD;
		datasource.setServerName(hostname);
		//Initializer();
	}
	private void Initializer()
	{
	}
	protected Connection getConnection()

	{
		Connection con = null;
		try {
			Class.forName("org.postgresql.Driver");
			try {
				con = datasource.getConnection();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return con;
	}

	public LinkedList<String> findSameSubjects(String subjectId)
	{
		String[] sameSubjects=null;
		LinkedList<String> sameSubjectIds=new LinkedList<String>();
		String PatientName=null;
		String PatientBirthdate=null;
		String PatientAge=null;
		String xnat_dob=null;
		String xnat_age=null;
		Connection newcon=this.getConnection();
		try {
			stmt = newcon.createStatement();	
			ResultSet rs = stmt.executeQuery("SELECT * FROM subjectinfo WHERE subjectid=\'"+subjectId+"\';");
			while (rs.next())
			{
				//get the whole phi data
				String phidata=rs.getString("phidata");
				// get name and dob
				HashMap<String,String> phidatamap=SubjectInfo.transphiMap(phidata);
				if (phidatamap.containsKey("PatientName"))
				{
					PatientName=phidatamap.get("PatientName");	
					//System.out.println("search for name:"+PatientName);
					if (phidatamap.containsKey("PatientBirthdate"))
						PatientBirthdate=phidatamap.get("PatientBirthdate");
					if (phidatamap.containsKey("PatientAge"))
						PatientAge=phidatamap.get("PatientAge");
					if (phidatamap.containsKey("xnat:dob"))
						xnat_dob=phidatamap.get("xnat:dob");
					if (phidatamap.containsKey("xnat:age"))
						xnat_age=phidatamap.get("xnat:age");
					newcon=this.getConnection();
					stmt = newcon.createStatement();
					ResultSet findSame=stmt.executeQuery("SELECT subjectid, phidata FROM subjectinfo WHERE phidata like \'%"+PatientName+"%\' AND subjectid<>\'"+subjectId+"\' ;");
					while (findSame.next())
					{
						//System.out.println("find subjects with same name");
						String tmpPhidata=findSame.getString("phidata");
						HashMap<String,String> tmpPhidataMap=SubjectInfo.transphiMap(tmpPhidata);
						if (phidatamap.containsKey("PatientBirthdate") && (PatientBirthdate.equals(tmpPhidataMap.get("PatientBirthdate"))))
							{
							sameSubjectIds.add(findSame.getString("subjectid"));
							break;
							}	
						if (phidatamap.containsKey("PatientAge") && (PatientAge.equals(tmpPhidataMap.get("PatientAge"))))
							{
							sameSubjectIds.add(findSame.getString("subjectid"));
							break;
							}
						if (phidatamap.containsKey("xnat:dob") && (xnat_dob.equals(tmpPhidataMap.get("xnat:dob"))))
							{
							sameSubjectIds.add(findSame.getString("subjectid"));
							break;
							}
						if (phidatamap.containsKey("xnat:age") && (xnat_age.equals(tmpPhidataMap.get("xnat:age"))))
							{
							sameSubjectIds.add(findSame.getString("subjectid"));
							break;
							}						
					}
					newcon.close();
					return sameSubjectIds;
				}
				else
				{
					newcon.close();
					return sameSubjectIds;
				}
					
			}
		}catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			newcon.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sameSubjectIds;
	}
	
	
	public HashMap<String,String> getSubjectCheckOutInfo(String subjectid,String userid)
	{
		Connection newcon = this.getConnection();
		HashMap<String,String> checkoutMap=new HashMap<String,String>();
		try {
			stmt = newcon.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT rinfo.checkoutinfo FROM subjectinfo sinfo, requestinfo rinfo WHERE sinfo.subjectid="+subjectid+" AND rinfo.userid=\'"+userid+"\';");
			while (rs.next())
			{
				String checkoutinfo=rs.getString("checkoutinfo");
				//System.out.println(checkoutinfo);
				String[] checkouinfolist=RequestInfo.checkoutinfoParser(checkoutinfo);
				for (String key:checkouinfolist)
				{
					//System.out.println("Checked out fields "+key+" for "+subjectid+" by "+userid);
					if (!checkoutMap.containsKey(key))
					checkoutMap.put(key, "1");
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			newcon.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return checkoutMap;
		
		
	}
	
	private  HashMap<String,String> mergeHashMap(HashMap<String,String> origin,  HashMap<String,String> update)
	{
		for (String key : update.keySet())
		{
			if (!origin.containsKey(key))
			origin.put(key, update.get(key));
		}
		return origin;		
	}
	
	private void updateCheckOutInfo(HashMap<String,HashMap<String,String>> 	checkoutinfo, String subjectid, HashMap<String,String> update )
	{
		HashMap<String,String> origin=checkoutinfo.get(subjectid);
		origin=mergeHashMap(origin,update);
		checkoutinfo.remove(subjectid);
		checkoutinfo.put(subjectid, origin);
	}
	
	public HashMap<String,HashMap<String,String>> getUserCheckOutInfo(String userid)
	{
		Connection newcon = null;
		try {
			newcon = datasource.getConnection();
			HashMap<String,HashMap<String,String>> 	checkoutinfo=new HashMap<String,HashMap<String,String>>();
			LinkedList<RequestInfo> newinfo = new LinkedList<RequestInfo>();
			//Find the associated userids
				stmt = newcon.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT * FROM requestinfo WHERE userid=\'"+userid+"\';");
				//rs.getBigDecimal("");
				while(rs.next())
				{				
					newinfo.add(new RequestInfo(rs.getBigDecimal("requestid"),rs.getString("userid"),rs.getString("date"),rs.getString("adminid"),rs.getString("affectedsubjects"),rs.getString("checkoutinfo")));
				}
				//for all the requests the user had
				for (RequestInfo info:newinfo)
				{
					//for all the subjects in this request
					if (info.getaffectedsubjects()!=null)
					for (String subjectid:info.getaffectedsubjects())	
					{
						//update the checkou map for this subject id
						if (!checkoutinfo.containsKey(subjectid) || checkoutinfo.keySet()==null)
							checkoutinfo.put(subjectid, getSubjectCheckOutInfo(subjectid,userid));
						else
							updateCheckOutInfo(checkoutinfo,subjectid,checkoutinfo.get(subjectid));					
						//search and update the same subject with different if
						/*LinkedList<String> samesubjects=findSameSubjects(subjectid);
						if (samesubjects==null) break;
						if (samesubjects.isEmpty()) break;			
						for (String samesubject : samesubjects)
						{
							System.out.println("same subject "+samesubject+" for "+subjectid);
							updateCheckOutInfo(checkoutinfo,subjectid, getSubjectCheckOutInfo(samesubject,userid));
							if (!checkoutinfo.containsKey(samesubject))
								checkoutinfo.put(samesubject, getSubjectCheckOutInfo(samesubject,userid));
							else
								updateCheckOutInfo(checkoutinfo,samesubject,checkoutinfo.get(samesubject));	
						}*/
					}
				}
				newcon.close();
				//out put the checkoutinfo
				for (String subject : checkoutinfo.keySet())
				{
					System.out.println("For subject:"+subject);
					for (String key : checkoutinfo.get(subject).keySet())
					{
						System.out.println("key = "+key+" value = "+checkoutinfo.get(subject).get(key));
					}		
				}
				return checkoutinfo;
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				newcon.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;	
}
	
	public String getSubjectID(String subjectname, String dateofbirth)
	{
		Connection newcon = this.getConnection();
		String subjectid=null;
		try {
			stmt = newcon.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT subjectid FROM subjectinfo WHERE subjectname=\'"+subjectname+"\' AND dateofbirth=\'"+dateofbirth+"\';");
			//Check if the subject is already exist
			if (rs.next())
			{
				subjectid=rs.getString("subjectid");
			}
			newcon.close();			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return subjectid;				
	}
	
	public BigDecimal lookupSubject(SubjectInfo sinfo)
	{
		Connection newcon = this.getConnection();
		BigDecimal subjectid=null;
		try {
			stmt = newcon.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT subjectid FROM subjectinfo WHERE subjectname=\'"+sinfo.getSubjectname()+"\' AND dateofbirth=\'"+sinfo.getDateofbirth()+"\';");
			//Check if the subject is already exist
			if (rs.next())
			{
				subjectid=rs.getBigDecimal("subjectid");
			}
			if (rs.wasNull()) return null;
			newcon.close();			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return subjectid;				
	}
	
	
	public BigDecimal insertSubjectInfo(SubjectInfo sinfo)
	{
		
		Connection newcon = this.getConnection();
		BigDecimal subjectid=lookupSubject(sinfo);
		BigDecimal nextid = null;
		ResultSet id_rs;
		try {
			stmt = newcon.createStatement();
			id_rs = stmt.executeQuery("SELECT nextval('next_requestid')");
			if (id_rs.next())
			{
			nextid=id_rs.getBigDecimal("nextval");
			//System.out.println("VALUES ("+nextid.toString()+", \'"+sinfo.getphidata()+"\',\'"+sinfo.getProjectid()+"\',\'"+sinfo.getRequestidText()+"\',\'"+sinfo.getSubjectname()+"\',\'"+sinfo.getDateofbirth()+"\');");
			}
			if (subjectid==null) {
				stmt = newcon.createStatement();
				stmt.execute("INSERT INTO subjectinfo (subjectid , phidata , projectid , requestids , subjectname , dateofbirth)  VALUES ("+nextid.toString()+", \'"+sinfo.getphidata()+"\',\'"+sinfo.getProjectid()+"\',\'"+sinfo.getRequestidText()+"\',\'"+sinfo.getSubjectname()+"\',\'"+sinfo.getDateofbirth()+"\');");
				newcon.close();		
				return nextid;
			}
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	
		try {
			if (subjectid!=null) {
					//use update instead
					System.out.println("subject record already exist will update it");
					sinfo.setSubjectid(subjectid);
					updateSubjectInfo(sinfo);
					newcon.close();		
					return subjectid;
				}				
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	public BigDecimal getNextRequestID()
	{		
			BigDecimal nextid = null;
			Connection newcon = this.getConnection();
			try {	
					stmt = newcon.createStatement();
					ResultSet id_rs = stmt.executeQuery("SELECT nextval('next_requestid')");
					if (id_rs.next())
					{
						nextid=id_rs.getBigDecimal("nextval");
						//System.out.println("next id "+nextid);
					}	
				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return nextid;		
	}
	public BigDecimal insertRequestInfo(RequestInfo rinfo)
	{		
		BigDecimal nextid = null;
		Connection newcon = this.getConnection();
		try {	
			if (rinfo.getRequestid()==null)
			{
				stmt = newcon.createStatement();
				ResultSet id_rs = stmt.executeQuery("SELECT nextval('next_requestid')");
				if (id_rs.next())
				{
					nextid=id_rs.getBigDecimal("nextval");
					//System.out.println("next id "+nextid);
				}	
			}
			else
				nextid=rinfo.getRequestid();
			stmt = newcon.createStatement();
			stmt.execute("INSERT INTO requestinfo (requestid, userid, date, adminid, affectedsubjects,checkoutinfo) VALUES(\'"+nextid+"\',\'"+rinfo.getUserid()+"\',\'"+rinfo.getDate()+"\',\'"+rinfo.getAdminid()+"\',\'"+rinfo.getaffectedsubjectstext()+"\',\'"+rinfo.getcheckoutinfo()+"\');");
		
			newcon.close();			
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
		return nextid;
	}
	
	/*public void updateRequestInfo(RequestInfo rinfo)
	{		
		//can be called only by insertSubjectInfo to avoid update a non existing record
		Connection newcon = this.getConnection();
		try {
			stmt = newcon.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM subjectinfo WHERE subjectid=\'"+rinfo.getRequestid()+"\';");
			RequestInfo  oldrinfo=null;
			if (rs.next())
			{
				//public RequestInfo(BigDecimal requestid,String userid,String date,String adminid,String subjectids,String cinfo)
				oldrinfo=new RequestInfo(rs.getBigDecimal("requestid"),rs.getString("userid"),rs.getString("date"),rs.getString("adminid"),rs.getString("affectedsubjects"),rs.getString("checkoutinfo"));
				//sinfo.merge(oldsinfo);
			}
			rs.close();			
			
			stmt = newcon.createStatement();
			stmt.execute("UPDATE requestinfo SET requestid=\'"+rinfo.getRequestid()+"\', userid=\'"+rinfo.getUserid()+"\', date=\'"+rinfo.getDate()+"\', affectedsubjects=\'"+rinfo.getaffectedsubjectstext()+"\', checkoutinfo=\'"+rinfo.getcheckoutinfo()+"\' WHERE subjectid=\'"+rinfo.getRequestid()+"\';");
			newcon.close();			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/
	
	private void updateSubjectInfo(SubjectInfo sinfo)
	{
		//can be called only by insertSubjectInfo to avoid update a non existing record
		Connection newcon = this.getConnection();
		try {
			stmt = newcon.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM subjectinfo WHERE subjectid=\'"+sinfo.getSubjectid()+"\';");
			SubjectInfo  oldsinfo=null;
			if (rs.next())
			{
				oldsinfo=new SubjectInfo(rs.getBigDecimal("subjectid"),rs.getString("phidata"),rs.getString("projectid"),rs.getString("requestids"));
				sinfo.merge(oldsinfo);
			}
			rs.close();			
			
			stmt = newcon.createStatement();
			stmt.execute("UPDATE subjectinfo SET phidata=\'"+sinfo.getphidata()+"\', requestids=\'"+sinfo.getRequestidText()+"\' WHERE subjectid=\'"+sinfo.getSubjectid()+"\';");
			newcon.close();			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public BigDecimal lookupSubjectid(String xnatid)
	{
		BigDecimal subjectid = null;
		Connection newcon = this.getConnection();
		try {	
				stmt = newcon.createStatement();
				ResultSet id_rs = stmt.executeQuery("SELECT subjectid FROM subjectidmap WHERE xnatid LIKE \'%,"+xnatid+",%\'");
				if (id_rs.next())
				{
					subjectid=id_rs.getBigDecimal("subjectid");
				}
				else
					subjectid=null;
				newcon.close();			
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
		return subjectid;	
	}
	public void insertSubjectidMap(BigDecimal subjectid,String xnatid)
	{
		Connection newcon = this.getConnection();
		try {	
				stmt = newcon.createStatement();
				ResultSet id_rs = stmt.executeQuery("SELECT subjectid,xnatid FROM subjectidmap WHERE subjectid="+subjectid);
				if (id_rs.next())
				{
					String oldxnatid=id_rs.getString("xnatid");
					//assume get only one xnatid at a time
					//update
					if (!oldxnatid.contains(xnatid)){
					stmt=newcon.createStatement();
					stmt.execute("UPDATE subjectidmap SET xnatid=\'"+oldxnatid+","+xnatid+"\' WHERE subjectid=\'"+subjectid+"\')");	
					}
				}
				else
				{
					//insert
					stmt=newcon.createStatement();
					stmt.execute("INSERT INTO subjectidmap(subjectid,xnatid) VALUES(\'"+subjectid+"\',\',"+xnatid+",\')");				
				}
				newcon.close();			
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}	
	}

}
