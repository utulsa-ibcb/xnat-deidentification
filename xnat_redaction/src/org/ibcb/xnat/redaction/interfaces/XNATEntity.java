package org.ibcb.xnat.redaction.interfaces;

import java.util.HashMap;
import java.util.LinkedList;

public abstract class XNATEntity {
	protected XNATEntity parent;
	
	protected String parent_type;
	protected String entity_type;
	
	protected String id;
	protected String destination_id;
	
	public XNATEntity getParent(){
		return parent;
	}
	
	public void setParent(XNATEntity e){
		this.parent=e;
	}
	
	public String getParentType(){
		return parent_type;
	}
	
	public String getEntityType(){
		return entity_type;
	}
	
	public abstract String getPath();
	
	public String getID(){
		return id;
	}
	
	public String getDestinationID(){
		return destination_id;
	}
	
	public void setDestinationID(String id){
		this.destination_id = id;
	}
	
	public abstract void download();
	public abstract void upload();
	
	public abstract XNATEntity create(String id);
	
	public abstract HashMap<String, String> redact(LinkedList<String> preserved_fields);
	
	private static HashMap<String, XNATEntity> entityClasses = new HashMap<String, XNATEntity>();
	
	static{
		XNATEntity t;
		
		t = new XNATScan();
		entityClasses.put(t.entity_type, t);
		
		t = new XNATScanFile();
		entityClasses.put(t.entity_type, t);
		
		t = new XNATProject();
		entityClasses.put(t.entity_type, t);
		
		t = new XNATExperiment();
		entityClasses.put(t.entity_type, t);
		
		t = new XNATSubject();
		entityClasses.put(t.entity_type, t);
	}
	
	public static XNATEntity getEntity(String type, String id){
		if(entityClasses.containsKey(type))
			return entityClasses.get(type).create(id);
		
		return null;
	}
}
