package org.ibcb.xnat.redaction.interfaces;

import java.io.IOException;
import java.rmi.ConnectException;
import java.util.HashMap;

import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

public abstract class XNATFileHandler {
	protected XNATEntity fileEntity;	
	
	public abstract String collection();	// return the collection type of this file handler, eg. DICOM
	
	public abstract HashMap<String, String> getRedactedData();
	public abstract void redact();
	
	public abstract String getRedactedFileLocation();
	
	public abstract XNATFileHandler create(String localfile, XNATEntity fileEntity);
	
	
}
