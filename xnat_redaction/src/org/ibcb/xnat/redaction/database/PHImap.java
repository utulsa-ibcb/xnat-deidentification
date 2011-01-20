package org.ibcb.xnat.redaction.database;
import java.sql.*;

import java.util.HashMap;
public class PHImap {
	private HashMap<Integer,String> PHIMap;
	public String getPHIname(int phi_id)
	{
		if (PHIMap.containsKey(phi_id))
		{
			return PHIMap.get((Integer)phi_id);			
		}
		else
		return null;
	}
	public int getPHIId(String PHI)
	{
		if (PHIMap.containsValue(PHI))
		{
			for (Integer o:PHIMap.keySet())
			{
				if (PHIMap.get(o).equals(PHI))
					return (int)o;			
			}
		}
		return -1;
	}
	public PHImap(DBManager db)
	{
		PHIMap=new HashMap<Integer,String>();
		Connection con=db.getConnection();
		Statement stmt;
		try {
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM PHImap;");
			while(rs.next())
			{
				
				String PHI=rs.getString("PHI");
				int UID=Integer.parseInt(rs.getString("UID"));
				PHIMap.put((Integer)UID,PHI);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

}
