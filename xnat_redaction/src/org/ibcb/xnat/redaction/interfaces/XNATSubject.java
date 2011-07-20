package org.ibcb.xnat.redaction.interfaces;

import java.io.IOException;
import java.rmi.ConnectException;
import java.util.HashMap;
import java.util.LinkedList;

import javax.xml.transform.TransformerException;

import org.dcm4che2.data.SpecificCharacterSet;
import org.ibcb.xnat.redaction.config.XNATSchema;
import org.ibcb.xnat.redaction.synchronization.Globals;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

public class XNATSubject extends XNATEntity{
	private static int destIdCounter = 0;
	
	public boolean passed=false;
	
	public String newLabel;
	String group;
	
	String subject_xml;
	
	boolean generated = false;
	
	public boolean redacted=false;
	
	public LinkedList<String> experiment_ids=new LinkedList<String>();
	
	public HashMap<String, LinkedList<String>> scan_ids = new HashMap<String, LinkedList<String>>();
	public HashMap<String, XNATScan> scans = new HashMap<String, XNATScan>();
	
	public void setNewLabel(String nLabel){
		newLabel=nLabel;
	}
	
	public String extractXML(String project_id){
		if(generated) return subject_xml;
		Node subject_node = xml.getDocument().getElementsByTagName("xnat:Subject").item(0);
		
		 subject_xml = 
			 "<xnat:Subject";
		 	
		 	for(int index = 0; index < subject_node.getAttributes().getLength(); index++){
		 		Node n = subject_node.getAttributes().item(index);
		 		
		 		String name = n.getNodeName();
		 		String value = n.getNodeValue();
		 		
		 		if(name.equalsIgnoreCase("id"))
		 			subject_xml += " " + name + "=\"\"";
		 		else if(name.equalsIgnoreCase("project"))
		 			subject_xml += " " + name + "=\""+project_id+"\"";
		 		else if(name.equalsIgnoreCase("label"))
		 			subject_xml += " " + name + "=\""+newLabel+"\"";
		 		else
		 			subject_xml += " " + name + "=\""+value+"\"";
		 	}
		 subject_xml += ">\n";
		 subject_xml +=
			 "   <xnat:demographics xsi:type=\"xnat:demographicData\">\n";
			 
			 if(demographics==null) this.getDemographics();
		 
			 for(String key : demographics.keySet()){
				 subject_xml += "      <"+key+">"+demographics.get(key)+"</"+key+">\n";
			 }
			 
			 subject_xml +=
			 "    </xnat:demographics>\n"+
			 "</xnat:Subject>";
		 
		 boolean generated = true;
			 
		 return subject_xml;
	}
	
	public void getDemographics(){
		demographics = new HashMap<String,String>();
		
		Node s_node = xml.getDocument().getElementsByTagName("xnat:Subject").item(0);
		
		for(int i = 0; i < s_node.getChildNodes().getLength(); i++){
			Node x_node = s_node.getChildNodes().item(i);
			
			if(x_node.getLocalName() != null && x_node.getPrefix().equalsIgnoreCase("xnat") && x_node.getLocalName().equalsIgnoreCase("demographics")){
				
				for(int j = 0; j  < x_node.getChildNodes().getLength(); j++){
					Node y_node = x_node.getChildNodes().item(j);
					
					if(y_node.getLocalName()!=null){
						String tag_name = (y_node.getPrefix()!=null ? y_node.getPrefix()+":" : "") + y_node.getLocalName();
						
						demographics.put(tag_name, y_node.getTextContent());
					}
				}
			}
		}
	}
	
	public String generateDestinationId(){
		return ""+Globals.idnf.format(destIdCounter);
	}
	
	public void print(){
		System.out.println("    XNATSubject: " + id);
		
		System.out.println("    Experiments:");
		for(String eid : experiment_ids){
			System.out.println(eid);
			
			System.out.println("        Scans:");
			
			for(String scan_id : scan_ids.get(eid)){
				scans.get(scan_id).print();
			}
		}
	}
	
	public XNATSubject(){
		this.entity_type = "subjects";
		this.parent_type = "projects";
		
		this.xmlIDField = "ID";
	}
	
