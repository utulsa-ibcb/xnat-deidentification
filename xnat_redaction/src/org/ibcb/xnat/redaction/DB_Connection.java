package org.ibcb.xnat.redaction;

import java.sql.*;
import org.postgresql.jdbc2.optional.PoolingDataSource;
//import org.postgresql.jdbc4.Jdbc4Connection;

//import java.sql.;
public class DB_Connection extends Thread{
	PoolingDataSource datasource;
	Object data;
	public DB_Connection()
	{
		datasource=new PoolingDataSource();   //Use pooling data source to provide connection pool
		datasource.setDataSourceName("A Pooling Source");
		datasource.setServerName("localhost");
		datasource.setDatabaseName("PrivacyDB");
		datasource.setUser("xnat");
		datasource.setPassword("xnat");
		datasource.setMaxConnections(20);
	}
	public DB_Connection(SubjectInfo s)
	{
		datasource=new PoolingDataSource();
		datasource.setDataSourceName("A Pooling Source");
		datasource.setServerName("localhost");
		datasource.setDatabaseName("PrivacyDB");
		datasource.setUser("xnat");
		datasource.setPassword("xnat");
		datasource.setMaxConnections(20);
		data=s;
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
				Statement stmt = newcon.createStatement();
				result = stmt.executeUpdate("INSERT INTO subjectinfo VALUES('"+s.getSubjectid()+"','"+s.getFakephidata()+"','"+s.getRequestid()+"','"+s.getSubjectid()+"');");
				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("Foreign key con");
			}
			
			
		}
	}

}
