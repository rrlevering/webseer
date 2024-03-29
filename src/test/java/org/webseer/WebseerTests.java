package org.webseer;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.webseer.java.JavaRuntimeFactory;
import org.webseer.model.User;
import org.webseer.model.UserFactory;
import org.webseer.model.meta.FileVersion;
import org.webseer.model.meta.Library;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.TransformationException;
import org.webseer.streams.model.PreviewBuffer;
import org.webseer.streams.model.Workspace;
import org.webseer.streams.model.WorkspaceBucket;
import org.webseer.streams.model.WorkspaceFactory;
import org.webseer.streams.model.program.TransformationEdge;
import org.webseer.streams.model.program.TransformationGraph;
import org.webseer.streams.model.program.TransformationNode;
import org.webseer.streams.model.program.TransformationNodeInput;
import org.webseer.streams.model.program.TransformationNodeOutput;
import org.webseer.streams.model.program.WorkspaceBucketNode;
import org.webseer.streams.model.runtime.BucketReader;
import org.webseer.streams.model.runtime.RuntimeConfiguration;
import org.webseer.streams.model.runtime.RuntimeConfigurationImpl;
import org.webseer.streams.model.trace.Item;
import org.webseer.streams.model.trace.ItemView;
import org.webseer.transformation.TransformationFactory;

public class WebseerTests extends TestCase {

	private static final String GENERATE_FUNCTION_NAME = "org.webseer.GenerateFunction";

	private static final String GENERATE_MULTI_FUNCTION_NAME = "org.webseer.GenerateMultiFunction";

	private static final String CONVERT_FUNCTION_NAME = "org.webseer.ConvertFunction";

	private static final String TIME_FUNCTION_NAME = "org.webseer.TimeFunction";

	private static final String OUTPUTS_FUNCTION_NAME = "org.webseer.OutputsFunction";

	private static final String COUNT_FUNCTION_NAME = "org.webseer.CountFunction";

	private static final String GENERATE_MULTI_FUNCTION = "package org.webseer; import org.webseer.java.JavaFunction; import org.webseer.java.OutputChannel; import java.util.List; import java.util.Arrays; public class GenerateMultiFunction implements JavaFunction { @OutputChannel public List<String> generatedStrings; public void execute() { generatedStrings = Arrays.asList(\"AAAAAAAAAA\", \"BBBBBBBBBB\", \"CCCCCCCCCC\"); }}";

	private static final String TIME_FUNCTION = "package org.webseer; import org.webseer.java.JavaFunction; import org.webseer.java.OutputChannel; public class TimeFunction implements JavaFunction { @OutputChannel public Long currentTime; public void execute() { currentTime = System.currentTimeMillis();}}";

	private static final String GENERATE_FUNCTION = "package org.webseer; import org.webseer.java.JavaFunction; import org.webseer.java.OutputChannel; public class GenerateFunction implements JavaFunction { @OutputChannel public String generatedString; public void execute() { generatedString = String.valueOf(\"String from time \" + System.currentTimeMillis());}}";

	private static final String OUTPUTS_FUNCTION = "package org.webseer; import org.webseer.java.JavaFunction; import org.webseer.java.OutputChannel; public class OutputsFunction implements JavaFunction { @OutputChannel public String firstString; @OutputChannel public String secondString; public void execute() { firstString = String.valueOf(\"First string from time \" + System.currentTimeMillis()); secondString = String.valueOf(\"Second string from time \" + System.currentTimeMillis());}}";

	private static final String CONVERT_FUNCTION = "package org.webseer; import org.webseer.java.JavaFunction; import org.webseer.java.OutputChannel; import org.webseer.java.InputChannel; public class ConvertFunction implements JavaFunction { @InputChannel public String toConvert; @OutputChannel public String convertedString; public void execute() { convertedString = toConvert.substring(0, 6); }}";

	private static final String COUNT_FUNCTION = "package org.webseer; import org.webseer.java.JavaFunction; import org.webseer.java.OutputChannel; import org.webseer.java.InputChannel; public class CountFunction implements JavaFunction { @InputChannel public Iterable<String> toCount; @OutputChannel public int count; public void execute() { count = 0; for (String item : toCount) { count++; } }}";

