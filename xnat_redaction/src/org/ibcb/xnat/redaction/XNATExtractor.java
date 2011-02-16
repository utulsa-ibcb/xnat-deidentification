package org.ibcb.xnat.redaction;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.SpecificCharacterSet;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.ibcb.xnat.redaction.config.Configuration;
import org.ibcb.xnat.redaction.config.DICOMSchema;
import org.ibcb.xnat.redaction.config.RedactionRuleset;
import org.ibcb.xnat.redaction.config.XNATSchema;
import org.ibcb.xnat.redaction.exceptions.PipelineServiceException;
import org.ibcb.xnat.redaction.helpers.Pair;
import org.ibcb.xnat.redaction.interfaces.RedactionPipelineService;
import org.w3c.dom.Node;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

public class XNATExtractor extends RedactionPipelineService{
	
	static XNATExtractor singleton = null;
	XNATSchema schema;
	
	public XNATSchema getSchema(){
		return schema;
	}
	
	public XNATExtractor(){
		schema = null;
	}
	
	public static XNATExtractor instance(){
		if(singleton==null) singleton=new XNATExtractor();
		return singleton;
	}
	
	public void initialize() throws PipelineServiceException {
		String file=null;
		try{
			schema = new XNATSchema();
			file=Configuration.instance().getProperty("xnat_schema");
			schema.loadXnatSchema(file);
		}catch(Exception e){
			PipelineServiceException pe = new PipelineServiceException("Error loading schema file from `"+file+"`: " + e.getMessage());
			pe.setStackTrace(e.getStackTrace());
			throw pe;
		}
	}
	
	public void run(){
		
		boolean running=true;
		
		while(running){
			if(shuttingDown()){
				// make sure all jobs are done
				// and all child threads have finished running
				// then set
				running=false;
			}
			// check for jobs
			// start processing jobs
		}
		// notify threads that we are exiting. The Main class will wait for this notification, if the system is shutting down.
		
		this.notifyAll();
	}
	
	public void insertData(DOMParser xnat_subject, String field, String value){
		SpecificCharacterSet scs = new SpecificCharacterSet("latin1"); 
		
		HashMap<String,String> demographics = new HashMap<String,String>();
		
		Node s_node = xnat_subject.getDocument().getElementsByTagName("xnat:Subject").item(0);
	
		for(int i = 0; i < s_node.getChildNodes().getLength(); i++){
			Node x_node = s_node.getChildNodes().item(i);
			
			if(x_node.getLocalName() != null && x_node.getPrefix().equalsIgnoreCase("xnat") && x_node.getLocalName().equalsIgnoreCase("demographics")){
				Node fieldTag = xnat_subject.getDocument().createElement(field);
				fieldTag.setPrefix("xnat");
				fieldTag.setTextContent(value);
				x_node.appendChild(fieldTag);
			}
		}
	}
	
	public HashMap<String, String> extractNameValuePairs(DOMParser xnat_subject, boolean redact, RedactionRuleset rules){

		SpecificCharacterSet scs = new SpecificCharacterSet("latin1"); 
	
		HashMap<String,String> demographics = new HashMap<String,String>();
		
		Node s_node = xnat_subject.getDocument().getElementsByTagName("xnat:Subject").item(0);
	
		for(int i = 0; i < s_node.getChildNodes().getLength(); i++){
			Node x_node = s_node.getChildNodes().item(i);
			
			if(x_node.getLocalName() != null && x_node.getPrefix().equalsIgnoreCase("xnat") && x_node.getLocalName().equalsIgnoreCase("demographics")){
				
				for(int j = 0; j  < x_node.getChildNodes().getLength(); j++){
					Node y_node = x_node.getChildNodes().item(j);
					
					if(y_node.getLocalName()!=null){
						String tag_name = (y_node.getPrefix()!=null ? y_node.getPrefix()+":" : "") + y_node.getLocalName();
						
						String fieldname = schema.getXnatFieldName(tag_name);
						
						demographics.put(fieldname, y_node.getTextContent());
						
						System.out.println("Tag: " + tag_name + " mapped to: " + fieldname);
						
						if(redact && rules.redact(RedactionRuleset.PROTO_XNAT, fieldname)){
							y_node.setTextContent("");
							System.out.println("Redacted: " + fieldname);
						}
					}
				}
			}
		}
		
		return demographics;
	}
	
	public static void main(String args[]) throws PipelineServiceException{
		XNATExtractor de = XNATExtractor.instance();
		de.initialize();
	
		RedactionRuleset rules = new RedactionRuleset();
		try{
			rules.parseRuleset(Configuration.instance().getProperty("redaction_rules"));
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		
	}
}
