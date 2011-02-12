package org.ibcb.xnat.redaction.database;

import java.sql.*;
import java.util.HashMap;
import java.util.LinkedList;

import org.postgresql.jdbc2.optional.PoolingDataSource;
//import org.postgresql.jdbc4.Jdbc4Connection;

//import java.sql.;
public class DBManager extends Thread{

	private static PoolingDataSource datasource;
	private static HashMap<String,String> PHImap;
	private Object data;
	public static final int SINGLE_THREAD=0;
	public static final int INSERT_REQUESTINFO=1;
	public static final int INSERT_SUBJECTINFO=2;
	public static final int QUERY_REQUESTINFO=3;
	public static final int QUERY_SUBJECTINFO=4;
	public static final int QUERY_UNITED=5;
	public static final int UPDATE_REQUESTINFO=6;
	public static final int UPDATE_SUBJECTINFO=7;
	public static final int GET_CHECKOUTINFO=8;
	private int type_of_work;
	protected Statement stmt;
	static 
	{
		datasource=new PoolingDataSource();   //Use pooling data source to provide connection pool
		datasource.setDataSourceName("A Pooling Source");
		datasource.setServerName("localhost");
		datasource.setDatabaseName("PrivacyDB");
		datasource.setUser("xnat");
		datasource.setPassword("xnat");
		datasource.setMaxConnections(20);
		
	}
	public DBManager(SubjectInfo s,int type_of_work)
	{
		data=s;
		this.type_of_work=type_of_work;
		Initializer();
	}
	public DBManager(RequestInfo r,int type_of_work)
	{
		data=r;
		this.type_of_work=type_of_work;
		Initializer();
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
		Connection newcon = this.getConnection();
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
	public static final String SUBJECTID="subjectid";
	public static final String phidata="phidata";
	public static final String REQUESTID="requestid";
	public static final String PROJECTID="projectid";
	public static final String USERID="userid";
	public static final String DATE="date";
	public static final String ADMINID="adminid";
	public static final String CHECKOUTINFO="checkoutinfo";
	private String target;
	private String tar_val;
	private String reference;
	private String ref_val;
	public DBManager(int type_of_work,String target,String tar_val,String reference,String ref_val )
	{
		//data=s;
		this.type_of_work=type_of_work;
		this.target=target;
		this.tar_val=tar_val;
		this.reference=reference;
		this.ref_val=ref_val;
		
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
	
	public void insert_requestinfo()
	{
		String array="";
		Connection newcon=this.getConnection();
		RequestInfo r=(RequestInfo)data;
		int result =0;
		try {
				for(int i=0;i<r.getCheckoutinfo().length;i++)
				{
					if(r.getCheckoutinfo().length==1)
					array=r.getCheckoutinfo()[i]+"";	
					else
					{
						if(i==r.getCheckoutinfo().length-1)
						array=array+r.getCheckoutinfo()[i];
						else
						array=array+r.getCheckoutinfo()[i]+",";
					}
				}
				stmt = newcon.createStatement();
				result=stmt.executeUpdate("INSERT INTO requestinfo VALUES('"+r.getRequestid()+"','"+r.getUserid()+"','"+r.getDate()+"','"+r.getAdminid()+"',ARRAY["+array+"]);");
				
			} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void insert_subjectinfo()
	{
		Connection newcon=this.getConnection();
		SubjectInfo s=(SubjectInfo)data;
		int result = 0;
		try {
				stmt = newcon.createStatement();
				result = stmt.executeUpdate("INSERT INTO subjectinfo VALUES('"+s.getSubjectid()+"','"+s.getphidata()+"','"+s.getRequestid()+"','"+s.getSubjectid()+"');");
		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//System.out.println("Foreign key con");
		}
	}

	public void query_requestinfo()
	{
		Connection newcon = this.getConnection();
		if(this.type_of_work==QUERY_REQUESTINFO)
		{
			try {
				stmt = newcon.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT * FROM requestinfo;");
				System.out.println("requestid"+"\t"+"userid"+"\t\t"+"date"+"\t\t"+"adminid"+"\t\t"+"checkoutinfo");
				while(rs.next())
				{
					System.out.print(rs.getString("requestid")+"\t");
					System.out.print(rs.getString("userid")+"\t");
					System.out.print(rs.getString("date")+"\t");
					System.out.print(rs.getString("adminid")+"\t");
					System.out.print(rs.getString("checkoutinfo")+"\t");
					RequestInfo newinfo=new RequestInfo(rs.getString("requestid"),rs.getString("userid"),rs.getString("date"),rs.getString("adminid"),rs.getString("checkoutinfo"));
					System.out.println();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	public RequestInfo[] query_requestinfo(String userid)
	{
		Connection newcon = this.getConnection();
		
		if(this.type_of_work==QUERY_REQUESTINFO)
		{
			try {
				stmt = newcon.createStatement();
				
				ResultSet rs = stmt.executeQuery("SELECT * FROM requestinfo WHERE userid=\'"+userid+"\';");
				rs.last();
				int rowCount = rs.getRow();
				rs.first();
				RequestInfo[] newinfo=new RequestInfo[rowCount];
				int i=0;
				while(rs.next())
				{		
					newinfo[i]=new RequestInfo(rs.getString("requestid"),rs.getString("userid"),rs.getString("date"),rs.getString("adminid"),rs.getString("checkoutinfo"));
				}
				return newinfo;
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
		}
		return null;
		
	}
	public void query_subjectinfo()
	{
		Connection newcon = this.getConnection();
		if(this.type_of_work==QUERY_SUBJECTINFO)
		{
			try {
				stmt = newcon.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT * FROM subjectinfo;");
				System.out.println("subjectid"+"\t"+"phidata"+"\t"+"requestid"+"\t"+"projectid");
				while(rs.next())
				{
					System.out.print(rs.getString("subjectid")+"\t");
					System.out.print(rs.getString("phidata")+"\t");
					System.out.print(rs.getString("requestid")+"\t");
					System.out.print(rs.getString("projectid")+"\t");
					System.out.println();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	public void query_united()
	{
		Connection newcon = this.getConnection();
		if(this.type_of_work==QUERY_UNITED)
		{
			try {
				stmt = newcon.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT * FROM subjectinfo sub, requestinfo re where sub.requestid=re.requestid;");
				System.out.println("subjectid"+"\t"+"phidata"+"\t"+"requestid"+"\t"+"projectid\t"+"date\t\t"+"adminid\t\t"+"checkoutinfo\t");
				while(rs.next())
				{
					System.out.print(rs.getString("subjectid")+"\t");
					System.out.print(rs.getString("phidata")+"\t");
					System.out.print(rs.getString("requestid")+"\t");
					System.out.print(rs.getString("projectid")+"\t");
					System.out.print(rs.getString("date")+"\t");
					System.out.print(rs.getString("adminid")+"\t");
					System.out.print(rs.getArray("checkoutinfo")+"\t");
					System.out.println();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	public void getCheckOutInfo(String userid,HashMap<String,String> checkOutMap) throws SQLException
	{
		Connection newcon = this.getConnection();
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
					newinfo[i]=new RequestInfo(rs.getString("requestid"),rs.getString("userid"),rs.getString("date"),rs.getString("adminid"),rs.getString("checkoutinfo"));
				}
				//LinkedList<String> subjectIds=new LinkedList<String>();
				for (RequestInfo info:newinfo)
				{
					//subjectIds.add(info.get)
					
					
				}
				
				
				if (newinfo!=null)
				{
				for(RequestInfo info : newinfo)
				{
					int[] checkoutarray=info.getCheckoutinfo();
					checkOutMap.put("Total_Checkout_By_User",Integer.toString(checkoutarray.length));
					for (int checkOutId : checkoutarray)
					{
						if (!checkOutMap.containsKey(checkOutId))
						checkOutMap.put(PHImap.get(Integer.toString(checkOutId)), "1");
					}
				}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			

			
			
			
		
	}
	public void update_subjectinfo()
	{
		Connection newcon = this.getConnection();
				
		if(this.type_of_work==UPDATE_SUBJECTINFO)
		{
			try {
				stmt = newcon.createStatement();
				int result = stmt.executeUpdate("UPDATE subjectinfo SET "+this.target+"='"+this.tar_val+"' where "+this.reference+"='"+this.ref_val+"';");
			
				System.out.println("SUCCESSFULLY UPDATED,"+result+"COLUMN(S) AFFECTED");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	public void update_requestinfo()
	{
		Connection newcon = this.getConnection();
		
		if(this.type_of_work==UPDATE_REQUESTINFO)
		{
			try {
				stmt = newcon.createStatement();
				int result = stmt.executeUpdate("UPDATE requestinfo SET "+this.target+"='"+this.tar_val+"' where "+this.reference+"='"+this.ref_val+"';");
			
				System.out.println("SUCCESSFULLY UPDATED,"+result+"COLUMN(S) AFFECTED");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub

		switch(this.type_of_work)
		{
		case QUERY_REQUESTINFO:
			this.query_requestinfo();
			break;
		case QUERY_SUBJECTINFO:
			this.query_subjectinfo();
			break;
		case QUERY_UNITED:
			this.query_united();
			break;
		case UPDATE_SUBJECTINFO:	
			this.update_subjectinfo();
			break;
		case UPDATE_REQUESTINFO:
			this.update_requestinfo();
			break;
		case INSERT_SUBJECTINFO:
			if (data.getClass()==SubjectInfo.class)
			{
				this.insert_subjectinfo();
				System.out.println("succeed in inserting into subjectinfo!!");
			}
			break;
		case INSERT_REQUESTINFO:
			if(data.getClass()==RequestInfo.class)
			{
				this.insert_requestinfo();
				System.out.println("succeed in inserting into requestinfo!!");
			}
			break;
		case SINGLE_THREAD:
			break;
			default:
				System.out.println("WRONG TYPE OF WORK!!!");
				break;
			
		}
		
	}
	

}