	public static TestSuite suite() {
		return new TestSuite(WebseerTests.class);
	}

	public void setUp() throws Exception {
		// Clean up the test domain directory
		FileUtils.deleteDirectory(new File("testDomain"));
	}

	public void tearDown() throws Exception {
		// Clean up the test domain directory
		FileUtils.deleteDirectory(new File("testDomain"));
	}

	public void testSimpleRun() throws Exception {
		EmbeddedGraphDatabase neo = setupTestDB();

		Transaction tran = neo.beginTx();
		try {
			TransformationFactory factory = TransformationFactory.getTransformationFactory(neo);

			Transformation transformation = factory.getLatestTransformationByName(GENERATE_FUNCTION_NAME);

			// Create a transient program graph with a single node
			TransformationGraph graph = new TransformationGraph(neo);
			TransformationNode node = new TransformationNode(neo, transformation, graph);

			// Run the graph
			RuntimeConfiguration config = getRuntime(neo);
			Iterator<Item> items = config.getBucketReader(node, "generatedString").getItems();

			assertTrue(items.hasNext());
			Item single = items.next();

			// Print the output
			Object data = single.get();
			assertTrue(data instanceof String);
			assertTrue(((String) data).startsWith("String from time"));

			tran.success();
		} finally {
			tran.finish();
			neo.shutdown();
		}
	}

	public void testSimpleFill() throws Exception {
		EmbeddedGraphDatabase neo = setupTestDB();

		Transaction tran = neo.beginTx();
		try {
			TransformationFactory factory = TransformationFactory.getTransformationFactory(neo);

			Transformation transformation = factory.getLatestTransformationByName(GENERATE_FUNCTION_NAME);

			// Create a transient program graph with a single node
			TransformationGraph graph = new TransformationGraph(neo);
			TransformationNode node = new TransformationNode(neo, transformation, graph);

			WorkspaceBucket bucket = getBucket(neo, "test");

			assertEmpty(bucket.getItems());

			WorkspaceBucketNode bucketNode = new WorkspaceBucketNode(neo, graph, bucket);
			TransformationNodeOutput output = node.getOutput("generatedString");

			// Connect the generator to the bucket
			output.addOutgoingEdge(neo, bucketNode.getInputs().iterator().next());

			// Run the graph
			RuntimeConfiguration config = getRuntime(neo);
			config.fill(bucketNode);

			ItemView single = getSingle(bucket.getItems());

			// Print the output
			Object data = single.get();
			assertTrue(data instanceof String);
			assertTrue(((String) data).startsWith("String from time"));

			tran.success();
		} finally {
			tran.finish();
			neo.shutdown();
		}
	}

	public void testTwoStepFill() throws Exception {
		EmbeddedGraphDatabase neo = setupTestDB();

		Transaction tran = neo.beginTx();
		try {
			TransformationFactory factory = TransformationFactory.getTransformationFactory(neo);

			Transformation transformation = factory.getLatestTransformationByName(GENERATE_FUNCTION_NAME);

			// Create a transient program graph with a single node
			TransformationGraph graph = new TransformationGraph(neo);
			TransformationNode node = new TransformationNode(neo, transformation, graph);

			WorkspaceBucket bucket = getBucket(neo, "test");

			assertEmpty(bucket.getItems());

			transformation = factory.getLatestTransformationByName(CONVERT_FUNCTION_NAME);

			TransformationNode node2 = new TransformationNode(neo, transformation, graph);

			TransformationNodeOutput output = node.getOutput("generatedString");
			TransformationNodeInput input = node2.getInput("toConvert");
			output.addOutgoingEdge(neo, input);

			WorkspaceBucketNode bucketNode = new WorkspaceBucketNode(neo, graph, bucket);

			// Connect the generator to the bucket
			output = node2.getOutput("convertedString");
			output.addOutgoingEdge(neo, bucketNode.getInputs().iterator().next());

			// Run the graph
			RuntimeConfiguration config = getRuntime(neo);
			config.fill(bucketNode);

			ItemView single = getSingle(bucket.getItems());

			// Print the output
			Object data = single.get();
			assertTrue(data instanceof String);
			assertTrue(((String) data).equals("String"));

			tran.success();
		} finally {
			tran.finish();
			neo.shutdown();
		}
	}

