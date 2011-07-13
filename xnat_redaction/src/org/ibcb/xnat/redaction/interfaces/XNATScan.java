package org.ibcb.xnat.redaction.interfaces;

import java.io.IOException;
import java.rmi.ConnectException;
import java.util.HashMap;
import java.util.LinkedList;

import javax.xml.transform.TransformerException;

import org.ibcb.xnat.redaction.synchronization.Globals;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

public class XNATScan extends XNATEntity{
	
	public XNATExperiment experiment;
	
	public LinkedList<XNATScanFile> files = new LinkedList<XNATScanFile>();
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
			
			XNATScanFile file = new XNATScanFile();
			
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
		for(XNATScanFile f : files){
			System.out.println("                "+f.format+": " + f.URI);
		}
		System.out.println("                Downloaded Files:");
		for(String f : localFiles){
			System.out.println("                "+f);
		}
	}
	
	public XNATScan(){
		this.parent_type = "experiments";
		this.entity_type = "scans";
		
		this.xmlIDField = "ID";
	}
	
	public XNATEntity create(String id){
		XNATScan exp = new XNATScan();
		exp.id = id;
		
		return exp;
	}
	
	public String getPath(){
		return parent.getPath() + "/scans/"+ this.id; 
	}
	
	public String getDestinationPath(){
		return parent.getDestinationPath() + "/scans/"+ this.destination_id; 
	}
	
	boolean downloaded;
	public boolean isDownloaded(){
		return downloaded;
	}
	public void download() throws IOException, SAXException, ConnectException, TransformerException {
		XNATRestAPI.instance().retrieveResource(this);
		downloaded=true;
	}
	
	public HashMap<String, String> getRedactedData(){
		return null;
	}
		
	public void redact() {
		
		
	}
	public void upload() throws IOException, SAXException, ConnectException, TransformerException {
		XNATRestAPI.instance().postREST(XNATRestAPI.instance().getURL()+this.getDestinationPath(), this.extractXML(this.getParent().getParent().getParent().getDestinationID(),this.getParent().getParent().getDestinationID(), this.getParent().getDestinationID()));
	}
}
