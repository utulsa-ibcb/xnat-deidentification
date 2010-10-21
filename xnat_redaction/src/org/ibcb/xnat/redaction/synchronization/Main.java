package org.ibcb.xnat.redaction.synchronization;

import org.ibcb.xnat.redaction.DICOMExtractor;

public class Main implements Runnable{
	
	
	public void run(){
		// stop taking new jobs
		
		JobQueue.instance().shutdown();
		DICOMExtractor.instance().shutdown();
		
		boolean shutdown_complete =  DICOMExtractor.instance().isActive(); // || other_service.isActive()
		
		while(!shutdown_complete){
			try {
				if(DICOMExtractor.instance().isActive()) DICOMExtractor.instance().wait();
				// etc for other services
			} catch(InterruptedException ie){
				
			}
			shutdown_complete = DICOMExtractor.instance().isActive(); // || other_service.isActive()
		}
	}
	
	public static void main(String args[]){
		JobQueue.instance();
		DICOMExtractor.instance().start();
		
		
		Runtime.getRuntime().addShutdownHook(new Thread(new Main()));
	}
}
