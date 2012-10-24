package org.webseer.streams.model.runtime;

import java.util.Iterator;

import org.neo4j.graphdb.GraphDatabaseService;
import org.webseer.model.meta.TransformationException;
import org.webseer.streams.model.program.TransformationEdge;
import org.webseer.streams.model.program.TransformationNode;
import org.webseer.streams.model.program.WorkspaceBucketNode;
import org.webseer.streams.model.trace.Bucket;
import org.webseer.streams.model.trace.ItemView;
import org.webseer.streams.model.trace.OutputGroup;
import org.webseer.transformation.BucketReader;
import org.webseer.transformation.InputReader;

public interface RuntimeConfiguration {

	public GraphDatabaseService getService();

	public RuntimeTransformationNode getCurrentNode(TransformationNode node);

	public void startRunning(RuntimeTransformationNode node);

	public OutputGroup getCurrentOutputGroup(RuntimeTransformationNode runtimeTransformationNode, Bucket bucket);

	public void fill(WorkspaceBucketNode node) throws TransformationException;

	/**
	 * Gets a reader that pulls from a bucket dynamically.
	 */
	public BucketReader getBucketReader(TransformationNode node, String output) throws TransformationException;

	public BucketReader[] getBucketReaders(TransformationNode node, String[] outputs) throws TransformationException;

	/**
	 * Gets a reader that pulls from a bucket, applies back-projection and casts to the target of the edge. This will
	 * use input queues to track
	 */
	public InputReader getInputReader(TransformationEdge edge, RuntimeTransformationNode targetNode)
			throws TransformationException;

	public void endRunning(RuntimeTransformationNode node);

	public void initRunning(RuntimeTransformationNode node);

	Iterator<ItemView> previewWire(TransformationEdge toClone, int previewSize) throws TransformationException;

}
