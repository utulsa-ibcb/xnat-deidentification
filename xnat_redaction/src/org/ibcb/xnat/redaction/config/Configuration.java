package org.ibcb.xnat.redaction.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

import org.ibcb.xnat.redaction.synchronization.Globals;

public class Configuration {
	Properties configFile = new Properties();
	
	static Configuration singleton;
	
	public static Configuration instance(){
		if(singleton==null)singleton=new Configuration();
		return singleton;
	}
	
	private Configuration(){
		// load config file from path
		try{
			FileInputStream in = new FileInputStream("./data/config.cfg");
			configFile.load(in);
			in.close();
		}catch(IOException e){
			File f = new File("./data/config.cfg");
			try{
				if(!f.exists())
					f.createNewFile();
				FileInputStream in = new FileInputStream("./data/config.cfg");
				configFile.load(in);
				in.close();
			}catch(IOException e2){
				e2.printStackTrace();
				System.err.println("Could not load configuration file! Exiting...");
				System.exit(1);
			}
		}
	}
	
	public String getProperty(String name){
		return configFile.getProperty(name);
	}
	
	public void setProperty(String name, String value){
		configFile.setProperty(name, value);
	}
	
	public void save() {
		try{
			FileOutputStream out = new FileOutputStream("./data/config.cfg");
			configFile.store(out, "---XNAT Redaction Config File update: " + Globals.dateFormatter.format(Calendar.getInstance().getTime()));
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}
