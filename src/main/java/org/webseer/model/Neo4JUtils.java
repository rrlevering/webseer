package org.webseer.model;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import name.levering.ryan.util.IterableUtils;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.webseer.streams.model.WorkspaceBucket;
import org.webseer.streams.model.program.TransformationNodeInput;
import org.webseer.streams.model.trace.DataItem;
import org.webseer.streams.model.trace.Item;
import org.webseer.streams.model.trace.Reference;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

/**
 * This class breaks all the encapsulation of the data backing. This is necessary to be able to use package protection
 * on the data classes while still allowing inter-package sharing of underlying data. This should hold all the nastiness
 * that deals with the backend implementation of Neo4J.
 * 
 * @author ryan
 * 
 */
public class Neo4JUtils {

	public final static Node createNode(GraphDatabaseService service) {
		return service.createNode();
	}

	public final static Node createNode(GraphDatabaseService service, Class<?> type) {
		Node node = service.createNode();
		node.setProperty("CLASS", type.getName());
		return node;
	}

	public final static String toString(Node node) {
		if (node.hasProperty("CLASS")) {
			return getString(node, "CLASS") + "[" + node.getId() + "]";
		} else {
			return node.toString();
		}
	}

	public final static Node getNode(WorkspaceBucket bucket) {
		return bucket.getUnderlyingNode();
	}

	public final static String[] getStringArray(Node underlyingNode, String propName) {
		String propString = getString(underlyingNode, propName);
		if (propString == null) {
			return null;
		}
		return Iterables.toArray(Splitter.on(',').split(propString), String.class);
	}

	public final static void setStringArray(Node underlyingNode, String propName, String[] values) {
		underlyingNode.setProperty(propName, Joiner.on(',').join(values));
	}

	public final static byte[] getByteArray(Node underlyingNode, String propName) {
		return (byte[]) underlyingNode.getProperty(propName, null);
	}

	public final static String getString(Node underlyingNode, String propName) {
		return (String) underlyingNode.getProperty(propName, null);
	}

	public static Integer getInteger(Node underlyingNode, String propName) {
		return (Integer) underlyingNode.getProperty(propName, null);
	}

	public static interface NodeReader<T> {

		T convertNode(Node node);
	}

	public static final <T> Iterable<T> getIterable(final Node underlyingNode, final NeoRelationshipType edge,
			final NodeReader<T> converter) {
		return new Iterable<T>() {

			public Iterator<T> iterator() {
				return new Iterator<T>() {

					private Iterator<Relationship> underlyingIterator = underlyingNode.getRelationships(edge)
							.iterator();

					public boolean hasNext() {
						return underlyingIterator.hasNext();
					}

					public T next() {
						return converter.convertNode(underlyingIterator.next().getOtherNode(underlyingNode));
					}

					public void remove() {
						underlyingIterator.remove();
					}

				};
			}

		};
	}

	public static final <T> Iterable<T> getIterable(final Node underlyingNode, final NeoRelationshipType edge,
			final Class<T> clazz) {
		return getIterable(underlyingNode, edge, Direction.BOTH, clazz);
	}

	public static final <T> Iterable<T> getIterable(final Node underlyingNode, final NeoRelationshipType edge,
			final Direction dir, final Class<T> clazz) {
		return new Iterable<T>() {

			public Iterator<T> iterator() {
				return new Iterator<T>() {

					private Iterator<Relationship> underlyingIterator = underlyingNode.getRelationships(edge)
							.iterator();

					public boolean hasNext() {
						return underlyingIterator.hasNext();
					}

					public T next() {
						try {
							return clazz.getConstructor(Node.class).newInstance(
									underlyingIterator.next().getOtherNode(underlyingNode));
						} catch (IllegalArgumentException e) {
							throw new RuntimeException("Internal constructors not created for node wrapping", e);
						} catch (SecurityException e) {
							throw new RuntimeException("Internal constructors not created for node wrapping", e);
						} catch (InstantiationException e) {
							throw new RuntimeException("Internal constructors not created for node wrapping", e);
						} catch (IllegalAccessException e) {
							throw new RuntimeException("Internal constructors not created for node wrapping", e);
						} catch (InvocationTargetException e) {
							throw new RuntimeException("Internal constructors not created for node wrapping", e);
						} catch (NoSuchMethodException e) {
							throw new RuntimeException("Internal constructors not created for node wrapping", e);
						}
					}

					public void remove() {
						underlyingIterator.remove();
					}

				};
			}

		};
	}

