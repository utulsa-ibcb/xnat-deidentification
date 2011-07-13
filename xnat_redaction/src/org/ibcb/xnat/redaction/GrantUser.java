package org.ibcb.xnat.redaction;

import java.io.IOException;
import java.rmi.ConnectException;
import java.util.LinkedList;

import javax.xml.transform.TransformerException;

import org.ibcb.xnat.redaction.interfaces.XNATEntity;
import org.ibcb.xnat.redaction.synchronization.Globals;
import org.xml.sax.SAXException;

public class GrantUser {
	
	
	
	public static void main(String args[]){
		String project_id = args[0];
		
		String co_user_id = args[1];
		String co_admin_id = args[2];
		
		try {
			
			Globals.application_log.write(true, "r", "Attempting to grant user \"" + co_user_id + "\" access to project: " + project_id);
			XNATEntity tproject = XNATEntity.getEntity("projects", project_id);
			// get redacted project
			XNATEntity.downloadAll(tproject, "experiments");
			
			// download redacted subject information
			
			
			// match labels and find indicated redacted information
			
			
			// download user data access from PrivacyDB for each subject
			
			
			// check filter status
			
			
			// Grant permissions if all subjects passed filter
			
			
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
