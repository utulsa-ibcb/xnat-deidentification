package org.ibcb.xnat.redaction.interfaces;

import java.util.LinkedList;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

public class XNATScan {
	String id;
	
	DOMParser xml;
	
	LinkedList<XNATFile> files = new LinkedList<XNATFile>();
	LinkedList<XNATFile> localFiles = new LinkedList<XNATFile>();
}
