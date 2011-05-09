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
		//DBinit newinit=new DBinit();
		//newinit.setupDB("129.244.244.25");
		DBManager singlethreadtest=new DBManager(DBManager.SINGLE_THREAD,"localhost");
		System.out.println("try to insert subject info record001");
		SubjectInfo s1info=new SubjectInfo("1","PatientName,Liang;PatientAge,99;StudyDate,10/20/10;","testproject","testrequest001;","liang","05/19/99");
		singlethreadtest.insertSubjectInfo(s1info);
		System.out.println("try to insert subject info record002");
		SubjectInfo s2info=new SubjectInfo("2","PatientName,Liang;PatientAge,99;","testproject","testrequest002;","Richard","06/16/10");
		singlethreadtest.insertSubjectInfo(s2info);
		System.out.println("try to insert request info record");
		RequestInfo r1info=new RequestInfo("testuser1","2011-1-23","admin","1;","PatientName;PatientAge;StudyDate;");
		singlethreadtest.insertRequestInfo(r1info);
		System.out.println("try to insert request info record");
		RequestInfo r2info=new RequestInfo("testuser1","2011-1-25","admin","2;","PatientName;PatientAge;");
		singlethreadtest.insertRequestInfo(r2info);
		singlethreadtest.getUserCheckOutInfo("testuser1");
	}

}
