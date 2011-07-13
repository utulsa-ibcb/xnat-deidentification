package org.ibcb.xnat.redaction;

import java.util.HashMap;

import org.ibcb.xnat.redaction.config.Configuration;
import org.ibcb.xnat.redaction.config.DICOMSchema;
import org.ibcb.xnat.redaction.exceptions.PipelineServiceException;
import org.ibcb.xnat.redaction.interfaces.RedactionPipelineService;
import org.ibcb.xnat.redaction.interfaces.XNATExperiment;
import org.ibcb.xnat.redaction.interfaces.XNATProject;
import org.ibcb.xnat.redaction.interfaces.XNATRestAPI;
import org.ibcb.xnat.redaction.interfaces.XNATScan;
import org.ibcb.xnat.redaction.interfaces.XNATSubject;

public class Checkout extends RedactionPipelineService{
	
	static Checkout singleton = null;

	public Checkout(){
		
	}
	
	public static Checkout instance(){
		if(singleton==null) singleton=new Checkout();
		return singleton;
	}
	
	public void initialize() throws PipelineServiceException {
		
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
	
	public HashMap<String, String> getRequestingUserData(String user_id, String subject_id){
		HashMap<String, String> map = new HashMap<String, String>();
	
		
		return map;
	}
	
	public void downloadProjectXML(XNATProject project){
		XNATRestAPI.instance().retreiveProject(project);
		
		XNATRestAPI.instance().retrieveSubjectIds(project);
		XNATRestAPI.instance().retrieveExperimentIds(project);
		
		for(String eid : project.experiment_ids){
			XNATRestAPI.instance().retreiveExperiment(project, eid);
		}
	}
	
	public void downloadSubjectXML(XNATProject project, String sid){
		XNATRestAPI.instance().retrieveSubject(project, sid);
		
		XNATSubject subject = project.subjects.get(sid);
		XNATRestAPI.instance().retrieveExperimentIds(project, subject);
		
		for(String eid: subject.experiment_ids){
			XNATExperiment experiment = project.experiments.get(eid);
			XNATRestAPI.instance().retrieveScans(project, subject, experiment);
		}
		
		for(String s : subject.scans.keySet()){
			subject.scans.get(s).extractFiles();
		}
	}
	
	public void downloadSubjectFiles(XNATProject project, String sid){
			XNATSubject subject = project.subjects.get(sid);
			
			for(String s : subject.scans.keySet()){
				XNATScan scan = subject.scans.get(s);
				XNATExperiment experiment = scan.experiment;
				
				XNATRestAPI.instance().downloadDICOMFiles(project, subject, experiment, scan);	
			}
	}
}