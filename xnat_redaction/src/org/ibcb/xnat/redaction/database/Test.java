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
		//DBManager db2 = new DBManager(DBManager.QUERY_REQUESTINFO);
		//DBManager db3 = new DBManager(DBManager.QUERY_SUBJECTINFO);
		//DBManager united = new DBManager(DBManager.QUERY_UNITED);
		
		
		//new Thread(db).run();
		//new Thread(db1).run();
		//new Thread(db2).run();
		//new Thread(db3).run();
		//new Thread(united).run();
		
		
		//DBManager update1 = new DBManager(DBManager.UPDATE_SUBJECTINFO,DBManager.FAKEPHIDATA,"phi_test11",DBManager.PROJECTID,"sub_test1");
		//new Thread(update1).run();
		DBManager update2 = new DBManager(DBManager.UPDATE_REQUESTINFO,DBManager.CHECKOUTINFO,"{update,update,update,update}",DBManager.ADMINID,"adm_test1");
			
		new Thread(update2).run();
	}

}