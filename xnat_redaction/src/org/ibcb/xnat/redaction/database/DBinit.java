package org.ibcb.xnat.redaction.database;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
public class DBinit {
	
	//For first time use 
	//create user and database first
	//CREATE USER xnat with password 'xnat';
	//CREATE DATABASE privacydb;

	public static void setupDB(String hostname) throws SQLException{
		 
			String username="xnat";
			String pass="xnat";
		  System.out.println("-------- PostgreSQL JDBC Connection Testing ------------");
		  
		  try {
		    Class.forName("org.postgresql.Driver");
	 
		  } catch (ClassNotFoundException e) {
		    System.out.println("Can't find PostgreSQL JDBC Driver? Please include it in library path!");
		    e.printStackTrace();
		    return;
		  }
	 
		  System.out.println("PostgreSQL JDBC Driver Registered!");
	 
		  Connection connection = null;
	 
		  try {
			  
			 connection = DriverManager.getConnection("jdbc:postgresql://"+hostname+":5432/privacydb",username, pass);
	 
		  } catch (SQLException e) {
		    System.out.println("Connection Failed! Check output console");
		    e.printStackTrace();
		    return;
		  }
	 
		  if (connection != null)
		  {
	//  System.out.println("You made it, take control your database now!");
				 // System.out.println("Dont have SubjectTable, will create one");
			  if (!connection.getMetaData().getTables(null, null, "subjectinfo", null).next())
			  {
				  System.out.println("Dont have requestinfo, will create one");
				  String sequence_cmd ="CREATE SEQUENCE next_subjectid INCREMENT 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1;ALTER TABLE next_requestid OWNER TO "+username+";";
				  Statement sequence_stat=connection.createStatement();
				  try{sequence_stat.execute(sequence_cmd);}
				  catch(SQLException e)
				  {
					  System.out.println("ERROR");
					  e.printStackTrace();					  
				  }
				  System.out.println("Dont have SubjectTable, will create one");
				  String cmd ="CREATE TABLE subjectinfo(subjectid integer DEFAULT nextval('next_subjectid'::regclass), phidata text, projectid character varying(80), requestids text,  dateofbirth date, subjectname character varying(40), CONSTRAINT subjectprimary PRIMARY KEY (subjectid)) WITH (OIDS=FALSE); ALTER TABLE subjectinfo OWNER TO "+username+";";
				  Statement stat=connection.createStatement();
				  try{stat.execute(cmd);}
				  catch(SQLException e)
				  {
					  System.out.println("ERROR");
					  e.printStackTrace();
					  
				  }
			  }
			  else
				  System.out.println("Already have subjectinfo table");
			  if (!connection.getMetaData().getTables(null, null, "requestinfo", null).next())
			  {
				  System.out.println("Dont have requestinfo, will create one");
				  String sequence_cmd ="CREATE SEQUENCE next_requestid INCREMENT 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1;ALTER TABLE next_requestid OWNER TO "+username+";";
				  Statement sequence_stat=connection.createStatement();
				  try{sequence_stat.execute(sequence_cmd);}
				  catch(SQLException e)
				  {
					  System.out.println("ERROR");
					  e.printStackTrace();					  
				  }
				  String cmd ="CREATE TABLE requestinfo(requestid integer DEFAULT nextval('next_requestid'::regclass),  userid character varying(80),  date date,  adminid character varying(80), affectedsubjects text,  checkoutinfo text,  CONSTRAINT requestprimary PRIMARY KEY (requestid))WITH (  OIDS=FALSE);ALTER TABLE requestinfo OWNER TO "+username+";";
				  Statement stat=connection.createStatement();
				  try{stat.execute(cmd);}
				  catch(SQLException e)
				  {
					  System.out.println("ERROR");
					  e.printStackTrace();
					  
				  }
			  }
			  else
				  System.out.println("Already have requestinfo table");
			  if (!connection.getMetaData().getTables(null, null, "phimap", null).next())
			  {
				  System.out.println("Dont have PHImap, will create one");
				  String cmd ="CREATE TABLE PHImap(  UID integer,  PHI char[])WITH (  OIDS=FALSE);ALTER TABLE PHImap OWNER TO "+username+";";
				  Statement stat=connection.createStatement();
				  try{stat.execute(cmd);}
				  catch(SQLException e)
				  {
					  System.out.println("ERROR");
					  e.printStackTrace();
					  
				  }
			  }
			  else
				  System.out.println("Already have PHImap table");
		  }
}
	  public static void main(String[] argv) throws SQLException {
		  DBinit myDB=new DBinit();
		  myDB.setupDB("localhost");
	  }
}
