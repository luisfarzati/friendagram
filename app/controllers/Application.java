package controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jobs.FollowJob;
import models.Account;
import play.Logger;
import play.Play;
import play.libs.F.Promise;
import play.libs.WS;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

import com.google.gson.JsonObject;

@With(InstagramApi.class)
public class Application extends Controller {
	static class JobManager {
		private Map<Long,FollowJob> jobs = new HashMap<Long,FollowJob>();
		private Map<FollowJob,Promise> tasks = new HashMap<FollowJob,Promise>();
		
		public FollowJob get(long id) {
			return jobs.get(id);
		}
		
		public void start(long id, String shortCode) {
			FollowJob job = jobs.get(id);
			if(job != null) {
				if(tasks.get(job).isDone()) {
					tasks.remove(job);
					jobs.remove(id);
				}
				else {
					throw new RuntimeException("A job for this account is still running");
				}
			}
			
			String token = ((Account)Account.findById(id)).token;
			job = new FollowJob(token, shortCode);
			jobs.put(id, job);
			tasks.put(job, job.now());
		}
	}
	
	private static final JobManager JOBS = new JobManager();
	
	@SuppressWarnings("unused")
	@Before
	private static void before() {
		renderArgs.put("clientId", Play.configuration.getProperty("instagram.clientId"));
		renderArgs.put("redirectUri", Play.configuration.getProperty("instagram.redirectUri"));
		
		List<Account> accounts = Account.findAll();
		renderArgs.put("accounts", accounts);
	}
	
	
	public static void index(Long id) {
		if(id != null) {
			renderArgs.put("job", JOBS.get(id));
			renderArgs.put("id", id);
		}
		render();
	}
	
	public static void status(long id) {
		renderJSON(JOBS.get(id));
	}
	
    public static void runJob(final long id, final String shortCode) throws InterruptedException {
    	JOBS.start(id, shortCode.replace("/", ""));
//        render(url, users, error, followCount);
    }
}