package org.ibcb.xnat.redaction.config;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.ibcb.xnat.redaction.exceptions.CompileException;
import org.ibcb.xnat.redaction.helpers.Pair;

public class XNATSchema {
	Map<String, String> xnat_location_map;
	Map<String, String> xnat_name_map;
	
	public XNATSchema(){
		xnat_location_map = new HashMap<String, String>();
		xnat_name_map = new HashMap<String, String>();
	}
	

	public String getXnatFieldName(String field_id){
		if(xnat_name_map.containsKey(field_id)){
			return xnat_name_map.get(field_id);
		}
		return null;
	}
	
	private void parseLine(String line, int lnum) throws CompileException{
		if(line.endsWith("\n"))
			line = line.substring(0, line.length()-1);
		line = line.trim();
		
		try{
			String items[] = line.split("\\s+");
			
			String fieldname = items[0];
			String field_location = items[1];
			
			xnat_name_map.put(fieldname, field_location);
			xnat_location_map.put(field_location, fieldname);
			
//			System.out.println("Schema maps Field: '"+fieldname+"' to '"+field_location+"'");
			
		}catch(Exception e){
			CompileException ce = new CompileException("Compile exception caused by: " + e.getMessage() + " check code near line: " + lnum);
			ce.setStackTrace(e.getStackTrace());
			throw ce;
		}
	}
	
	public void loadXnatSchema(String file) throws IOException, FileNotFoundException, CompileException{
		BufferedReader bfr = new BufferedReader(new FileReader(file));
		String line = null;
		int lnum = 0;
		while((line = bfr.readLine()) != null){
			parseLine(line, lnum);			
			lnum++;
		}
	}
}
