package models;

public class User {
	public String name;
	public String picture;
	public boolean isNew;
	
	public User(String name, String picture, boolean isNew) {
		this.name = name;
		this.picture = picture;
		this.isNew = isNew;
	}
}
