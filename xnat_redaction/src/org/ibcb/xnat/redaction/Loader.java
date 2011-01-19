package org.ibcb.xnat.redaction;

import java.util.HashMap;

import org.ibcb.xnat.redaction.config.CheckoutRuleset;
import org.ibcb.xnat.redaction.config.Configuration;

public class Loader {

	
	public static void main(String args[]){
		
		String project_id = args[0];
		String co_user_id = args[1];
		String co_admin_id = args[2];
		
		// load schemas and extractors
		
		// load redaction rules
		
		// load checkout ruleset
		CheckoutRuleset cr = new CheckoutRuleset();
		cr.loadRuleSet(Configuration.instance().getProperty("checkout_rules"));
		
		// download project and subject ids
		
		// download checkout user information from our database -Liang
		
		// populate map of checkout fields -Liang
		HashMap<String, String> requesting_user_data = new HashMap<String, String>();
		
		
		
		// run permissions checks against checkout ruleset information
		boolean passed = cr.filter(requesting_user_data);
		
		// if passed, download remainder of project information
		
			// redact XNATSubject demographics
		
			// redact DICOMFiles
		
			// upload redacted information to database -Matt
		
			// reinsert requested, authorized information into XNAT and DICOM -Matt
		
			// update information about checked out PHI to database -Liang
		
			// upload new project -Matt
		
			// upload experiment information -Matt
		
			// upload subject information -Matt
			
				// upload scans -Matt
				// upload DICOM files -Matt
		
		// Send error messages, warnings, incident reports, and other items to an administrative user -Stephen
		
		// Return completion code
	}
}