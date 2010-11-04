package org.ibcb.xnat.redaction.database;

import java.sql.*;

import org.postgresql.jdbc2.optional.PoolingDataSource;
//import org.postgresql.jdbc4.Jdbc4Connection;

//import java.sql.;
public class DBManager extends Thread{

	private static PoolingDataSource datasource;
	private Object data;
	public static final int INSERT_REQUESTINFO=0;
	public static final int INSERT_SUBJECTINFO=1;
	public static final int QUERY_REQUESTINFO=2;
	public static final int QUERY_SUBJECTINFO=3;
	public static final int QUERY_UNITED=4;
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
	}
	public DBManager(RequestInfo r,int type_of_work)
	{
		data=r;
		this.type_of_work=type_of_work;
	}
	public DBManager(int type_of_work)
	{
		this.type_of_work=type_of_work;
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

	public void query_print(String var_name,String table_name,Connection con)
	{
		try{
		PreparedStatement pstat = con.prepareStatement("select "+var_name+" from "+table_name+";");
		ResultSet rs = pstat.executeQuery();
		while(rs.next())
		{
			
			System.out.println(rs.getString("subjectid"));
		}
		}catch(SQLException e)
		{
			e.printStackTrace();
		}
	}
	public ResultSet query(String var_name,String table_name,Connection con)
	{
		PreparedStatement pstat;
		ResultSet rs =null;
		try {
			pstat = con.prepareStatement("select "+var_name+" from "+table_name+";");
			rs = pstat.executeQuery();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rs;
	}
	public int add(Connection con,String sql)
	{
		int result=0;
		try {
			Statement stat = con.createStatement();
			
			result = stat.executeUpdate(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	public int update(Connection con,String sql)
	{
		int result=0;
		try {
			Statement stat = con.createStatement();
			
			result = stat.executeUpdate(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	public void insert_requestinfo()
	{
		Connection newcon=this.getConnection();
		RequestInfo r=(RequestInfo)data;
		int result =0;
		try {
				stmt = newcon.createStatement();
				result=stmt.executeUpdate("INSERT INTO requestinfo VALUES('"+r.getRequestid()+"','"+r.getUserid()+"','"+r.getDate()+"','"+r.getAdminid()+"','"+r.getCheckoutinfo()+"');");
				
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
				result = stmt.executeUpdate("INSERT INTO subjectinfo VALUES('"+s.getSubjectid()+"','"+s.getFakephidata()+"','"+s.getRequestid()+"','"+s.getSubjectid()+"');");
		
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
					System.out.println();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	public void query_subjectinfo()
	{
		Connection newcon = this.getConnection();
		if(this.type_of_work==QUERY_SUBJECTINFO)
		{
			try {
				stmt = newcon.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT * FROM subjectinfo;");
				System.out.println("subjectid"+"\t"+"fakephidata"+"\t"+"requestid"+"\t"+"projectid");
				while(rs.next())
				{
					System.out.print(rs.getString("subjectid")+"\t");
					System.out.print(rs.getString("fakephidata")+"\t");
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
				System.out.println("subjectid"+"\t"+"fakephidata"+"\t"+"requestid"+"\t"+"projectid\t"+"date\t\t"+"adminid\t\t"+"checkoutinfo\t");
				while(rs.next())
				{
					System.out.print(rs.getString("subjectid")+"\t");
					System.out.print(rs.getString("fakephidata")+"\t");
					System.out.print(rs.getString("requestid")+"\t");
					System.out.print(rs.getString("projectid")+"\t");
					System.out.print(rs.getString("date")+"\t");
					System.out.print(rs.getString("adminid")+"\t");
					System.out.print(rs.getString("checkoutinfo")+"\t");
					System.out.println();
				}
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
			default:
				System.out.println("WRONG TYPE OF WORK!!!");
				break;
			
		}
		
	}
	

}
