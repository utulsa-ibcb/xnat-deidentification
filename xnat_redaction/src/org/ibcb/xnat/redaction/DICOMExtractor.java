package org.ibcb.xnat.redaction;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.dcm4che2.*;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.SpecificCharacterSet;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.ibcb.xnat.redaction.config.Configuration;
import org.ibcb.xnat.redaction.config.DICOMSchema;
import org.ibcb.xnat.redaction.config.RedactionRuleset;
import org.ibcb.xnat.redaction.exceptions.PipelineServiceException;
import org.ibcb.xnat.redaction.helpers.Pair;
import org.ibcb.xnat.redaction.interfaces.RedactionPipelineService;
import org.ibcb.xnat.redaction.synchronization.JobQueue;

public class DICOMExtractor extends RedactionPipelineService{
	
	static DICOMExtractor singleton = null;
	DICOMSchema schema;
	
	public DICOMSchema getSchema(){
		return schema;
	}
	
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
	
	
	public void writeDicom(String destination, DicomObject file){
		File f = new File(destination);
		FileOutputStream fos=null;
		try{
			fos = new FileOutputStream(f);
		}catch(FileNotFoundException fnfe){
			try{
			f.createNewFile();
			fos = new FileOutputStream(f);
			}catch(Exception e){
				e.printStackTrace();
				return;
			}
		}
		
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		DicomOutputStream dos = new DicomOutputStream(bos);
		
		try{
			dos.writeDicomFile(file);
		}catch(IOException e){
			e.printStackTrace();
			return;
		}finally{
			try{
				dos.close();
			}catch(IOException ignore){
				
			}
		}
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
	
	public HashMap<String, String> extractNameValuePairs(DicomObject dcmObj, RedactionRuleset rules, List<String> request_fields){

		SpecificCharacterSet scs = new SpecificCharacterSet("latin1"); 
		
		HashMap<String, String> dicomPairs = new HashMap<String,String>();
		List<Integer> redacted = new LinkedList<Integer>();
		Iterator<DicomElement> it = dcmObj.iterator();
		while(it.hasNext()){
			DicomElement e = it.next();
			try{
				int tag = e.tag();
				
				int high = (tag & 0xFFFF0000) >> 16;
				int low = tag & 0x0000FFFF;
				
				Pair<Integer,Integer> id = new Pair<Integer,Integer>(high,low);
				String field = schema.getDICOMFieldName(id);
				
				boolean redact = rules.redact(RedactionRuleset.PROTO_DICOM, field); 
				if(redact || rules.translate(RedactionRuleset.PROTO_DICOM, field)){
					String value = e.getValueAsString(scs, 100);			
					dicomPairs.put(field, value);
					
					if(redact && !request_fields.contains(field)){
						redacted.add(tag);
					}
					
//					System.out.println("("+Integer.toHexString(high)+","+Integer.toHexString(low)+") ["+value+"]");
//					System.out.println(e.toString());
				}
			}catch(UnsupportedOperationException uoe){
				System.out.println("Couldn't convert: " + e.toString());
			}
		}
		
		for(int id : redacted){
			DicomElement e = dcmObj.get(id);
			
			dcmObj.remove(id);
			dcmObj.putString(id, dcmObj.vrOf(id), "");
		}
		
		return dicomPairs;
	}
	
	public static void main(String args[]) throws PipelineServiceException{
		DICOMExtractor de = DICOMExtractor.instance();
		de.initialize();
	
		RedactionRuleset rules = new RedactionRuleset();
		try{
			rules.parseRuleset(Configuration.instance().getProperty("redaction_rules"));
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		
		LinkedList<String> req_fields = new LinkedList<String>();
		req_fields.add("PatientName");
		
		String input = "./data/BRAINIX/IRM/T2W-FE-EPI - 501/IM-0001-0001.dcm";
		DicomObject obj = de.loadDicom(input);
		
		HashMap<String,String> hs = de.extractNameValuePairs(obj, rules, req_fields);
		System.out.println("+-------+");
		System.out.println("| Pre:  |");
		System.out.println("+-------+");
		for(String k : hs.keySet()){
			System.out.printf("%-20.20s: %s\n", k, hs.get(k));
		}
		
		File f = new File(input);
		String nfilename = Configuration.instance().getProperty("temp_dicom_storage")+f.getName()+".redacted";
		de.writeDicom(nfilename, obj);
		
		DicomObject obj2_test = de.loadDicom(nfilename);
		
		hs = de.extractNameValuePairs(obj2_test, rules, req_fields);
		System.out.println("+-------+");
		System.out.println("| Post: |");
		System.out.println("+-------+");
		for(String k : hs.keySet()){
			System.out.printf("%-20.20s: %s\n", k, hs.get(k));
		}
	}
}
