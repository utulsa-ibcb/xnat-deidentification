package org.ibcb.xnat.redaction;

import java.io.IOException;
import java.rmi.ConnectException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedList;

import javax.xml.transform.TransformerException;

import org.ibcb.xnat.redaction.interfaces.XNATEntity;
import org.xml.sax.SAXException;

public class PipelineEntrypoint {
	static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static void main(String args[]){
		String project_id = args[0];
		String dest_project_id = args[1];
		
		String co_user_id = args[2];
		String co_admin_id = args[3];
		
		String request_fields="";
		
		String resource_path = "projects/"+project_id;
		
		if(args.length>4)
			request_fields = args[4];
		
		if(args.length>5)
			resource_path += args[5];
		
		
		LinkedList<String> req_field_names = new LinkedList<String>();
		
		String []fields = request_fields.split(",");
		
		for(String f : fields){
			req_field_names.add(f);
		}
		
		try {
			XNATEntity proj = XNATEntity.getEntity("projects", project_id);
			
			System.out.println("Downloading project info: " + proj.getID());
			
			proj.download();
				
			XNATEntity.batchCreate(proj, "subjects");
			
			for(XNATEntity subject : proj.getChildren()){
				System.out.println("Subject: " + subject.getID());
			}
			
			XNATEntity subject = proj.getChildren().iterator().next();
			
			System.out.println("Downloading subject: " + subject.getID());
			
			subject.download();
			
			XNATEntity.batchCreate(subject, "experiments");
			
			for(XNATEntity experiment : subject.getChildren()){
				System.out.println("Experiment: " + experiment.getID());
			}
			
			XNATEntity experiment = subject.getChildren().iterator().next();
			
			System.out.println("Downloading experiment: " + experiment.getID());
			
			experiment.download();
			
			XNATEntity.batchCreate(experiment, "scans");
			
			for(XNATEntity scan : experiment.getChildren()){
				System.out.println("Scan: " + scan.getID());
			}
			
			
			XNATEntity scan = experiment.getChildren().iterator().next();
			
			System.out.println("Downloading scan: " + scan.getID());
			
			scan.download();
			
			XNATEntity.batchCreate(scan, "files");
			
			for(XNATEntity file : scan.getChildren()){
				System.out.println("File: " + file.getID());
				
				file.download();
				
				file.redact();
			}
			
			
			
			
		} catch (ConnectException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}
}
