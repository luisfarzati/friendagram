package controllers;

import static play.Play.configuration;

import models.Account;

import org.apache.commons.lang.StringUtils;

import com.google.gson.JsonObject;

import play.Logger;
import play.libs.OAuth2;
import play.libs.WS;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Router;

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
		final String fullname = userInfo.get("full_name").getAsString();
		final String photo = userInfo.get("profile_picture").getAsString();

		if(Account.findById(id) == null) {
			final Account account = new Account(id, username, token);
			account.fullname = fullname;
			account.photo = photo;
			account.save();
			flash("account", username);
		}
	}
}
