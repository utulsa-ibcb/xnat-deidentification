package org.ibcb.xnat.redaction.client;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class DataBaseConnector {
	public static int PLATFORM_POSTGRES=1;
	public static int PLATFORM_SQLITE=2;
	public static int PLATFORM_MYSQL=3;
	private static int batch_update_max_size=100;
	private static int fetch_size_limit=100;
	
	private int platform;
	
	private static class Table {
		String name;
		LinkedList<String> fields = new LinkedList<String>();
		LinkedList<String> fieldTypes = new LinkedList<String>();
	}
	
	private static class OpenQuery{
		int quid;
		ResultSet dataset;
		ResultSet genKeys;
		Statement datastatement;
	}
	
	private LinkedList<Table> tables;
	private HashMap<Integer, OpenQuery> queries;
	
	private static int quid = 0;
	
	private Connection con = null;
	
	public boolean ready(){
		try{
			return con !=null && !con.isClosed();
		}catch(SQLException e){
			e.printStackTrace();
			return false;
		}
	}
	
	public DataBaseConnector(String database, String user, String pwd, String URL, int platform){
		this.platform=platform;
		if(URL!=null){
			try {
				if(platform==PLATFORM_MYSQL){
					System.out.println("Getting Driver");
				      Class.forName("com.mysql.jdbc.Driver").newInstance();
				      System.out.println("Getting Connection");
				      con = DriverManager.getConnection("jdbc:mysql://"+URL, user, pwd);
				      
				      if(!con.isClosed())
				    	  System.out.println("Successfully connected to " +
				          						"MySQL server using TCP/IP...");
				      
				      if(database!=null)
				    	  setDatabase(database);
				      
				      queries = new HashMap<Integer, OpenQuery>();
				      tables = new LinkedList<Table>();
				      
				      
				      // get tables
				}
				else if(platform==PLATFORM_SQLITE){

					System.out.println("Getting Driver");
				      Class.forName("org.sqlite.JDBC").newInstance();
				      System.out.println("Getting Connection");
				      con = DriverManager.getConnection("jdbc:sqlite://"+URL);
				      
				      if(!con.isClosed())
				    	  System.out.println("Successfully connected to " +
				          						"MySQL server using TCP/IP...");
				      
				      if(database!=null)
				    	  setDatabase(database);
				      
				      queries = new HashMap<Integer, OpenQuery>();
				      tables = new LinkedList<Table>();
				      
				      
				      // get tables
				}
				else if(platform == PLATFORM_POSTGRES){
					Class.forName("org.postgresql.Driver").newInstance();
					  
					con = DriverManager.getConnection("jdbc:postgresql://"+URL+"/"+database+"?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory", user, pwd);
					
					  
					if(!con.isClosed())
						System.out.println("Successfully connected to " +
											"PostgreSQL server using TCP/IP...");
					  
					//		      setDatabase(database);
					queries = new HashMap<Integer, OpenQuery>();
					tables = new LinkedList<Table>();
					// get tables;
					  
					Statement st = con.createStatement();
					ResultSet rs = st.executeQuery("SELECT * FROM pg_tables WHERE NOT (tablename LIKE 'sql%' OR tablename LIKE 'pg%')");
					  
					while(rs.next()){
						String name = rs.getString("tablename");
						Table t = new Table();
						t.name=name;
						Statement st2 = con.createStatement();
						ResultSet rs2 = st2.executeQuery("select column_name, data_type from information_schema.columns where table_schema = 'public' and table_name = '"+name+"'");
						  
						while(rs2.next()){
							t.fields.add(rs2.getString("column_name"));
							t.fieldTypes.add(rs2.getString("data_type"));
						}
							  
							  
						rs2.close();
						st2.close();
						tables.add(t);
					}
					  
					  
					rs.close();
					st.close();
				}
		    } catch(Exception e) {
		      e.printStackTrace();
		      System.exit(1);
		    }
		}
	}
	
	public static class Column {
		String datatype;
		String name;
		String def;
		
		public Column(ResultSet rs) throws SQLException{
			datatype = rs.getString("Type");
			name = rs.getString("Field");
			def = rs.getString("Default");
		}

		public String getDatatype() {
			return datatype;
		}

		public void setDatatype(String datatype) {
			this.datatype = datatype;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDef() {
			return def;
		}

		public void setDef(String def) {
			this.def = def;
		}
	}
	
	public LinkedList<Column> getColumns(String table, String db){
		String query = "SHOW COLUMNS FROM `"+table+"` FROM `"+db+"`;";
		int quid=this.openQuery(query);
		ResultSet rs = this.getResults(quid);
		LinkedList<Column> columns = new LinkedList<Column>();
		try{
			while(rs.next()){
				columns.add(new Column(rs));
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		
		this.closeQuery(quid);
		
		return columns;
	}
	
	public LinkedList<String> getTables(String db){
		LinkedList<String> tables = new LinkedList<String>();
		
		String sql = "SHOW TABLES IN `"+db+"`;";
		
		int quid=this.openQuery(sql);
		ResultSet rs = this.getResults(quid);
		
		try{
			while(rs.next()){
				tables.add(rs.getString(1));
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		
		this.closeQuery(quid);
		
		return tables;
	}
	
	public LinkedList<String> getDatabases(){
		LinkedList<String> dbs = new LinkedList<String>();
		
		String sql = "SHOW DATABASES;";
		
		int quid=this.openQuery(sql);
		ResultSet rs = this.getResults(quid);
		
		try{
			while(rs.next()){
				dbs.add(rs.getString(1));
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		
		this.closeQuery(quid);
		
		return dbs;
	}
	
	public void executeBatch(List<String> sql){
		LinkedList<String> sql2 = new LinkedList<String>();
			
		for(String s : sql){
			sql2.add(s);
		}
		
		try{
			
			while(sql2.size()>0){
				Statement st = con.createStatement();
				int cnt = 0;
				
				while(sql2.size()>0 && cnt < batch_update_max_size){
					st.addBatch(sql2.remove());
					cnt++;
				}
				
				st.executeBatch();
				st.close();
			}
		}
		catch(SQLException e){
			if(e instanceof java.sql.BatchUpdateException){
				//e.printStackTrace();
			}
			int cnt=0;
			for(String s : sql){
				System.out.println("Query "+(cnt++)+":" + s);
			}
			e.printStackTrace();
		}
	}
	
	public void executeUpdate(String sql){
		try{
			Statement s = con.createStatement();
			
			s.executeUpdate(sql);
			
			s.close();
		}
		catch(SQLException e){
			System.out.println("Query:" + sql);
			e.printStackTrace();
		}
	}
	
	public int executeUpdateGetKeys(String sql){
		return executeUpdateGetKeys(sql,null);
	}
	
	public int executeUpdateGetKeys(String sql, String seq_name){
		OpenQuery oq = new OpenQuery();
		try{
			if(platform==PLATFORM_MYSQL){
				Statement s = con.createStatement();
				oq.datastatement = s;
				s.executeUpdate(sql,
	                   Statement.RETURN_GENERATED_KEYS);
			}
			else if(platform==PLATFORM_POSTGRES){
				PreparedStatement s = con.prepareStatement(sql + "SELECT currval('"+seq_name+"');");
				oq.datastatement = s; 
				s.execute();
			}
			queries.put(quid, oq);
			
			return quid++;
		}
		catch(SQLException e){
			System.out.println("Query:" + sql + "SELECT currval('"+seq_name+"');");
			e.printStackTrace();
			try{
				if(oq.datastatement!=null && !oq.datastatement.isClosed()){
					oq.datastatement.close();
				}
			}catch(SQLException e2){
				e2.printStackTrace();
			}
		}
		return -1;
	}
	
	public ResultSet getGeneratedKeys(int quid){
		if(quid<0) return null;
		if(queries.containsKey(quid)){
			OpenQuery oq = queries.get(quid);
			try{
				if(platform==PLATFORM_MYSQL){
					oq.genKeys = oq.datastatement.getGeneratedKeys();
					return oq.genKeys;
				}else if(platform==PLATFORM_POSTGRES){
					int nInserted = oq.datastatement.getUpdateCount();
					
					if(nInserted==1 && oq.datastatement.getMoreResults()){
						oq.genKeys = oq.datastatement.getResultSet();
						return oq.genKeys;
					}
					else {						
						return null;
					}
				}
			}
			catch(SQLException e){
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public int openQuery(String sql){
		OpenQuery q = new OpenQuery();
		try{
			q.quid=quid;
			q.datastatement = con.createStatement();
			
			q.datastatement.setFetchSize(fetch_size_limit);
			
			q.dataset = q.datastatement.executeQuery(sql);
			q.dataset.setFetchSize(fetch_size_limit);
			
			queries.put(quid, q);
			return quid++;
		}
		catch(SQLException e){
			System.out.println("Query:" + sql);
			e.printStackTrace();
			try{
				if(q.dataset!=null && !q.dataset.isClosed()){
					q.dataset.close();
				}
				if(q.datastatement!=null && !q.datastatement.isClosed()){
					q.datastatement.close();
				}
			}
			catch(SQLException e2){
				e2.printStackTrace();
				System.err.println("Fatal Error in deallocating sql statements");
				System.exit(1);
			}
		}
		return -1;
	}
	
	public ResultSet getResults(int quid){
		if(quid<0) return null;
		if(queries.containsKey(quid)){
			OpenQuery oq = queries.get(quid);
			return oq.dataset;
		}
		return null;
	}
	
	public void closeQuery(int quid){
		if(quid<0) return;
		if(queries.containsKey(quid)){
			OpenQuery oq = queries.get(quid);
			try{
				if(oq.dataset!=null && !oq.dataset.isClosed())
					oq.dataset.close();
				if(oq.genKeys!=null && !oq.genKeys.isClosed())
					oq.genKeys.close();
				if(oq.datastatement!=null && !oq.datastatement.isClosed())
					oq.datastatement.close();
			}
			catch(SQLException e){
				e.printStackTrace();
			}
			queries.remove(quid);
		}
	}
	
	private void setDatabase(String dbName)
	{
		try {
		Statement stmt;
			stmt = con.createStatement();
			stmt.execute("USE " + dbName + ";");
			stmt.close();
		}
		catch (Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}
}
