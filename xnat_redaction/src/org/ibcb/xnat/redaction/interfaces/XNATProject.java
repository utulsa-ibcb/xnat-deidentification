package org.ibcb.xnat.redaction.interfaces;

import java.util.HashMap;
import java.util.LinkedList;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

public class XNATProject extends XNATEntity{
	public DOMParser xml;
	
	public String id;
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
	}
	
	public String toString(){
		return "ID:          "+id+"\nName:        "+name+"\nDescription: "+description;
	}
	
	public void print(){
		System.out.println("XNAT Project: " + id);
		System.out.println("Name: " + name);
		System.out.println("Description: " + description);
		
		System.out.println("Experiments:");
		for(String eid : experiment_ids){
			System.out.println(eid);
		}
		
		System.out.println("\n\nSubjects:");
		for(String sid : subject_ids){
			subjects.get(sid).print();
		}
	}
	
	public XNATEntity create(String id){
		XNATProject exp = new XNATProject();
		exp.id = id;
		
		return exp;
	}
	
	public String getPath(){
		return "/data/archive/projects/"+ this.id; 
	}
	
	public void download() {
		
	}
	
	public String entityType() {
		
		
		return null;
	}
	
	public HashMap<String, String> redact(LinkedList<String> preservedFields) {
		
		
		return null;
	}
	public void upload() {
		
		
	}
}
