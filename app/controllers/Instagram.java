package controllers;

import static play.Play.configuration;

import java.net.MalformedURLException;
import java.net.URL;

import models.Account;

import org.apache.commons.lang.StringUtils;

import play.Logger;
import play.libs.OAuth2;
import play.libs.WS;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Router;

import com.google.gson.JsonObject;

public class Instagram extends Controller {
	static class Properties {
		static final String ACCESS_TOKEN_URL = "https://api.instagram.com/oauth/access_token";
		static final String AUTHORIZATION_URL = "https://api.instagram.com/oauth/authorize";

		public static String accessTokenUrl() {
			return configuration.getProperty("instagram.oauth.accessTokenUrl", ACCESS_TOKEN_URL);
		}
		public static String authorizationUrl() {
			return configuration.getProperty("instagram.oauth.authorizationUrl", AUTHORIZATION_URL);
		}
		public static String clientId() {
			return configuration.getProperty("instagram.clientId");
		}
		public static String clientSecret() {
			return configuration.getProperty("instagram.clientSecret");
		}
		public static String redirectUri() {
			return configuration.getProperty("instagram.redirectUri", Router.getFullUrl(request.action));
		}
	}

	@SuppressWarnings("unused")
	@Before
	private static void before() {
		if(StringUtils.trimToNull(Properties.clientId()) == null) {
			error("Instagram API not configured: 'instagram.clientId' value missing.");
		}
		if(StringUtils.trimToNull(Properties.clientSecret()) == null) {
			error("Instagram API not configured: 'instagram.clientSecret' value missing.");
		}
	}

	public static void oauth() {
		if(OAuth2.isCodeResponse()) {
			Logger.debug(request.url);
			saveToken(retrieveAccessToken(request.params.get("code")));
			Settings.accounts();
		}

		authorize();
	}

	private static void authorize() {
		
		final String authorizeUrl = 
				String.format("%s?client_id=%s&redirect_uri=%s&response_type=code&scope=relationships", 
						Properties.authorizationUrl(), Properties.clientId(), Properties.redirectUri());
		
		Logger.debug(authorizeUrl);
		
		redirect(authorizeUrl);
	}
	
	private static JsonObject retrieveAccessToken(String code) {
		final JsonObject tokenInfo = WS.url(Properties.accessTokenUrl())
				.setParameter("client_id", Properties.clientId())
				.setParameter("client_secret", Properties.clientSecret())
				.setParameter("grant_type", "authorization_code")
				.setParameter("redirect_uri", Properties.redirectUri())
				.setParameter("code", code)
				.post().getJson().getAsJsonObject();
		
		Logger.debug(tokenInfo.toString());
		return tokenInfo;
	}

	private static void saveToken(JsonObject tokenInfo) {
		final String token = tokenInfo.get("access_token").getAsString();
		final JsonObject userInfo = tokenInfo.get("user").getAsJsonObject(); 
		final long id = userInfo.get("id").getAsLong();
		final String username = userInfo.get("username").getAsString();
		URL photo;
		try {
			photo = new URL(userInfo.get("profile_picture").getAsString());
		} catch (MalformedURLException e) {
			e.printStackTrace();
			photo = null;
		}

		if(Account.findById(id) == null) {
			new Account(id, username, photo, token).save();
			flash("account", username);
		}
	}
}