	public void testTwoStepPreview() throws Exception {
		EmbeddedGraphDatabase neo = setupTestDB();

		long count, afterCount;
		Transaction tran = neo.beginTx();
		try {
			TransformationFactory factory = TransformationFactory.getTransformationFactory(neo);

			Transformation transformation = factory.getLatestTransformationByName(GENERATE_FUNCTION_NAME);

			// Create a transient program graph with a single node
			TransformationGraph graph = new TransformationGraph(neo);
			TransformationNode node = new TransformationNode(neo, transformation, graph);

			WorkspaceBucket bucket = getBucket(neo, "test");

			assertEmpty(bucket.getItems());

			transformation = factory.getLatestTransformationByName(CONVERT_FUNCTION_NAME);

			TransformationNode node2 = new TransformationNode(neo, transformation, graph);

			TransformationNodeOutput output = node.getOutput("generatedString");
			TransformationNodeInput input = node2.getInput("toConvert");
			output.addOutgoingEdge(neo, input);

			WorkspaceBucketNode bucketNode = new WorkspaceBucketNode(neo, graph, bucket);

			// Connect the generator to the bucket
			output = node2.getOutput("convertedString");
			TransformationEdge edge = output.addOutgoingEdge(neo, bucketNode.getInputs().iterator().next());

			// Create this first so it's not in the count
			WorkspaceBucket previewBucket = getPreviewBucket(neo);

			count = neo.getNodeManager().getNumberOfIdsInUse(Node.class);

			// Run the graph
			RuntimeConfiguration config = getRuntime(neo);

			Iterator<ItemView> items = config.previewWire(edge, 1);

			assertTrue(items.hasNext());
			ItemView single = items.next();
			Object data = single.get();
			assertTrue(data instanceof String);
			assertTrue(((String) data).equals("String"));

			previewBucket.removeAll();

			tran.success();
		} finally {
			tran.finish();
		}

		try {
			afterCount = neo.getNodeManager().getNumberOfIdsInUse(Node.class);
			assertEquals("Should not have extra references after all the items from the run are deleted", count,
					afterCount);
		} finally {
			neo.shutdown();
		}
	}

	public void testTwoStepCastingFill() throws Exception {
		EmbeddedGraphDatabase neo = setupTestDB();

		Transaction tran = neo.beginTx();
		try {
			TransformationFactory factory = TransformationFactory.getTransformationFactory(neo);

			Transformation transformation = factory.getLatestTransformationByName(TIME_FUNCTION_NAME);

			// Create a transient program graph with a single node
			TransformationGraph graph = new TransformationGraph(neo);
			TransformationNode node = new TransformationNode(neo, transformation, graph);

			WorkspaceBucket bucket = getBucket(neo, "test");

			assertEmpty(bucket.getItems());

			transformation = factory.getLatestTransformationByName(CONVERT_FUNCTION_NAME);

			TransformationNode node2 = new TransformationNode(neo, transformation, graph);

			TransformationNodeOutput output = node.getOutput("currentTime");
			TransformationNodeInput input = node2.getInput("toConvert");
			output.addOutgoingEdge(neo, input);

			WorkspaceBucketNode bucketNode = new WorkspaceBucketNode(neo, graph, bucket);

			// Connect the generator to the bucket
			output = node2.getOutput("convertedString");
			output.addOutgoingEdge(neo, bucketNode.getInputs().iterator().next());

			// Run the graph
			RuntimeConfiguration config = getRuntime(neo);
			config.fill(bucketNode);

			ItemView single = getSingle(bucket.getItems());

			// Print the output
			Object data = single.get();
			assertTrue(data instanceof String);
			assertTrue(((String) data).matches("\\d{6}"));

			tran.success();
		} finally {
			tran.finish();
			neo.shutdown();
		}
	}

