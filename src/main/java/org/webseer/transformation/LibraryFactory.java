package org.webseer.transformation;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.Library;
import org.webseer.model.meta.LibraryResource;
import org.webseer.model.meta.Neo4JMetaUtils;

public class LibraryFactory {

	private static Map<GraphDatabaseService, LibraryFactory> SINGLETON = new HashMap<GraphDatabaseService, LibraryFactory>();

	private final Node underlyingNode;

	public LibraryFactory(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public void addLibrary(Library library) {
		if (getLibrary(library.getGroup(), library.getName(),
				library.getVersion()) != null) {
			throw new RuntimeException("Can't add a type with the same name");
		}
		this.underlyingNode.createRelationshipTo(
				Neo4JMetaUtils.getNode(library),
				NeoRelationshipType.LIBRARY_FACTORY_LIBRARY);
	}

	public Library getLibrary(String group, String name, String version) {
		for (Library type : getAllLibraries()) {
			if (type.getName().equals(name) && type.getGroup().equals(group)
					&& type.getVersion().equals(version)) {
				return type;
			}
		}
		return null;
	}

	public Iterable<Library> getAllLibraries() {
		return Neo4JUtils.getIterable(underlyingNode,
				NeoRelationshipType.LIBRARY_FACTORY_LIBRARY, Library.class);
	}
	
	public LibraryResource getResource(String name) {
		for (Library library : getAllLibraries()) {
			LibraryResource resource = library.getResource(name);
			if (resource != null) {
				return resource;
			}
		}
		return null;
	}

	public void removeLibrary(Library library) {
		Neo4JMetaUtils.getNode(library)
				.getSingleRelationship(NeoRelationshipType.LIBRARY_FACTORY_LIBRARY, Direction.INCOMING)
				.delete();
	}

	public static LibraryFactory getLibraryFactory(GraphDatabaseService service) {
		return getLibraryFactory(service, false);
	}

	public static LibraryFactory getLibraryFactory(GraphDatabaseService service, boolean bootstrap) {
		if (!SINGLETON.containsKey(service)) {
			LibraryFactory factory = Neo4JUtils.getSingleton(service,
					LibraryFactory.class);
			SINGLETON.put(service, factory);
			return factory;
		}
		return SINGLETON.get(service);
	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	public Iterable<LibraryResource> getResourcesInPackage(String pkg) {
		// TODO Auto-generated method stub
		return null;
	}

}
