package org.ibcb.xnat.redaction;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedList;

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
	}
}