	public void testMultiRun() throws Exception {
		EmbeddedGraphDatabase neo = setupTestDB();

		Transaction tran = neo.beginTx();
		try {
			TransformationFactory factory = TransformationFactory.getTransformationFactory(neo);

			Transformation transformation = factory.getLatestTransformationByName(GENERATE_MULTI_FUNCTION_NAME);

			// Create a transient program graph with a single node
			TransformationGraph graph = new TransformationGraph(neo);
			TransformationNode node = new TransformationNode(neo, transformation, graph);

			// Run the graph
			RuntimeConfiguration config = getRuntime(neo);
			Iterator<Item> items = config.getBucketReader(node, "generatedStrings").getItems();

			assertTrue(items.hasNext());
			Item single = items.next();
			Object data = single.get();
			assertEquals("AAAAAAAAAA", data);

			assertTrue(items.hasNext());
			single = items.next();
			data = single.get();
			assertEquals("BBBBBBBBBB", data);

			assertTrue(items.hasNext());
			single = items.next();
			data = single.get();
			assertEquals("CCCCCCCCCC", data);

			tran.success();
		} finally {
			tran.finish();
			neo.shutdown();
		}
	}

	public void testMultiFill() throws Exception {
		EmbeddedGraphDatabase neo = setupTestDB();

		Transaction tran = neo.beginTx();
		try {
			TransformationFactory factory = TransformationFactory.getTransformationFactory(neo);

			Transformation transformation = factory.getLatestTransformationByName(GENERATE_MULTI_FUNCTION_NAME);

			// Create a transient program graph with a single node
			TransformationGraph graph = new TransformationGraph(neo);
			TransformationNode node = new TransformationNode(neo, transformation, graph);

			WorkspaceBucket bucket = getBucket(neo, "test");

			assertEmpty(bucket.getItems());

			WorkspaceBucketNode bucketNode = new WorkspaceBucketNode(neo, graph, bucket);
			TransformationNodeOutput output = node.getOutput("generatedStrings");

			// Connect the generator to the bucket
			output.addOutgoingEdge(neo, bucketNode.getInputs().iterator().next());

			// Run the graph
			RuntimeConfiguration config = getRuntime(neo);
			config.fill(bucketNode);

			Iterator<ItemView> items = bucket.getItems().iterator();

			assertTrue(items.hasNext());
			ItemView single = items.next();
			Object data = single.get();
			assertEquals("AAAAAAAAAA", data);

			assertTrue(items.hasNext());
			single = items.next();
			data = single.get();
			assertEquals("BBBBBBBBBB", data);

			assertTrue(items.hasNext());
			single = items.next();
			data = single.get();
			assertEquals("CCCCCCCCCC", data);

			tran.success();
		} finally {
			tran.finish();
			neo.shutdown();
		}
	}

	public void testMultiTwoStepFill() throws Exception {
		EmbeddedGraphDatabase neo = setupTestDB();

		Transaction tran = neo.beginTx();
		try {
			TransformationFactory factory = TransformationFactory.getTransformationFactory(neo);

			Transformation transformation = factory.getLatestTransformationByName(GENERATE_MULTI_FUNCTION_NAME);

			// Create a transient program graph with a single node
			TransformationGraph graph = new TransformationGraph(neo);
			TransformationNode node = new TransformationNode(neo, transformation, graph);

			WorkspaceBucket bucket = getBucket(neo, "test");

			assertEmpty(bucket.getItems());

			transformation = factory.getLatestTransformationByName(CONVERT_FUNCTION_NAME);

			TransformationNode node2 = new TransformationNode(neo, transformation, graph);

			TransformationNodeOutput output = node.getOutput("generatedStrings");
			TransformationNodeInput input = node2.getInput("toConvert");
			output.addOutgoingEdge(neo, input);

			WorkspaceBucketNode bucketNode = new WorkspaceBucketNode(neo, graph, bucket);

			// Connect the generator to the bucket
			output = node2.getOutput("convertedString");
			output.addOutgoingEdge(neo, bucketNode.getInputs().iterator().next());

			// Run the graph
			RuntimeConfiguration config = getRuntime(neo);
			config.fill(bucketNode);

			Iterator<ItemView> items = bucket.getItems().iterator();

			assertTrue(items.hasNext());
			ItemView single = items.next();
			Object data = single.get();
			assertEquals("AAAAAA", data);

			assertTrue(items.hasNext());
			single = items.next();
			data = single.get();
			assertEquals("BBBBBB", data);

			assertTrue(items.hasNext());
			single = items.next();
			data = single.get();
			assertEquals("CCCCCC", data);

			tran.success();
		} finally {
			tran.finish();
			neo.shutdown();
		}
	}

