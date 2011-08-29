package org.ibcb.xnat.redaction.interfaces;

import java.io.IOException;
import java.rmi.ConnectException;
import java.util.HashMap;
import java.util.LinkedList;

import javax.xml.transform.TransformerException;

import org.dcm4che2.data.SpecificCharacterSet;
import org.ibcb.xnat.redaction.config.XNATSchema;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

public class XNATExperiment extends XNATEntity{
	
	public String subject_id;
	
	public LinkedList<XNATScanFile> files = new LinkedList<XNATScanFile>();
	public LinkedList<XNATScan> scans = new LinkedList<XNATScan>();
	
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
	
	String experiment_xml;
	boolean generated=false;
	
	public String extractXML(String dest_project_id, String dest_subject_id){
		if(generated) return experiment_xml;
		experiment_xml = "<xnat:MRSession ID=\"\" project=\""+dest_project_id+"\" ";
		
		Node exp_node = xml.getDocument().getElementsByTagName("xnat:MRSession").item(0);
		
	 	for(int index = 0; index < exp_node.getAttributes().getLength(); index++){
	 		Node n = exp_node.getAttributes().item(index);
	 		
	 		String name = n.getNodeName();
	 		String value = n.getNodeValue();
	 		
	 		if(name.equalsIgnoreCase("id") || name.equalsIgnoreCase("project"))
	 			experiment_xml += "";
	 		else if(name.equalsIgnoreCase("label")){
	 			experiment_xml += " " + name + "=\"" + ((XNATSubject)this.getParent()).newLabel + "\"";
	 		}
	 		else
	 			experiment_xml += " " + name + "=\""+value+"\"";
	 	}
		
		experiment_xml += ">\n";
		
		Node date = xml.getDocument().getElementsByTagName("xnat:date").item(0);
		Node time = xml.getDocument().getElementsByTagName("xnat:time").item(0);
		Node fields = xml.getDocument().getElementsByTagName("xnat:fields").item(0);
		
		experiment_xml+= "<xnat:date>"+date.getTextContent()+"</xnat:date>\n";
		experiment_xml+= "<xnat:time>"+time.getTextContent()+"</xnat:time>\n";
		experiment_xml+= "<xnat:fields>\n";
		
		for(int index = 0; index < fields.getChildNodes().getLength(); index++){
			Node field = fields.getChildNodes().item(index);
			
			if(field.getNodeName()!= null && field.getNodeName().equals("xnat:field")){
				String name = field.getAttributes().getNamedItem("name").getNodeValue();
				String text = field.getTextContent();
				
				if(text==null)
					experiment_xml+="   <xnat:field name=\""+name+"\"/>\n";
				else
					experiment_xml+="   <xnat:field name=\""+name+"\">"+text+"</xnat:field>\n";
			}
		}
//		<xnat:field name="studyComments">
//		</xnat:field>
		experiment_xml+= "</xnat:fields>\n";
		experiment_xml += "<xnat:subject_ID>"+dest_subject_id+"</xnat:subject_ID>\n";
		
		experiment_xml += "</xnat:MRSession>\n";
		
		return experiment_xml;
	}
	
	public XNATExperiment(){
		this.parent_type = "subjects";
		this.entity_type = "experiments";
		
		this.xmlIDField = "ID";
	}
	
	public XNATEntity create(String id){
		XNATExperiment exp = new XNATExperiment();
		exp.id = id;
		
		this.xmlIDField = "ID";
		
		return exp;
	}
	
	public String getPath(){
		return parent.getPath() + "/experiments/"+ this.id; 
	}
	
	public String getDestinationPath(){
		return parent.getDestinationPath() + "/experiments/"+ this.destination_id; 
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
		String response = XNATRestAPI.instance().postREST(XNATRestAPI.instance().getURL()+this.getParent().getDestinationPath()+"/experiments", this.extractXML(this.getParent().getParent().getDestinationID(), this.getParent().getDestinationID()));
		
		this.destination_id = response.substring(response.lastIndexOf('/')+1);
	}
}
