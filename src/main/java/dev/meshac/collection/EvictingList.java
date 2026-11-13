package dev.meshac.collection;

import java.util.Collection;
import java.util.LinkedList;

/** Fixed-capacity list that drops the oldest element once full. Ported from Vulcan's EvictingList. */
public final class EvictingList<E> extends LinkedList<E> {

	private final int capacity;

	public EvictingList(int capacity) {
		this.capacity = capacity;
	}

	public EvictingList(Collection<? extends E> elements, int capacity) {
		super(elements);
		this.capacity = capacity;
	}

	@Override
	public boolean add(E element) {
		if (size() >= getCapacity()) removeFirst();
		return super.add(element);
	}

	public boolean isFull() {
		return size() >= getCapacity();
	}

	public int getCapacity() {
		return capacity;
	}
}
