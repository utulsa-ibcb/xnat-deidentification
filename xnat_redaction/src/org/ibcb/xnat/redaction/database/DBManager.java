package org.ibcb.xnat.redaction.database;

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
	static 
	{
		datasource=new Jdbc3PoolingDataSource();   //Use pooling data source to provide connection pool
		datasource.setDataSourceName("A Pooling Source");
		datasource.setServerName("localhost");
		datasource.setDatabaseName("PrivacyDB");
		datasource.setUser("xnat");
		datasource.setPassword("xnat");
		datasource.setMaxConnections(20);
		
	}
	public DBManager(int type_of_work)
	{
		this.type_of_work=type_of_work;
		Initializer();
	}
	private void Initializer()
	{
		//init the phi map
		PHImap= new HashMap<String,String>();
		Connection newcon = null;
		try {
			newcon = datasource.getConnection();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
				stmt = newcon.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT * FROM phimap;");
				while(rs.next())
				{
					PHImap.put(rs.getString("UID"), rs.getString("PHI"));
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
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

	public String[] findSameSubjects(String subjectId)
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
					if (phidatamap.containsKey("PatientBirthdate"))
						PatientBirthdate=phidatamap.get("PatientBirthdate");
					if (phidatamap.containsKey("PatientAge"))
						PatientAge=phidatamap.get("PatientAge");
					if (phidatamap.containsKey("xnat:dob"))
						xnat_dob=phidatamap.get("xnat:dob");
					if (phidatamap.containsKey("xnat:age"))
						xnat_age=phidatamap.get("xnat:age");
					stmt = newcon.createStatement();
					ResultSet findSame=stmt.executeQuery("SELECT subjectid, phidata FROM subjectinfo WHERE phidata like \'"+PatientName+"\' ;");
					while (findSame.next())
					{
						String tmpPhidata=findSame.getString("phidata");
						HashMap<String,String> tmpPhidataMap=SubjectInfo.transphiMap(tmpPhidata);
						if (phidatamap.containsKey("PatientBirthdate") && (PatientBirthdate==tmpPhidataMap.get("PatientBirthdate")))
							{
							sameSubjectIds.add(rs.getString("subjectid"));
							break;
							}
						if (phidatamap.containsKey("PatientAge") && (PatientAge==tmpPhidataMap.get("PatientAge")))
							{
							sameSubjectIds.add(rs.getString("subjectid"));
							break;
							}
						if (phidatamap.containsKey("xnat:dob") && (xnat_dob==tmpPhidataMap.get("xnat:dob")))
							{
							sameSubjectIds.add(rs.getString("subjectid"));
							break;
							}
						if (phidatamap.containsKey("xnat:age") && (xnat_age==tmpPhidataMap.get("xnat:age")))
							{
							sameSubjectIds.add(rs.getString("subjectid"));
							break;
							}						
					}
					sameSubjects=new String[sameSubjects.length];
					int i=0;
					for (String id:sameSubjectIds)
					{
						sameSubjects[i]=id;
						i++;
					}
					return sameSubjects;
				}
				else
				{
					return sameSubjects;
				}
					
			}
		}catch (SQLException e) {
			e.printStackTrace();
		}
		
		return sameSubjects;
	}
	public HashMap<String,String> getSubjectCheckOutInfo(String subjectid)
	{
		Connection newcon = this.getConnection();
		HashMap<String,String> checkoutMap=new HashMap<String,String>();
		try {
			ResultSet rs = stmt.executeQuery("SELECT phidata FROM subjectinfo WHERE subjectid=\'"+subjectid+"\';");
			while (rs.next())
			{
				String phidata=rs.getString("phidata");
				HashMap<String,String> phidatamap=SubjectInfo.transphiMap(phidata);
				for (String key:phidatamap.keySet())
				{
					checkoutMap.put(key, "1");
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return checkoutMap;
		
		
	}
	public void updateSubjectCheckOutInfo(String subjectid,HashMap<String,String> checkoutMap)
	{
		Connection newcon = null;
		try {
			newcon = datasource.getConnection();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			ResultSet rs = stmt.executeQuery("SELECT phidata FROM subjectinfo WHERE subjectid=\'"+subjectid+"\';");
			while (rs.next())
			{
				String phidata=rs.getString("phidata");
				HashMap<String,String> phidatamap=SubjectInfo.transphiMap(phidata);
				for (String key:phidatamap.keySet())
				{
					if (!checkoutMap.containsKey(key))
					checkoutMap.put(key, "1");
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public HashMap<String,HashMap<String,String>> getUserCheckOutInfo(String userid) throws SQLException
	{
		Connection newcon = null;
		try {
			newcon = datasource.getConnection();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		HashMap<String,HashMap<String,String>> 	checkoutinfo=new HashMap<String,HashMap<String,String>>();
		RequestInfo[] newinfo = null;
			//Find the associated userids
			try {
				ResultSet rs = stmt.executeQuery("SELECT * FROM requestinfo WHERE userid=\'"+userid+"\';");
				if (rs.next())
				{
					rs.last();
					int rowCount = rs.getRow();	
					rs.first();
					newinfo=new RequestInfo[rowCount];
				}
				int i=0;
				while(rs.next())
				{		
					newinfo[i]=new RequestInfo(rs.getString("requestid"),rs.getString("userid"),rs.getString("date"),rs.getString("adminid"),rs.getString("affectedsubjects"));
				}
				//for all the requests the user had
				for (RequestInfo info:newinfo)
				{
					//for all the subjects in this request
					for (String subjectid:info.getaffectedsubjects())	
					{
						//update the checkou map for this subject id
						if (!checkoutinfo.containsKey(subjectid))
							checkoutinfo.put(subjectid, getSubjectCheckOutInfo(subjectid));
						else
							updateSubjectCheckOutInfo(subjectid,checkoutinfo.get(subjectid));					
						//search and update the same subject with different if
						String[] samesubjects=findSameSubjects(subjectid);
						if (samesubjects==null) break;
						if (samesubjects.length==0) break;			
						for (String samesubject : samesubjects)
						{
							if (!checkoutinfo.containsKey(samesubject))
								checkoutinfo.put(samesubject, getSubjectCheckOutInfo(samesubject));
							else
								updateSubjectCheckOutInfo(samesubject,checkoutinfo.get(samesubject));					
						}
					}
				}
				return checkoutinfo;
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return null;	
}
	public void insertSubjectInfo()
	{
		
	}
	public void insertRequestInfo()
	{
		
	}
	public void updateSubjectInfo()
	{
		
	}

}
