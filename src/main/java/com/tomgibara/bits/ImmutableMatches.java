package com.tomgibara.bits;

import java.util.Collections;
import java.util.ListIterator;
import java.util.SortedSet;

import com.tomgibara.bits.BitStore.BitMatches;

class ImmutableMatches extends BitMatches {

	private final BitStore store;
	private final BitMatches matches;

	ImmutableMatches(BitStore store, BitMatches matches) {
		this.store = store;
		this.matches = matches;
	}
	
	@Override
	public BitStore store() {
		return store;
	}
	@Override
	public boolean bit() {
		return matches.bit();
	}
	@Override
	public BitStore sequence() {
		return matches.sequence();
	}
	@Override
	public BitMatches range(int from, int to) {
		return store.range(from, to).match(matches.bit());
	}
	@Override
	public boolean isAll() {
		return matches.isAll();
	}
	@Override
	public int count() {
		return matches.count();
	}
	@Override
	public int first() {
		return matches.first();
	}
	@Override
	public int last() {
		return matches.last();
	}
	@Override
	public int next(int position) {
		return matches.next(position);
	}
	@Override
	public int previous(int position) {
		return matches.previous(position);
	}
	@Override
	public ListIterator<Integer> positions() {
		return Bits.newListIterator(this, 0);
	}
	@Override
	public ListIterator<Integer> positions(int position) {
		return Bits.newListIterator(this, position);
	}
	
	@Override
	public SortedSet<Integer> asSet() {
		return Collections.unmodifiableSortedSet(matches.asSet());
	}
	
}
