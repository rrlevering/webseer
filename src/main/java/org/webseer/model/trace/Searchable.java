package org.webseer.model.trace;

public interface Searchable {

	public Item getItem(String outputLabel);

	public Iterable<? extends Item> getItems(String outputLabel);

}
