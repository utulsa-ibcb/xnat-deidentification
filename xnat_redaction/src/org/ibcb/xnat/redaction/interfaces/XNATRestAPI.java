package org.ibcb.xnat.redaction.interfaces;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import org.ibcb.xnat.redaction.XNATExtractor;
import org.ibcb.xnat.redaction.config.Configuration;
import org.ibcb.xnat.redaction.config.RedactionRuleset;
import org.ibcb.xnat.redaction.exceptions.PipelineServiceException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

import sun.misc.BASE64Encoder;


public class XNATRestAPI {
	// REST QUERY:
	// GET/PUT/POST/DELETE page HTTP/1.1
	// Host: server
	// Authorization: Basic <base 64 encoding of "user:pass"
	// can specify items in xml or url-variables or POST vars
	
	// /REST/projects/PROJECT_ID/files
	static XNATRestAPI instance = null;
	
	boolean enable_auth=true;
	
	String url="http://central.xnat.org";
	String user="mkmatlock";
	String pass="Z38l!v35";
	
	public static XNATRestAPI instance(){
		if(instance==null)instance=new XNATRestAPI();
		return instance;
	}
	
	public XNATRestAPI(){
		
		
	}
	
	public void printInputStream(InputStream stream) throws IOException{
		int read = 0;
		byte[] bytes = new byte[50];
		read=stream.read(bytes);
		while(read>0){
			System.out.print(new String(bytes,0,read));
			
			read=stream.read(bytes);
		}
	}
	
	public DOMParser queryREST(String query) throws IOException, SAXException{
		System.out.println("Querying: " + query);
		HttpURLConnection con = (HttpURLConnection) new URL(query).openConnection();
		con.setRequestMethod("GET");
		
		BASE64Encoder enc = new BASE64Encoder();
		String userpass = user+":"+pass;
		String encoded = enc.encode(userpass.getBytes());
		con.addRequestProperty("Authorization", "Basic "+encoded);
		
		InputStream stuff = con.getInputStream();
		
		DOMParser parse = new DOMParser();
		parse.parse(new InputSource(stuff));
		return parse;
	}
	
	public void retrieveFilePaths(XNATSubject subject){
		
	}
	
