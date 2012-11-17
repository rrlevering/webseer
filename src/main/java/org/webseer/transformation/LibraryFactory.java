package org.webseer.transformation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.Library;
import org.webseer.model.meta.Neo4JMetaUtils;

public class LibraryFactory {

	private static Map<GraphDatabaseService, LibraryFactory> SINGLETON = new HashMap<GraphDatabaseService, LibraryFactory>();

	private static final Logger log = Logger.getLogger(LibraryFactory.class
			.getName());

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
					NeoRelationshipType.REFERENCE_LIBRARY_FACTORY, LibraryFactory.class);
			SINGLETON.put(service, factory);
			if (bootstrap) {
				factory.bootstrapBuiltins(service);
			}
			return factory;
		}
		return SINGLETON.get(service);
	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	@SuppressWarnings("unchecked")
	private void bootstrapBuiltins(GraphDatabaseService service) {
		Set<Library> blah = new HashSet<Library>();
		for (Library library : getAllLibraries()) {
			blah.add(library);
		}
		for (Library library : blah) {
			log.info("Removing library: " + library.getGroup() + "/" + library.getName());
			removeLibrary(library);
		}

		// Add/update all the builtin webseer transformations
		Transaction tran = service.beginTx();
		try {
			// Get the jar repository FIXME
			File jarRepository = new File(
					"/workspaces/webseer/webseer/WebContent/WEB-INF/lib");

			// Load the builtin jar registry
			InputStream registryStream = LibraryFactory.class
					.getResourceAsStream("/jar-registry");
			List<String> lines;
			try {
				lines = IOUtils.readLines(registryStream);
				for (String line : lines) {
					String[] entry = line.split(",");
					String group = entry[0];
					String name = entry[1];
					String version = entry[2];
					File file = new File(jarRepository, entry[3]);
					log.info("Adding library " + file + " with name " + group
							+ "/" + name + " and version " + version);
					
					byte[] libraryData = FileUtils.readFileToByteArray(file);
					
					Library library = new Library(service, group, name, version, libraryData);
					
					addLibrary(library);
				}
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			tran.success();
		} finally {
			tran.finish();
		}
	}

}
