package org.webseer.streams.model.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import name.levering.ryan.util.IterableUtils;
import name.levering.ryan.util.Pair;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webseer.model.meta.TransformationException;
import org.webseer.model.meta.Type;
import org.webseer.streams.model.PreviewBuffer;
import org.webseer.streams.model.Workspace;
import org.webseer.streams.model.WorkspaceBucket;
import org.webseer.streams.model.program.DisconnectedWorkspaceBucketNode;
import org.webseer.streams.model.program.TransformationEdge;
import org.webseer.streams.model.program.TransformationGraph;
import org.webseer.streams.model.program.TransformationNode;
import org.webseer.streams.model.program.TransformationNodeInput;
import org.webseer.streams.model.program.WorkspaceBucketNode;
import org.webseer.streams.model.trace.Bucket;
import org.webseer.streams.model.trace.InputGroup;
import org.webseer.streams.model.trace.Item;
import org.webseer.streams.model.trace.ItemView;
import org.webseer.streams.model.trace.OutputGroup;
import org.webseer.streams.model.trace.Reference;
import org.webseer.streams.model.trace.TransformationGroup;
import org.webseer.transformation.InputReader;
import org.webseer.transformation.InputReaders;
import org.webseer.transformation.PullRuntimeTransformation;
import org.webseer.transformation.RuntimeTransformationException;

public class RuntimeConfigurationImpl implements RuntimeConfiguration {

	private static final Logger log = LoggerFactory.getLogger(RuntimeConfigurationImpl.class);

	private final GraphDatabaseService service;
	private final Workspace workspace;
	private Map<TransformationNode, RuntimeTransformationNode> runtimeStack = new HashMap<TransformationNode, RuntimeTransformationNode>();
	private Map<RuntimeTransformationNode, TransformationGroup> currentGroup = new HashMap<RuntimeTransformationNode, TransformationGroup>();
	private final String sessionId;

	public RuntimeConfigurationImpl(GraphDatabaseService service, Workspace workspace, String username) {
		this.service = service;
		this.workspace = workspace;
		this.sessionId = username;
	}

	public GraphDatabaseService getService() {
		return service;
	}

	public RuntimeTransformationNode getCurrentNode(TransformationNode node) {
		return runtimeStack.get(node);
	}

	public void initRunning(RuntimeTransformationNode node) {
		TransformationGroup group = new TransformationGroup(service, node);
		group.setStartTime(System.currentTimeMillis());
		currentGroup.put(node, group);
	}

	public void startRunning(RuntimeTransformationNode node) {
		currentGroup.get(node).setStartTime(System.currentTimeMillis());
	}

	public void endRunning(RuntimeTransformationNode node) {
		TransformationGroup group = currentGroup.get(node);
		group.setEndTime(System.currentTimeMillis());
	}

	public OutputGroup getCurrentOutputGroup(RuntimeTransformationNode runtimeTransformationNode, Bucket bucket) {
		TransformationGroup group = currentGroup.get(runtimeTransformationNode);
		return group.getOutputGroup(service, bucket);
	}

	public InputGroup getCurrentInputGroup(RuntimeTransformationNode runtimeTransformationNode,
			TransformationNodeInput input) {
		TransformationGroup group = currentGroup.get(runtimeTransformationNode);
		return group.getInputGroup(service, input);
	}

	public void fill(WorkspaceBucketNode node) throws TransformationException {
		if (node.getInputs().iterator().next().getIncomingEdge() == null) {
			log.info("Can't fill something with no connections");
			// Nothing to pull from
			return;
		}

		log.info("Filling bucket");

		DisconnectedWorkspaceBucketNode cloned = new DisconnectedWorkspaceBucketNode(new WorkspaceBucketNode(service,
				null, node.getLinkedBucket()));

		TransformationNodeInput input = node.getInputs().iterator().next();
		TransformationEdge source = input.getIncomingEdge();

		// Copy the graph
		TransformationEdge newEdge = TransformationGraph.createRuntimeGraph(service, source, cloned, cloned.getInputs()
				.iterator().next().getInputField().getName(), new HashMap<Node, Node>());

		InputReaderImpl queue = getInputReader(newEdge, cloned);
		Iterator<ItemView> stream = queue.getViews();

		// Start a transformation group for this fill
		WorkspaceBucket bucket = node.getLinkedBucket();
		while (stream.hasNext()) {
			ItemView item = stream.next();
			item.getInputGroup().advance();
			bucket.addBucketItem(service, item);
		}

		// In case nothing happened, check the bucket for deletion

	}

