package org.ibcb.xnat.redaction.database;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		SubjectInfo info = new SubjectInfo("sub_test","phi_test","req_test","proj_test");
		RequestInfo rinfo = new RequestInfo("req_test","usr_test","2010-10-21","adm_test","{test1,test2,test3,test4}");
		DBManager db1 = new DBManager(rinfo,DBManager.INSERT_REQUESTINFO);
		DBManager db = new DBManager(info,DBManager.INSERT_SUBJECTINFO);
		
		new Thread(db1).run();
		
		new Thread(db).run();
		
			
	}

}
