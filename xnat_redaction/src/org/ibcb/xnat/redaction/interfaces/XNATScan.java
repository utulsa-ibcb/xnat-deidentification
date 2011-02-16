package org.ibcb.xnat.redaction.interfaces;

import java.util.LinkedList;

import org.ibcb.xnat.redaction.synchronization.Globals;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

public class XNATScan {
	public String id;
	public String destination_id;
	
	public DOMParser xml;
	
	public XNATExperiment experiment;
	
	public LinkedList<XNATFile> files = new LinkedList<XNATFile>();
	public LinkedList<String> localFiles = new LinkedList<String>();
	
	public String tmp_folder;
	
	public String extractXML(String dest_project_id, String dest_subject_id, String dest_experiment_id){
		LinkedList<String> ignore = new LinkedList<String>();
		ignore.add("ID");
		
		Node scan_node = xml.getDocument().getElementsByTagName("xnat:MRScan").item(0);
		
		String scan_xml = "<xnat:MRScan" + Globals.extractAttributes(scan_node, ignore)+">\n";
		
		for(int index = 0; index < scan_node.getChildNodes().getLength(); index++){
			Node child = scan_node.getChildNodes().item(index);
			
			if(child.getNodeName()!=null){
				if(child.getNodeName().equalsIgnoreCase("xnat:image_session_ID")){
					scan_xml+="<xnat:image_session_ID>"+dest_experiment_id+"</xnat:image_session_ID>\n";
				}
				else if(child.getNodeName().equalsIgnoreCase("xnat:file")){
					
				}
				else{
					if(child.getNodeType() == child.ELEMENT_NODE)
						scan_xml+=Globals.nodeToString(child)+"\n";
				}
			}
		}
		
		scan_xml+="</xnat:MRScan>\n";
		return scan_xml;
	}
	
	public void extractFiles(){
		NodeList file_fields = xml.getDocument().getElementsByTagName("xnat:file");
		
		for(int i = 0; i < file_fields.getLength(); i++){
			Node n = file_fields.item(i);
			
			XNATFile file = new XNATFile();
			
			file.label = n.getAttributes().getNamedItem("label").getNodeValue();
			file.URI = n.getAttributes().getNamedItem("URI").getNodeValue();
			file.format = n.getAttributes().getNamedItem("format").getNodeValue();
			file.type = n.getAttributes().getNamedItem("xsi:type").getNodeValue();
			file.content = n.getAttributes().getNamedItem("content").getNodeValue();
			
			System.out.println("Extracted: " + file.label + " at: " + file.URI + " of type: " + file.type);
			
			files.add(file);
		}
	}
	
	public void print(){
		System.out.println("            Scan: " + id);
		System.out.println("                Files:");
		for(XNATFile f : files){
			System.out.println("                "+f.format+": " + f.URI);
		}
		System.out.println("                Downloaded Files:");
		for(String f : localFiles){
			System.out.println("                "+f);
		}
	}
}
