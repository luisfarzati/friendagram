package controllers;

import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;

import models.Account;
import models.JobManager;
import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;
import redis.clients.jedis.Jedis;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@With(Instagram.class)
public class Application extends Controller {
	
	@SuppressWarnings("unused")
	@Before
	private static void before() {
		List<Account> accounts = Account.findAll();
		renderArgs.put("accounts", accounts);
	}
	
	public static void follow() {
		render();
	}

	public static void tasks(String username, String job) {
    	final Jedis jedis = new Jedis("localhost");
    	final List<String> tasksSerialized;
    	try {
    		tasksSerialized = jedis.lrange(username + ":" + job + ":tasks", 0, -1);
    		Logger.debug(username + ":" + job + ":tasks = %d", tasksSerialized.size());
    	}
    	finally {
    		if(jedis.isConnected()) jedis.disconnect();
    	}
    	
    	renderJSON("[" + StringUtils.join(tasksSerialized, ",") + "]");
	}

	public static void enqueue(String username, String job, String shortcode) {
    	final Jedis jedis = new Jedis("localhost");
    	try {
    		jedis.rpush(username + ":" + job + ":tasks", "{ \"shortCode\": \"" + shortcode + "\" }");
    	}
    	finally {
    		if(jedis.isConnected()) jedis.disconnect();
    	}
	}
	
	public static void delete(String username, String job, String shortcode) {
    	final Jedis jedis = new Jedis("localhost");
    	try {
    		jedis.lrem(username + ":" + job + ":tasks", 1, "{ \"shortCode\": \"" + shortcode + "\" }");
    	}
    	finally {
    		if(jedis.isConnected()) jedis.disconnect();
    	}
	}

	public static void history() {
    	final Jedis jedis = new Jedis("localhost");
    	final List<String> activities;
    	try {
    		activities = jedis.lrange("history", 0, -1);
    	}
    	finally {
    		if(jedis.isConnected()) jedis.disconnect();
    	}
    	
    	JsonArray history = new JsonArray();
    	Gson gson = new Gson();
    	for(String activity : activities) {
    		history.add(gson.fromJson(activity, JsonObject.class));
    	}
    	
    	renderJSON(history);
	}
}