	public BucketReader getBucketReader(TransformationNode node, String outputPoint) throws TransformationException {
		return getBucketReaders(node, new String[] { outputPoint })[0];
	}

	public BucketReader[] getBucketReaders(TransformationNode node, String[] outputPoints)
			throws TransformationException {
		Map<TransformationNode, Pair<RuntimeTransformationNode, PullRuntimeTransformation>> runtimeStack = new HashMap<TransformationNode, Pair<RuntimeTransformationNode, PullRuntimeTransformation>>();
		BucketReader[] outputQueues = new BucketReader[outputPoints.length];
		Pair<RuntimeTransformationNode, PullRuntimeTransformation> runtime = createRuntimeNode(node, runtimeStack);

		for (int i = 0; i < outputPoints.length; i++) {

			// Add the bucket to read from
			Bucket bucket = new Bucket(service, runtime.getFirst(), node.getOutput(outputPoints[i]));
			addBucketWriter(runtime.getSecond(), bucket);

			if (node.asDisconnectedWorkspaceBucketNode() == null) {
				outputQueues[i] = new TransformationReader(runtime.getSecond(), runtime.getFirst()
						.getBucket(outputPoints[i]).getItems().iterator());
			} else {
				outputQueues[i] = new WorkspaceBucketReader(outputPoints[i], runtime.getFirst(),
						node.asDisconnectedWorkspaceBucketNode());
			}
		}
		return outputQueues;
	}

	public InputReaderImpl getInputReader(TransformationEdge edge, DisconnectedWorkspaceBucketNode targetNode)
			throws TransformationException {
		Map<TransformationNode, Pair<RuntimeTransformationNode, PullRuntimeTransformation>> runtimeStack = new HashMap<TransformationNode, Pair<RuntimeTransformationNode, PullRuntimeTransformation>>();
		Pair<RuntimeTransformationNode, PullRuntimeTransformation> runtime = createRuntimeNode(edge.getOutput()
				.getTransformationNode(), runtimeStack);

		String inputFieldFilter = edge.getInputField();
		Type targetType;
		if (inputFieldFilter == null) {
			targetType = edge.getInput().getType();
		} else {
			targetType = edge.getInput().getType().getFieldType(inputFieldFilter);
		}

		BucketReader reader;
		if (edge.getOutput().getTransformationNode().asDisconnectedWorkspaceBucketNode() == null) {

			reader = new TransformationReader(runtime.getSecond(), runtime.getFirst()
					.getBucket(edge.getOutput().getOutputField().getName()).getItems().iterator());
		} else {
			reader = new WorkspaceBucketReader(edge.getOutput().getOutputField().getName(), runtime.getFirst(), edge
					.getOutput().getTransformationNode().asDisconnectedWorkspaceBucketNode());
		}

		final RuntimeTransformationNode workspaceBucketRuntime = new RuntimeTransformationNode(service, targetNode);
		initRunning(workspaceBucketRuntime); // Create an input/transformation group

		return new InputReaderImpl(edge, reader, targetType, workspaceBucketRuntime);
	}

