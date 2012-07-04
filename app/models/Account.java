package models;

import javax.persistence.Entity;
import javax.persistence.Id;

import play.db.jpa.GenericModel;

import com.google.gson.annotations.Expose;

@Entity(name="accounts")
public class Account extends GenericModel {
	@Id
	public long id;
	public String username;
	public String fullname;
	public String photo;

	@Expose(serialize=false)
	public String token;

	public Account(long id, String username, String token) {
		this.id = id;
		this.username = username;
		this.token = token;
	}
}
