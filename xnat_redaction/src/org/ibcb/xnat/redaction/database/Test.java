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

		//SubjectInfo info = new SubjectInfo("sub_test3","phi_test3","req_test3","proj_test3");
		//RequestInfo rinfo = new RequestInfo("req_test3","usr_test3","2010-10-28","adm_test3","{test!!!,test!!!,test!!!,test!!!}");
		//DBManager db1 = new DBManager(rinfo,DBManager.INSERT_REQUESTINFO);
		//DBManager db = new DBManager(info,DBManager.INSERT_SUBJECTINFO);
		DBManager db2 = new DBManager(DBManager.QUERY_REQUESTINFO);
		
		
		//new Thread(db).run();
		//new Thread(db1).run();
		new Thread(db2).run();
		
		
			
	}

}
