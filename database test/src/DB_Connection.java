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
		 
	}
	public void query()
	{
		
	}

}
