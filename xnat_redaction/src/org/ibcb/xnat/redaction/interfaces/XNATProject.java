package org.ibcb.xnat.redaction.interfaces;

import java.util.HashMap;
import java.util.LinkedList;

public class XNATProject {
	String id;
	String name;
	String description;
	
	LinkedList<String> subject_ids;
	LinkedList<String> experiment_ids;
	
	HashMap<String, XNATSubject> subjects;
	HashMap<String, XNATExperiment> experiments;
	
	public XNATProject(){
		subject_ids = new LinkedList<String>();
		experiment_ids = new LinkedList<String>();
		
		subjects = new HashMap<String, XNATSubject>();

		experiments = new HashMap<String, XNATExperiment>();
	}
	
	public String toString(){
		return "ID:          "+id+"\nName:        "+name+"\nDescription: "+description;
	}
}