	public static void createLink(Object source, Object target, NeoRelationshipType edge) {
		try {
			Node sourceNode = (Node) source.getClass().getField("underlyingNode").get(source);
			Node targetNode = (Node) target.getClass().getField("underlyingNode").get(target);

			sourceNode.createRelationshipTo(targetNode, edge);

		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Does not have underlying node");
		} catch (SecurityException e) {
			throw new RuntimeException("Does not have underlying node");
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Does not have underlying node");
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Does not have underlying node");
		}
	}

	public static Node getLinkedNode(Node underlyingNode, NeoRelationshipType edge) {
		Relationship rel = underlyingNode.getSingleRelationship(edge, Direction.BOTH);
		if (rel == null) {
			return null;
		}
		return rel.getOtherNode(underlyingNode);
	}

	public static <T> T getWrapped(Node underlyingNode, Class<T> clazz) {
		try {
			return clazz.getConstructor(Node.class).newInstance(underlyingNode);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Internal constructors not created for node wrapping", e);
		} catch (SecurityException e) {
			throw new RuntimeException("Internal constructors not created for node wrapping", e);
		} catch (InstantiationException e) {
			throw new RuntimeException("Internal constructors not created for node wrapping", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Internal constructors not created for node wrapping", e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException("Internal constructors not created for node wrapping", e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Internal constructors not created for node wrapping", e);
		}
	}

	public static <T> T getLinked(Node underlyingNode, NeoRelationshipType edge, Class<T> clazz) {
		Relationship rel = underlyingNode.getSingleRelationship(edge, Direction.BOTH);
		if (rel == null) {
			return null;
		}
		return getWrapped(rel.getOtherNode(underlyingNode), clazz);
	}

	public static <T> T getSingleton(GraphDatabaseService service, NeoRelationshipType edge, Class<T> clazz) {
		Node ref = service.getReferenceNode();
		Relationship rel = ref.getSingleRelationship(edge, Direction.OUTGOING);
		if (rel == null) {
			Node newSingleton = Neo4JUtils.createNode(service);
			ref.createRelationshipTo(newSingleton, edge);
		}
		return getLinked(ref, edge, clazz);
	}

	public static <T> T getOutgoing(Node underlyingNode, NeoRelationshipType edge, Class<T> clazz) {
		Relationship rel = underlyingNode.getSingleRelationship(edge, Direction.OUTGOING);
		if (rel == null) {
			return null;
		}
		return getWrapped(rel.getOtherNode(underlyingNode), clazz);
	}

	public static Node getIncomingNode(Node underlyingNode, NeoRelationshipType edge) {
		Relationship rel = underlyingNode.getSingleRelationship(edge, Direction.INCOMING);
		if (rel == null) {
			return null;
		}
		return rel.getOtherNode(underlyingNode);
	}

	public static Node getOutgoingNode(Node underlyingNode, NeoRelationshipType edge) {
		Relationship rel = underlyingNode.getSingleRelationship(edge, Direction.OUTGOING);
		if (rel == null) {
			return null;
		}
		return rel.getOtherNode(underlyingNode);
	}

	public static Item wrapItem(Node linkedNode) {
		if (linkedNode.hasRelationship(NeoRelationshipType.REFERENCE_ITEM, Direction.OUTGOING)) {
			return new Reference(linkedNode);
		} else if (linkedNode.hasRelationship(NeoRelationshipType.NODE_NODEINPUT)) {
			return new TransformationNodeInput(linkedNode);
		}
		return DataItem.wrap(linkedNode);
	}

	public static Long getLong(Node underlyingNode, String propName) {
		return (Long) underlyingNode.getProperty(propName, null);
	}

