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
	public int insert_subjectinfo(SubjectInfo s,Connection con)
	{
		int result = 0;
		try {
			Statement stmt = con.createStatement();
			result = stmt.executeUpdate("INSERT INTO subjectinfo VALUES('"+s.getSubjectid()+"','"+s.getFakephidata()+"','"+s.getRequestid()+"','"+s.getSubjectid()+"');");
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		if (data.getClass()==SubjectInfo.class)
		{
			Connection newcon=this.getConnection();
			SubjectInfo s=(SubjectInfo)data;
			int result = 0;
			try {
				if(this.type_of_work==INSERT_SUBJECTINFO)
				{
					stmt = newcon.createStatement();
					result = stmt.executeUpdate("INSERT INTO subjectinfo VALUES('"+s.getSubjectid()+"','"+s.getFakephidata()+"','"+s.getRequestid()+"','"+s.getSubjectid()+"');");
				
					System.out.println(result);
				}
				if(this.type_of_work==INSERT_REQUESTINFO)
				{
					stmt = newcon.createStatement();
					ResultSet rs = stmt.executeQuery("SELECT subjectid FROM subjectinfo;");
					while(rs.next())
					{
						System.out.println(rs.getString("subjectid"));
					}
				}
				else
				{
					System.out.println("Wrong type of work");
				}
				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				//System.out.println("Foreign key con");
			}
			
			
		}
		if(data.getClass()==RequestInfo.class)
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
	}
	

}
