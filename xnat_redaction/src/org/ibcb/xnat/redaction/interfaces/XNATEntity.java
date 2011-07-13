package org.ibcb.xnat.redaction.interfaces;

import java.io.IOException;
import java.rmi.ConnectException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import javax.xml.transform.TransformerException;

import org.ibcb.xnat.redaction.interfaces.XNATResultSet.Row;
import org.xml.sax.SAXException;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

public abstract class XNATEntity {
	protected DOMParser xml;
	protected Row xml_listing;
	
	protected XNATEntity parent;
	
	protected String xmlIDField;
	
	protected String parent_type;
	protected String entity_type;
	
	protected String id;
	protected String destination_id;
	
	HashMap<String, XNATEntity> children = new HashMap<String, XNATEntity>();
	
	public void addChild(XNATEntity e){
		children.put(e.getID(), e);
		e.setParent(this);
	}
	
	public Collection<XNATEntity> getChildren(){
		return children.values();
	}
	
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
	
	public void setXML(DOMParser dom){
		xml=dom;
	}
	
	public DOMParser getXML(){
		return xml;
	}
	
	public String getID(){
		return id;
	}
	
	public void setID(String id){
		this.id=id;
	}
	
	public String getDestinationID(){
		return destination_id;
	}
	
	public void setDestinationID(String id){
		this.destination_id = id;
	}
	
	public abstract String getPath();
	public abstract String getDestinationPath();
	
	public abstract HashMap<String, String> getRedactedData();
	
	public abstract void download() throws IOException, SAXException, ConnectException, TransformerException;
	public abstract void upload() throws IOException, SAXException, ConnectException, TransformerException;
	
	public abstract XNATEntity create(String id);
	
	public abstract void redact();
	
	
	private static HashMap<String, XNATEntity> entityClasses = new HashMap<String, XNATEntity>();
	
	private static LinkedList<String> preserve = new LinkedList<String>();
	
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
		
		System.out.print("Valid entities: ");
		for(String k : entityClasses.keySet()){
			System.out.print(k+" ");
		}
		System.out.println();
	}
	

	public static String xmlIDFieldName(String type){
		if(entityClasses.containsKey(type))
			return entityClasses.get(type).xmlIDField;
		
		return null;
	}
	
	public static XNATEntity getEntity(String type, String id){
		if(entityClasses.containsKey(type))
			return entityClasses.get(type).create(id);
		
		return null;
	}
	
	public static void batchCreate(XNATEntity parent, String type){
		if(entityClasses.containsKey(type)){
			
			System.out.println("Getting listing for " + type);
			
			XNATResultSet listing = new XNATResultSet();
			listing.type = type;
			
			XNATRestAPI.instance().retrieveResourceListing(parent, listing);
			
			System.out.println("Rows: " + listing.getRows().size());
			
			for(Row r : listing.getRows()){
				String id = r.getValue(xmlIDFieldName(type));
				
				XNATEntity child = getEntity(type, id);
				
				child.xml_listing = r;
				
				parent.addChild(child);
			}
		}else{
			System.err.println("No such resource type:" + type);
		}
	}
	
	public static void addPreservedFields(LinkedList<String> preserved){
		for(String s : preserved){
			preserve.add(s);
		}
	}
	
	
	public static void addPreservedFields(String ... preserved){
		for(String s : preserved){
			preserve.add(s);
		}
	}
	
	public static LinkedList<String> preservedFields(){
		return preserve;
	}
	
	private static void downloadAll(XNATEntity xext, HashSet<String> exclude_types) throws IOException, TransformerException, SAXException, ConnectException{
		
		for(String entityType : entityClasses.keySet()){
			if(entityClasses.get(entityType).getParentType().equals(xext.getEntityType()) && !exclude_types.contains(entityType)){
				XNATEntity.batchCreate(xext, entityType);
			}
		}
		
		for(XNATEntity child : xext.children.values()){
			child.download();
			downloadAll(child);
		}
	}
	public static void downloadAll(XNATEntity xext, String ... exclude) throws IOException, TransformerException, SAXException, ConnectException{
		HashSet<String> exclude_types = new HashSet<String>();
		
		for(String s : exclude){
			exclude_types.add(s);
		}
		
		downloadAll(xext, exclude_types);
	}
		
	public void printResourceTree(){
		System.out.println(this.getPath());
		
		for(XNATEntity child : this.children.values()){
			child.printResourceTree();
		}
	}
}