	public InputReader getInputReader(TransformationEdge edge, RuntimeTransformationNode targetNode)
			throws TransformationException {
		Map<TransformationNode, Pair<RuntimeTransformationNode, PullRuntimeTransformation>> runtimeStack = new HashMap<TransformationNode, Pair<RuntimeTransformationNode, PullRuntimeTransformation>>();
		Pair<RuntimeTransformationNode, PullRuntimeTransformation> runtime = createRuntimeNode(edge.getOutput()
				.getTransformationNode(), runtimeStack);

		String inputFieldFilter = edge.getInputField();
		Type targetType;
		if (inputFieldFilter == null) {
			targetType = edge.getInput().getType();
		} else {
			targetType = edge.getInput().getType().getFieldType(inputFieldFilter);
		}

		BucketReader reader;
		if (edge.getOutput().getTransformationNode().asDisconnectedWorkspaceBucketNode() == null) {
			reader = new TransformationReader(runtime.getSecond(), runtime.getFirst()
					.getBucket(edge.getOutput().getOutputField().getName()).getItems().iterator());
		} else {
			reader = new WorkspaceBucketReader(edge.getOutput().getOutputField().getName(), runtime.getFirst(), edge
					.getOutput().getTransformationNode().asDisconnectedWorkspaceBucketNode());
		}

		return new InputReaderImpl(edge, reader, targetType, targetNode);
	}

	private void addBucketWriter(PullRuntimeTransformation pull, final Bucket bucket) {
		ItemOutputStream<?> outputStream = new ItemOutputStream<Object>(new OutputGroupGetter() {

			@Override
			public OutputGroup getOutputGroup() {
				return getCurrentOutputGroup(bucket.getRuntimeNode(), bucket);
			}

			@Override
			public GraphDatabaseService getNeoService() {
				return service;
			}

		});
		pull.addOutputChannel(bucket.getTransformationNodeOutput().getOutputField().getName(), outputStream);
	}

	private Pair<RuntimeTransformationNode, PullRuntimeTransformation> createRuntimeNode(TransformationNode node,
			Map<TransformationNode, Pair<RuntimeTransformationNode, PullRuntimeTransformation>> completed)
			throws TransformationException {
		if (completed.containsKey(node)) {
			return completed.get(node);
		}

		// Generate a runtime node to track this
		final RuntimeTransformationNode runtimeNode = new RuntimeTransformationNode(service, node);

		if (node.asDisconnectedWorkspaceBucketNode() != null) {
			Pair<RuntimeTransformationNode, PullRuntimeTransformation> result = new Pair<RuntimeTransformationNode, PullRuntimeTransformation>(
					runtimeNode, null);
			completed.put(node, result);
			return result;
		}

		// Get the actual executor
		final PullRuntimeTransformation pull = runtimeNode.getPullTransformation(this);

		for (final Bucket bucket : runtimeNode.getBuckets()) {
			addBucketWriter(pull, bucket);
		}

		// Add input channels that pull from input queue
		// They pull on demand from the previous transformations
		for (final TransformationNodeInput input : node.getInputs()) {
			Iterable<TransformationEdge> sources = input.getIncomingEdges();
			if (!sources.iterator().hasNext()) {
				// Nothing to pull from, attempt to pull from config values
				if (input.hasValue() || input.getMeta() != null) {
					pull.addInputChannel(input.getInputField().getName(), new TransformationNodeInputReader(input,
							runtimeNode));
				} else {
					pull.addInputChannel(input.getInputField().getName(), InputReaders.getEmptyReader());
				}
			} else {
				final InputReader reader = getInputReader(input.getIncomingEdge(), runtimeNode);

				pull.addInputChannel(input.getInputField().getName(), reader);
			}
		}
		Pair<RuntimeTransformationNode, PullRuntimeTransformation> result = new Pair<RuntimeTransformationNode, PullRuntimeTransformation>(
				runtimeNode, pull);
		completed.put(node, result);
		return result;
	}

	private Iterator<Item> getWeir(Item actualItem, TransformationNodeInput backReference) {
		return findSource(backReference, Collections.singletonList(actualItem));
	}

