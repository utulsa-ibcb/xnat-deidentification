import java.sql.*;
public class DB_Connection {
	
	public Connection getConnection()
	{
		Connection con = null;
		try {
			Class.forName("org.postgresql.Driver");
			try {
				con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/PrivacyDB","xnat", "xnat");
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
	public void createTable(Connection con,String table_name)
	{
		 int res = 1;
		 try {
			PreparedStatement pstat = con.prepareStatement("SELECT COUNT (relname) FROM pg_class WHERE relname = '"+table_name+"';");
			ResultSet rs = pstat.executeQuery();
			while(rs.next())
			{
				res = rs.getInt(1);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(res==0)
		{
			System.out.println("TABLE DOES NOT EXIT, WILL CREATE ONE");
			String cmd ="CREATE TABLE "+table_name+"(SubjectID varchar(80), FakePHIData text,RequestID varchar(80),ProjectID varchar(80) );";
			try{
				Statement stat=con.createStatement();
				stat.execute(cmd);
			}catch(SQLException e)
			{
				System.err.print(e);
			}
			
		}
		else
		{
			System.out.println("TABLE ALREADY EXIST!!");
		}
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

}
