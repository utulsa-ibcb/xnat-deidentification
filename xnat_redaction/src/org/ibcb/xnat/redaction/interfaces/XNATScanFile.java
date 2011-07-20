package org.ibcb.xnat.redaction.interfaces;

import java.io.File;
import java.io.IOException;
import java.rmi.ConnectException;
import java.util.HashMap;
import java.util.LinkedList;

import javax.xml.transform.TransformerException;

import org.dcm4che2.data.DicomObject;
import org.ibcb.xnat.redaction.DICOMExtractor;
import org.ibcb.xnat.redaction.config.Configuration;
import org.ibcb.xnat.redaction.exceptions.PipelineServiceException;
import org.ibcb.xnat.redaction.interfaces.files.DICOMFileHandler;
import org.ibcb.xnat.redaction.synchronization.Globals;
import org.xml.sax.SAXException;

public class XNATScanFile extends XNATEntity{
	String label;
	String URI;
	
	String format;
	String content;
	String type;
	
	String localFile;
	
	XNATFileHandler handler;
	
	public XNATScanFile(){
		this.entity_type = "files";
		this.parent_type = "scans";
		
		this.xmlIDField = "Name";
	}
	
	public XNATEntity create(String id){
		XNATScanFile exp = new XNATScanFile();
		exp.id = id;
		
		return exp;
	}
	
	public String getPath(){
		return parent.getPath() + "/files/"+ this.id; 
	}
	
	public String getDestinationPath(){
		return parent.getDestinationPath() + "/files/"+ this.id; 
	}
	
	boolean downloaded;
	public boolean isDownloaded(){
		return downloaded;
	}
	public void download()  throws IOException, SAXException, ConnectException, TransformerException {
		if(fileHandlers.containsKey(xml_listing.getValue("collection"))){
			localFile = Configuration.instance().getProperty("temp_dicom_storage")+parent.getPath()+"/"+this.id;
			
			Globals.createDirectory(Configuration.instance().getProperty("temp_dicom_storage")+parent.getPath());
			
			XNATRestAPI.instance().downloadREST(XNATRestAPI.instance().url+xml_listing.getValue("URI"), localFile);
			
			handler = fileHandlers.get(xml_listing.getValue("collection")).create(localFile, this);
		}
		downloaded=true;
	}
	
	public HashMap<String, String> getRedactedData(){
		if(handler!=null)
			return handler.getRedactedData();
		
		return null;
	}
	
	public void redact() {
		if(handler!=null)
			handler.redact();
	}
	public void upload()  throws IOException, SAXException, ConnectException, TransformerException {
		if(handler!=null){
			XNATRestAPI.instance().postFile(XNATRestAPI.instance().getURL()+this.getDestinationPath(), handler.getRedactedFileLocation());
		}
	}
	
	static HashMap<String, XNATFileHandler> fileHandlers = new HashMap<String, XNATFileHandler>();
	
	static {
		XNATFileHandler xf = new DICOMFileHandler();
		fileHandlers.put(xf.collection(),xf);
	}
}
