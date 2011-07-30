package org.ibcb.xnat.redaction.database;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.ibcb.xnat.redaction.config.Configuration;
public class DBinit {
	
	//For first time use 
	//create user and database first
	//CREATE USER xnat with password 'xnat';
	//CREATE DATABASE privacydb;

	public String hostname=new String();
	public String databasename=new String();
	public String user=new String();
	public String pass=new String();
	
	public void setupDB() throws SQLException{
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
			  
			 connection = DriverManager.getConnection("jdbc:postgresql://"+hostname+":5432/privacydb",user, pass);
	 
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
				  String sequence_cmd ="CREATE SEQUENCE next_subjectid INCREMENT 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1;ALTER TABLE next_requestid OWNER TO "+user+";";
				  Statement sequence_stat=connection.createStatement();
				  try{sequence_stat.execute(sequence_cmd);}
				  catch(SQLException e)
				  {
					  System.out.println("ERROR");
					  e.printStackTrace();					  
				  }
				  System.out.println("Dont have SubjectTable, will create one");
				  String cmd ="CREATE TABLE subjectinfo(subjectid integer DEFAULT nextval('next_subjectid'::regclass), phidata text, projectid character varying(80), requestids text,  dateofbirth date, subjectname character varying(40), CONSTRAINT subjectprimary PRIMARY KEY (subjectid)) WITH (OIDS=FALSE); ALTER TABLE subjectinfo OWNER TO "+user+";";
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
				  String sequence_cmd ="CREATE SEQUENCE next_requestid INCREMENT 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1;ALTER TABLE next_requestid OWNER TO "+user+";";
				  Statement sequence_stat=connection.createStatement();
				  try{sequence_stat.execute(sequence_cmd);}
				  catch(SQLException e)
				  {
					  System.out.println("ERROR");
					  e.printStackTrace();					  
				  }
				  String cmd ="CREATE TABLE requestinfo(requestid integer DEFAULT nextval('next_requestid'::regclass),  userid character varying(80),  date date,  adminid character varying(80), affectedsubjects text,  checkoutinfo text,  CONSTRAINT requestprimary PRIMARY KEY (requestid))WITH (  OIDS=FALSE);ALTER TABLE requestinfo OWNER TO "+user+";";
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
			  if (!connection.getMetaData().getTables(null, null, "subjectidmap", null).next())
			  {
				  System.out.println("Dont have subjectidmap, will create one");
				  String cmd ="CREATE TABLE subjectidmap(  subjectid integer NOT NULL,  xnatid text,  CONSTRAINT mapprimarykey PRIMARY KEY (subjectid))WITH (  OIDS=FALSE);ALTER TABLE subjectidmap OWNER TO "+user+";";
				  Statement stat=connection.createStatement();
				  try{stat.execute(cmd);}
				  catch(SQLException e)
				  {
					  System.out.println("ERROR");
					  e.printStackTrace();
					  
				  }
			  }
			  else
				  System.out.println("Already have subjectidmap table");
			  
			  if (!connection.getMetaData().getTables(null, null, "resourcemap", null).next())
			  {
				  System.out.println("Dont have resourcemap, will create one");
				  String cmd ="CREATE TABLE resourcemap( type character varying(20), src_project character varying(20), dest_project  character varying(20), src_rid  character varying(20), dest_rid  character varying(20), CONSTRAINT rmapprimary PRIMARY KEY (type,src_project,dest_project,src_rid))WITH (  OIDS=FALSE);ALTER TABLE resourcemap OWNER TO "+user+";CREATE INDEX fastfind ON resourcemap (type, src_project, src_rid);";
				  Statement stat=connection.createStatement();
				  try{stat.execute(cmd);}
				  catch(SQLException e)
				  {
					  System.out.println("ERROR");
					  e.printStackTrace();
					  
				  }
			  }
			  else
				  System.out.println("Already have resourcemap table");
			  
			  
			  if (!connection.getMetaData().getTables(null, null, "projectlock", null).next())
			  {
				  System.out.println("Dont have projectlock, will create one");
				  String cmd ="CREATE TABLE projectlock(  inuse boolean,  projectid character varying(80) NOT NULL,  CONSTRAINT projectlockprimarykey PRIMARY KEY (projectid))WITH (  OIDS=FALSE);ALTER TABLE projectlock OWNER TO "+user+";";
				  Statement stat=connection.createStatement();
				  try{stat.execute(cmd);}
				  catch(SQLException e)
				  {
					  System.out.println("ERROR");
					  e.printStackTrace();
					  
				  }
			  }
			  else
				  System.out.println("Already have projectlock table");
		  }
}
	  public static void main(String[] argv) throws SQLException {
		  DBinit myDB=new DBinit();
		  myDB.hostname=Configuration.instance().getProperty("database_hostname");
		  myDB.databasename=Configuration.instance().getProperty("database_name");
		  myDB.user=Configuration.instance().getProperty("database_user");
		  myDB.pass=Configuration.instance().getProperty("database_pass");
		  //Configuration.instance().getProperty(name)
		  myDB.setupDB();
	  }
}