	private Iterator<Item> findSource(TransformationNodeInput reference, List<Item> items) {
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
			if (nodeInput.equals(reference) && IterableUtils.size(nodeInput.getIncomingEdges()) == 0) {
				if (nodeInput.hasValue() || nodeInput.getMeta() != null) {
					return Collections.singletonList((Item) nodeInput).iterator();
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

		if (itemInputs.containsKey(reference)) {
			return itemInputs.get(reference).iterator();
		}

		// Now try to go backward
		// Loop through all the items in the input groups and create a list of all the output groups they came from
		for (TransformationNodeInput input : tGroup.getInputs()) {
			if (itemInputs.containsKey(input) && !itemInputs.get(input).isEmpty()) {
				Iterator<Item> found = findSource(reference, itemInputs.get(input));
				if (found != null) {
					return found;
				}
			}
		}

		return null;
	}

	private final class BucketIterator extends OnDemandBucketFiller {
		final Iterator<ItemView> workspaceBucketItems;
		private Bucket bucket;

		Map<InputGroup, InputGroup> mapped = new HashMap<InputGroup, InputGroup>();
		Map<InputGroup, OutputGroup> mappedOutputs = new HashMap<InputGroup, OutputGroup>();
		private InputQueue queue;

		private BucketIterator(Iterator<Item> bucketIterator, Bucket bucket, DisconnectedWorkspaceBucketNode bucketNode) {
			super(bucketIterator);
			this.workspaceBucketItems = getBucket(bucketNode).getItems().iterator();
			this.bucket = bucket;

			// This is a fake queue that is actually a copy
			this.queue = new InputQueue(service, bucketNode.getInputs().iterator().next(), bucket.getRuntimeNode());
		}

		@Override
		protected boolean advance() {
			if (!workspaceBucketItems.hasNext()) {
				return false;
			}
			ItemView workspaceBucketItem = workspaceBucketItems.next();
			// Copy it
			InputGroup mappedGroup = mapped.get(workspaceBucketItem.getInputGroup());
			if (mappedGroup == null) {
				TransformationGroup tGroup = new TransformationGroup(service, bucket.getRuntimeNode());
				mappedGroup = new InputGroup(service, queue, tGroup);
				OutputGroup group = new OutputGroup(service, bucket, tGroup);
				mappedOutputs.put(workspaceBucketItem.getInputGroup(), group);
				mapped.put(workspaceBucketItem.getInputGroup(), mappedGroup);
			}
			OutputGroup mappedOutput = mappedOutputs.get(workspaceBucketItem.getInputGroup());
			ItemView copy = new ItemView(service, workspaceBucketItem.getViewScope(),
					workspaceBucketItem.getViewData(), workspaceBucketItem.getDataField(), mappedGroup);
			queue.addItem(copy);
			mappedGroup.advance();
			Reference reference = new Reference(service, mappedOutput, copy);
			bucket.addItem(reference);

			return true;
		}
	}

	public static interface OutputGroupGetter {

		public OutputGroup getOutputGroup();

		public GraphDatabaseService getNeoService();

	}

	public static interface InputGroupGetter {

		public InputGroup getInputGroup();

		public GraphDatabaseService getNeoService();

	}

	/**
	 * Input reader that reads from configuration/hard-coded values on a transformation node.
	 */
	private class TransformationNodeInputReader implements InputReader {

		private TransformationNodeInput input;
		private RuntimeTransformationNode runtime;
		private InputQueue queue;

		TransformationNodeInputReader(TransformationNodeInput input, RuntimeTransformationNode runtime) {
			this.input = input;
			this.runtime = runtime;

			this.queue = new InputQueue(service, input, runtime);
		}

		@Override
		public ItemInputStream iterator() {

			return new ItemInputStream(getViews(), null, new InputGroupGetter() {

				@Override
				public InputGroup getInputGroup() {
					return null;
				}

				@Override
				public GraphDatabaseService getNeoService() {
					return getService();
				}

			});
		}

		public Iterator<ItemView> getViews() {
			return new Iterator<ItemView>() {

				boolean pulled = false;

				@Override
				public boolean hasNext() {
					return !pulled;
				}

				@Override
				public ItemView next() {
					if (pulled) {
						throw new IllegalStateException();
					}
					pulled = true;
					InputGroup iGroup = getCurrentInputGroup(runtime, input);
					ItemView view = new ItemView(getService(), null, input, null, iGroup);
					queue.addItem(view);
					iGroup.advance();
					return view;
				}

				@Override
				public void remove() {
					// TODO Auto-generated method stub

				}

			};
		}

	}

	/**
	 * Input reader to read from the input queue in a normal runtime transformation path.
	 */
	private class InputReaderImpl implements InputReader {

		private final Type targetType;
		private final RuntimeTransformationNode targetNode;

		private BucketReader reader;
		private InputQueue queue;
		private TransformationEdge edge;

		public InputReaderImpl(TransformationEdge edge, BucketReader reader, Type targetType,
				RuntimeTransformationNode targetNode) {
			this.reader = reader;
			this.targetType = targetType;
			this.targetNode = targetNode;
			this.edge = edge;

			this.queue = new InputQueue(service, edge.getInput(), targetNode);
		}

		@Override
		public ItemInputStream iterator() {
			return new ItemInputStream(getViews(), targetType, new InputGroupGetter() {

				@Override
				public InputGroup getInputGroup() {
					return getCurrentInputGroup(targetNode, queue.getInput());
				}

				@Override
				public GraphDatabaseService getNeoService() {
					return getService();
				}

			});
		}

		public Iterator<ItemView> getViews() {
			return new OnDemandIterator(queue, reader.getItems(), edge.getLinkedPoint(), edge.getOutputField(),
					targetNode, edge.getInput());
		}
	}

	private class WorkspaceBucketReader implements BucketReader {
		private String outputName;
		private Iterator<Item> bucketIterator;
		private DisconnectedWorkspaceBucketNode bucketNode;
		private RuntimeTransformationNode runtimeNode;

		WorkspaceBucketReader(String outputName, final RuntimeTransformationNode runtimeNode,
				DisconnectedWorkspaceBucketNode bucketNode) {
			this.outputName = outputName;
			this.bucketIterator = runtimeNode.getBucket(outputName).getItems().iterator();
			this.bucketNode = bucketNode;
			this.runtimeNode = runtimeNode;
		}

		@Override
		public Iterator<Item> getItems() {
			return new BucketIterator(bucketIterator, runtimeNode.getBucket(outputName), bucketNode);
		}
	}

	private class TransformationReader implements BucketReader {

		private final PullRuntimeTransformation function;
		private final Iterator<Item> bucketIterator;

		public TransformationReader(PullRuntimeTransformation function, Iterator<Item> bucketIterator) {
			this.function = function;
			this.bucketIterator = bucketIterator;
		}

		@Override
		public Iterator<Item> getItems() {
			return new TransformationIterator(bucketIterator, function);
		}
	}

	private class OnDemandIterator implements Iterator<ItemView> {
		/**
		 * The input queue that persists the progress.
		 */
		final InputQueue queue;

		Iterator<Item> bucketItems;

		Item currentScope = null;

		Iterator<Item> linkedOutputs = null;

		ItemView currentView = null;

		final String outputField;

		TransformationNodeInput weir;

		private RuntimeTransformationNode targetNode;

		private TransformationNodeInput targetInput;

		private OnDemandIterator(InputQueue queue, Iterator<Item> bucketIterator, TransformationNodeInput weir,
				String outputField, RuntimeTransformationNode targetNode, TransformationNodeInput targetInput) {
			this.bucketItems = bucketIterator;
			this.outputField = outputField;
			this.weir = weir;
			this.queue = queue;
			this.targetNode = targetNode;
			this.targetInput = targetInput;
		}

		public boolean hasNext() {
			if (linkedOutputs != null && linkedOutputs.hasNext()) {
				return true;
			}
			return bucketItems.hasNext();
		}

		/**
		 * This is a bit complicated. We want to first pull from already existing elements of the queue. If the queue is
		 * empty, pull from any existing weir references we have. If none, then try to advance the bucket we're pulling
		 * from.
		 */
		public ItemView next() {
			ItemView view;
			if (currentView != null && queue != null && InputQueue.getNext(currentView) != null) {
				currentView = InputQueue.getNext(currentView);
				return currentView;
			} else {
				if (linkedOutputs != null && linkedOutputs.hasNext()) {
					view = new ItemView(service, currentScope, linkedOutputs.next(), outputField, getCurrentInputGroup(
							targetNode, targetInput));
				} else {
					// Advance the input group pointer
					currentScope = bucketItems.next();

					if (weir != null) {
						linkedOutputs = getWeir(currentScope, weir);
						assert linkedOutputs != null;
						view = new ItemView(service, currentScope, linkedOutputs.next(), outputField,
								getCurrentInputGroup(targetNode, targetInput));
					} else {
						// No weir
						view = new ItemView(service, currentScope, currentScope, outputField, getCurrentInputGroup(
								targetNode, targetInput));
					}
				}
				queue.addItem(view);
				currentView = view;
			}
			return view;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private abstract class OnDemandBucketFiller implements Iterator<Item> {

		private final Iterator<Item> bucketIterator;

		public OnDemandBucketFiller(Iterator<Item> bucketIterator) {
			this.bucketIterator = bucketIterator;
		}

		@Override
		public boolean hasNext() {
			while (!bucketIterator.hasNext()) {
				if (!advance()) {
					return false;
				}
			}
			return true;
		}

		@Override
		public Item next() {
			while (!bucketIterator.hasNext()) {
				if (!advance()) {
					throw new IllegalStateException();
				}
			}
			return bucketIterator.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		protected abstract boolean advance();

	}

	private class TransformationIterator extends OnDemandBucketFiller {

		private final PullRuntimeTransformation pull;

		private TransformationIterator(Iterator<Item> bucketIterator, PullRuntimeTransformation pull) {
			super(bucketIterator);
			this.pull = pull;
		}

		@Override
		protected boolean advance() {
			try {
				return pull.transform();
			} catch (TransformationException e) {
				e.printStackTrace();
			} catch (RuntimeTransformationException e) {
				e.printStackTrace();
				// This means there could be more valid ones
				return true;
			}
			return false;
		}
	}

	@Override
	public Iterator<ItemView> previewWire(TransformationEdge toClone, int previewSize) throws TransformationException {

		WorkspaceBucket previewBucket = PreviewBuffer.getPreviewBuffer(service).getPreviewBucket(service, sessionId);
		previewBucket.removeAll();

		DisconnectedWorkspaceBucketNode cloned = new DisconnectedWorkspaceBucketNode(new WorkspaceBucketNode(service,
				null, previewBucket));

		Map<Node, Node> converted = new HashMap<Node, Node>();
		TransformationGraph.createRuntimeGraph(service, toClone, cloned, cloned.getInputs().iterator().next()
				.getInputField().getName(), converted);

		TransformationNodeInput input = cloned.getInputs().iterator().next();
		TransformationEdge source = input.getIncomingEdge();

		InputReaderImpl queue = getInputReader(source, cloned);
		Iterator<ItemView> stream = queue.getViews();

		// Start a transformation group for this fill
		int count = 0;
		while (count++ < previewSize && stream.hasNext()) {
			ItemView item = stream.next();
			item.getInputGroup().advance();
			previewBucket.addBucketItem(service, item);
		}

		return previewBucket.getItems().iterator();
	}

	public WorkspaceBucket getBucket(DisconnectedWorkspaceBucketNode bucketNode) {
		if (bucketNode.getLinkedBucketName().equals("PREVIEW")) {
			return PreviewBuffer.getPreviewBuffer(service).getPreviewBucket(service, sessionId);
		} else {
			return workspace.getWorkspaceBucket(bucketNode.getLinkedBucketName());
		}
	}
}