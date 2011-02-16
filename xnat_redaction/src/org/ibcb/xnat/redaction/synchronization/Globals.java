package org.ibcb.xnat.redaction.synchronization;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedList;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.ibcb.xnat.redaction.config.Configuration;
import org.ibcb.xnat.redaction.helpers.Log;
import org.w3c.dom.Node;

public class Globals {
	public static final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static Log application_log;
	
	public static final NumberFormat idnf = new DecimalFormat("00000");
	
	static{
		application_log = new Log();
		
		application_log.setStream("e", Configuration.instance().getProperty("error_log"), false);
		application_log.setStream("r", Configuration.instance().getProperty("redaction_log"), false);
		application_log.setStream("w", Configuration.instance().getProperty("redaction_warn_log"), false);
	}
	
	public static String nodeToString(Node node) {
		StringWriter sw = new StringWriter();
		try {
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			t.transform(new DOMSource(node), new StreamResult(sw));
		} catch (TransformerException te) {
			System.out.println("nodeToString Transformer Exception");
		}
		return sw.toString();
	}
	
	public static String extractAttributes(Node node, LinkedList<String> ignore){
		String attribute_list = "";
		
		for(int index = 0; index < node.getAttributes().getLength(); index++){
	 		Node n = node.getAttributes().item(index);
	 		
	 		String name = n.getNodeName();
	 		String value = n.getNodeValue();
	 		if(!ignore.contains(name))
	 			attribute_list += " " + name + "=\""+value+"\"";
	 	}
		
		return attribute_list;
	}
	
	
	public static String stackTraceConvert(Throwable t){
		StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return sw.toString();
	}
 
}
