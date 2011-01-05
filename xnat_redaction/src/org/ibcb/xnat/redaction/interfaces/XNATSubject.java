package org.ibcb.xnat.redaction.interfaces;

import java.util.HashMap;
import java.util.LinkedList;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

public class XNATSubject {
	DOMParser xml;
	
	String id;
	
	boolean redacted=false;
	
	HashMap<String,String> demographics;
	
	LinkedList<String> experiment_ids=new LinkedList<String>();
}
