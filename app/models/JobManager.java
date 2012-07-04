package models;

import java.util.HashMap;
import java.util.Map;

import jobs.FollowJob;
import play.libs.F.Promise;

public class JobManager {
	public static final JobManager instance = new JobManager();
	
	private Map<Long,Map<String,FollowJob>> jobs = new HashMap<Long,Map<String,FollowJob>>();
	private Map<FollowJob,Promise> tasks = new HashMap<FollowJob,Promise>();
	
	public Map<String,FollowJob> get(long accountId) {
		if(!jobs.containsKey(accountId)) jobs.put(accountId, new HashMap<String,FollowJob>());
		return jobs.get(accountId); 
	}

	public FollowJob get(long accountId, String code) {
		return get(accountId).get(code);
	}
	
	public void start(long accountId, String code) {
		FollowJob job = get(accountId) != null ? get(accountId, code) : null;
		if(job != null) {
			if(tasks.get(job).isDone()) {
				tasks.remove(job);
				get(accountId).remove(code);
			}
			else {
				throw new RuntimeException("A job for this account+photo is still running");
			}
		}

		String token = ((Account)Account.findById(accountId)).token;
		job = new FollowJob(token, code);
		get(accountId).put(code, job);
		tasks.put(job, job.now());
	}
}
