package org.ibcb.xnat.redaction.config;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class Configuration {
	Properties configFile = new Properties();
	
	Configuration singleton;
	
	public Configuration getInstance(){
		if(singleton==null)singleton=new Configuration();
		return singleton;
	}
	
	private Configuration(){
		// load config file from path
		try{
			configFile.load(this.getClass().getClassLoader().getResourceAsStream("./data/redaction.cfg"));
		}catch(IOException e){
			File f = new File("./data/redaction.cfg");
			try{
				if(!f.exists())
					f.createNewFile();
				configFile.load(this.getClass().getClassLoader().getResourceAsStream("./data/redaction.cfg"));
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
}
