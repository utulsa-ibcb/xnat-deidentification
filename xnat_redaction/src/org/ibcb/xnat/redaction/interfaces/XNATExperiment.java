package org.ibcb.xnat.redaction.interfaces;

import java.util.LinkedList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

public class XNATExperiment {
	DOMParser xml;
	String id;
	String subject_id;
	
	LinkedList<XNATFile> files = new LinkedList<XNATFile>();
	LinkedList<XNATScan> scans = new LinkedList<XNATScan>();
	
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
}
