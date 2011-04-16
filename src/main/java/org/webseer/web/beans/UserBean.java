package org.webseer.web.beans;

public class UserBean {

	private final String login;

	private final String email;

	private final String name;

	public UserBean(String login, String email, String name) {
		this.login = login;
		this.email = email;
		this.name = name;
	}

	public String getLogin() {
		return login;
	}

	public String getEmail() {
		return email;
	}

	public String getName() {
		return name;
	}

}
