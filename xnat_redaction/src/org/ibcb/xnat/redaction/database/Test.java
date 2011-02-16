package org.ibcb.xnat.redaction.database;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Array;
import java.util.HashMap;


public class Test {

	/**
	 * @param args
	 * @throws SQLException 
	 */
	public static void main(String[] args) throws SQLException {
		// TODO Auto-generated method stub
		DBinit newinit=new DBinit();
		newinit.setupDB();
		DBManager singlethreadtest=new DBManager(DBManager.SINGLE_THREAD);
		System.out.println("try to insert subject info record");
		SubjectInfo sinfo=new SubjectInfo("test001","PatientName,Liang;PatientAge,99;StudyDate,10/20/10;","testproject","testrequest001;");
		singlethreadtest.insertSubjectInfo(sinfo);
		System.out.println("try to insert request info record");
		RequestInfo rinfo=new RequestInfo("testrequest1","testuser1","2011-1-23","admin","test001;");
		singlethreadtest.insertRequestInfo(rinfo);
	}

}
