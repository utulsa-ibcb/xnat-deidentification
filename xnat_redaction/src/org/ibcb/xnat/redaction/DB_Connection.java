package org.ibcb.xnat.redaction;

import java.sql.*;

import org.postgresql.jdbc2.optional.PoolingDataSource;


public class DB_Connection implements Runnable{
	PoolingDataSource datasource;
	Object data;
	private String type_of_work;
	protected Statement stmt;
	public DB_Connection()
	{
		datasource=new PoolingDataSource();
		datasource.setDataSourceName("A Pooling Source");
		datasource.setServerName("localhost");
		datasource.setDatabaseName("PrivacyDB");
		datasource.setUser("xnat");
		datasource.setPassword("xnat");
		datasource.setMaxConnections(20);
	}
	public DB_Connection(SubjectInfo s,String type_of_work)
	{
		datasource=new PoolingDataSource();
		datasource.setDataSourceName("A Pooling Source");
		datasource.setServerName("localhost");
		datasource.setDatabaseName("PrivacyDB");
		datasource.setUser("xnat");
		datasource.setPassword("xnat");
		datasource.setMaxConnections(20);
		data=s;
		this.type_of_work=type_of_work;
	}
	public DB_Connection(RequestInfo r,String type_of_work)
	{
		datasource=new PoolingDataSource();
		datasource.setDataSourceName("A Pooling Source");
		datasource.setServerName("localhost");
		datasource.setDatabaseName("PrivacyDB");
		datasource.setUser("xnat");
		datasource.setPassword("xnat");
		datasource.setMaxConnections(20);
		data=r;
		this.type_of_work=type_of_work;
	}
<<<<<<< HEAD
	

	public Connection getConnection()
=======
	protected Connection getConnection()
>>>>>>> afaad2726e8ef736779f88544fced16a045b1e16
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
				if(this.type_of_work.equalsIgnoreCase("insert_into_subjectinfo"))
				{
					stmt = newcon.createStatement();
					result = stmt.executeUpdate("INSERT INTO subjectinfo VALUES('"+s.getSubjectid()+"','"+s.getFakephidata()+"','"+s.getRequestid()+"','"+s.getSubjectid()+"');");
				
					System.out.println(result);
				}
				if(this.type_of_work.equalsIgnoreCase("select_subjectid_data"))
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
				//e.printStackTrace();
				System.out.println("Foreign key con");
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
