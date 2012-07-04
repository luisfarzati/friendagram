package controllers;

import java.util.List;

import models.Account;
import play.mvc.Controller;
import redis.clients.jedis.Jedis;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class Settings extends Controller {

	public static void accounts() {
		List<Account> accounts = Account.findAll();

		if(request.format.equals("json")) {
			renderJSON(accounts);
		}
		else {
			render(accounts);
		}
	}
	
	public static void addAccount(Account account) {
		account.save();
		flash("added", null);
		accounts();
	}
	
	public static void deleteAccount(Account account) {
		account.delete();
		flash("deleted", null);
		accounts();
	}
}
