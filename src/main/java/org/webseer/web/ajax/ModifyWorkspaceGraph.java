package org.webseer.web.ajax;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import name.levering.ryan.util.IterableUtils;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.meta.Field;
import org.webseer.model.meta.InputPoint;
import org.webseer.model.meta.OutputPoint;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.TransformationException;
import org.webseer.model.meta.Type;
import org.webseer.streams.model.Program;
import org.webseer.streams.model.Workspace;
import org.webseer.streams.model.WorkspaceBucket;
import org.webseer.streams.model.program.DisconnectedWorkspaceBucketNode;
import org.webseer.streams.model.program.TransformationEdge;
import org.webseer.streams.model.program.TransformationGraph;
import org.webseer.streams.model.program.TransformationNode;
import org.webseer.streams.model.program.TransformationNodeInput;
import org.webseer.streams.model.program.TransformationNodeOutput;
import org.webseer.streams.model.program.WorkspaceBucketNode;
import org.webseer.streams.model.runtime.RuntimeConfiguration;
import org.webseer.streams.model.trace.InputGroup;
import org.webseer.streams.model.trace.Item;
import org.webseer.streams.model.trace.ItemView;
import org.webseer.streams.model.trace.TransformationGroup;
import org.webseer.transformation.TransformationFactory;
import org.webseer.util.WebseerConfiguration;
import org.webseer.web.WebConfigurationImpl;
import org.webseer.web.WorkspaceServlet;
import org.webseer.web.model.WebEnhancedTransformationGraph;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class ModifyWorkspaceGraph extends WorkspaceServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void serviceWorkspace(Workspace workspace, HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		GraphDatabaseService service = getNeoService();

		Transaction tran = service.beginTx();
		try {
			Program mainProgram = workspace.getProgram("Main");
			if (mainProgram == null) {
				TransformationGraph programGraph = new TransformationGraph(service);
				mainProgram = new Program(service, workspace, programGraph, "Main");
			}

			String bucketIO = WebseerConfiguration.getConfiguration().getString("WEBSEER_BUCKET_IO");
			RuntimeConfiguration config = new WebConfigurationImpl(req, resp, workspace,
					getCurrentUser(req).getLogin(), getServletContext().getRealPath(""), service, isUser(
							workspace.getOwner(), req), bucketIO);

			JsonParser parser = new JsonParser();
			JsonElement request = parser.parse(req.getReader());
			JsonObject object = request.getAsJsonObject();
			int requestId = object.get("id").getAsInt();
			String method = object.get("method").getAsString();
			JsonElement params = object.get("params");

			JsonElement returnValue;

			if (method.equals("listWirings")) {
				returnValue = listWirings(params, mainProgram);
			} else if (method.equals("saveWiring")) {
				returnValue = saveWiring(params, mainProgram);
			} else if (method.equals("addContainer")) {
				returnValue = addContainer(params, mainProgram);
			} else if (method.equals("addBucketContainer")) {
				returnValue = addBucketContainer(params, mainProgram);
			} else if (method.equals("addWire")) {
				returnValue = addWire(params, mainProgram);
			} else if (method.equals("removeContainer")) {
				returnValue = removeContainer(params, mainProgram);
			} else if (method.equals("removeWire")) {
				returnValue = removeWire(params, mainProgram);
			} else if (method.equals("previewWire")) {
				returnValue = previewWire(params, mainProgram, config);
			} else if (method.equals("fillBucketContainer")) {
				returnValue = fillBucketContainer(params, mainProgram, config);
			} else if (method.equals("previewBucketContainer")) {
				returnValue = previewBucketContainer(params, mainProgram);
			} else if (method.equals("renameBucketContainer")) {
				returnValue = renameBucketContainer(params, mainProgram);
			} else if (method.equals("viewItemHistory")) {
				returnValue = viewItemHistory(params, mainProgram);
			} else if (method.equals("changeWorkspaceName")) {
				returnValue = changeWorkspaceName(params, mainProgram);
			} else if (method.equals("moveContainer")) {
				returnValue = moveContainer(params, mainProgram);
			} else if (method.equals("getSource")) {
				returnValue = getSource(params, mainProgram);
			} else if (method.equals("deleteItem")) {
				returnValue = deleteItem(params, mainProgram);
			} else if (method.equals("setInputValue")) {
				returnValue = setInputValue(params, mainProgram);
			} else if (method.equals("deleteBucket")) {
				returnValue = deleteBucket(params, mainProgram);
			} else if (method.equals("getWireOptions")) {
				returnValue = getWireOptions(params, mainProgram);
			} else if (method.equals("getModules")) {
				returnValue = getModules(params, mainProgram);
			} else if (method.equals("setWeir")) {
				returnValue = setWeir(params, mainProgram);
			} else {
				throw new ServletException("Could not find RPC method for " + method + ":" + requestId);
			}

			JsonObject wrapper = new JsonObject();
			wrapper.add("result", returnValue);

			Writer writer = resp.getWriter();
			writer.write(wrapper.toString());
			writer.close();

			tran.success();
		} finally {
			try {
				tran.finish();
			} catch (TransactionFailureException e) {
				// When we preview wires, this is expected, so don't freak out
				e.printStackTrace();
			}
		}

	}

	private JsonElement setWeir(JsonElement params, Program mainProgram) throws ServletException {
		JsonObject paramObject = params.getAsJsonObject();
		long sourceId = paramObject.get("srcId").getAsLong();
		long targetId = paramObject.get("targetId").getAsLong();
		String weirIdField = paramObject.get("weirId").getAsString();
		int weirId = Integer.parseInt(weirIdField.split(",")[0]);
		String weirField = weirIdField.split(",", -1)[1];
		if (weirField.isEmpty()) {
			weirField = null;
		}
		String sourceOutput = paramObject.get("srcOutput").getAsString();
		String targetInput = paramObject.get("targetInput").getAsString();

		TransformationNode source = Neo4JUtils.get(getNeoService(), sourceId, TransformationNode.class);
		TransformationNode target = Neo4JUtils.get(getNeoService(), targetId, TransformationNode.class);

		TransformationNodeOutput output = source.getOutput(sourceOutput);
		if (output == null) {
			return new JsonObject();
		}
		TransformationNodeInput input = target.getInput(targetInput);
		if (input == null) {
			return new JsonObject();
		}

		TransformationNodeInput toLink = null;
		if (weirId > 0) {
			toLink = Neo4JUtils.get(getNeoService(), weirId, TransformationNodeInput.class);
		}

		for (TransformationEdge edge : output.getOutgoingEdges()) {
			if (edge.getInput().equals(input)) {
				// Found it
				edge.setLinkedPoint(toLink);
				edge.setOutputField(weirField);
				return new JsonObject();
			}
		}

		throw new ServletException("Unable to find edge to link");
	}

	private JsonObject getType(Type type) {
		JsonObject typeObject = new JsonObject();
		typeObject.add("name", new JsonPrimitive(type.getName()));
		JsonArray fields = new JsonArray();
		for (org.webseer.model.meta.Field field : type.getFields()) {
			JsonObject fieldObject = new JsonObject();
			fieldObject.add("field", new JsonPrimitive(field.getName()));
			fieldObject.add("type", getType(field.getType()));
			fields.add(fieldObject);
		}
		typeObject.add("fields", fields);

		return typeObject;
	}

	private JsonElement getModules(JsonElement params, Program mainProgram) {
		JsonArray modules = new JsonArray();

		TransformationFactory factory = TransformationFactory.getTransformationFactory(getNeoService(), true);
		// First add all the transformations
		for (Transformation transformation : factory.getAllTransformations()) {
			JsonObject transformationObject = new JsonObject();
			transformationObject.add("name", new JsonPrimitive(transformation.getSimpleName()));
			transformationObject.add("category", new JsonPrimitive(transformation.getPackage()));
			JsonObject container;
			if (transformation.getOutput("httpResponse") == null) {
				container = createTransformationContainer(transformation);
			} else {
				container = createRendererContainer(transformation);
			}
			transformationObject.add("container", container);
			modules.add(transformationObject);
		}

		// Now add the buckets
		for (WorkspaceBucket bucket : mainProgram.getWorkspace().getWorkspaceBuckets()) {
			JsonObject bucketObject = new JsonObject();
			bucketObject.add("name", new JsonPrimitive(bucket.getName()));
			bucketObject.add("category", new JsonPrimitive("Bucket"));
			JsonObject container = new JsonObject();
			bucketObject.add("container", container);
			container.add("xtype", new JsonPrimitive("Webseer.WebseerBucketContainer"));
			container.add("label", new JsonPrimitive(bucket.getName()));
			if (bucket.getType() != null) {
				container.add("type", getType(bucket.getType()));
			}
			modules.add(bucketObject);
		}

		JsonObject bucketObject = new JsonObject();
		bucketObject.add("name", new JsonPrimitive("New Bucket"));
		bucketObject.add("category", new JsonPrimitive("Bucket"));
		JsonObject container = new JsonObject();
		bucketObject.add("container", container);
		container.add("xtype", new JsonPrimitive("Webseer.WebseerBucketContainer"));
		container.add("label", new JsonPrimitive("Bucket"));
		modules.add(bucketObject);

		return modules;
	}

	private JsonObject createTransformationContainer(Transformation transformation) {
		JsonParser parser = new JsonParser();
		JsonObject container = new JsonObject();
		container.add("xtype", new JsonPrimitive("Webseer.WebseerContainer"));
		container.add("typeId", new JsonPrimitive(transformation.getName()));
		container.add("version", new JsonPrimitive(transformation.getVersion()));
		container.add("title", new JsonPrimitive(transformation.getSimpleName()));
		container.add(
				"height",
				new JsonPrimitive(50 + 30 * Math.max(IterableUtils.size(transformation.getInputPoints()),
						IterableUtils.size(transformation.getOutputPoints()))));
		JsonArray terminals = new JsonArray();
		container.add("terminals", terminals);
		int counter = 1;
		for (InputPoint input : transformation.getInputPoints()) {
			JsonObject terminal = new JsonObject();
			terminal.add("name", new JsonPrimitive(input.getName()));
			terminal.add("type", getType(input.getType()));
			terminal.add("direction", parser.parse("[-1,0]"));
			terminal.add("offsetPosition", parser.parse("[-14,0]"));
			terminal.add("multiple", new JsonPrimitive(input.isVarArgs()));
			terminal.add(
					"relativePosition",
					parser.parse("[0,"
							+ (counter++ / (double) (1 + IterableUtils.size(transformation.getInputPoints()))) + "]"));
			terminals.add(terminal);
		}
		counter = 1;
		for (OutputPoint output : transformation.getOutputPoints()) {
			JsonObject terminal = new JsonObject();
			terminal.add("name", new JsonPrimitive(output.getName()));
			terminal.add("type", getType(output.getType()));
			terminal.add("direction", parser.parse("[1,0]"));
			terminal.add("offsetPosition", parser.parse("[-16,0]"));
			terminal.add(
					"relativePosition",
					parser.parse("[1,"
							+ (counter++ / (double) (1 + IterableUtils.size(transformation.getOutputPoints()))) + "]"));
			terminals.add(terminal);
		}
		return container;
	}

	private JsonObject createRendererContainer(Transformation transformation) {
		JsonParser parser = new JsonParser();
		JsonObject transformationObject = new JsonObject();
		transformationObject.add("name", new JsonPrimitive(transformation.getSimpleName()));
		transformationObject.add("category", new JsonPrimitive(transformation.getPackage()));
		JsonObject container = new JsonObject();
		transformationObject.add("container", container);
		container.add("xtype", new JsonPrimitive("Webseer.RendererContainer"));
		container.add("typeId", new JsonPrimitive(transformation.getName()));
		container.add("version", new JsonPrimitive(transformation.getVersion()));
		container.add("height", new JsonPrimitive(50 + 30 * IterableUtils.size(transformation.getInputPoints())));
		JsonArray terminals = new JsonArray();
		container.add("terminals", terminals);
		int counter = 1;
		for (InputPoint input : transformation.getInputPoints()) {
			JsonObject terminal = new JsonObject();
			terminal.add("name", new JsonPrimitive(input.getName()));
			terminal.add("type", getType(input.getType()));
			terminal.add("direction", parser.parse("[-1,0]"));
			terminal.add("offsetPosition", parser.parse("[-14,0]"));
			terminal.add("multiple", new JsonPrimitive(input.isVarArgs()));
			terminal.add(
					"relativePosition",
					parser.parse("[0,"
							+ (counter++ / (double) (1 + IterableUtils.size(transformation.getInputPoints()))) + "]"));
			terminals.add(terminal);
		}
		return container;
	}

	private JsonObject createBucketContainer(Type type, String label) {
		JsonObject config = new JsonObject();
		config.add("xtype", new JsonPrimitive("Webseer.WebseerBucketContainer"));
		config.add("label", new JsonPrimitive(label));
		if (type != null) {
			config.add("type", getType(type));
		}
		return config;
	}

	private JsonObject createTransformationNodeContainer(TransformationNode node) {
		JsonObject config;

		WorkspaceBucketNode asBucket = node.asWorkspaceBucketNode();
		DisconnectedWorkspaceBucketNode asDisconnectedBucket = node.asDisconnectedWorkspaceBucketNode();
		if (node.getOutput("httpResponse") != null) {
			config = createRendererContainer(node.getTransformation());
		} else if (asBucket != null) {
			config = createBucketContainer(asBucket.getLinkedBucket().getType(), asBucket.getLinkedBucket().getName());
		} else if (asDisconnectedBucket != null) {
			config = createBucketContainer(asDisconnectedBucket.getType(), asDisconnectedBucket.getLinkedBucketName());
		} else {
			config = createTransformationContainer(node.getTransformation());
		}
		config.add("dbId", new JsonPrimitive(node.getNodeId()));

		// Add input values
		if (config.has("terminals")) {
			JsonArray terminals = config.get("terminals").getAsJsonArray();
			for (int i = 0; i < terminals.size(); i++) {
				JsonObject terminal = terminals.get(i).getAsJsonObject();
				TransformationNodeInput inputPoint = node.getInput(terminal.get("name").getAsString());
				if (inputPoint != null) {
					if (inputPoint.hasValue()) {
						terminal.add("value", new JsonPrimitive(inputPoint.getType().getValue(inputPoint).toString()));
					} else if (inputPoint.getMeta() != null) {
						terminal.add("value", new JsonPrimitive(inputPoint.getMeta()));
					}
				}
			}
		}

		return config;
	}

	public JsonElement listWirings(JsonElement params, Program program) {
		TransformationGraph graph = program.getGraph();
		JsonParser parser = new JsonParser();

		JsonArray array = new JsonArray(); // Holds all of the 1 wirings :)
		JsonObject main = new JsonObject();
		main.add("name", new JsonPrimitive("Main"));
		JsonObject working = new JsonObject();
		JsonArray modules = new JsonArray();
		JsonArray wires = new JsonArray();
		Map<TransformationNode, Integer> positions = new HashMap<TransformationNode, Integer>();
		int pos = 0;
		for (TransformationNode node : graph.getNodes()) {
			JsonObject module = new JsonObject();
			JsonObject config = createTransformationNodeContainer(node);
			if (node.getUIProperty("position") != null) {
				config.add("position", parser.parse(node.getUIProperty("position")));
			} else {
				JsonArray posArray = new JsonArray();
				posArray.add(new JsonPrimitive(0));
				posArray.add(new JsonPrimitive(0));
				config.add("position", posArray);
			}
			WorkspaceBucketNode asBucket = node.asWorkspaceBucketNode();
			if (node.getOutput("httpResponse") != null) {
				module.add("name", new JsonPrimitive(node.getTransformation().getSimpleName()));
			} else if (asBucket == null) {
				module.add("name", new JsonPrimitive(node.getTransformation().getSimpleName()));
			} else {
				module.add("name", new JsonPrimitive(asBucket.getLinkedBucket().getName()));
			}

			module.add("config", config);
			modules.add(module);
			positions.put(node, pos);
			pos++;
		}
		for (TransformationNode node : graph.getNodes()) {
			// Add all the edges from here
			for (TransformationNodeOutput output : node.getOutputs()) {
				for (TransformationEdge edge : output.getOutgoingEdges()) {
					TransformationNodeInput input = edge.getInput();
					JsonObject wire = new JsonObject();
					wire.add("xtype", new JsonPrimitive("Webseer.WebseerWire"));
					if (edge.getInputField() != null) {
						wire.add("targetField", new JsonPrimitive(edge.getInputField()));
					}
					if (edge.getOutputField() != null) {
						wire.add("srcField", new JsonPrimitive(edge.getOutputField()));
					}
					JsonObject src = new JsonObject();
					src.add("moduleId", new JsonPrimitive(positions.get(node)));
					src.add("terminal", new JsonPrimitive(output.getOutputField().getName()));
					wire.add("src", src);
					JsonObject tgt = new JsonObject();
					tgt.add("moduleId", new JsonPrimitive(positions.get(input.getTransformationNode())));
					tgt.add("terminal", new JsonPrimitive(input.getInputField().getName()));
					wire.add("tgt", tgt);
					if (edge.getLinkedPoint() != null) {
						TransformationNodeInput linkPoint = edge.getLinkedPoint();
						String outputField = edge.getOutputField();
						TransformationNode linkSource = linkPoint.getTransformationNode();

						if (linkSource.asWorkspaceBucketNode() != null) {
							wire.add("label", new JsonPrimitive(linkSource.asWorkspaceBucketNode().getLinkedBucket()
									.getName()
									+ " contents"));
						} else {
							wire.add("label", new JsonPrimitive(linkSource.getTransformation().getName() + "::"
									+ linkPoint.getInputField().getName()
									+ (outputField == null ? "" : "." + outputField)));
						}
					}
					wires.add(wire);
				}
			}
		}
		working.add("modules", modules);
		working.add("wires", wires);
		JsonObject properties = new JsonObject();
		properties.add("name", new JsonPrimitive(""));
		properties.add("description", new JsonPrimitive(""));
		working.add("properties", properties);
		main.add("working", working);
		main.add("language", new JsonPrimitive("webseer"));
		array.add(main);

		return array;
	}

	public JsonElement saveWiring(JsonElement params, Program program) {
		TransformationGraph graph = program.getGraph();

		WebEnhancedTransformationGraph webGraph = WebEnhancedTransformationGraph.get(getNeoService(),
				graph.getGraphId());
		webGraph.getUI().setGraphSnapshot(params.toString());

		return new JsonObject();
	}

	public JsonElement addContainer(JsonElement params, Program program) {
		TransformationGraph graph = program.getGraph();
		JsonObject paramObject = params.getAsJsonObject();
		String uri = paramObject.get("typeId").getAsString();
		JsonElement positionObject = paramObject.get("position");
		Transformation transformation = TransformationFactory.getTransformationFactory(getNeoService())
				.getLatestTransformationByName(uri);
		TransformationNode node = new TransformationNode(getNeoService(), transformation, graph);
		node.setUIProperty("position", positionObject.toString());
		JsonObject returnValue = new JsonObject();
		returnValue.add("dbId", new JsonPrimitive(node.getNodeId()));
		return returnValue;
	}

	public JsonElement addBucketContainer(JsonElement params, Program program) {
		JsonObject paramObject = params.getAsJsonObject();
		JsonElement positionObject = paramObject.get("position");
		JsonElement nameObject = paramObject.get("name");
		TransformationGraph graph = program.getGraph();

		Workspace workspace = program.getWorkspace();
		WorkspaceBucket bucket = workspace.getWorkspaceBucket(nameObject.getAsString());
		if (bucket == null) {
			bucket = new WorkspaceBucket(getNeoService(), program.getWorkspace(), nameObject.getAsString());
		}

		TransformationNode node = new WorkspaceBucketNode(getNeoService(), graph, bucket);
		node.setUIProperty("position", positionObject.toString());
		JsonObject returnValue = new JsonObject();
		returnValue.add("dbId", new JsonPrimitive(node.getNodeId()));
		return returnValue;
	}

	public JsonElement addWire(JsonElement params, Program program) {
		JsonObject paramObject = params.getAsJsonObject();
		long sourceId = paramObject.get("srcId").getAsLong();
		long targetId = paramObject.get("targetId").getAsLong();
		String sourceOutput = paramObject.get("srcOutput").getAsString();
		String targetInput = paramObject.get("targetInput").getAsString();

		TransformationNode source = Neo4JUtils.get(getNeoService(), sourceId, TransformationNode.class);
		TransformationNode target = Neo4JUtils.get(getNeoService(), targetId, TransformationNode.class);
		TransformationNodeOutput output = source.getOutput(sourceOutput);
		TransformationNodeInput input = target.getInput(targetInput);
		TransformationEdge newEdge = output.addOutgoingEdge(getNeoService(), input);
		if (paramObject.has("srcField") && !paramObject.get("srcField").isJsonNull()) {
			newEdge.setOutputField(paramObject.get("srcField").getAsString());
		}
		if (paramObject.has("targetField") && !paramObject.get("targetField").isJsonNull()) {
			newEdge.setInputField(paramObject.get("targetField").getAsString());
		}

		return new JsonObject();
	}

	public JsonElement removeContainer(JsonElement params, Program program) {
		JsonObject paramObject = params.getAsJsonObject();
		JsonElement containerIdObject = paramObject.get("containerId");
		if (containerIdObject == null) {
			return new JsonObject(); // LOG
		}
		long containerId = containerIdObject.getAsLong();
		TransformationNode node = Neo4JUtils.get(getNeoService(), containerId, TransformationNode.class);
		if (node == null) {
			System.out.println("Could not find node with id " + containerId);
			return new JsonObject();
		}
		node.delete();
		return new JsonObject();
	}

	private JsonElement getSource(JsonElement params, Program program) {
		JsonObject paramObject = params.getAsJsonObject();
		JsonElement containerIdObject = paramObject.get("containerId");
		if (containerIdObject == null) {
			return new JsonObject(); // LOG
		}
		long containerId = containerIdObject.getAsLong();
		TransformationNode node = Neo4JUtils.get(getNeoService(), containerId, TransformationNode.class);
		if (node == null) {
			// LOG
			return new JsonObject();
		}
		return new JsonPrimitive(StringEscapeUtils.escapeHtml(node.getTransformation().getCode()));
	}

	private JsonElement moveContainer(JsonElement params, Program mainProgram) {
		JsonObject paramObject = params.getAsJsonObject();
		JsonElement containerIdObject = paramObject.get("containerId");
		JsonElement positionObject = paramObject.get("position");
		if (containerIdObject == null) {
			return new JsonObject(); // LOG
		}
		long containerId = containerIdObject.getAsLong();
		TransformationNode node = Neo4JUtils.get(getNeoService(), containerId, TransformationNode.class);
		if (node == null) {
			// LOG
			return new JsonObject();
		}
		node.setUIProperty("position", positionObject.toString());
		return new JsonObject();
	}

	public JsonElement getWireOptions(JsonElement params, Program program) {
		JsonObject paramObject = params.getAsJsonObject();
		long sourceId = paramObject.get("srcId").getAsLong();
		String sourceOutput = paramObject.get("srcOutput").getAsString();

		TransformationNode source = Neo4JUtils.get(getNeoService(), sourceId, TransformationNode.class);
		TransformationNodeOutput output = source.getOutput(sourceOutput);
		JsonArray possibilities = new JsonArray();
		JsonObject possibility = new JsonObject();
		possibility.add("id", new JsonPrimitive("-1,"));
		possibility.add("label", new JsonPrimitive(output.getOutputField().getName()));
		possibilities.add(possibility);
		accumulateTypeOptions(-1, output.getOutputField().getName() + ".", "", output.getOutputField().getType(),
				possibilities);

		if (source.asWorkspaceBucketNode() == null) {
			// Walk back the graph. Add any output points you find and any runtime structure from buckets.
			accumulateWeirPossibilities(source, possibilities, new HashSet<TransformationNode>());
		}
		return possibilities;
	}

	private void accumulateTypeOptions(long rootId, String prefix, String name, Type type, JsonArray possibilities) {
		for (Field field : type.getFields()) {
			JsonObject possibility = new JsonObject();
			possibility.add("id", new JsonPrimitive(rootId + "," + field.getName()));
			possibility
					.add("label", new JsonPrimitive(prefix + (name.isEmpty() ? "" : (name + ".")) + field.getName()));
			possibilities.add(possibility);
			accumulateTypeOptions(rootId, prefix, name.isEmpty() ? field.getName() : (name + "." + field.getName()),
					field.getType(), possibilities);
		}
	}

	private void accumulateWeirPossibilities(TransformationNode source, JsonArray possibilities,
			Set<TransformationNode> handled) {
		handled.add(source);

		// Add all the output points that feed into this
		for (TransformationNodeInput input : source.getInputs()) {
			JsonObject possibility = new JsonObject();
			possibility.add("id", new JsonPrimitive(input.getId() + ","));
			possibility.add("label", new JsonPrimitive(source.getTransformation().getName() + "::"
					+ input.getInputField().getName()));
			possibilities.add(possibility);
			accumulateTypeOptions(input.getId(), source.getTransformation().getName() + "::"
					+ input.getInputField().getName() + ".", "", input.getType(), possibilities);

			for (TransformationEdge edge : input.getIncomingEdges()) {
				TransformationNodeOutput output = edge.getOutput();
				TransformationNode node = output.getTransformationNode();
				if (node.asWorkspaceBucketNode() == null) {
					if (!handled.contains(node)) {
						accumulateWeirPossibilities(node, possibilities, handled);
					}
				} else {
					// Loop through the items and track back
					// FIXME: This should be cached on the bucket
					WorkspaceBucket bucket = node.asWorkspaceBucketNode().getLinkedBucket();
					Set<TransformationNodeInput> dupes = new HashSet<TransformationNodeInput>();
					for (ItemView item : bucket.getItems()) {
						if (item.getViewScope() != null) {
							findSource(Collections.singletonList(item.getViewScope()), possibilities, dupes);
						}
					}
				}
			}
		}
	}

	private void findSource(Iterable<Item> items, JsonArray possibilities, Set<TransformationNodeInput> handled) {
		Item first = null;
		Set<TransformationGroup> transforms = new LinkedHashSet<TransformationGroup>();
		for (Item item : items) {
			TransformationGroup group = item.getOutputGroup().getTransformationGroup();
			transforms.add(group);

			if (first == null) {
				first = item;
			}
		}

		TransformationNode tGroup = first.getOutputGroup().getTransformationGroup().getRuntimeNode()
				.getTransformationNode();

		for (TransformationNodeInput nodeInput : tGroup.getInputs()) {
			if (IterableUtils.size(nodeInput.getIncomingEdges()) == 0 && !handled.contains(nodeInput)) {
				if (nodeInput.hasValue() || nodeInput.getMeta() != null) {
					System.out.println("Adding " + nodeInput);
					handled.add(nodeInput);
					JsonObject possibility = new JsonObject();
					possibility.add("id", new JsonPrimitive(nodeInput.getId() + ","));
					possibility.add("label", new JsonPrimitive(nodeInput.getTransformationNode().getTransformation()
							.getName()
							+ "::" + nodeInput.getInputField().getName()));
					possibilities.add(possibility);
				}
			}
		}

		Map<TransformationNodeInput, List<Item>> itemInputs = new HashMap<TransformationNodeInput, List<Item>>();
		for (TransformationGroup group : transforms) {
			for (InputGroup iGroup : group.getInputGroups()) {
				TransformationNodeInput previous = iGroup.getInputQueue().getInput();
				List<Item> previousNodes = itemInputs.get(previous);
				if (previousNodes == null) {
					previousNodes = new ArrayList<Item>();
					itemInputs.put(previous, previousNodes);
				}
				for (ItemView item : iGroup.getItems()) {
					if (item.getViewScope() != null) {
						previousNodes.add(item.getViewScope());
					}
				}
			}
		}

		// Now try to go backward
		// Loop through all the items in the input groups and create a list of all the output groups they came from
		for (TransformationNodeInput input : tGroup.getInputs()) {
			if (itemInputs.containsKey(input) && !handled.contains(input)) {
				handled.add(input);
				JsonObject possibility = new JsonObject();
				possibility.add("id", new JsonPrimitive(input.getId()));
				possibility.add("label", new JsonPrimitive(input.getTransformationNode().getTransformation().getName()
						+ "::" + input.getInputField().getName()));
				possibilities.add(possibility);

				findSource(itemInputs.get(input), possibilities, handled);
			}
		}
	}

	public JsonElement removeWire(JsonElement params, Program program) {
		JsonObject paramObject = params.getAsJsonObject();
		long sourceId = paramObject.get("srcId").getAsLong();
		String sourceField = paramObject.get("srcField").isJsonNull() ? null : paramObject.get("srcField")
				.getAsString();
		long targetId = paramObject.get("targetId").getAsLong();
		String targetField = paramObject.get("targetField").isJsonNull() ? null : paramObject.get("targetField")
				.getAsString();
		String sourceOutput = paramObject.get("srcOutput").getAsString();
		String targetInput = paramObject.get("targetInput").getAsString();

		TransformationNode source = Neo4JUtils.get(getNeoService(), sourceId, TransformationNode.class);
		TransformationNode target = Neo4JUtils.get(getNeoService(), targetId, TransformationNode.class);
		TransformationNodeOutput output = source.getOutput(sourceOutput);
		if (output == null) {
			return new JsonObject();
		}
		TransformationNodeInput input = target.getInput(targetInput);
		if (input == null) {
			return new JsonObject();
		}

		output.removeOutgoingEdge(input, sourceField, targetField);

		return new JsonObject();
	}

	public JsonElement previewWire(JsonElement params, Program program, RuntimeConfiguration config) {
		JsonObject paramObject = params.getAsJsonObject();
		long sourceId = paramObject.get("srcId").getAsLong();
		String sourceOutput = paramObject.get("srcOutput").getAsString();
		String sourceField = null;
		if (paramObject.has("srcField") && !paramObject.get("srcField").isJsonNull()) {
			sourceField = paramObject.get("srcField").getAsString();
		}
		long targetId = paramObject.get("targetId").getAsLong();
		String targetInput = paramObject.get("targetInput").getAsString();

		TransformationNode source = Neo4JUtils.get(getNeoService(), sourceId, TransformationNode.class);
		TransformationNode target = Neo4JUtils.get(getNeoService(), targetId, TransformationNode.class);

		TransformationEdge toPreview = null;
		for (TransformationEdge edge : source.getOutput(sourceOutput).getOutgoingEdges()) {
			if (edge.getInput().getInputField().getName().equals(targetInput)
					&& edge.getInput().getTransformationNode().equals(target)) {
				toPreview = edge;
			}
		}

		Iterator<ItemView> items;
		try {
			items = config.previewWire(toPreview, 20);
		} catch (TransformationException e) {
			e.printStackTrace();
			return new JsonObject();
		}

		JsonArray array = new JsonArray();
		while (items.hasNext()) {
			ItemView item = items.next();
			JsonObject testObject = new JsonObject();
			testObject.add("id", new JsonPrimitive(item.getId()));
			testObject.add("description", new JsonPrimitive(abbreviate(item)));
			array.add(testObject);
		}

		return array;
	}

	public JsonElement fillBucketContainer(JsonElement params, Program program, RuntimeConfiguration config) {
		JsonObject paramObject = params.getAsJsonObject();
		JsonElement containerIdObject = paramObject.get("containerId");
		if (containerIdObject == null) {
			return new JsonObject(); // LOG
		}
		long containerId = containerIdObject.getAsLong();
		WorkspaceBucketNode bucketNode = Neo4JUtils.get(getNeoService(), containerId, WorkspaceBucketNode.class);

		try {
			config.fill(bucketNode);
		} catch (TransformationException e) {
			e.printStackTrace();
			return new JsonObject();
		}

		if (IterableUtils.size(bucketNode.getLinkedBucket().getItems()) > 0) {
			return getType(bucketNode.getLinkedBucket().getType());
		}

		return new JsonNull();
	}

	public JsonElement previewBucketContainer(JsonElement params, Program program) {
		JsonObject paramObject = params.getAsJsonObject();
		JsonElement containerIdObject = paramObject.get("containerId");
		if (containerIdObject == null) {
			return new JsonObject(); // LOG
		}
		long containerId = containerIdObject.getAsLong();
		WorkspaceBucketNode bucketNode = Neo4JUtils.get(getNeoService(), containerId, WorkspaceBucketNode.class);

		WorkspaceBucket bucket = bucketNode.getLinkedBucket();

		JsonArray array = new JsonArray();
		Iterator<ItemView> bucketIt = bucket.getItems().iterator();
		int count = 0;
		while (bucketIt.hasNext() && count++ < 20) {
			ItemView item = bucketIt.next();
			JsonObject testObject = new JsonObject();
			testObject.add("id", new JsonPrimitive(item.getId()));
			testObject.add("description", new JsonPrimitive(abbreviate(item)));
			array.add(testObject);
		}
		if (count > 20 && bucketIt.hasNext()) {
			JsonObject size = new JsonObject();
			size.add("id", new JsonPrimitive(-1));
			size.add("description", new JsonPrimitive("+ " + (bucket.size() - 20) + " more"));
			array.add(size);
		}

		return array;
	}

	public JsonElement renameBucketContainer(JsonElement params, Program program) throws ServletException {
		JsonObject paramObject = params.getAsJsonObject();
		JsonElement containerIdObject = paramObject.get("containerId");
		JsonElement nameObject = paramObject.get("name");
		if (containerIdObject == null) {
			return new JsonObject(); // LOG
		}
		long containerId = containerIdObject.getAsLong();
		WorkspaceBucketNode bucketNode = Neo4JUtils.get(getNeoService(), containerId, WorkspaceBucketNode.class);

		WorkspaceBucket bucket = bucketNode.getLinkedBucket();
		if (!bucket.getName().equals(nameObject.getAsString())) {
			if (bucket.getWorkspace().getWorkspaceBucket(nameObject.getAsString()) != null) {
				throw new ServletException("Invalid bucket rename, name already exists");
			}
			bucket.setName(nameObject.getAsString());
		}

		return new JsonObject();
	}

	public JsonElement deleteBucket(JsonElement params, Program program) throws ServletException {
		JsonObject paramObject = params.getAsJsonObject();
		JsonElement nameObject = paramObject.get("name");
		if (nameObject == null) {
			return new JsonObject(); // LOG
		}
		WorkspaceBucket bucket = program.getWorkspace().getWorkspaceBucket(nameObject.getAsString());
		bucket.delete();

		return new JsonObject();
	}

	private JsonElement deleteItem(JsonElement params, Program mainProgram) {
		JsonObject paramObject = params.getAsJsonObject();
		JsonElement itemIdObject = paramObject.get("itemId");
		JsonElement containerIdObject = paramObject.get("containerId");

		if (itemIdObject == null) {
			return new JsonObject(); // LOG
		}

		ItemView item = Neo4JUtils.get(getNeoService(), itemIdObject.getAsLong(), ItemView.class);

		long containerId = containerIdObject.getAsLong();
		WorkspaceBucketNode bucketNode = Neo4JUtils.get(getNeoService(), containerId, WorkspaceBucketNode.class);

		bucketNode.getLinkedBucket().removeItem(item);
		return new JsonObject();
	}

	private JsonElement viewItemHistory(JsonElement params, Program mainProgram) {
		JsonObject paramObject = params.getAsJsonObject();
		JsonElement itemIdObject = paramObject.get("itemId");
		if (itemIdObject == null) {
			return new JsonObject(); // LOG
		}

		ItemView item = Neo4JUtils.get(getNeoService(), itemIdObject.getAsLong(), ItemView.class);

		// Get the description of the last transformation
		JsonArray transformations = new JsonArray();
		JsonArray edges = new JsonArray();

		JsonObject fakeTransformation = new JsonObject();
		fakeTransformation.add("name", new JsonPrimitive(abbreviate(item)));
		transformations.add(fakeTransformation);

		followHistory(0, "item", Arrays.asList(item.getViewScope()), transformations, edges,
				new HashMap<TransformationNode, Integer>());

		JsonObject object = new JsonObject();
		object.add("transformations", transformations);
		object.add("edges", edges);
		return object;
	}

	private void followHistory(int transformGroupIndex, String inputName, List<Item> items, JsonArray transformations,
			JsonArray wires, Map<TransformationNode, Integer> completed) {
		String label = null;
		int count = 0;

		Item first = null;
		Set<TransformationGroup> transforms = new LinkedHashSet<TransformationGroup>();
		long time = 0;
		for (Item item : items) {
			TransformationGroup group = item.getOutputGroup().getTransformationGroup();
			if (transforms.add(group) && group.getStartTime() != null) {
				time += (group.getEndTime() - group.getStartTime());
			}

			if (label == null) {
				label = StringEscapeUtils.escapeHtml(StringUtils.abbreviate(item.get().toString(), 40));
				first = item;
			}
			count++;
		}

		TransformationNode tGroup = first.getOutputGroup().getTransformationGroup().getRuntimeNode()
				.getTransformationNode();
		Integer id = completed.get(tGroup);
		if (id == null) {
			// Add a transformation for this
			JsonObject transformation = createTransformationNodeContainer(tGroup);
			transformation.add("time", new JsonPrimitive(time / transforms.size()));

			id = transformations.size();
			transformations.add(transformation);
			completed.put(tGroup, id);
		}

		Map<TransformationNodeInput, List<Item>> itemInputs = new HashMap<TransformationNodeInput, List<Item>>();
		for (TransformationGroup group : transforms) {
			for (InputGroup iGroup : group.getInputGroups()) {
				TransformationNodeInput previous = tGroup.getInput(iGroup.getInputQueue().getInput().getInputField()
						.getName());
				List<Item> previousNodes = itemInputs.get(previous);
				if (previousNodes == null) {
					previousNodes = new ArrayList<Item>();
					itemInputs.put(previous, previousNodes);
				}
				for (ItemView item : iGroup.getItems()) {
					if (item.getViewScope() != null) {
						previousNodes.add(item.getViewScope());
					} else {
						System.out.println("No scope");
					}
				}
			}
		}

		if (count > 1) {
			label = count + " " + first.getType().getName() + "s";
		}

		// Connect this transform
		JsonObject wire = new JsonObject();
		wire.add("sourceId", new JsonPrimitive(id));
		wire.add("sourceOutput", new JsonPrimitive(first.getOutputGroup().getBucket().getTransformationNodeOutput()
				.getOutputField().getName()));
		wire.add("targetId", new JsonPrimitive(transformGroupIndex));
		wire.add("targetInput", new JsonPrimitive(inputName));
		wire.add("label", new JsonPrimitive(label));

		wires.add(wire);

		// Now try to go backward
		// Loop through all the items in the input groups and create a list of all the output groups they came from
		for (TransformationNodeInput input : tGroup.getInputs()) {
			if (itemInputs.containsKey(input) && !itemInputs.get(input).isEmpty()) {
				followHistory(id, input.getInputField().getName(), itemInputs.get(input), transformations, wires,
						completed);
			}
		}
	}

	private JsonElement changeWorkspaceName(JsonElement params, Program mainProgram) {
		JsonObject paramObject = params.getAsJsonObject();
		JsonElement nameObject = paramObject.get("newName");
		if (nameObject == null) {
			return new JsonObject(); // LOG
		}
		mainProgram.getWorkspace().setName(nameObject.getAsString());
		return new JsonObject();
	}

	private JsonElement setInputValue(JsonElement params, Program mainProgram) throws ServletException {
		JsonObject paramObject = params.getAsJsonObject();
		JsonElement containerIdObject = paramObject.get("containerId");
		if (containerIdObject == null) {
			return new JsonObject(); // LOG
		}
		long containerId = containerIdObject.getAsLong();
		TransformationNode transformation = Neo4JUtils.get(getNeoService(), containerId, TransformationNode.class);
		TransformationNodeInput input = transformation.getInput(paramObject.get("input").getAsString());

		Object value;
		if (input.getInputField().getType().getName().equals("string")) {
			value = paramObject.get("value").getAsString();
		} else if (input.getInputField().getType().getName().equals("bool")) {
			value = paramObject.get("value").getAsBoolean();
		} else if (input.getInputField().getType().getName().equals("int32")) {
			value = Integer.parseInt(paramObject.get("value").getAsString());
		} else {
			throw new ServletException("Can't handle this type of data");
		}
		input.getType().setValue(input, value);

		return new JsonObject();
	}

	private String abbreviate(ItemView output) {
		return StringEscapeUtils.escapeHtml(StringUtils.abbreviate(output.get().toString(), 40));
	}

}
