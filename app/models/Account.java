package models;

import javax.persistence.Entity;
import javax.persistence.Id;

import play.db.jpa.GenericModel;

@Entity(name="accounts")
public class Account extends GenericModel {
	@Id
	public long id;
	public String username;
	public String token;
	public String fullname;
	public String photo;

	public Account(long id, String username, String token) {
		this.id = id;
		this.username = username;
		this.token = token;
	}
}
