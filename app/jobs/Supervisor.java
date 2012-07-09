package jobs;

import models.Account;
import play.jobs.Job;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class Supervisor {
	public static final ListMultimap<Account, Job> jobs = ArrayListMultimap.create();
}
