package org.ibcb.xnat.redaction.interfaces.files;

import java.io.File;
import java.util.HashMap;

import org.dcm4che2.data.DicomObject;
import org.ibcb.xnat.redaction.DICOMExtractor;
import org.ibcb.xnat.redaction.config.Configuration;
import org.ibcb.xnat.redaction.exceptions.PipelineServiceException;
import org.ibcb.xnat.redaction.interfaces.XNATEntity;
import org.ibcb.xnat.redaction.interfaces.XNATFileHandler;

public class DICOMFileHandler extends XNATFileHandler {

	String localfile;
	String redactedfile;
	
	HashMap<String, String> dcmData;
	
	@Override
	public String collection() {
		// TODO Auto-generated method stub
		return "DICOM";
	}

	@Override
	public XNATFileHandler create(String localfile, XNATEntity fileEntity) {
		// TODO Auto-generated method stub
		DICOMFileHandler dcmFile = new DICOMFileHandler();
		
		dcmFile.localfile = localfile;
		dcmFile.fileEntity = fileEntity;
		
		return dcmFile;
	}

	@Override
	public HashMap<String, String> getRedactedData() {
		// TODO Auto-generated method stub
		return dcmData;
	}

	@Override
	public String getRedactedFileLocation() {
		// TODO Auto-generated method stub
		return redactedfile;
	}

	@Override
	public void redact() {
		String path = localfile.substring(0, localfile.lastIndexOf('/'));
		redactedfile = path + "/redacted/" + this.fileEntity.getID();
		
		File dir = new File(path+"/redacted/");
		if(!dir.exists()){
			dir.mkdir();
		}
		
		try{
			DicomObject dcmObj = DICOMExtractor.instance().loadDicom(localfile);
			
			System.out.println("Redacting to: " + redactedfile);
			
			dcmData = DICOMExtractor.instance().extractNameValuePairs(dcmObj, XNATEntity.preservedFields());
			DICOMExtractor.instance().writeDicom(redactedfile, dcmObj);
		}catch(PipelineServiceException pse){
			pse.printStackTrace();
		}
	}

}
