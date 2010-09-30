package com.ibcb.xnat.redaction.synchronization;

import java.util.HashMap;

public class JobQueue {
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
		
		public void createTask(String name){
			job_status.put(name, new Status());
		}
		
		public void setTaskState(String task, int state){
			if(job_status.containsKey(task)){
				job_status.get(task).state = state;
				
				// if complete or error, wake up any threads waiting on this job
			}
		}
		
		public void setCompletion(String task, int percent){
			if(job_status.containsKey(task)){
				job_status.get(task).percent = percent;
				if(percent==100)
					setTaskState(task, TASK_STATE_COMPLETE);
			}
		}
		
		public String getMessage(String task){
			if(job_status.containsKey(task)){
				return job_status.get(task).message;
			}
			return null;
		}
		
	}
	
	public static synchronized JobQueue instance(){
		if(singleton==null) singleton = new JobQueue();
		return singleton;
	}
	
	public HashMap<Long, Job> jobs_by_id;
	public HashMap<String, Job> jobs_by_name;
	
	public JobQueue(){
		jobs_by_id = new HashMap<Long, Job>();
		jobs_by_name = new HashMap<String, Job>();
	}
	
	public long	createJob(String name){
		Job j = new Job(name);
		jobs_by_id.put(j.job_id, j);
		jobs_by_name.put(name, j);
		return j.job_id;
	}
	
	public Job getJob(long id){
		if(jobs_by_id.containsKey(id))
			return jobs_by_id.get(id);
		return null;
	}
	
	public Job getJob(String name){
		if(jobs_by_name.containsKey(name))
			return jobs_by_name.get(name);
		return null;
	}
}
