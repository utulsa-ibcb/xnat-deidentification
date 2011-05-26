package org.ibcb.xnat.redaction.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

import org.ibcb.xnat.redaction.exceptions.CompileException;
import org.ibcb.xnat.redaction.helpers.Pair;

public class RedactionRuleset {
	public static int OP_NONE=0;
	public static int OP_ELAPSED_TIME=1;	
	
	public static int PROTO_DICOM=0;
	public static int PROTO_XNAT=1;
	
	public static class Rule {
		int protocol;
		String field;
		
		boolean redact;
		
		boolean translate;
		int operation;
		String destination;
	}
	
	private LinkedList<Rule> ruleset;
	
	public LinkedList<String> getFieldNames(){
		LinkedList<String> fields = new LinkedList<String>();
		
		for(Rule r : ruleset){
			if(!fields.contains(r.field)){
				fields.add(r.field);
			}
		}
		
		return field;
		
	}
	
	private int getOperationId(String op){
		if(op.toLowerCase().equals("timelapse"))
			return OP_ELAPSED_TIME;
		
		return -1;
	}
	
	private int getProtocolId(String proto){
		if(proto.toLowerCase().equals("dicom"))
			return PROTO_DICOM;
		if(proto.toLowerCase().equals("xnat"))
			return PROTO_XNAT;
		return -1;
	}
	
	public boolean redact(int protocol, String field){
		for(Rule r : ruleset){
			if(r.protocol==protocol && r.field.equals(field) && r.redact){
				return true;
			}
		}
		
		return false;
	}
	
	public boolean translate(int protocol, String field){
		for(Rule r : ruleset){
			if(r.protocol==protocol && r.field.equals(field) && r.translate){
				return true;
			}
		}
		
		return false;
	}
	
	public void parseRuleset(String file) throws CompileException{
		ruleset = new LinkedList<Rule>();
		String line = "";
		int lnum = 0;
		BufferedReader bfr = null;
		try{
			bfr = new BufferedReader(new FileReader(file));
			
			while((line = bfr.readLine()) != null)
			{
				if(line.endsWith("\n"))
					line = line.substring(0, line.length()-1);
				
				String items[] = line.split("\\s+");
				Rule r = new Rule();
				
				r.protocol = getProtocolId(items[0]);
				r.field = items[1];
				
				String mode = items[2];
				
				
				if(mode.toLowerCase().equals("translate")){
					r.translate=true;
					r.redact=false;
					
					r.destination = items[4];
					r.operation = getOperationId(items[3]);
				}
				else{
					r.translate=false;
					r.redact=true;
				}
				
				ruleset.add(r);
				
				lnum++;
			}
		}catch(IOException e){
			try{
				if(bfr!=null) bfr.close();
			}catch(IOException ioe){}
			
			CompileException ce = new CompileException(e.getMessage());
			ce.setStackTrace(e.getStackTrace());
			throw ce;
		}
		
	}
}