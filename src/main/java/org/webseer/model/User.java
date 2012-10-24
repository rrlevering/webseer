package org.webseer.model;

import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.streams.model.Workspace;

public class User {

	private static final String LOGIN = "login";
	private static final String NAME = "name";
	private static final String PASSWORD = "password";
	private static final String EMAIL = "email";

	private final org.neo4j.graphdb.Node underlyingNode;

	public User(GraphDatabaseService service, UserFactory factory, String login, String password) {
		if (factory.getUser(login, service) != null) {
			throw new IllegalArgumentException("Login must be unique to the UserFactory");
		}
		this.underlyingNode = Neo4JUtils.createNode(service);
		setLogin(login);
		setPassword(password);
		factory.getUnderlyingNode().createRelationshipTo(underlyingNode, NeoRelationshipType.USERFACTORY_USER);
		factory.registerUser(this, service);
	}

	public User(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public User(GraphDatabaseService service, UserFactory factory, String login, String password, String name,
			String email) {
		this(service, factory, login, password);
		setName(name);
		setEmail(email);
	}

	public void setLogin(String login) {
		if (StringUtils.isNotEmpty(login)) {
			underlyingNode.setProperty(LOGIN, login);
		}
	}

	public String getLogin() {
		return (String) underlyingNode.getProperty(LOGIN);
	}

	public void setPassword(String password) {
		if (StringUtils.isNotEmpty(password)) {
			underlyingNode.setProperty(PASSWORD, password);
		}
	}

	public String getPassword() {
		return (String) underlyingNode.getProperty(PASSWORD);
	}

	public void setName(String name) {
		if (StringUtils.isNotEmpty(name)) {
			underlyingNode.setProperty(NAME, name);
		}
	}

	public String getName() {
		return (String) underlyingNode.getProperty(NAME);
	}

	public void setEmail(String email) {
		if (StringUtils.isNotEmpty(email)) {
			underlyingNode.setProperty(EMAIL, email);
		}
	}

	public String getEmail() {
		return (String) underlyingNode.getProperty(EMAIL);
	}

	public Iterable<Workspace> getWorkspaces() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.USER_WORKSPACE, Workspace.class);
	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	public int hashCode() {
		return this.underlyingNode.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof User)) {
			return false;
		}
		return ((User) o).getUnderlyingNode().equals(underlyingNode);
	}

	public String toString() {
		return "User[" + getLogin() + "]";
	}

}
