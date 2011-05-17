package org.ibcb.xnat.redaction;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TimeZone;

import org.dcm4che2.data.DicomObject;
import org.ibcb.xnat.redaction.config.CheckoutRuleset;
import org.ibcb.xnat.redaction.config.Configuration;
import org.ibcb.xnat.redaction.config.RedactionRuleset;
import org.ibcb.xnat.redaction.config.XNATSchema;
import org.ibcb.xnat.redaction.database.*;
import org.ibcb.xnat.redaction.exceptions.CompileException;
import org.ibcb.xnat.redaction.exceptions.PipelineServiceException;
import org.ibcb.xnat.redaction.interfaces.XNATExperiment;
import org.ibcb.xnat.redaction.interfaces.XNATProject;
import org.ibcb.xnat.redaction.interfaces.XNATRestAPI;
import org.ibcb.xnat.redaction.interfaces.XNATScan;
import org.ibcb.xnat.redaction.interfaces.XNATSubject;
import org.ibcb.xnat.redaction.synchronization.Globals;
public class Loader {
	static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	// log_flags:  errors = errors to log file or email or both
	//             redaction = redaction information to log, email or both
	//             redaction_warnings = redaction warnings to log, email or both
	//			   
	
	public static void main(String args[]){
		String project_id = args[0];
		String dest_project_id = args[1];
		
		String co_user_id = args[2];
		String co_admin_id = args[3];
		
		String request_fields = args[4];
		
		LinkedList<String> req_field_names = new LinkedList<String>();
		
		// initialize logging and email handling
		
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
		
		
		try{

			// load schemas and extractors
			
			DICOMExtractor dext = DICOMExtractor.instance();
			dext.initialize();
			
			XNATExtractor xext = XNATExtractor.instance();
			xext.initialize();
			
			for(String item : request_fields.split(";")){
				if(!item.trim().equals(""))
					req_field_names.add(xext.getSchema().getXnatFieldName("xnat:"+item.trim()));
			}
			
			// load redaction rules
			
			RedactionRuleset ruleset = new RedactionRuleset();
			ruleset.parseRuleset(Configuration.instance().getProperty("redaction_rules"));
			
			// load checkout ruleset and checkout system
			CheckoutRuleset cr = new CheckoutRuleset();
			cr.setFields(Configuration.instance().getProperty("filter_fields").split(","));
			cr.loadRuleSet(Configuration.instance().getProperty("checkout_rules"));
			
			Checkout.instance().initialize();
			
			XNATRestAPI api = XNATRestAPI.instance();
			
			// download project and subject ids
			XNATProject project = new XNATProject();
			project.id = project_id;

			Checkout.instance().downloadProjectXML(project);
			
			// ***Possibly Canceled Feature*** upload new project -Matt
			
			// Get target project
			
			XNATProject target = new XNATProject();
			target.id = dest_project_id;
			api.retreiveProject(target);
			
			//init DB manager
//			DBManager db=new DBManager();
			//init a new request
			Date dt=new Date();
			//leave affected subjectids blank for now
//			RequestInfo r_info=new RequestInfo(co_user_id,dt.toString(),co_admin_id,"",request_fields);
//			BigDecimal requestId=db.insertRequestInfo(r_info);
//			HashMap<String,HashMap<String,String>> overallCheckoutInfo=db.getUserCheckOutInfo(co_user_id);
			
			
			// for each user in the project
			for(String subject_id : project.subject_ids){
				
				// download subject information and redact
				Checkout.instance().downloadSubjectXML(project, subject_id);
				Checkout.instance().downloadSubjectFiles(project, subject_id);
				// redact XNATSubject demographics
				
				XNATSubject subject = project.subjects.get(subject_id);
				HashMap<String, String> xnat_demographics = XNATExtractor.instance().extractNameValuePairs(subject.xml, true, ruleset);
				
				HashMap<String, LinkedList<String>> dicom_demographics = new HashMap<String, LinkedList<String>>();
				
				for(String experiment_id : subject.experiment_ids){
					for(String scan_id : subject.scan_ids.get(experiment_id)){
					// redact DICOMFiles
						XNATScan scan = subject.scans.get(scan_id);
						for(String file : scan.localFiles){
							String input = scan.tmp_folder+"/"+file;
							
							System.out.println("Processing: " + input);
							
							DicomObject obj = dext.loadDicom(input);
							
							HashMap<String,String> hs = dext.extractNameValuePairs(obj, ruleset, req_field_names);
							
							// Store hs data in dicom map
							
							for(String key : hs.keySet()){
								String val = hs.get(key);
								
								if(!dicom_demographics.containsKey(key))
									dicom_demographics.put(key, new LinkedList<String>());
								if(!dicom_demographics.get(key).contains(val))
									dicom_demographics.get(key).add(val);
							}
							
							File dir = new File(scan.tmp_folder+"/redacted");
							if(!dir.exists()){
								dir.mkdirs();
							}
							String nfilename = scan.tmp_folder+"/redacted/"+file;
							dext.writeDicom(nfilename, obj);
							
//							DicomObject obj2_test = dext.loadDicom(nfilename);							
//							hs = dext.extractNameValuePairs(obj2_test, ruleset);
						}
					}
				}
				
				// download checkout user information from our database -Liang
/*				HashMap<String,String> subjectCheckoutInfo=overallCheckoutInfo.get(subject_id);
				
				// populate map of checkout fields -Liang
				HashMap<String, String> requesting_user_data = Checkout.instance().getRequestingUserData(co_user_id, subject_id);
				HashMap<String, String> filter_data = new HashMap<String,String>();
				int checkoutCount=0;
				for (String key:subjectCheckoutInfo.keySet())
				{
					if (subjectCheckoutInfo.get(key).equals(new String("1")))
					{
						checkoutCount++;
						String requestName="request_"+key;
						filter_data.put(requestName, "1");
					}
					
				}
				String phi_checked="phi_checked_out";
				filter_data.put(phi_checked, Integer.toString(checkoutCount));
				for (String key:requesting_user_data.keySet())
				{
					if (requesting_user_data.get(key).equals(new String("1")))
					{
						String requestName="request_"+key;
						filter_data.put(requestName, "1");
					}
					
				}
				*/
				// Using the above data, along with req_field_names and insert resulting data into the filter_data hashmap
				// example:
				// user has already checked out Age previously, and is requesting to check out Race now
				// filter_data looks like this:
				// phi_checked_out		2
				// request_PatientAge	1
				// request_PatientRace	1
				// request_...			0
				// ...					0
				// 
				// 
				
				
				// run permissions checks against checkout ruleset information
				
				subject.passed = true;//cr.filter(filter_data);
				// upload redacted information to database -Liang			
				// PatientAge = [31, 32]
				// subject_id, field, values
				
				// update information about checked out PHI to database -Liang
				
				// don't forget to store the destination ids for tracking our redacted data: subject.destination_id
				// xnat_demographics and dicom_demographics applies to the current subject object
				
				
				// upload subject information -Matt
				if(subject.passed){
					//Create a subject info for passed subject
//					SubjectInfo s_info=new SubjectInfo(null,subject.demographics.toString(),project_id,requestId.toPlainString());
//					db.insertSubjectInfo(s_info);
					
					
					// reinsert requested, authorized information into XNAT and DICOM -Matt			
					for(String field : req_field_names){
						if(xnat_demographics.containsKey(field)){
							xext.insertData(subject.xml, "xnat:"+field, xnat_demographics.get(field));
						}
					}
					
					String response = api.postSubject(target);
					subject.destination_id = response.substring(response.lastIndexOf('/')+1);
					
					if(!api.putSubject(target, subject)){
						throw new PipelineServiceException("Unable to upload subject: " + subject.id);
					}

					// upload experiment information -Matt
					for(String eid : subject.experiment_ids){
						XNATExperiment experiment = project.experiments.get(eid);
						
						response = api.postExperiment(target, subject, experiment);
						experiment.destination_id = response.substring(response.lastIndexOf('/')+1);

						
						// upload scans -Matt
						for(String scan_id : subject.scan_ids.get(eid)){
							XNATScan scan = subject.scans.get(scan_id);
							
							response = api.postScan(target, subject, experiment, scan);
							scan.destination_id = response.substring(response.lastIndexOf('/')+1);
						

							// upload DICOM files -Matt
							api.uploadDICOMFiles(target, subject, experiment, scan);
						}
					}
				}
			}
		}catch(PipelineServiceException pse){
			pse.printStackTrace();
			
			if(email_errors){
				String error_text = "["+human_date+"]: Pipeline Service Exception encountered in XNATRedaction engine, contact your system administrator";
			
				// upload error message to database
			}
			
			Globals.application_log.write("e", "Pipeline Service Exception Encountered: " + pse.getMessage());
			Globals.application_log.write("e", Globals.stackTraceConvert(pse));
		}catch(CompileException ce){
			ce.printStackTrace();
			
			if(email_errors){
				String error_text = "["+human_date+"]: Compiler Exception encountered in XNATRedaction engine, contact systema administrator";

				// upload error message to database
			}
			
			Globals.application_log.write("e", "Compiler Exception Encountered: " + ce.getMessage());
			Globals.application_log.write("e", Globals.stackTraceConvert(ce));
		}
	}

}
