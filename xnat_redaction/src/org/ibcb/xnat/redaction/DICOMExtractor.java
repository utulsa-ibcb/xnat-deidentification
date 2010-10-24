package org.ibcb.xnat.redaction;

import org.dcm4che2.*;
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
	
	public static void main(){
		
	}
}
