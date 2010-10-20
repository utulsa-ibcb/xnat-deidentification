package org.ibcb.xnat.redaction;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		
		//Connection con = db.getConnection();
		SubjectInfo info = new SubjectInfo("sub_test","phi_test","req_test,","proj_test");
		DB_Connection db = new DB_Connection(info);
		//db.insert_subjectinfo(info,con);
		new Thread(db).run();
	
	}

}
