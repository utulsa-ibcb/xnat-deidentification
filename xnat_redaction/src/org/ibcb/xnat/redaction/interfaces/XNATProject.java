package org.ibcb.xnat.redaction.interfaces;

import java.util.HashMap;
import java.util.LinkedList;

import org.w3c.dom.Node;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

public class XNATProject extends XNATEntity{
	
	public String name;
	public String description;
	
	public LinkedList<String> subject_ids;
	public LinkedList<String> experiment_ids;
	
	public HashMap<String, XNATSubject> subjects;
	public HashMap<String, XNATExperiment> experiments;
	
	public XNATProject(){
		subject_ids = new LinkedList<String>();
		experiment_ids = new LinkedList<String>();
		
		subjects = new HashMap<String, XNATSubject>();

		experiments = new HashMap<String, XNATExperiment>();
		
		this.entity_type = "projects";
		this.parent_type  = null;
		
		this.xmlIDField = "ID";
	}
	
	public String toString(){
		return "ID:          "+id+"\nName:        "+name+"\nDescription: "+description;
	}
	
	public XNATEntity create(String id){
		XNATProject exp = new XNATProject();
		exp.id = id;
		
		return exp;
	}
	
	public String getPath(){
		return "/projects/"+ this.id; 
	}
	
	public String getDestinationPath(){
		return "/projects/"+ this.destination_id; 
	}
	
	public void download() {
		XNATRestAPI.instance().retrieveResource(this);
		
		Node proj_node = xml.getDocument().getElementsByTagName("xnat:Project").item(0);
		
//		System.out.println("Nodes: " + proj_node.getChildNodes().getLength());
		for(int s = 0; s < proj_node.getChildNodes().getLength(); s++){
			Node n = proj_node.getChildNodes().item(s);
			
//			System.out.println("Node: " + n.getPrefix()+":"+n.getLocalName());
			
			if(n.getPrefix() != null && n.getPrefix().equalsIgnoreCase("xnat")){
				if(n.getLocalName().equalsIgnoreCase("name")){
					this.name = n.getTextContent();
				}
				else if(n.getLocalName().equalsIgnoreCase("description")){
					this.description = n.getTextContent();
				}
			}
		}
		
		System.out.println(this.toString());
	}
	
	public HashMap<String, String> getRedactedData(){
		return null;
	}
	
	public void redact() {

	}
	
	public void upload() {
		 
		
	}
}