	/**
	 * Tests a bucket pulling from a previous bucket directly.
	 */
	public void testChainBucket() throws Exception {
		EmbeddedGraphDatabase neo = setupTestDB();

		Transaction tran = neo.beginTx();
		try {
			TransformationFactory factory = TransformationFactory.getTransformationFactory(neo);
			Transformation transformation = factory.getLatestTransformationByName(GENERATE_MULTI_FUNCTION_NAME);

			// Create a transient program graph with a single node
			TransformationGraph graph = new TransformationGraph(neo);
			TransformationNode node = new TransformationNode(neo, transformation, graph);

			WorkspaceBucket bucket = getBucket(neo, "test");

			assertEmpty(bucket.getItems());

			WorkspaceBucketNode bucketNode = new WorkspaceBucketNode(neo, graph, bucket);
			TransformationNodeOutput output = node.getOutput("generatedStrings");

			// Connect the generator to the bucket
			output.addOutgoingEdge(neo, bucketNode.getInputs().iterator().next());

			// Run the graph
			RuntimeConfiguration config = getRuntime(neo);
			config.fill(bucketNode);

			// Now connect another bucket
			bucket = getBucket(neo, "test 2");
			assertEmpty(bucket.getItems());
			WorkspaceBucketNode bucketNode2 = new WorkspaceBucketNode(neo, graph, bucket);
			bucketNode.getOutputs().iterator().next().addOutgoingEdge(neo, bucketNode2.getInputs().iterator().next());

			// Pull into the second bucket
			config = getRuntime(neo);
			config.fill(bucketNode2);

			Iterator<ItemView> items = bucket.getItems().iterator();

			assertTrue(items.hasNext());
			ItemView single = items.next();
			Object data = single.get();
			assertEquals("AAAAAAAAAA", data);

			assertTrue(items.hasNext());
			single = items.next();
			data = single.get();
			assertEquals("BBBBBBBBBB", data);

			assertTrue(items.hasNext());
			single = items.next();
			data = single.get();
			assertEquals("CCCCCCCCCC", data);

			tran.success();
		} finally {
			tran.finish();
			neo.shutdown();
		}
	}

	public void testDeletingBucket() throws Exception {
		EmbeddedGraphDatabase neo = setupTestDB();

		Transaction tran = neo.beginTx();
		long count, afterCount;
		try {
			TransformationFactory factory = TransformationFactory.getTransformationFactory(neo);
			Transformation transformation = factory.getLatestTransformationByName(GENERATE_MULTI_FUNCTION_NAME);

			// Create a transient program graph with a single node
			TransformationGraph graph = new TransformationGraph(neo);
			TransformationNode node = new TransformationNode(neo, transformation, graph);

			WorkspaceBucket bucket = getBucket(neo, "test");

			assertEmpty(bucket.getItems());

			WorkspaceBucketNode bucketNode = new WorkspaceBucketNode(neo, graph, bucket);
			TransformationNodeOutput output = node.getOutput("generatedStrings");

			// Connect the generator to the bucket
			output.addOutgoingEdge(neo, bucketNode.getInputs().iterator().next());

			// Save the node count...after we do the fill and then delete, we should have no extra references
			count = neo.getNodeManager().getNumberOfIdsInUse(Node.class);

			// Run the graph
			RuntimeConfiguration config = getRuntime(neo);
			config.fill(bucketNode);

			// Now we have three things in the bucket
			// Make sure that the whole graph history is intact

			Iterator<ItemView> items = bucket.getItems().iterator();

			assertTrue(items.hasNext());
			ItemView single = items.next();
			Object data = single.get();
			assertEquals("AAAAAAAAAA", data);

			single.getViewData();

			assertTrue(items.hasNext());
			single = items.next();
			data = single.get();
			assertEquals("BBBBBBBBBB", data);

			assertTrue(items.hasNext());
			single = items.next();
			data = single.get();
			assertEquals("CCCCCCCCCC", data);

			assertFalse(items.hasNext());

			// Remove the last item
			bucket.removeItem(single);

			items = bucket.getItems().iterator();

			assertTrue(items.hasNext());
			single = items.next();
			data = single.get();
			assertEquals("AAAAAAAAAA", data);

			assertTrue(items.hasNext());
			single = items.next();
			data = single.get();
			assertEquals("BBBBBBBBBB", data);

			assertFalse(items.hasNext());

			// Remove the last item
			bucket.removeItem(single);

			items = bucket.getItems().iterator();

			assertTrue(items.hasNext());
			single = items.next();
			data = single.get();
			assertEquals("AAAAAAAAAA", data);

			assertFalse(items.hasNext());

			// Remove the last item
			bucket.removeItem(single);

			items = bucket.getItems().iterator();

			assertFalse(items.hasNext());

			tran.success();
		} finally {
			tran.finish();
		}

		try {
			afterCount = neo.getNodeManager().getNumberOfIdsInUse(Node.class);
			assertEquals("Should not have extra references after all the items from the run are deleted", count,
					afterCount);
		} finally {
			neo.shutdown();
		}
	}

