package org.webseer.java;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.webseer.model.meta.Library;
import org.webseer.model.meta.OutputPoint;
import org.webseer.model.meta.Transformation;
import org.webseer.repository.Repository;

import com.google.common.collect.Iterables;

@RunWith(JUnit4.class)
public class JavaRuntimeFactoryTests {

	private JavaRuntimeFactory factory = new JavaRuntimeFactory(Repository.getDefaultInstance());

	@Test
	public void testGetTransformationLocations() throws Exception {
		GraphDatabaseService db = setupEmptyDB();

		// Setup
		Transaction tran = db.beginTx();
		Library library;
		try {
			library = new Library(db, "commons-lang", "commons-lang", "2.4");
			tran.success();
		} finally {
			tran.finish();
		}
		Iterable<String> locations = factory.getTransformationLocations(library);

		Assert.assertEquals(796, Iterables.size(locations));
	}

	@Test
	public void testGenerateTransformationFromLibrary() throws Exception {
		GraphDatabaseService db = setupEmptyDB();

		// Setup
		Transaction tran = db.beginTx();
		Library library;
		try {
			library = new Library(db, "commons-lang", "commons-lang", "2.4");

			Transformation transformation = factory.generateTransformation("test", library,
					"org.apache.commons.lang.ArrayUtils::add(long[],int,long)");

			Assert.assertEquals(1, Iterables.size(transformation.getOutputPoints()));

			OutputPoint returnOutput = transformation.getOutputPoints().iterator().next();

			Assert.assertEquals("int64", returnOutput.getType().getName());
			Assert.assertTrue(returnOutput.getField().isRepeated());

			Assert.assertEquals(3, Iterables.size(transformation.getInputPoints()));

			tran.success();
		} finally {
			tran.finish();
		}
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
