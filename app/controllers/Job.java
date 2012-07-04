package controllers;

import models.JobManager;
import play.mvc.Controller;

public class Job extends Controller {
	public static void status(final long id, final String code) {
		renderJSON(JobManager.instance.get(id, code));
	}
	
    public static void enqueue(final long id, final String shortCode) {
    	JobManager.instance.start(id, shortCode.replace("/", ""));
    }
}
