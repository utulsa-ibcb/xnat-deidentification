package org.ibcb.xnat.redaction.interfaces;

import java.util.HashMap;
import java.util.LinkedList;

public abstract class XNATEntity {
	XNATEntity parent;
	
	String id;
	String destination_id;
	
	public abstract void download();
	public abstract void upload();
	
	public abstract String entityType();
	public abstract HashMap<String, String> redact(LinkedList<String> preserved_fields);
}
