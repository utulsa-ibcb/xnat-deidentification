package org.ibcb.xnat.redaction;

import java.io.IOException;
import java.math.BigDecimal;
import java.rmi.ConnectException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TimeZone;

import javax.xml.transform.TransformerException;

import org.ibcb.xnat.redaction.config.CheckoutRuleset;
import org.ibcb.xnat.redaction.config.Configuration;
import org.ibcb.xnat.redaction.config.DICOMSchema;
import org.ibcb.xnat.redaction.config.XNATSchema;
import org.ibcb.xnat.redaction.database.DBManager;
import org.ibcb.xnat.redaction.database.RequestInfo;
import org.ibcb.xnat.redaction.exceptions.CompileException;
import org.ibcb.xnat.redaction.helpers.Message;
import org.ibcb.xnat.redaction.interfaces.XNATEntity;
import org.ibcb.xnat.redaction.interfaces.XNATRestAPI;
import org.ibcb.xnat.redaction.synchronization.Globals;
import org.xml.sax.SAXException;

public class PipelineEntrypoint {
	static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static class FailedRootException extends Exception{
		public FailedRootException(String msg){
			super(msg);
		}
	}
	
	public static void main(String args[]){
		String project_id = args[0];
		String dest_project_id = args[1];
		
		String co_user_id = args[2];
		String co_admin_id = args[3];
		LinkedList<String> req_field_names = new LinkedList<String>();
		String request_fields="";
		
		String resource_path = "projects/"+project_id+"/";
		
		if(args.length>4)
			request_fields = args[4];
		
		if(args.length>5)
			resource_path += args[5];
		
		
		String [] fields = request_fields.split(",");
		XNATEntity.addPreservedFields(fields);
		
		long current_time_stamp = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
		String human_date = df.format(Calendar.getInstance().getTime());
		
		boolean email_errors=Configuration.instance().getProperty("errors").contains("email");
		boolean log_errors=Configuration.instance().getProperty("errors").contains("log");
		
		boolean email_warnings=Configuration.instance().getProperty("redaction_warnings").contains("email");
		boolean log_warnings=Configuration.instance().getProperty("redaction_warnings").contains("log");
		
		boolean email_redaction=Configuration.instance().getProperty("redaction").contains("email");
		boolean log_redaction=Configuration.instance().getProperty("redaction").contains("log");
		
		if(log_errors){
			Globals.application_log.enableFlags("e");
		}else
			Globals.application_log.disableFlags("e");
		
		if(log_redaction){
			Globals.application_log.enableFlags("r");
		}else
			Globals.application_log.disableFlags("r");
		
		if(log_warnings){
			Globals.application_log.enableFlags("w");
		}else
			Globals.application_log.disableFlags("w");
		
		LinkedList<Message> messages = new LinkedList<Message>();
		
		
		try {
			// Load checkout rules
			
			LinkedList<String> complete_field_names = new LinkedList<String>();
			
			for(String f : DICOMSchema.instance().getMappedFields()){
				if(!complete_field_names.contains(f))
					complete_field_names.add(f);
			}
			
			for(String f : XNATSchema.instance().getMappedFields()){
				if(!complete_field_names.contains(f))
					complete_field_names.add(f);
			}
			
			for(String item : request_fields.split(",")){
				if(!item.trim().equals(""))
					req_field_names.add(XNATSchema.instance().getXnatFieldName("xnat:"+item.trim()));
			}
			// load redaction rules

			LinkedList<String> filter_fields = new LinkedList<String>();
			
			for(String field : complete_field_names){
				if(!filter_fields.contains("request_"+field)){
					filter_fields.add("request_"+field);
				}
			}
			
			for(String field : Configuration.instance().getProperty("filter_fields").split(",")){
				if(!filter_fields.contains(field)){
					filter_fields.add(field);
				}
			}
			// load checkout ruleset and checkout system
			CheckoutRuleset cr = new CheckoutRuleset();
			cr.setFields(filter_fields.toArray(new String[]{}));
			cr.loadRuleSet(Configuration.instance().getProperty("checkout_rules"));
			
			
			// initialize DB Manager
			DBManager db=new DBManager(Configuration.instance().getProperty("database_hostname"),Configuration.instance().getProperty("database_name"),Configuration.instance().getProperty("database_user"),Configuration.instance().getProperty("database_pass"));
			Date dt=new Date();
			System.out.println("check out field "+request_fields);
			RequestInfo r_info=new RequestInfo(co_user_id,dt.toString(),co_admin_id,"",req_field_names);
			BigDecimal requestId=db.getNextRequestID();
			r_info.setRequestid(requestId);
			HashMap<String,HashMap<String,String>> overallCheckoutInfo=db.getUserCheckOutInfo(co_user_id);	
			// get target project resource tree
			
			
			XNATEntity tproject = XNATEntity.getEntity("projects", dest_project_id);			
			XNATEntity.downloadAll(tproject, "files");
			
			
			// get source project resource path
			
			String [] path = resource_path.split("/");
			
			System.out.println("Path:"+resource_path);
			
			XNATEntity [] parents = new XNATEntity[path.length/2];
			
			parents[0] = XNATEntity.getEntity("projects", path[1]);
			
			int mode = 0;
			int i = 1;
			XNATEntity project = null;
			
			for(int j = 2; j < path.length; j++){
				System.out.print(path[j]+"/");
				
				if(mode == 0){
					XNATEntity.batchCreate(parents[i-1], path[j]);
					
					mode=1;
				}else{
					
					for(XNATEntity child : parents[i-1].getChildren()){
						if(child.getID().equals(path[j])){
							parents[i] = child;
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
			
			System.out.println("Resource Tree: ");
			root.printResourceTree();
			
			
			// redact
			
			for(i = 0; i < parents.length-1; i++){
				parents[i].redact();
			}
			
			XNATEntity.redactAll(root);
			
			
			// collect a list of subject objects in this redaction
			
			LinkedList<XNATEntity> failedSubjects = new LinkedList<XNATEntity>();
			
			for(XNATEntity child : parents[0].getChildren()){
				if(child.getEntityType().equals("subjects") && child.isDownloaded()){
					
					// get aggregate redacted data
					HashMap<String,String> data = XNATEntity.aggregateRedactedData(child);
					
					// Recover subject identity
					String uniSubjectid=null;
					if (db.lookupSubjectid(child.getID())!=null)
					uniSubjectid=db.lookupSubjectid(child.getID()).toString();
					HashMap<String,String> subjectCheckoutInfo=null;
					if (uniSubjectid!=null)
					subjectCheckoutInfo=overallCheckoutInfo.get(uniSubjectid);
					
					// populate map of checkout fields -Liang
					HashMap<String, String> requesting_user_data = subjectCheckoutInfo;
					HashMap<String, String> filter_data = new HashMap<String,String>();
					int checkoutCount=0;
					for (String field : complete_field_names)
					{
							String requestName="request_"+field;
							filter_data.put(requestName, "0");						
					}
					
					if (subjectCheckoutInfo!=null){
						for (String key:subjectCheckoutInfo.keySet())
						{
							if (subjectCheckoutInfo.get(key).equals(new String("1")))
							{
								checkoutCount++;
								String requestName="request_"+key;
								filter_data.put(requestName, "1");
							}					
						}
						for (String key:requesting_user_data.keySet())
						{
							if (requesting_user_data.get(key).equals(new String("1")))
							{
								String requestName="request_"+key;
								filter_data.put(requestName, "1");
							}
							
						}
					}
					for (String fieldName : req_field_names)
					{
						String key="request_"+fieldName;
						if (!filter_data.get(key).equals(new String("1")))
								{
									filter_data.remove(key);
									filter_data.put(key, "1");
									checkoutCount++;
								}					
					}
					String phi_checked="phi_checked_out";
					filter_data.put(phi_checked, Integer.toString(checkoutCount));
					System.out.println("Check out map for subject "+child.getID());
					for(String key : filter_data.keySet())
					{
						System.out.println(key+"  |  "+filter_data.get(key));					
					}
					
					boolean pass;
					// FILTER! and add to failedSubjects
					pass = cr.filter(filter_data);
					//pass=true;
					if(!pass){
						failedSubjects.add(child);
					}
				}
			}
			
			// upload resources not children of failed subjects
			// create any necessary resources
			
			// upload parents of the root
			for(i = 1; i < parents.length-2; i++){
				
				if(parents[i].getEntityType().equals("subjects") && failedSubjects.contains(parents[i])){
					throw new FailedRootException("Subject "+parents[i].getID()+" failed checkout conditions, resource cannot be added...");
				}
				
				String resource_id = parents[i].getID();
				String resource_type = parents[i].getEntityType();
				
				String resource = db.getResourceDestinationID(resource_type, project_id, resource_id, dest_project_id);
				
				if(resource == null){
					parents[i].upload();
					db.setResourceDestinationID(resource_type, project_id, resource_id, dest_project_id, parents[i].getDestinationID());
					Message m = new Message();
					m.message = "Uploaded new resource '"+resource_type+"' from '"+project_id+"/"+resource_id+"' to '"+dest_project_id+"/"+parents[i].getDestinationID()+"'";
					m.type=Message.TYPE_INFO;
					messages.add(m);
				}
			}
			
			// upload the root and all children
			// if the resource in question already exists, delete the target resource
			
			
			LinkedList<XNATEntity> toProcess = new LinkedList<XNATEntity>();
			toProcess.add(root);
			
			while(toProcess.size() > 0){
				
				
				XNATEntity e = toProcess.pop();
				
				if(e.getEntityType().equals("subjects") && failedSubjects.contains(e)){
								
					Message m = new Message();
					m.message = "Subject "+parents[i].getID()+" failed checkout conditions, resource cannot be added...";
					m.type = Message.TYPE_WARNING;
					messages.add(m);
					continue;
				}
				
				String resource_id = e.getID();
				String resource_type = e.getEntityType();
				
				String resource = db.getResourceDestinationID(resource_type, project_id, resource_id, dest_project_id);
				
				if(resource != null){
					String query = e.getParent().getDestinationPath() + "/"+ resource_type +"/" + resource;
					XNATRestAPI.instance().deleteREST(query);
					
					Message m = new Message();
					m.message = "Deleted resource '"+dest_project_id+"/"+resource+"'";
					m.type=Message.TYPE_INFO;
					messages.add(m);
				}
				
				e.upload();
				
				Message m = new Message();
				m.message = "Uploaded new resource '"+resource_type+"' from '"+project_id+"/"+resource_id+"' to '"+dest_project_id+"/"+parents[i].getDestinationID()+"'";
				m.type=Message.TYPE_INFO;
				messages.add(m);
				
				db.setResourceDestinationID(resource_type, project_id, resource_id, dest_project_id, e.getDestinationID());
				
				for(XNATEntity child : e.getChildren()){
					toProcess.add(child);
				}
			}
			
		} catch(FailedRootException e){
			
			Message m = new Message();
			m.type = Message.TYPE_INFO;
			m.message = e.getMessage();
			
			messages.add(m);
		} catch(CompileException ce){
			ce.printStackTrace();
			
			Message m = new Message();
			m.e = ce;
			m.type = Message.TYPE_ERROR;
			m.message = ce.getMessage();
			
			messages.add(m);
			
		} catch (ConnectException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		} catch (Exception e){
			Message m = new Message();
			m.e = e;
			m.type = Message.TYPE_ERROR;
			m.message = "Unexpected Exception Type Encountered, Fatal: " + e.getMessage();
			messages.add(m);
		}
		
		
		// add e-mail messaging support
		for(Message m : messages){
			if(m.type == Message.TYPE_ERROR){
				if(log_errors){
					Globals.application_log.write(false, "e", m.logText());
				}
			}
			if(m.type == Message.TYPE_INFO){
				if(log_redaction){
					Globals.application_log.write(false, "r", m.logText());
				}
			}
			if(m.type == Message.TYPE_WARNING){
				if(log_warnings){
					Globals.application_log.write(false, "w", m.logText());
				}
			}
		}
	}
}
