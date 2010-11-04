package org.ibcb.xnat.redaction;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.dcm4che2.*;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.io.DicomInputStream;
import org.ibcb.xnat.redaction.interfaces.RedactionPipelineService;
import org.ibcb.xnat.redaction.synchronization.JobQueue;

public class DICOMExtractor extends RedactionPipelineService{
	
	static DICOMExtractor singleton = null;
	
	public static DICOMExtractor instance(){
		if(singleton==null) singleton=new DICOMExtractor();
		return singleton;
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
	
	public HashMap<String, String> extractNameValuePairs(String DICOMfile){
		HashMap<String, String> dicomPairs = new HashMap<String,String>();
		
		DicomObject dcmObj;
		DicomInputStream din = null;
		
		try{
			din = new DicomInputStream(new File(DICOMfile));
			dcmObj = din.readDicomObject();
			Iterator<DicomElement> it = dcmObj.fileMetaInfoIterator();
			while(it.hasNext()){
				DicomElement e = it.next();
				System.out.println(e.toString());
			}
			System.out.println();
			it = dcmObj.iterator();
			while(it.hasNext()){
				DicomElement e = it.next();
				System.out.print(e.toString());
				System.out.println(" Items: " + e.countItems());
				if(e.countItems() > 0){
					
				}
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		finally{
			try{
				din.close();
			}catch(IOException ignore){
				
			}
		}
		
		return dicomPairs;
	}
	
	public static void main(String args[]){
		DICOMExtractor de = DICOMExtractor.instance();
		
		HashMap<String,String> hs = de.extractNameValuePairs("/opt/xnat_deidentification/xnat_redaction/data/BRAINIX/IRM/T2W-FE-EPI - 501/IM-0001-0001.dcm");
		
		for(String k : hs.keySet()){
			System.out.println(k+": "+hs.get(k));
		}
	}
}
