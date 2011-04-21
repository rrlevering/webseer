package org.webseer.model;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;

public class UserFactory {

	private final Node underlyingNode;

	public UserFactory(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public static UserFactory getUserFactory(GraphDatabaseService service) {
		return Neo4JUtils.getSingleton(service, NeoRelationshipType.REFERENCE_USERFACTORY, UserFactory.class);
	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	void registerUser(User user, GraphDatabaseService service) {
		IndexManager indexManager = service.index();
		Index<Node> index = indexManager.forNodes("users");
		index.add(user.getUnderlyingNode(), "login", user.getLogin());
	}

	public User getUser(String login, GraphDatabaseService service) {
		IndexManager indexManager = service.index();
		Index<Node> index = indexManager.forNodes("users");
		IndexHits<Node> hits = index.get("login", login);
		Node userNode = hits.getSingle();
		if (userNode == null) {
			return null;
		}
		return Neo4JUtils.getWrapped(userNode, User.class);
	}

	public User getUser(String userid, String password, GraphDatabaseService service) {
		User match = getUser(userid, service);
		if (match == null) {
			return null;
		}
		if (match.getPassword().equals(password)) {
			return match;
		}
		return null;
	}
}
