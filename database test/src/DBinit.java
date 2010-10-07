
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
public class DBinit {
	public static void queryDB(String table_name,String colum_name)
	{
		
	}
	public static void setupDB(String table_name) throws SQLException{
		 
		  System.out.println("-------- PostgreSQL JDBC Connection Testing ------------");
		  
		  try {
		    Class.forName("org.postgresql.Driver");
	 
		  } catch (ClassNotFoundException e) {
		    System.out.println("Where is your PostgreSQL JDBC Driver? Include in your library path!");
		    e.printStackTrace();
		    return;
		  }
	 
		  System.out.println("PostgreSQL JDBC Driver Registered!");
	 
		  Connection connection = null;
	 
		  try {
	 
			 connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/PrivacyDB","xnat", "xnat");
	 
		  } catch (SQLException e) {
		    System.out.println("Connection Failed! Check output console");
		    e.printStackTrace();
		    return;
		  }
	 
		  if (connection != null)
		  {
	//  System.out.println("You made it, take control your database now!");
		 
			  int res =0;
			  PreparedStatement pstat = connection.prepareStatement("SELECT COUNT (relname) FROM pg_class WHERE relname = '"+table_name+"';");
			  ResultSet rs = pstat.executeQuery();
			  while(rs.next())
			  {
				   res = rs.getInt(1);
			  }
			  System.out.println(res);
			  if (res==0)
			  {
				  System.out.println("TABLE DOES NOT EXIT, WILL CREATE ONE");
				  String cmd ="CREATE TABLE "+table_name+"(SubjectID varchar(80), FakePHIData text,RequestID varchar(80),ProjectID varchar(80) );";
				  try{
				  Statement stat=connection.createStatement();
				  stat.execute(cmd);
				  }catch(SQLException e)
				  {
					 System.err.print(e);
				  }
				  
			  }
			  else
			  {
				  System.out.println("already there");
			  }
		  }
	}

	  public static void main(String[] argv) throws SQLException {
		  DBinit myDB=new DBinit();
		  myDB.setupDB("abc");
	  }
}