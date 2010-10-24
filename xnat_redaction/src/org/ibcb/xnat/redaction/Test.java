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

		SubjectInfo info = new SubjectInfo("sub_test","phi_test","req_test","proj_test");
		//RequestInfo rinfo = new RequestInfo("req_test","usr_test","2010-10-21","adm_test","{test1,test2,test3,test4}");
		DB_Connection db = new DB_Connection(info,"insert_into_subjectinfo");
		//DB_Connection db1 = new DB_Connection(rinfo,"");
	
		new Thread(db).run();
		//new Thread(db1).run();
	
			
	}

}
