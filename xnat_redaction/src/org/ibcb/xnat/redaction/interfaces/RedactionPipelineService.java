package org.ibcb.xnat.redaction.interfaces;

public abstract class RedactionPipelineService implements Runnable {
	private Thread myThread=null;
	private boolean shutdown=false;
	
	public synchronized boolean isActive(){
		return myThread!=null ? myThread.isAlive() : false;
	}
	
	public synchronized void shutdown(){
		shutdown = true;
	}
	
	protected synchronized boolean shuttingDown(){
		return shutdown;
	}
	
	public synchronized boolean isShutDown(){
		return myThread!=null ? !myThread.isAlive() : true;
	}
	
	public synchronized Thread getThread(){
		return myThread;
	}
	
	public synchronized void start(){
		myThread = new Thread(this);
		myThread.start();
	}
	
	public abstract void run();
}
