package org.ibcb.xnat.redaction.config;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.ibcb.xnat.redaction.exceptions.CompileException;
import org.ibcb.xnat.redaction.helpers.Pair;

public class DICOMSchema {
	Map<String, Pair<Integer, Integer>> dicom_location_map;
	Map<Pair<Integer, Integer>, String> dicom_name_map;
	
	public DICOMSchema(){
		dicom_location_map = new HashMap<String, Pair<Integer, Integer>>();
		dicom_name_map = new HashMap<Pair<Integer, Integer>, String>();
	}
	
	public Set<String> getMappedFields(){
		return dicom_location_map.keySet();
	}
	
	public Pair<Integer, Integer> getDICOMFieldLocation(String field_name){
		if(dicom_location_map.containsKey(field_name)){
			return dicom_location_map.get(field_name);
		}
		return null;
	}

	public String getDICOMFieldName(Pair<Integer, Integer> field_id){
		if(dicom_name_map.containsKey(field_id)){
			return dicom_name_map.get(field_id);
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
			
			field_location = field_location.substring(field_location.indexOf("(")+1, field_location.indexOf(")"));
			String items2[] = field_location.split(",");
			
			int location = Integer.parseInt(items2[0], 16);
			int offset = Integer.parseInt(items2[1], 16);
			
			Pair<Integer, Integer> p = new Pair<Integer, Integer>(location, offset);
			dicom_location_map.put(fieldname, p);
			dicom_name_map.put(p, fieldname);
			
		}catch(Exception e){
			CompileException ce = new CompileException("Compile exception caused by: " + e.getMessage() + " check code near line: " + lnum);
			ce.setStackTrace(e.getStackTrace());
			throw ce;
		}
	}
	
	public void loadDicomSchema(String file) throws IOException, FileNotFoundException, CompileException{
		BufferedReader bfr = new BufferedReader(new FileReader(file));
		String line = null;
		int lnum = 0;
		while((line = bfr.readLine()) != null){
			parseLine(line, lnum);			
			lnum++;
		}
	}
}