	public void testMultipleOutputs() throws Exception {
		EmbeddedGraphDatabase neo = setupTestDB();

		Transaction tran = neo.beginTx();
		try {
			TransformationFactory factory = TransformationFactory.getTransformationFactory(neo);

			Transformation transformation = factory.getLatestTransformationByName(OUTPUTS_FUNCTION_NAME);

			// Create a transient program graph with a single node
			TransformationGraph graph = new TransformationGraph(neo);
			TransformationNode node = new TransformationNode(neo, transformation, graph);

			// Run the graph
			RuntimeConfiguration config = getRuntime(neo);
			BucketReader[] readers = config.getBucketReaders(node, new String[] { "firstString", "secondString" });

			Iterator<Item> firstStrings = readers[0].getItems();
			Iterator<Item> secondStrings = readers[1].getItems();

			assertTrue(firstStrings.hasNext());
			Item single = firstStrings.next();

			// Print the output
			Object data = single.get();
			assertTrue(data instanceof String);
			assertTrue(((String) data).startsWith("First string from time"));

			assertTrue(secondStrings.hasNext());
			single = secondStrings.next();

			// Print the output
			data = single.get();
			assertTrue(data instanceof String);
			assertTrue(((String) data).startsWith("Second string from time"));

			tran.success();
		} finally {
			tran.finish();
			neo.shutdown();
		}
	}

	/**
	 * Feeds multiples into an aggregate input that counts the number fed in.
	 */
	public void testAggregateInput() throws Exception {
		EmbeddedGraphDatabase neo = setupTestDB();

		Transaction tran = neo.beginTx();
		try {
			TransformationFactory factory = TransformationFactory.getTransformationFactory(neo);
			Transformation transformation = factory.getLatestTransformationByName(GENERATE_MULTI_FUNCTION_NAME);

			// Create a transient program graph with a single node
			TransformationGraph graph = new TransformationGraph(neo);
			TransformationNode node = new TransformationNode(neo, transformation, graph);

			WorkspaceBucket bucket = getBucket(neo, "test");

			assertEmpty(bucket.getItems());

			transformation = factory.getLatestTransformationByName(COUNT_FUNCTION_NAME);

			TransformationNode node2 = new TransformationNode(neo, transformation, graph);

			TransformationNodeOutput output = node.getOutput("generatedStrings");
			TransformationNodeInput input = node2.getInput("toCount");
			output.addOutgoingEdge(neo, input);

			WorkspaceBucketNode bucketNode = new WorkspaceBucketNode(neo, graph, bucket);

			// Connect the generator to the bucket
			output = node2.getOutput("count");
			output.addOutgoingEdge(neo, bucketNode.getInputs().iterator().next());

			// Run the graph
			RuntimeConfiguration config = getRuntime(neo);
			config.fill(bucketNode);

			ItemView view = getSingle(bucket.getItems());
			assertEquals(3, view.get());

			tran.success();
		} finally {
			tran.finish();
			neo.shutdown();
		}
	}

