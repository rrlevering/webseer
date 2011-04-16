package org.webseer.model;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.util.index.IndexService;
import org.neo4j.util.index.LuceneIndexService;

public class UserFactory {

	private final Node underlyingNode;

	public UserFactory(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public static UserFactory getUserFactory(NeoService service) {
		return Neo4JUtils.getSingleton(service, NeoRelationshipType.REFERENCE_USERFACTORY, UserFactory.class);
	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	void registerUser(User user, NeoService service) {
		IndexService index = new LuceneIndexService(service);
		index.index(user.getUnderlyingNode(), "login", user.getLogin());
	}

	public User getUser(String login, NeoService service) {
		IndexService index = new LuceneIndexService(service);
		Node userNode = index.getSingleNode("login", login);
		if (userNode == null) {
			return null;
		}
		return Neo4JUtils.getWrapped(userNode, User.class);
	}

	public User getUser(String userid, String password, NeoService service) {
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