	public XNATSubject retrieveSubject(XNATProject project, String subject_id){
		try{
			String query = url+"/REST/projects/"+project.id+"/subjects/"+subject_id+"?format=xml";
			DOMParser parse = queryREST(query);
			
			XNATSubject subject = new XNATSubject();
			
			subject.xml=parse;
			subject.id=subject_id;
			
			project.subjects.put(subject_id, subject);
			
			return subject;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public void retrieveExperimentIds(XNATProject project){
		try{
			String query = url+"/REST/projects/"+project.id+"/experiments?format=xml";
			DOMParser parse = queryREST(query);
			
//			printInputStream(stuff);
			
			XNATResultSet rs = XNATResultSet.parseXML(parse);
			
//			System.out.println(rs.toString());
			
			for(XNATResultSet.Row r : rs.getRows()){
				project.experiment_ids.add(r.getValue("ID"));
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void retreiveExperiment(XNATProject project, XNATSubject subject, String experiment_id){
		try{
			String query = url+"/REST/projects/"+project.id+"/experiments/"+experiment_id+"?format=xml";
			DOMParser parse = queryREST(query);
			
			XNATExperiment exp = new XNATExperiment();
			
			exp.xml=parse;
			exp.id = experiment_id;
			exp.subject_id = subject.id;
			
			project.experiments.put(experiment_id, exp);
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void retreiveExperiment(XNATProject project, String experiment_id){
		try{
			String query = url+"/REST/projects/"+project.id+"/experiments/"+experiment_id+"?format=xml";
			DOMParser parse = queryREST(query);
			
			XNATExperiment exp = new XNATExperiment();
			
			exp.xml=parse;
			exp.id = experiment_id;
			
			project.experiments.put(experiment_id, exp);
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void retrieveExperimentIds(XNATSubject subject, XNATProject project){
		try{
			String query = url+"/REST/projects/"+project.id+"/subjects/"+subject.id+"/experiments?format=xml";
			DOMParser parse = queryREST(query);
			
//			printInputStream(stuff);
			
			XNATResultSet rs = XNATResultSet.parseXML(parse);
			
//			System.out.println(rs.toString());
			
			for(XNATResultSet.Row r : rs.getRows()){
				subject.experiment_ids.add(r.getValue("ID"));
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void retrieveSubjectIds(XNATProject project){
		try{
			String query = url+"/REST/projects/"+project.id+"/subjects?format=xml";
			
			DOMParser parse = queryREST(query);
			
//			printInputStream(stuff);
			
			XNATResultSet rs = XNATResultSet.parseXML(parse);
			
//			System.out.println(rs.toString());
			
			for(XNATResultSet.Row r : rs.getRows()){
				project.subject_ids.add(r.getValue("ID"));
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public String downloadDICOMFiles(XNATProject project, XNATSubject subject, XNATExperiment experiment, XNATScan scan){
		// execute wget as external process
		String query = url+"/REST/projects/"+project.id+"/subjects/"+subject.id+"/experiments/"+experiment.id+"/scans/"+scan.id+"/resources/DICOM/files?format=zip";
		String tmp_folder = Configuration.instance().getProperty("tmp_directory")+"projects/"+project.id+"/subjects/"+subject.id+"/experiments/"+experiment.id+"/scans/"+scan.id;
		String tmp_location = tmp_folder+"/files.zip";
		
		String []mkdir_com = new String[]{"mkdir","-p",tmp_location.substring(0, tmp_location.lastIndexOf("/"))};
		String []wget_com = new String[]{"wget","--http-user="+user,"--http-password="+pass,"--auth-no-challenge", query, "-O", tmp_location};
		String []unzip_com = new String[]{"unzip", tmp_location};
		
		try{
			Process mkdir_proc = Runtime.getRuntime().exec(mkdir_com);
			mkdir_proc.waitFor();
			System.out.println("Mkdir Operation Succeeded");
		}catch(Exception io){
			io.printStackTrace();
			return null;
		}
		try{		
			Process wget_proc = Runtime.getRuntime().exec(wget_com);
			wget_proc.waitFor();
			System.out.println("Wget Operation Succeeded");
		}catch(Exception io){
			io.printStackTrace();
			return null;
		}
		
		try{
			Process unzip_proc = Runtime.getRuntime().exec(unzip_com);
			unzip_proc.waitFor();
			System.out.println("Unzip Operation Succeeded");
		}catch(Exception io){
			io.printStackTrace();
			return null;
		}
		
		return tmp_folder;
	}
	
	public void getScanXML(XNATProject project, XNATSubject subject, XNATExperiment experiment, XNATScan scan){
		try{
			String query = url+"/REST/projects/"+project.id+"/subjects/"+subject.id+"/experiments/"+experiment.id+"/scans/"+scan.id+"?format=xml";
			
			DOMParser parse = queryREST(query);
			scan.xml = parse;
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void getScanIds(XNATProject project, XNATSubject subject, XNATExperiment experiment){
		try{
			String query = url+"/REST/projects/"+project.id+"/subjects/"+subject.id+"/experiments/"+experiment.id+"/scans?format=xml";
			
			DOMParser parse = queryREST(query);
			XNATResultSet rs = XNATResultSet.parseXML(parse);
			
			for(XNATResultSet.Row r : rs.getRows()){
				XNATScan s = new XNATScan();
				
				s.id = r.getValue("ID");
				
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void downloadDICOMFiles(XNATSubject subject, XNATExperiment experiment, XNATScan scan){
		
	}
	
	// /REST/projects/PROJECT_ID
	public XNATProject getProjectXML(String project_id){
		try{
			String query = url+"/REST/projects/"+project_id+"?format=xml";
			
			DOMParser parse = queryREST(query);
			
			Node proj_node = parse.getDocument().getElementsByTagName("xnat:Project").item(0);
			
			XNATProject project = new XNATProject();
			project.id = project_id;
			
//			System.out.println("Nodes: " + proj_node.getChildNodes().getLength());
			for(int s = 0; s < proj_node.getChildNodes().getLength(); s++){
				Node n = proj_node.getChildNodes().item(s);
				
//				System.out.println("Node: " + n.getPrefix()+":"+n.getLocalName());
				
				if(n.getPrefix() != null && n.getPrefix().equalsIgnoreCase("xnat")){
					if(n.getLocalName().equalsIgnoreCase("name")){
						project.name = n.getTextContent();
					}
					else if(n.getLocalName().equalsIgnoreCase("description")){
						project.description = n.getTextContent();
					}
				}
			}
			
			System.out.println(project.toString());
			
			return project;
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return null;
	}
	
	// /REST/projects/PROJECT_ID/subject/SUBJECT_ID/files	
	
	public static void main(String args[])throws PipelineServiceException{
		XNATRestAPI api = XNATRestAPI.instance();

		XNATProject project = api.getProjectXML("NCIGT_PROSTATE");
		
		api.retrieveSubjectIds(project);
		api.retrieveExperimentIds(project);
		
		api.retrieveSubject(project, project.subject_ids.get(0));
		
		XNATSubject sub = project.subjects.get(project.subject_ids.get(0));
		
		RedactionRuleset rules = new RedactionRuleset();
		try{
			rules.parseRuleset(Configuration.instance().getProperty("redaction_rules"));
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		
		XNATExtractor redact = XNATExtractor.instance();
		redact.initialize();
		sub.demographics = redact.extractNameValuePairs(sub.xml, true, rules);
		
		System.out.println("Subject demographics: ");
		
		for(String key : sub.demographics.keySet()){
			System.out.println(key + ": " + sub.demographics.get(key));
		}
		
		HashMap<String,String> n_demo = redact.extractNameValuePairs(sub.xml, false, null);
		System.out.println("Redacted subject demographics: ");
		for(String key : n_demo.keySet()){
			System.out.println(key + ": " + n_demo.get(key));
		}
		
		
		api.retreiveExperiment(project, project.experiment_ids.get(0));
		
		project.experiments.get(project.experiment_ids.get(0)).extractFiles();
	}
}
