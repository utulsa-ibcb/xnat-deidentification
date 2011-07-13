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
			
			// get target project resource tree
			
			
			XNATEntity tproject = XNATEntity.getEntity("projects", dest_project_id);			
			XNATEntity.downloadAll(tproject, "files");
			
			
			// get source project resource path
			
			String [] path = resource_path.split("/");
			
			XNATEntity [] parents = new XNATEntity[path.length/2];
			
			parents[0] = XNATEntity.getEntity("projects", path[1]);
			
			int mode = 0;
			int i = 1;
			XNATEntity project = null;
			
			for(int j = 2; j < path.length; j++){
				if(mode == 0){
					XNATEntity.batchCreate(parents[i-1], path[j]);
					
					mode=1;
				}else{
					
					for(XNATEntity child : parents[i-1].getChildren()){
						if(child.getID().equals(path[j])){
							child.download();
							break;
						}
					}
					
					i++;
					mode=0;
				}
			}
			
			XNATEntity root = parents[parents.length-1];
			
			XNATEntity.downloadAll(root);
			
			root.printResourceTree();
			
			// create any necessary resources
			// if the resource in question already exists, delete the target resource
			
			// download source project resource path and children
			
			
			// redact
			
			
			// upload
			
			
			
			
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
