package controllers;

import static play.Play.configuration;

import org.apache.commons.lang.StringUtils;

import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Router;

public class InstagramApi extends Controller {
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
}
