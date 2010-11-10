package org.ibcb.xnat.redaction;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.dcm4che2.*;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.io.DicomInputStream;
import org.ibcb.xnat.redaction.config.Configuration;
import org.ibcb.xnat.redaction.config.DICOMSchema;
import org.ibcb.xnat.redaction.exceptions.PipelineServiceException;
import org.ibcb.xnat.redaction.interfaces.RedactionPipelineService;
import org.ibcb.xnat.redaction.synchronization.JobQueue;

public class DICOMExtractor extends RedactionPipelineService{
	
	static DICOMExtractor singleton = null;
	DICOMSchema schema;
	
	public DICOMExtractor(){
		schema = null;
	}
	
	public static DICOMExtractor instance(){
		if(singleton==null) singleton=new DICOMExtractor();
		return singleton;
	}
	
	public void initialize() throws PipelineServiceException {
		String file=null;
		try{
			schema = new DICOMSchema();
			file=Configuration.instance().getProperty("dicom_schema");
			schema.loadDicomSchema(file);
		}catch(Exception e){
			PipelineServiceException pe = new PipelineServiceException("Error loading shema file from `"+file+"`: " + e.getMessage());
			pe.setStackTrace(e.getStackTrace());
			throw pe;
		}
	}
	
	public void run(){
		
		boolean running=true;
		
		while(running){
			if(shuttingDown()){
				// make sure all jobs are done
				// and all child threads have finished running
				// then set
				running=false;
			}
			// check for jobs
			// start processing jobs
		}
		// notify threads that we are exiting. The Main class will wait for this notification, if the system is shutting down.
		
		this.notifyAll();
	}
	
	
	public DicomObject loadDicom(String DICOMfile) throws PipelineServiceException{
		DicomObject dcmObj = null;
		DicomInputStream din = null;
		PipelineServiceException pe=null;
		try{
			din = new DicomInputStream(new File(DICOMfile));
			dcmObj = din.readDicomObject();
		}catch(IOException e){
			e.printStackTrace();
			dcmObj=null;
			pe = new PipelineServiceException("Error loading DICOMFile: " + DICOMfile + " error: " + e.getMessage());
			pe.setStackTrace(e.getStackTrace());

		}
		finally{
			try{
				din.close();
			}catch(IOException ignore){
				
			}
			if(pe != null)
				throw pe;
		}
		
		return dcmObj;
	}
	
	public HashMap<String, String> extractNameValuePairs(DicomObject dcmObj){
		HashMap<String, String> dicomPairs = new HashMap<String,String>();
		
//		Iterator<DicomElement> it = dcmObj.fileMetaInfoIterator();
//		while(it.hasNext()){
//			DicomElement e = it.next();
//		}
		Iterator<DicomElement> it = dcmObj.iterator();
		while(it.hasNext()){
			DicomElement e = it.next();
		}
		return dicomPairs;
	}
	
	public static void main(String args[]) throws PipelineServiceException{
		System.out.println(Configuration.instance().getProperty("redaction_rules"));
		DICOMExtractor de = DICOMExtractor.instance();
		de.initialize();
		
		DicomObject obj = de.loadDicom("/opt/xnat_deidentification/xnat_redaction/data/BRAINIX/IRM/T2W-FE-EPI - 501/IM-0001-0001.dcm");
		HashMap<String,String> hs = de.extractNameValuePairs(obj);
		
		for(String k : hs.keySet()){
			System.out.println(k+": "+hs.get(k));
		}
	}
}
