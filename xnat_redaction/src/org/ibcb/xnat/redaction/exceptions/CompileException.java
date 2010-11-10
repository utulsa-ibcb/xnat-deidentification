package org.ibcb.xnat.redaction.exceptions;

public class CompileException extends Exception {
	static final long serialVersionUID=1;
	public CompileException(String s){
		super(s);
	}
	public CompileException(Exception e){
		super(e);
	}
}