	public static <T> T get(GraphDatabaseService service, long id, Class<T> clazz) {
		try {
			return getWrapped(service.getNodeById(id), clazz);
		} catch (Exception e) {
			return null;
		}
	}

	public static void addToList(GraphDatabaseService service, Node underlyingNode, Node nodeToAdd,
			NeoRelationshipType first, NeoRelationshipType last, NeoRelationshipType item) {
		Node listNode = Neo4JUtils.createNode(service);
		listNode.createRelationshipTo(nodeToAdd, item);

		Node lastNode = getLastItem(underlyingNode, last);
		if (lastNode == null) {
			underlyingNode.createRelationshipTo(listNode, first);
		} else {
			lastNode.createRelationshipTo(listNode, NeoRelationshipType.LIST_NEXT);
			underlyingNode.getSingleRelationship(last, Direction.OUTGOING).delete();
		}
		underlyingNode.createRelationshipTo(listNode, last);
	}

	public static void removeFromInlineList(Node start, Node stop, NeoRelationshipType first, NeoRelationshipType last,
			NeoRelationshipType next) {
		// Remove it from the bucket
		Node lastNode = Neo4JUtils.getIncomingNode(start, next);
		Node nextNode = Neo4JUtils.getOutgoingNode(stop, next);
		// Remove everything in between
		Node curr = start;
		while (!curr.equals(stop)) {
			Relationship nextRel = curr.getSingleRelationship(next, Direction.OUTGOING);
			curr = nextRel.getEndNode();
			nextRel.delete();
		}
		if (lastNode != null) {
			start.getSingleRelationship(next, Direction.INCOMING).delete();
			if (nextNode != null) {
				stop.getSingleRelationship(next, Direction.OUTGOING).delete();
				lastNode.createRelationshipTo(nextNode, next);
			} else {
				Relationship lastRel = stop.getSingleRelationship(last, Direction.INCOMING);
				Node list = lastRel.getStartNode();
				lastRel.delete();
				list.createRelationshipTo(lastNode, last);
			}
		} else {
			if (nextNode != null) {
				stop.getSingleRelationship(next, Direction.OUTGOING).delete();
				Relationship firstRel = start.getSingleRelationship(first, Direction.INCOMING);
				Node list = firstRel.getStartNode();
				firstRel.delete();
				list.createRelationshipTo(nextNode, first);
			} else {
				start.getSingleRelationship(first, Direction.INCOMING).delete();
				stop.getSingleRelationship(last, Direction.INCOMING).delete();
			}
		}
	}

	public static void removeFromInlineList(Node underlyingNode, NeoRelationshipType first, NeoRelationshipType last,
			NeoRelationshipType next) {
		removeFromInlineList(underlyingNode, underlyingNode, first, last, next);
	}

	public static boolean removeFromList(Node underlyingNode, Node nodeToRemove, NeoRelationshipType first,
			NeoRelationshipType last, NeoRelationshipType item) {
		Node lastNode = null;
		Node currentNode = getFirstItem(underlyingNode, first);
		while (currentNode != null) {
			if (Neo4JUtils.getLinkedNode(currentNode, item).equals(nodeToRemove)) {
				// First delete the pointer to the item
				currentNode.getSingleRelationship(item, Direction.OUTGOING).delete();
				Node nextNode = getNext(currentNode);

				if (lastNode == null) {
					// It's the first item in the list
					currentNode.getSingleRelationship(first, Direction.INCOMING).delete();
					if (nextNode != null) {
						currentNode.getSingleRelationship(NeoRelationshipType.LIST_NEXT, Direction.OUTGOING).delete();
						underlyingNode.createRelationshipTo(nextNode, first); // Repoint to the new first
					} else {
						// We were the last node as well, so delete
						currentNode.getSingleRelationship(last, Direction.INCOMING).delete();
					}
				} else {
					// We need to point last to next
					lastNode.getSingleRelationship(NeoRelationshipType.LIST_NEXT, Direction.OUTGOING).delete();
					if (nextNode != null) {
						lastNode.createRelationshipTo(nextNode, NeoRelationshipType.LIST_NEXT);
					} else {
						// We were the last node, so refresh the pointer
						currentNode.getSingleRelationship(last, Direction.INCOMING).delete();
						underlyingNode.createRelationshipTo(lastNode, last);
					}
				}
				// Finally delete the list node
				currentNode.delete();
				return true;
			}
			lastNode = currentNode;
			currentNode = getNext(currentNode);
		}
		return false;
	}

