import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		DB_Connection db = new DB_Connection();
		Connection con = db.getConnection();
		db.createTable(con, "abc");
		ResultSet rs =db.query("requestid", "subjectinfo", con);
		try {
			while(rs.next())
			{
				System.out.println(rs.getString("requestid"));
			}
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//db.add(con, "insert into subjectinfo values('11','22','33','44');");
		db.update(con, "update subjectinfo set subjectid ='xinchi-he' where fakephidata='test3';");
		try {
			con.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
