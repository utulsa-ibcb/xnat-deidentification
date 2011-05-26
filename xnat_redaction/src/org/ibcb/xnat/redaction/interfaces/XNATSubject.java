package org.ibcb.xnat.redaction.interfaces;

import java.util.HashMap;
import java.util.LinkedList;

import org.ibcb.xnat.redaction.config.RedactionRuleset;
import org.ibcb.xnat.redaction.synchronization.Globals;
import org.w3c.dom.Node;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

public class XNATSubject {
	private static int destIdCounter = 0;
	
	public DOMParser xml;
	
	public boolean passed=false;
	
	public String id;
	
	public String newLabel;
	
	String subject_xml;
	boolean generated = false;	
	public String destination_id;
	
	public boolean redacted=false;
	
	public HashMap<String,String> demographics=null;
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
}