	public static void clearList(Node underlyingNode, NeoRelationshipType first, NeoRelationshipType last,
			NeoRelationshipType item, DeleteHandler... handlers) {
		Node currentNode = getFirstItem(underlyingNode, first);
		if (currentNode != null) {
			underlyingNode.getSingleRelationship(first, Direction.OUTGOING).delete();
			while (currentNode != null) {
				Node nextNode = getNext(currentNode);
				Relationship itemRel = currentNode.getSingleRelationship(item, Direction.OUTGOING);
				Node otherNode = itemRel.getOtherNode(currentNode);
				itemRel.delete();
				for (DeleteHandler handler : handlers) {
					handler.handleDelete(otherNode);
				}
				if (nextNode != null) {
					currentNode.getSingleRelationship(NeoRelationshipType.LIST_NEXT, Direction.OUTGOING).delete();
				} else {
					currentNode.getSingleRelationship(last, Direction.INCOMING).delete();
				}
				IterableUtils.toString(currentNode.getRelationships());
				currentNode.delete();
				currentNode = nextNode;
			}
		}
	}

	public static interface DeleteHandler {

		public void handleDelete(Node node);

	}

	private static Node getFirstItem(Node underlyingNode, NeoRelationshipType first) {
		return Neo4JUtils.getLinkedNode(underlyingNode, first);
	}

	private static Node getLastItem(Node underlyingNode, NeoRelationshipType last) {
		return Neo4JUtils.getLinkedNode(underlyingNode, last);
	}

	private static Node getNext(Node current) {
		return Neo4JUtils.getOutgoingNode(current, NeoRelationshipType.LIST_NEXT);
	}

	public static <T> Iterable<T> getListIterable(final Node underlyingNode, final NeoRelationshipType first,
			final NeoRelationshipType item, final NodeReader<T> converter) {
		return new Iterable<T>() {

			public Iterator<T> iterator() {
				return new Iterator<T>() {

					private Node current = null;

					public boolean hasNext() {
						return (current == null && getFirstItem(underlyingNode, first) != null)
								|| (current != null && getNext(current) != null);
					}

					public T next() {
						if (current == null) {
							current = getFirstItem(underlyingNode, first);
						} else {
							current = getNext(current);
						}
						return converter.convertNode(Neo4JUtils.getLinkedNode(current, item));
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}

				};
			}

		};
	}

	public static boolean setListItemProperty(Node underlyingNode, NeoRelationshipType first, NeoRelationshipType item,
			Node toFind, String name, Object value) {
		Node currentNode = getFirstItem(underlyingNode, first);
		while (currentNode != null) {
			Node itemNode = getLinkedNode(currentNode, item);
			if (itemNode.equals(toFind)) {
				currentNode.setProperty(name, value);
				return true;
			}
			currentNode = getNext(currentNode);
		}
		return false;
	}

	public static Object getListItemProperty(Node underlyingNode, NeoRelationshipType first, NeoRelationshipType item,
			Node toFind, String name) {
		Node currentNode = getFirstItem(underlyingNode, first);
		while (currentNode != null) {
			Node itemNode = getLinkedNode(currentNode, item);
			if (itemNode.equals(toFind)) {
				return currentNode.getProperty(name);
			}
			currentNode = getNext(currentNode);
		}
		return null;
	}

	public static void setProperty(Node underlyingNode, String name, Object value) {
		if (value == null) {
			underlyingNode.removeProperty(name);
		} else {
			underlyingNode.setProperty(name, value);
		}
	}

	public static Node getNode(User owner) {
		return owner.getUnderlyingNode();
	}
}
