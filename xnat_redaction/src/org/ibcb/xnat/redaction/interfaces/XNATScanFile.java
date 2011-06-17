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
import org.ibcb.xnat.redaction.synchronization.Globals;
import org.xml.sax.SAXException;

public class XNATScanFile extends XNATEntity{
	String label;
	String URI;
	
	String format;
	String content;
	String type;
	
	String localFile;
	String redactedLocalFile;
	
	HashMap<String, String> dicom_fields;
	
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
	
	public void download()  throws IOException, SAXException, ConnectException, TransformerException {
		
		if(xml_listing.getValue("collection").equals("DICOM")){
			localFile = Configuration.instance().getProperty("temp_dicom_storage")+parent.getPath()+"/"+this.id;
			
			Globals.createDirectory(Configuration.instance().getProperty("temp_dicom_storage")+parent.getPath());
			
			XNATRestAPI.instance().downloadREST(XNATRestAPI.instance().getURL()+xml_listing.getValue("URI"), localFile);
		}
	}
	
	public HashMap<String, String> getRedactedData(){
		return dicom_fields;
	}
	
	public void redact() {
		
		if(xml_listing.getValue("collection").equals("DICOM")){
			
			redactedLocalFile = Configuration.instance().getProperty("temp_dicom_storage")+parent.getPath()+"/redacted/"+this.id;
			
			File dir = new File(Configuration.instance().getProperty("temp_dicom_storage")+parent.getPath()+"/redacted/");
			if(!dir.exists()){
				dir.mkdir();
			}
			
			try{
				DicomObject dcmObj = DICOMExtractor.instance().loadDicom(localFile);
				
				System.out.println("Redacting to: " + redactedLocalFile);
				
				dicom_fields = DICOMExtractor.instance().extractNameValuePairs(dcmObj, XNATEntity.preservedFields());
				DICOMExtractor.instance().writeDicom(redactedLocalFile, dcmObj);
			}catch(PipelineServiceException pse){
				pse.printStackTrace();
			}
		}
		
	}
	public void upload()  throws IOException, SAXException, ConnectException, TransformerException {
		if(xml_listing.getValue("collection").equals("DICOM")){
			XNATRestAPI.instance().postFile(XNATRestAPI.instance().getURL()+this.getDestinationPath(), redactedLocalFile);
		}
	}
}
