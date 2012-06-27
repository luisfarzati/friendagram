package controllers;

import java.util.List;

import models.Account;
import play.Logger;
import play.libs.OAuth2;
import play.libs.WS;
import play.mvc.Controller;
import play.mvc.With;

import com.google.gson.JsonObject;

import controllers.InstagramApi.Properties;

@With(InstagramApi.class)
public class Settings extends Controller {
	
	public static void oauth() {
		if(OAuth2.isCodeResponse()) {
			Logger.debug(request.url);
			saveToken(retrieveAccessToken(request.params.get("code")));
			accounts();
		}

		authorize();
	}
	
	public static void accounts() {
		final List<Account> accounts = Account.findAll();
		render(accounts);
	}
	
	public static void deleteAccount(long id) {
		Account.delete("id=?", id);
		flash("delete", null);
		accounts();
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