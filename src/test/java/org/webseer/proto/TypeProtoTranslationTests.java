package org.webseer.proto;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.webseer.java.JavaRuntimeFactory;
import org.webseer.model.meta.Field;
import org.webseer.model.meta.Library;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.Type;
import org.webseer.repository.Repository;

@RunWith(JUnit4.class)
public class TypeProtoTranslationTests {

	@Test
	public void testGenerateProtoCode() {
		EmbeddedGraphDatabase service = setupEmptyDB();

		Transaction tran = service.beginTx();
		Type type;
		try {
			type = new Type(service, "org.webseer.Aggregate");
			type.addField(service, new Field(service, new Type(service, "int64"), "large_number", false));

			tran.success();
		} finally {
			tran.finish();
		}
		
		TypeProtoTranslation.generateProtoCode(type);
	}

	@Test
	public void testGenerateProtoCode_Transformation() throws Exception {
		JavaRuntimeFactory factory = new JavaRuntimeFactory(Repository.getDefaultInstance());
		GraphDatabaseService db = setupEmptyDB();

		// Setup
		Transaction tran = db.beginTx();
		Transformation transformation;
		try {
			Library library = new Library(db, "commons-lang", "commons-lang", "2.4");

			transformation = factory.generateTransformation("addToArray", library,
					"org.apache.commons.lang.ArrayUtils::add(long[],int,long)");

			tran.success();
		} finally {
			tran.finish();
		}
		
		TypeProtoTranslation.generateProtoCode(transformation);
	}

	static EmbeddedGraphDatabase setupEmptyDB() {
		return new EmbeddedGraphDatabase("testDomain");
	}

	@Before
	public void setUp() throws Exception {
		// Clean up the test domain directory
		FileUtils.deleteDirectory(new File("testDomain"));
	}

	@After
	public void tearDown() throws Exception {
		// Clean up the test domain directory
		FileUtils.deleteDirectory(new File("testDomain"));
	}
}