	static EmbeddedGraphDatabase setupEmptyDB() {
		return new EmbeddedGraphDatabase("testDomain");
	}

	static WorkspaceBucket getBucket(GraphDatabaseService service, String name) {
		Workspace workspace = WorkspaceFactory.getWorkspaceFactory(service).getWorkspace("test");
		if (workspace.getWorkspaceBucket(name) == null) {
			new WorkspaceBucket(service, workspace, name);
		}
		return workspace.getWorkspaceBucket(name);
	}

	static WorkspaceBucket getPreviewBucket(GraphDatabaseService service) {
		return PreviewBuffer.getPreviewBuffer(service).getPreviewBucket(service, "test");
	}

	static RuntimeConfiguration getRuntime(GraphDatabaseService service) {
		Workspace workspace = WorkspaceFactory.getWorkspaceFactory(service).getWorkspace("test");
		return new RuntimeConfigurationImpl(service, workspace, "test");
	}

	static EmbeddedGraphDatabase setupTestDB() throws TransformationException {
		EmbeddedGraphDatabase neo = setupEmptyDB();

		Transaction tran = neo.beginTx();
		try {
			UserFactory users = UserFactory.getUserFactory(neo);
			WorkspaceFactory workspaces = WorkspaceFactory.getWorkspaceFactory(neo);
			User testUser = new User(neo, users, "test", "test");
			new Workspace(neo, workspaces, testUser, "test");

			TransformationFactory factory = TransformationFactory.getTransformationFactory(neo);

			FileVersion source = new FileVersion(neo, GENERATE_FUNCTION);
			Transformation transformation = JavaRuntimeFactory.getDefaultInstance().generateTransformation(
					GENERATE_FUNCTION_NAME, source, Collections.<Library> emptyList());
			transformation.setDescription("Cool function");

			factory.addTransformation(transformation);

			source = new FileVersion(neo, TIME_FUNCTION);
			transformation = JavaRuntimeFactory.getDefaultInstance().generateTransformation(TIME_FUNCTION_NAME, source,
					Collections.<Library> emptyList());
			transformation.setDescription("Outputs the current time");

			factory.addTransformation(transformation);

			source = new FileVersion(neo, CONVERT_FUNCTION);
			transformation = JavaRuntimeFactory.getDefaultInstance().generateTransformation(CONVERT_FUNCTION_NAME,
					source, Collections.<Library> emptyList());
			transformation.setDescription("Cool function");

			factory.addTransformation(transformation);

			source = new FileVersion(neo, GENERATE_MULTI_FUNCTION);
			transformation = JavaRuntimeFactory.getDefaultInstance().generateTransformation(
					GENERATE_MULTI_FUNCTION_NAME, source, Collections.<Library> emptyList());
			transformation.setDescription("Cool function");

			factory.addTransformation(transformation);

			source = new FileVersion(neo, OUTPUTS_FUNCTION);
			transformation = JavaRuntimeFactory.getDefaultInstance().generateTransformation(OUTPUTS_FUNCTION_NAME,
					source, Collections.<Library> emptyList());
			transformation.setDescription("Two outputs");

			factory.addTransformation(transformation);

			source = new FileVersion(neo, COUNT_FUNCTION);
			transformation = JavaRuntimeFactory.getDefaultInstance().generateTransformation(COUNT_FUNCTION_NAME,
					source, Collections.<Library> emptyList());
			transformation.setDescription("Counts the number of items");

			factory.addTransformation(transformation);

			tran.success();
		} finally {
			tran.finish();
		}

		return neo;
	}

	static <T> void assertEmpty(Iterable<T> iterable) {
		Iterator<T> iterator = iterable.iterator();
		assertFalse(iterator.hasNext());
	}

	static <T> T getSingle(Iterable<T> iterable) {
		Iterator<T> iterator = iterable.iterator();
		assertTrue(iterator.hasNext());

		T next = iterator.next();
		assertFalse(iterator.hasNext());
		return next;
	}
}