	public XNATEntity create(String id){
		XNATSubject exp = new XNATSubject();
		exp.id = id;
		
		return exp;
	}
	
	public String getPath(){
		return parent.getPath() + "/subjects/"+ this.id; 
	}
	
	public String getDestinationPath(){
		return parent.getDestinationPath() + "/subjects/"+ this.destination_id; 
	}
	
	boolean downloaded;
	public boolean isDownloaded(){
		return downloaded;
	}
	
	public void download()  throws IOException, SAXException, ConnectException, TransformerException {
		XNATRestAPI.instance().retrieveResource(this);
		downloaded=true;
	}
	
	public String entityType() {
		
		
		return "subjects";
	}
	
	HashMap<String,String> demographics;

	
	public HashMap<String,String> getRedactedData(){
		return demographics;
	}
	
	public void redact() {
		
		DOMParser xnat_subject = this.getXML();
		
		SpecificCharacterSet scs = new SpecificCharacterSet("latin1"); 
		
		demographics = new HashMap<String,String>();
		
		Node s_node = xnat_subject.getDocument().getElementsByTagName("xnat:Subject").item(0);
	
		for(int i = 0; i < s_node.getChildNodes().getLength(); i++){
			Node x_node = s_node.getChildNodes().item(i);
			
			if(x_node.getLocalName() != null && x_node.getPrefix().equalsIgnoreCase("xnat") && x_node.getLocalName().equalsIgnoreCase("demographics")){
				
				for(int j = 0; j  < x_node.getChildNodes().getLength(); j++){
					Node y_node = x_node.getChildNodes().item(j);
					
					if(y_node.getLocalName()!=null){
						String tag_name = (y_node.getPrefix()!=null ? y_node.getPrefix()+":" : "") + y_node.getLocalName();
						
						String fieldname = XNATSchema.instance().getXnatFieldName(tag_name);
						
						System.out.println("Tag: " + tag_name + " mapped to: " + fieldname);
						
						if(fieldname != null){
							
							demographics.put(fieldname, y_node.getTextContent());
							y_node.setTextContent("");
							System.out.println("Redacted: " + fieldname);
							
						}
					}
				}
			}
		}		
	}
	
	private void searchAggregateData(HashMap<String, String> aggregate_data, XNATEntity entity){
		HashMap<String, String> hs = entity.getRedactedData();
		
		if(hs != null)
		{		
			for(String k : hs.keySet()){
				if(!aggregate_data.containsKey(k)){
					aggregate_data.put(k, hs.get(k));
				}
			}
		}
			
		
		for(XNATEntity e : entity.children.values()){
			searchAggregateData(aggregate_data, e);
		}
	}
	
	public void upload()  throws IOException, SAXException, ConnectException, TransformerException {
		String target_xml = "<xnat:Subject ID=\"\" project=\""+parent.getDestinationID()+"\" group = \""+group+"\" label=\""+newLabel+"\" xmlns:xnat=\"http://nrg.wustl.edu/xnat\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n";
		target_xml+="\t<xnat:demographics xsi:type=\"xnat:demographicData\">\n";
		
		// insert demographic data
		
		HashMap<String, String> aggregate_data = new HashMap<String, String>();
		
		searchAggregateData(aggregate_data, this);
		
		for(String keep : XNATEntity.preservedFields()){
			if(aggregate_data.containsKey(keep)){
				String tag = XNATSchema.instance().getXnatFieldLocation(keep);
				target_xml += "\t\t<xnat:"+tag+">"+aggregate_data.get(keep)+"</xnat:"+tag+">\n";
			}
		}
		
		target_xml += "\t</xnat:demographics>\n";
		target_xml += "</xnat:Subject>\n";
		
		String response = XNATRestAPI.instance().postREST(XNATRestAPI.instance().getURL()+parent.getPath()+"/subjects", "");

		this.destination_id = response.substring(response.lastIndexOf('/')+1);
		
		XNATRestAPI.instance().putREST(XNATRestAPI.instance().getURL()+this.getDestinationPath(),target_xml);
	}
}