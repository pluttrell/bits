/*
 * Copyright 2015 Tom Gibara
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.tomgibara.bits;

import java.util.NoSuchElementException;

import com.tomgibara.bits.BitStore.Matches;
import com.tomgibara.bits.BitStore.Positions;

class BitStorePositions implements Positions {

	private static final int NOT_SET = Integer.MIN_VALUE;

	private final Matches matches;
	private final boolean disjoint;
	private final int size;
	private final boolean singleBit;
	private final int seqSize;
	private final boolean bit;

	private int previous;
	private int next;
	private int nextIndex;
	private int recent = NOT_SET;

	BitStorePositions(Matches matches, boolean disjoint, int position) {
		this.matches = matches;
		size = matches.store().size();
		BitStore sequence = matches.sequence();
		seqSize = sequence.size();
		singleBit = sequence.size() == 1;
		this.disjoint = disjoint || singleBit;
		bit = singleBit && sequence.getBit(0);
		previous = matches.previous(position);
		next = matches.next(position);
		nextIndex = previous == -1 ? 0 : NOT_SET;
	}

	@Override
	public boolean isDisjoint() {
		return disjoint;
	};

	@Override
	public boolean hasPrevious() {
		return previous != -1;
	}

	@Override
	public boolean hasNext() {
		return next != size;
	}

	@Override
	public int previousPosition() {
		if (previous == -1) return -1;
		recent = previous;
		next = recent;
		int index = disjoint ? recent - seqSize + 1 : recent;
		previous = matches.previous(index);
		if (nextIndex != NOT_SET) nextIndex--;
		return next;
	}

	@Override
	public Integer previous() {
		int position = previousPosition();
		if (position == -1) throw new NoSuchElementException();
		return position;
	}

	@Override
	public int nextPosition() {
		if (next == size) return size;
		recent = next;
		previous = recent;
		int index = disjoint ? recent + seqSize : recent + 1;
		next = matches.next(index);
		if (nextIndex != NOT_SET) nextIndex++;
		return previous;
	}

	@Override
	public void replace(BitStore replacement) {
		if (replacement == null) throw new IllegalArgumentException("null replacement");
		if (replacement.size() != seqSize) throw new IllegalArgumentException("replacement size does not match sequence size");
		if (recent == NOT_SET) throw new IllegalStateException("no position");
		if (singleBit) {
			if (replacement.getBit(0) != bit) matches.store().flipBit(recent);
		} else if (!replacement.equals(matches.sequence())) {
			matches.store().setStore(recent, replacement);
		}
	}

	@Override
	public void replace(boolean bits) {
		if (recent == NOT_SET) throw new IllegalStateException("no position");
		if (singleBit) {
			if (bits != bit) matches.store().setBit(recent, bits);
		} else {
			matches.store().range(recent, recent + seqSize).setAll(bits);
		}
	}

	@Override
	public Integer next() {
		int position = nextPosition();
		if (position == size) throw new NoSuchElementException();
		return position;
	}

	@Override
	public int previousIndex() {
		return nextIndex() - 1;
	}

	@Override
	public int nextIndex() {
		if (!disjoint || singleBit) {
			return nextIndex == NOT_SET ? nextIndex = matches.range(0, next).count() : nextIndex;
		} else {
			//TODO
			throw new IllegalStateException("TODO");
		}
	}

	@Override
	public void add(Integer e) {
		doAdd(e);
		recent = NOT_SET;
	}

	@Override
	public void remove() {
		doRemove();
		recent = NOT_SET;
	}

	@Override
	public void set(Integer e) {
		doRemove();
		doAdd(e);
		recent = NOT_SET;
	}

	private void doAdd(Integer e) {
		checkSingleBit();
		if (e == null) throw new IllegalArgumentException("null e");
		int i = e;
		if (i < previous) throw new IllegalArgumentException("e less than previous value: " + previous);
		if (i >= next) throw new IllegalArgumentException("e not less than next value: " + next);
		boolean changed = bit != matches.store().getThenSetBit(i, bit);
		if (changed) {
			if (nextIndex != NOT_SET) nextIndex ++;
			previous = i;
		}
	}

	private void doRemove() {
		checkSingleBit();
		if (recent == previous) { // we went forward
			previous = matches.range(0, recent).last();
			if (nextIndex != NOT_SET) nextIndex --;
		} else if (recent == next) { // we went backwards
			next = recent + 1 + matches.range(recent + 1, size).first();
		} else { // no recent value
			throw new IllegalStateException();
		}
		matches.store().setBit(recent, !bit);
	}

	private void checkSingleBit() {
		if (!singleBit) throw new UnsupportedOperationException("cannot add/remove abitrary sequence positions");
	}

}
