package jobs;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import models.User;
import play.Logger;
import play.Play;
import play.jobs.Job;
import play.libs.WS;
import redis.clients.jedis.Jedis;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class FollowJob extends Job {
	private static final String BASE_URL = "http://instagr.am/p/%s";
	private static final String OEMBED = "http://api.instagram.com/oembed?url=%s";
	private static final String LIKES = "https://api.instagram.com/v1/media/%s/likes?access_token=%s";
	private static final String FOLLOW = "https://api.instagram.com/v1/users/%s/relationship?access_token=%s";
	private static final Random RANDOM = new Random();

	public final String token;
	public final String shortCode;
	
	public int likeCount;
	public int followCount;
	public int processed;
	public long timestamp;
	public String error;
	public boolean finished;
	public String status;
	
	public FollowJob(final String token, final String shortCode) {
		this.token = token;
		this.shortCode = shortCode;
	}
	
	@Override
	public void onException(Throwable e) {
		error = e.getMessage();
		finished = true;
		status = "Finished with error";
		super.onException(e);
	}
	
	@Override
	public void doJob() throws Exception {
		super.doJob();
		
		final URL url = new URL(String.format(BASE_URL, shortCode));

		timestamp = System.currentTimeMillis();
    	
		this.status = "Fetching picture information";
		Logger.info("%s %s starting follow job", token, shortCode);
    	final JsonElement oembedInfo = WS.url(ombedize(url)).get().getJson();
    	Logger.trace(oembedInfo.toString());

    	final String mediaId = oembedInfo.getAsJsonObject().get("media_id").getAsString();
    	Logger.debug("%s %s media id is %s", token, shortCode, mediaId);

		this.status = "Getting users who liked";
    	final JsonElement likesInfo = WS.url(likes(mediaId)).get().getJson();
    	Logger.trace(likesInfo.toString());
    	String error = checkForErrors(likesInfo);
    	if(error != null) throw new RuntimeException(error);
    	
    	final JsonArray likes = likesInfo.getAsJsonObject().get("data").getAsJsonArray();
    	likeCount = likes.size();
    	Logger.info("%s %s has %d likes", token, shortCode, likes.size());
    	
    	final List<User> users = new ArrayList<User>();
    	
    	this.status = "Following users";
    	final Jedis jedis = new Jedis("localhost");
    	try {
	    	for(JsonElement like : likes) {
	    		final String userId = like.getAsJsonObject().get("id").getAsString();
	    		final String userName = like.getAsJsonObject().get("username").getAsString();
	    		final String userPhoto = like.getAsJsonObject().get("profile_picture").getAsString();
	    		final boolean exists = jedis.exists(userId);
	    		
	    		users.add(new User(userName, userPhoto, !exists));
	    		processed++;
	
	    		if(exists) {
	        		Logger.debug("%s %s user %s exists; ignoring", token, shortCode, userId);
		    		Thread.sleep(500);
	        		continue;
	    		}
	    		
	    		Logger.info("%s %s user %s new; following", token, shortCode, userId);
	    		final JsonElement followInfo = WS.url(follow(userId)).body("action=follow").post().getJson();
	    		Logger.trace(followInfo.toString());
	
	    		error = checkForErrors(followInfo);
	    		if(error != null) {
	    			Logger.error(error);
	    			if(error.contains("limit")) break;
	    		}
	
	    		followCount++;
	    		jedis.set(userId, "");
	    		Thread.sleep((long)Math.abs(RANDOM.nextFloat() * 2000 + 1000));
	    	}
    		Logger.info("%s %s finished", token, shortCode);
        	status = "Finished successfully";
    		finished = true;
    	}
    	finally {
    		if(jedis.isConnected()) jedis.disconnect();
    	}
	}

    private String checkForErrors(JsonElement json) {
		if(json.getAsJsonObject().get("meta").getAsJsonObject().get("code").getAsInt() != 200) {
			return json.getAsJsonObject().get("meta").getAsJsonObject().get("error_message").getAsString();
		}
		return null;
	}

	private String follow(final String userId) {
		return String.format(FOLLOW, userId, token);
    }

	private String likes(final String mediaId) {
		return String.format(LIKES, mediaId, token);
	}

	private String ombedize(final URL url) {
		return String.format(OEMBED, url);
	}
}