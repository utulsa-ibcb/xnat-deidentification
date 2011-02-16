package org.ibcb.xnat.redaction.helpers;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;

public class Log {
	public static class OutStream{
		boolean ready;
		OutputStreamWriter output;
		int last_flush=0;
	}
	
	static private int flush_frequency = 1;
	
	HashMap<Character, Boolean> enable=new HashMap<Character, Boolean>();
	HashMap<Character, OutStream> writers=new HashMap<Character, OutStream>();
	OutStream def;
	OutStream err;
	
	SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	boolean enabled=true;
	
	public Log(){
		enable.put('o', true);
		def = new OutStream();
		def.output=new OutputStreamWriter(System.out);
		def.ready=true;
		writers.put('o', def);
		
		enable.put('e', true);
		err = new OutStream();
		err.output=new OutputStreamWriter(System.err);
		err.ready=true;
		writers.put('e', err);
	}
	@Override
	public void finalize(){
		for(Character c : writers.keySet()){
			OutStream os = writers.get(c);
			if(os.last_flush>0){
				try{
					os.output.flush();
				}catch(IOException e){
					e.printStackTrace();
				}
				os.last_flush=0;
			}
		}
	}
	
	public void disable(){
		enabled=false;
	}
	
	public void enable(){
		enabled=true;
	}
	
	public boolean isEnabled(){
		return enabled;
	}
	
	public void setEnabled(boolean status){
		enabled=status;
	}

	public void write(boolean timestamp, String flags, String message){
		LinkedList<OutStream> outputs=new LinkedList<OutStream>();
		String stamp=(timestamp ? "["+date.format(Calendar.getInstance().getTime())+"]: " : "");
		
		for(int a = 0; a < flags.length(); a++){
			char c = flags.charAt(a);
			if(enable.containsKey(c) && enable.get(c)) {
				OutStream os = writers.get(c);
				if(!outputs.contains(os)) outputs.add(os);
			}
		}
		for(OutStream os : outputs){
			if(os.ready){
				try{
					os.output.write(stamp+message+"\n");
					os.last_flush++;
					if(os.last_flush>=flush_frequency){
						os.last_flush=0;
						os.output.flush();
					}
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}
	}
	
	public void write(String flags, String message){
		write(true,flags,message);
	}
	
	public void write(String message){
		write(true,"0",message);		
	}
	
	public void setStream(String flags, OutStream os){
		for(int a = 0; a < flags.length(); a++){
			writers.put(flags.charAt(a), os);
		}
	}
	
	public void setStream(String flags, OutputStreamWriter osw){
		OutStream os = new OutStream();
		os.output=osw;
		os.ready=true;
		
		setStream(flags, os);
	}
	
	public void setStream(String flags, String filename, boolean trunc){
		try{
			FileWriter fr = new FileWriter(filename, !trunc);
			setStream(flags,fr);
		}catch(IOException e){
			System.err.println("Error opening log file... " + filename);
		}
	}
	
	public void setStream(String flags, OutputStream os){
		setStream(flags, new OutputStreamWriter(os));
	}
	
	public void stdout(String flags){
		setStream(flags, def);
	}
	
	public void stderr(String flags){
		setStream(flags, err);
	}
	
	public void disableFlags(String flags){
		setFlags(flags,false);
	}
	
	public void enableFlags(String flags){
		setFlags(flags,true);
	}
	
	public void setFlags(String flags, boolean value){		
		for(int a = 0; a < flags.length(); a++){
			enable.put(flags.charAt(a), value);
		}
	}
}
