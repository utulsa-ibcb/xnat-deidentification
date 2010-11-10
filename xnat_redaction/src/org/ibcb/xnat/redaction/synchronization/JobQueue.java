package org.ibcb.xnat.redaction.synchronization;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.ibcb.xnat.redaction.exceptions.PipelineServiceException;
import org.ibcb.xnat.redaction.interfaces.RedactionPipelineService;

public class JobQueue extends RedactionPipelineService {
	static long current_job_id = 0;
	public static int TASK_STATE_WAITING = 0;
	public static int TASK_STATE_PROCESSING = 1;
	public static int TASK_STATE_ERROR = 2;
	public static int TASK_STATE_COMPLETE = 3;
	
	static JobQueue singleton = null;
	
	private static class Status{
		String message;
		int state;
		int percent;
		
		protected Status(){
			message = "";
			state = TASK_STATE_WAITING;
			percent = 0;
		}
	}
	
	public static class Job{
		long job_id = 0;
		
		String job_name;
		HashMap<String, Status> job_status;
		
		protected Job(String name){
			job_id = current_job_id++;	
			job_name = name;
			job_status = new HashMap<String, Status>();
		}
		
		public synchronized void createTask(String name){
			job_status.put(name, new Status());
		}
		
		public synchronized void setTaskState(String task, int state){
			if(job_status.containsKey(task)){
				job_status.get(task).state = state;
				
				// if complete or error, wake up any threads waiting on this job
				if(state == TASK_STATE_COMPLETE || state == TASK_STATE_ERROR){
					job_status.get(task).notifyAll();						
				}
			}
		}
		
		public synchronized void setCompletion(String task, int percent){
			if(job_status.containsKey(task)){
				job_status.get(task).percent = percent;
				if(percent==100)
					setTaskState(task, TASK_STATE_COMPLETE);
			}
		}
		
		public synchronized String getMessage(String task){
			if(job_status.containsKey(task)){
				return job_status.get(task).message;
			}
			return null;
		}
		
		private synchronized Status getTask(String task){
			if(job_status.containsKey(task))
				return job_status.get(task);
			return null;
		}
		
		public void waitOnTask(String task) throws InterruptedException{
			Status s = getTask(task);
			if(s !=null && s.state != TASK_STATE_COMPLETE && s.state != TASK_STATE_ERROR) s.wait();
		}
	}
	
	public static synchronized JobQueue instance(){
		if(singleton==null) singleton = new JobQueue();
		return singleton;
	}
	
	public synchronized void initialize() throws PipelineServiceException{
		
	}
	
	public HashMap<Long, Job> jobs_by_id;
	public HashMap<String, Job> jobs_by_name;
	public LinkedList<Job> completed_jobs;
	
	public JobQueue(){
		jobs_by_id = new HashMap<Long, Job>();
		jobs_by_name = new HashMap<String, Job>();
		completed_jobs = new LinkedList<Job>();
	}
	
	public synchronized List<Job> getJobsByUnfinishedTask(String task){
		List<Job> jobs = new LinkedList<Job>();
		for(long id : jobs_by_id.keySet()){
			Status t = jobs_by_id.get(id).getTask(task);
			if(t!=null && t.state != TASK_STATE_COMPLETE && t.state != TASK_STATE_ERROR)
				jobs.add(jobs_by_id.get(id));
		}
		return jobs;
	}
	
	public synchronized List<Job> getCurrentJobs(){
		List<Job> jobs = new LinkedList<Job>();
		for(long id : jobs_by_id.keySet())
			jobs.add(jobs_by_id.get(id));
		return jobs;
	}
	
	public synchronized long createJob(String name){
		Job j = new Job(name);
		jobs_by_id.put(j.job_id, j);
		jobs_by_name.put(name, j);
		
		this.notifyAll();
		
		return j.job_id;
	}
	
	public synchronized void completeJob(long id){
		Job j = jobs_by_id.get(id);
		if(j != null){
			completed_jobs.add(j);
			jobs_by_id.remove(id);
			jobs_by_name.remove(j.job_name);
		}
	}
	
	public synchronized Job getJob(long id){
		if(jobs_by_id.containsKey(id))
			return jobs_by_id.get(id);
		return null;
	}
	
	public synchronized Job getJob(String name){
		if(jobs_by_name.containsKey(name))
			return jobs_by_name.get(name);
		return null;
	}
	
	public void waitForJob() throws InterruptedException{
		this.wait();
	}
	
	public void run(){
		
		boolean running=true;
		while(running){
			if(!shuttingDown()){
				// check for new jobs
				// wake up any threads waiting on the queue for new jobs
			}
			else{
				if(jobs_by_name.size()>0){
					
				}
			}
		}
	}
}
