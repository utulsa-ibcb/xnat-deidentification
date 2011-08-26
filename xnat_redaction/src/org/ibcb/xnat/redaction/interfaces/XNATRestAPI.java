package org.ibcb.xnat.redaction.interfaces;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.ConnectException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.dcm4che2.data.DicomObject;
import org.ibcb.xnat.redaction.DICOMExtractor;
import org.ibcb.xnat.redaction.XNATExtractor;
import org.ibcb.xnat.redaction.config.Configuration;
import org.ibcb.xnat.redaction.exceptions.PipelineServiceException;
import org.ibcb.xnat.redaction.helpers.Downloader;
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
	final static int retry_count = 3;
	static int timeout=5000;
	boolean enable_auth=true;
	
	String url;
	String user;
	String pass;
	
	public static XNATRestAPI instance(){
		if(instance==null)instance=new XNATRestAPI();
		return instance;
	}
	
	public XNATRestAPI(){
		url =Configuration.instance().getProperty("xnat_server");
		user=Configuration.instance().getProperty("xnat_user");
		pass=Configuration.instance().getProperty("xnat_pass");
	}
	
	public String getURL(){
		return url+"/data/archive";
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
	

	
	public void downloadREST(String query, String location) throws IOException{
			System.out.println("Downloading: " + query);
			 
			//Use a stand alone downloader 
			BASE64Encoder enc = new BASE64Encoder();
			String userpass = user+":"+pass;
			String encoded = enc.encode(userpass.getBytes());
			//con.addRequestProperty("Authorization", "Basic "+encoded);
			
			File output=new File(location);
			Downloader downloader=new Downloader(new URL(query),output);
			Downloader.Authorization=encoded;
			Thread downloaderThread=new Thread(downloader);
			downloaderThread.start();
			int oldLength=-1;
			int oldpercent=0;
			long start = System.currentTimeMillis();
			while (!downloader.isCompleted()) {
				
				//check every timeout ms to see if the downloader has progress during that.
				
				if ((start<System.currentTimeMillis()-XNATRestAPI.timeout) && downloader.getProgressString()=="Downloading")
				{
					//System.out.println("downloaded: "+downloader.getDownloadedlength());
					int newLength=downloader.getDownloadedlength();
					System.out.println("downloaded: "+(newLength-oldLength)+" during "+XNATRestAPI.timeout+" ms");
					if (newLength-oldLength<1)	
					{
						System.out.println("download time out restart");
						downloaderThread.stop();
						//output.delete();
						output=new File(location);
						downloader=new Downloader(new URL(query),output);
						Downloader.Authorization=encoded;
						downloaderThread=new Thread(downloader);
						downloaderThread.start();
						oldLength=-1;
						start = System.currentTimeMillis();
					}
					oldLength=newLength;
					start=System.currentTimeMillis();
				}
				
			}			 
			return;
	}
	
	public String DOMtoXML(DOMParser xml) throws TransformerException {
        /////////////////
        //Output the XML

        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(xml.getDocument());
        trans.transform(source, result);
        return sw.toString();
	}
	
	public String postSubject(XNATProject project){
		String query = url+"/REST/projects/"+project.id+"/subjects";
		try{
			return postREST(query, "");
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean putSubject(XNATProject project, XNATSubject subject){
		String query = url+"/REST/projects/"+project.id+"/subjects/"+subject.destination_id;
		try{
			String content = subject.extractXML(project.id);
			System.out.println("PUTting: \n" + content);
			putREST(query, content);
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean putUser(XNATProject project, String user_id){
		String query = url+"/REST/projects/"+project.id+"/users/Members/"+user_id;
		try{
			putREST(query, "");
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public String postScan(XNATProject project, XNATSubject subject, XNATExperiment experiment, XNATScan scan){
		String query = url+"/REST/projects/"+project.id+"/subjects/"+subject.destination_id+"/experiments/"+experiment.destination_id+"/scans";
		try{
			String scan_xml = scan.extractXML(project.id, subject.destination_id, experiment.destination_id);
			
//			System.out.println("POSTing: " + scan_xml);
			
			return postREST(query, scan_xml);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public String postExperiment(XNATProject project, XNATSubject subject, XNATExperiment experiment){
		String query = url+"/REST/projects/"+project.id+"/subjects/"+subject.destination_id+"/experiments?activate=true&quarantine=false&triggerPipelines=false";
		try{
			return postREST(query, experiment.extractXML(project.id, subject.destination_id));
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean putExperiment(XNATProject project, XNATSubject subject, XNATExperiment experiment){
		String query = url+"/REST/projects/"+project.id+"/subjects/"+subject.destination_id+"/experiments/"+experiment.destination_id;
		try{
			String content = experiment.extractXML(project.id, subject.destination_id);
			System.out.println("PUTting: \n" + content);
			putREST(query, content);
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public String convertStreamToString(InputStream is)
    throws IOException {
		/*
		 * To convert the InputStream to String we use the
		 * Reader.read(char[] buffer) method. We iterate until the
		 * Reader return -1 which means there's no more data to
		 * read. We use the StringWriter class to produce the string.
		 */
		if (is != null) {
		    Writer writer = new StringWriter();
		
		    char[] buffer = new char[1024];
		    try {
		        Reader reader = new BufferedReader(
		                new InputStreamReader(is, "UTF-8"));
		        int n;
		        while ((n = reader.read(buffer)) != -1) {
		            writer.write(buffer, 0, n);
		        }
		    } finally {
		        is.close();
		    }
		    return writer.toString();
		} else {        
		    return "";
		}
	}

	
	public String postREST(String query, String content) throws IOException, SAXException, ConnectException, TransformerException{
		int tries=0;
		while((tries++)<retry_count){
			try{
				System.out.println("POSTing: " + query);
				HttpURLConnection con = (HttpURLConnection) new URL(query).openConnection();
				con.setRequestMethod("POST");
				con.setDoOutput(true);
				
				
				BASE64Encoder enc = new BASE64Encoder();
				String userpass = user+":"+pass;
				String encoded = enc.encode(userpass.getBytes());
				con.addRequestProperty("Authorization", "Basic "+encoded);
				
				OutputStreamWriter out = new OutputStreamWriter(
					    con.getOutputStream());
					out.write(content);
					out.close();
				
//				System.out.println("POST response: "+con.getResponseCode());
				
//				System.out.println(con.getResponseMessage());
				
				InputStream stuff = con.getInputStream();
				
//				DOMParser parse = new DOMParser();
//				parse.parse(new InputSource(stuff));
				
				return convertStreamToString(stuff);
			}catch(ConnectException ce){
				ce.printStackTrace();
				if(tries==retry_count) throw ce;
			}
		}
		return null;
	}
	
	public void putREST(String query, DOMParser xml) throws IOException, SAXException, ConnectException, TransformerException{
		String xml_string = DOMtoXML(xml);
		
		putREST(query, xml_string);
	}
	
	public void deleteREST(String query) throws IOException{
		int tries=0;
		while((tries++)<retry_count){
			try{
				System.out.println("DELETEting: " + query);
				HttpURLConnection con = (HttpURLConnection) new URL(query).openConnection();
				con.setRequestMethod("DELETE");
				con.setDoOutput(true);
				
				BASE64Encoder enc = new BASE64Encoder();
				String userpass = user+":"+pass;
				String encoded = enc.encode(userpass.getBytes());
				con.addRequestProperty("Authorization", "Basic "+encoded);
				
				
				System.out.println("Delete response: "+con.getResponseCode());
				
				System.out.println(con.getResponseMessage());
					
//				InputStream stuff = con.getInputStream();
				
//				DOMParser parse = new DOMParser();
//				parse.parse(new InputSource(stuff));
				
//				System.out.println(DOMtoXML(parse));
				
				break;
			}catch(ConnectException ce){
				ce.printStackTrace();
				if(tries==retry_count) throw ce;
			}
		}
	}
	
	public void putREST(String query, String content) throws IOException, SAXException, ConnectException, TransformerException{
		int tries=0;
		while((tries++)<retry_count){
			try{
				System.out.println("PUTting: " + query);
				HttpURLConnection con = (HttpURLConnection) new URL(query).openConnection();
				con.setRequestMethod("PUT");
				con.setDoOutput(true);
				
				BASE64Encoder enc = new BASE64Encoder();
				String userpass = user+":"+pass;
				String encoded = enc.encode(userpass.getBytes());
				con.addRequestProperty("Authorization", "Basic "+encoded);
				
				OutputStreamWriter out = new OutputStreamWriter(
					    con.getOutputStream());
					out.write(content);
					out.close();

				System.out.println("Put response: "+con.getResponseCode());
				
				if(con.getResponseCode() != 200){
					System.err.println("Offending Content: ");
					System.err.println(content);
					
					throw new IOException("Malformed content!");
				}
				
				System.out.println(con.getResponseMessage());
					
//				InputStream stuff = con.getInputStream();
				
//				DOMParser parse = new DOMParser();
//				parse.parse(new InputSource(stuff));
				
//				System.out.println(DOMtoXML(parse));
				
				break;
			}catch(ConnectException ce){
				ce.printStackTrace();
				if(tries==retry_count) throw ce;
			}
		}
	}
	
	public DOMParser queryREST(String query) throws IOException, SAXException, ConnectException{
		int tries=0;
		while((tries++)<retry_count){
			try{
				System.out.println("GETting: " + query);
				HttpURLConnection con = (HttpURLConnection) new URL(query).openConnection();
				con.setRequestMethod("GET");
				
				BASE64Encoder enc = new BASE64Encoder();
				String userpass = user+":"+pass;
				String encoded = enc.encode(userpass.getBytes());
				con.addRequestProperty("Authorization", "Basic "+encoded);
				
				InputStream stuff = con.getInputStream();
				
//				byte[] buffer = new byte[50];
//				int read = 0;
//				while((read = stuff.read(buffer)) > 0){
//					for(int i = 0; i < read; i++){
//						System.out.print((char)buffer[i]);
//					}
//				}
				
				DOMParser parse = new DOMParser();
				parse.parse(new InputSource(stuff));
				return parse;
			
			}catch(ConnectException ce){
				ce.printStackTrace();
				if(tries==retry_count) throw ce;
			}
		}
		throw new IOException("Unable to connect to host: " + query);
	}
	
	public boolean retrieveSubject(XNATProject project, String subject_id){
		try{
			String query = url+"/REST/projects/"+project.id+"/subjects/"+subject_id+"?format=xml";
			DOMParser parse = queryREST(query);
			
			XNATSubject subject = new XNATSubject();
			
			subject.xml=parse;
			subject.id=subject_id;
			
			project.subjects.put(subject_id, subject);
			
			return true;
		}catch(Exception e){
			e.printStackTrace();
		}
		return false;
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
	
	public void retrieveExperiment(XNATProject project, XNATSubject subject, String experiment_id){
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
	
	public void retrieveExperimentIds(XNATProject project, XNATSubject subject){
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
	
	   /**
     * The core Business Logic method that extracts a ZIP file maintaining
     * the folder structure
     *       
     * @param zipFileName
     *      The name of the ZIP file to be extracted
     *      
     * @throws IOException
     *      Problems while extacting the ZIP file
     */
    public static void unzip(String zipFileName)  throws IOException
    {
        ZipFile zipFile = null;
        InputStream inputStream = null;
 
        String root_dir = zipFileName.substring(0,zipFileName.lastIndexOf('/')+1);
        
        File inputFile = new File(zipFileName);
        try
        {
             // Wrap the input file with a ZipFile to iterate through
             // its contents
             zipFile = new ZipFile(inputFile);
             Enumeration<? extends ZipEntry> oEnum = zipFile.entries();
             while(oEnum.hasMoreElements())
             {
                 ZipEntry zipEntry = oEnum.nextElement();
               
               
                 
                 if(!zipEntry.isDirectory())
                 {
					String location = root_dir+zipEntry.getName();
					String filename = zipEntry.getName();
					
					if(filename.contains("/"))
						filename = filename.substring(filename.lastIndexOf('/')+1);
					
					System.out.println("Destination: " + root_dir+filename);
					File file = new File(root_dir+filename);
                	 
                     inputStream = zipFile.getInputStream(zipEntry);
                     write(inputStream, file);
                 }
             }
        }
        catch (IOException ioException)
        {
            throw ioException;
        }
        finally
        {
            // Clean up the I/O
            try
            {
                if (zipFile != null)
                {
                    zipFile.close();
                }
                if (inputStream != null)
                {
                    inputStream.close();
                }
            }
            catch(IOException problemsDuringClose)
            {
                System.out.println("Problems during cleaning up the I/O.");
            }
        }
    }
 
    /**
     * Writes to the supplied file with the contents read from the supplied input stream.
     * 
     * @param inputStream
     *      The Source input stream from where the contents will be read to write to the file.
     *      
     * @param fileToWrite
     *      The file to which the contents from the input stream will be written to.
     *      
     * @throws IOException
     *      Any problems while reading from the input stream or writing to the file.
     */
    public static void write(InputStream inputStream, File fileToWrite) throws IOException
    {        
            BufferedInputStream buffInputStream = new BufferedInputStream( inputStream );
            FileOutputStream fos = new FileOutputStream( fileToWrite );
            BufferedOutputStream bos = new BufferedOutputStream( fos );
 
            // write bytes
            int byteData;
            while( ( byteData = buffInputStream.read() ) != -1 )
            {
                 bos.write( (byte) byteData);
            }
 
            // close all the open streams
            bos.close();
            fos.close();
            buffInputStream.close();
    }
    
    public void putFileMultipartForm(String url, String filename) throws MalformedURLException, IOException{
    	String param = "value";
    	String charset = "UTF-8";
    	File binaryFile = new File(filename);
    	String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
    	String CRLF = "\r\n"; // Line separator required by multipart/form-data.

    	URLConnection connection = new URL(url).openConnection();
    	connection.setDoOutput(true);
    	connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
    	PrintWriter writer=null;
    	
    	((HttpURLConnection)connection).setRequestMethod("PUT");
		
		BASE64Encoder enc = new BASE64Encoder();
		String userpass = user+":"+pass;
		String encoded = enc.encode(userpass.getBytes());
		connection.addRequestProperty("Authorization", "Basic "+encoded);
    	
    	try {
    	    OutputStream output = connection.getOutputStream();
    	    writer = new PrintWriter(new OutputStreamWriter(output, charset), true); // true = autoFlush, important!

    	    // Send binary file.
    	    writer.append("--" + boundary).append(CRLF);
    	    writer.append("Content-Disposition: form-data; name=\"binaryFile\"; filename=\"" + binaryFile.getName() + "\"").append(CRLF);
    	    writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(binaryFile.getName())).append(CRLF);
    	    
    	    writer.append("Content-Transfer-Encoding: binary").append(CRLF);
    	    writer.append(CRLF).flush();
    	    InputStream input = null;
    	    try {
    	        input = new FileInputStream(binaryFile);
    	        byte[] buffer = new byte[1024];
    	        for (int length = 0; (length = input.read(buffer)) > 0;) {
    	            output.write(buffer, 0, length);
    	        }
    	        output.flush(); // Important! Output cannot be closed. Close of writer will close output as well.
    	    } catch(Exception e){
    	    	e.printStackTrace();
    	    }finally {
    	        if (input != null) try { input.close(); } catch (IOException logOrIgnore) {logOrIgnore.printStackTrace();}
    	    }
    	    writer.append(CRLF).flush(); // CRLF is important! It indicates end of binary boundary.

    	    // End of multipart/form-data.
    	    writer.append("--" + boundary + "--").append(CRLF);
    	    
    	    
    	    
    	} catch(Exception e){
	    	e.printStackTrace();
	    } finally {
    	    if (writer != null) writer.close();
    	}
	    
	    System.out.println("Server Response: " + ((HttpURLConnection)connection).getResponseCode() + " -- " + ((HttpURLConnection)connection).getResponseMessage());
		
    }
    
    public void putFile(String url, String filename)throws MalformedURLException, IOException{

		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		try {
			System.out.println("Putting file \""+filename+"\" to: " + url);
			
			
			HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
			con.setRequestMethod("PUT");
			con.setDoOutput(true);
			
			BASE64Encoder enc = new BASE64Encoder();
			String userpass = user+":"+pass;
			String encoded = enc.encode(userpass.getBytes());
			con.addRequestProperty("Authorization", "Basic "+encoded);
			
			bos = new BufferedOutputStream(con.getOutputStream());
			bis = new BufferedInputStream(new FileInputStream(filename));

			int i;
			// read byte by byte until end of stream
			while ((i = bis.read()) != -1) {
				bos.write(i);
			}
			
			System.out.println("Server Response: " + con.getResponseCode() + " -- " + con.getResponseMessage());
			
			InputStream stuff = con.getInputStream();
			
//			DOMParser parse = new DOMParser();
//			parse.parse(new InputSource(stuff));
			
			System.out.println(convertStreamToString(stuff));
			
		} catch(Exception e){
			e.printStackTrace();
		}finally {
			if (bis != null)
				try {
					bis.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			if (bos != null)
				try {
					bos.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
		}
    }

    
    public void uploadDICOMFiles(XNATProject project, XNATSubject subject, XNATExperiment experiment, XNATScan scan){
    	
    	for(String localFile : scan.localFiles){
    		
    		String destination = url+"/REST/projects/"+project.id+"/subjects/"+subject.destination_id+"/experiments/"+experiment.destination_id+"/scans/"+scan.destination_id+"/files/"+localFile+"?inbody=true";
    		String filename = scan.tmp_folder + "/redacted/" + localFile;
    		
    		System.out.println("Uploading Scan file: " + filename);
    		try{
    			System.out.println("POSTing: " + localFile + " to: " + destination);
    			putFile(destination, filename);
    			
    		}catch(MalformedURLException me){
    			me.printStackTrace();
    		}catch(IOException ioe){
    			ioe.printStackTrace();
    		}
    	}
    }

	
	public boolean downloadDICOMFiles(XNATProject project, XNATSubject subject, XNATExperiment experiment, XNATScan scan){
		
		String query = url+"/REST/projects/"+project.id+"/subjects/"+subject.id+"/experiments/"+experiment.id+"/scans/"+scan.id+"/resources/DICOM/files?format=zip";
		String tmp_folder = Configuration.instance().getProperty("temp_dicom_storage")+"projects/"+project.id+"/subjects/"+subject.id+"/experiments/"+experiment.id+"/scans/"+scan.id;
		String tmp_location = tmp_folder+"/files.zip";
		
		scan.tmp_folder = tmp_folder;
		File directory = new File(tmp_folder);
		
		directory.mkdirs();
		
		try{
			downloadREST(query, tmp_location);
			System.out.println("ZIP File downloaded to: " + tmp_location);
		}catch(Exception io){
			io.printStackTrace();
			return false;
		}
		
		try{
			XNATRestAPI.unzip(tmp_location);
		}catch(Exception io){
			io.printStackTrace();
			return false;
		}
		
		for(File file : directory.listFiles()){
			if(file.getName().equals("files.zip")){
				file.delete();
			}
			else if(file.isFile()){
				scan.localFiles.add(file.getName());
			}
		}
		
		return true;
	}
	
	private void retrieveScanXML(XNATProject project, XNATSubject subject, XNATExperiment experiment, XNATScan scan){
		try{
			String query = url+"/REST/projects/"+project.id+"/subjects/"+subject.id+"/experiments/"+experiment.id+"/scans/"+scan.id+"?format=xml";
			
			DOMParser parse = queryREST(query);
			scan.xml = parse;
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void retrieveScans(XNATProject project, XNATSubject subject, XNATExperiment experiment){
		try{
			String query = url+"/REST/projects/"+project.id+"/subjects/"+subject.id+"/experiments/"+experiment.id+"/scans?format=xml";
			
			DOMParser parse = queryREST(query);
			XNATResultSet rs = XNATResultSet.parseXML(parse);
			
			for(XNATResultSet.Row r : rs.getRows()){
				XNATScan s = new XNATScan();
				s.id = r.getValue("ID");
				s.experiment=experiment;
				
				retrieveScanXML(project,subject,experiment,s);
				
				if(!subject.scan_ids.containsKey(experiment.id))
					subject.scan_ids.put(experiment.id, new LinkedList<String>());
				subject.scan_ids.get(experiment.id).add(s.id);
				subject.scans.put(s.id, s);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	// /REST/projects/PROJECT_ID
	public boolean retreiveProject(XNATProject project){
		try{
			String query = url+"/REST/projects/"+project.id+"?format=xml";
			
			DOMParser parse = queryREST(query);
			
			Node proj_node = parse.getDocument().getElementsByTagName("xnat:Project").item(0);
			
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
			
			return true;
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return false;
	}
	
	public boolean retrieveResourceListing(XNATEntity resource, XNATResultSet category){
		try{
			String query = url + "/REST" + resource.getPath() +"/"+ category.type + "?format=xml";
			
			System.out.println("Listing: " + query);
			
			DOMParser parse = queryREST(query);
			
			category.parseXMLResult(parse);
			
			return true;
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return false;
	}
	
	public boolean retrieveResource(XNATEntity resource){
		try{
			String query = url + "/REST" + resource.getPath() + "?format=xml";
			
			DOMParser parse = queryREST(query);
			
			resource.setXML(parse);
			
			return true;
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return false;
	}
	

	
	// /REST/projects/PROJECT_ID/subject/SUBJECT_ID/files	
	
	public static void main(String args[])throws PipelineServiceException, TransformerException, IOException{
		
		XNATRestAPI api = XNATRestAPI.instance();

		String filename = "./data/dicom_storage/projects/Redaction_Sour/subjects/CENTRAL_S01668/experiments/CENTRAL_E04834/scans/3/redacted/I.002.dcm";
		String destination = "/projects/Redaction_Test/subjects/CENTRAL_S01757/experiments/CENTRAL_E04960/scans/3/resources/DICOM/files/I.002.dcm";
		api.putFileMultipartForm(api.getURL()+destination, filename);
		filename = "./data/dicom_storage/projects/Redaction_Sour/subjects/CENTRAL_S01668/experiments/CENTRAL_E04834/scans/3/redacted/I.003.dcm";
		destination = "/projects/Redaction_Test/subjects/CENTRAL_S01757/experiments/CENTRAL_E04960/scans/3/resources/DICOM/files/I.003.dcm";
		api.putFileMultipartForm(api.getURL()+destination, filename);
		
	}
}
