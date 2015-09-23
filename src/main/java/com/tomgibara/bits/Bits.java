package com.tomgibara.bits;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.ListIterator;

import com.tomgibara.bits.BitStore.Matches;
import com.tomgibara.bits.BitStore.Operation;
import com.tomgibara.bits.ImmutableBit.ImmutableOne;
import com.tomgibara.bits.ImmutableBit.ImmutableZero;
import com.tomgibara.hashing.HashSerializer;
import com.tomgibara.hashing.Hasher;
import com.tomgibara.hashing.Hashing;

public final class Bits {

	private static final Hasher<BitStore> bitStoreHasher = bitStoreHasher((b,s) -> b.writeTo(s));
	
	// public
	
	public static Hasher<BitStore> bitStoreHasher() {
		return bitStoreHasher;
	}
	
	//TODO optimize
	public static BitStore noBits() {
		return new AbstractBitStore() {

			@Override
			public boolean isMutable() {
				return true;
			}
			
			@Override
			public int compareTo(BitStore that) {
				return that.zeros().isAll() ? 0 : -1;
			}

			@Override
			public int size() {
				return 0;
			}

			@Override
			public boolean getBit(int index) {
				throw new IllegalArgumentException();
			}
			
		};
	}
	
	public static BitStore oneBit() {
		return ImmutableOne.INSTANCE;
	}
	
	public static BitStore zeroBit() {
		return ImmutableZero.INSTANCE;
	}
	
	public static BitStore bit(boolean bit) {
		return ImmutableBit.instanceOf(bit);
	}
	
	public static BitStore asBitStore(boolean bit) {
		return new Bit(bit);
	}
	
	public static BitStore asBitStore(long bits) {
		return new LongBitStore(bits);
	}
	
	public static BitStore asBitStore(long bits, int count) {
		return new LongBitStore(bits).range(0, count);
	}
	
	public static BitStore asBitStore(BitSet bitSet, int size) {
		if (bitSet == null) throw new IllegalArgumentException("null bitSet");
		if (size < 0) throw new IllegalArgumentException("negative size");
		return new BitSetBitStore(bitSet, 0, size, true);
	}

	public static BitStore asBitStore(byte[] bytes) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		if (bytes.length * 8L > Integer.MAX_VALUE) throw new IllegalArgumentException("index overflow");
		return new BytesBitStore(bytes, 0, bytes.length << 3, true);
	}

	public static BitStore asBitStore(byte[] bytes, int offset, int length) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		int size = bytes.length << 3;
		if (offset < 0) throw new IllegalArgumentException("negative offset");
		if (length < 0) throw new IllegalArgumentException("negative length");
		int finish = offset + length;
		if (finish < 0) throw new IllegalArgumentException("index overflow");
		if (finish > size) throw new IllegalArgumentException("exceeds size");
		return new BytesBitStore(bytes, offset, finish, true);
	}

	public static void transfer(BitReader reader, BitWriter writer, long count) {
		if (reader == null) throw new IllegalArgumentException("null reader");
		if (writer == null) throw new IllegalArgumentException("null writer");
		if (count < 0L) throw new IllegalArgumentException("negative count");
		while (count >= 64) {
			//TODO could benefit from reading into a larger buffer here - eg bytes?
			long bits = reader.readLong(64);
			writer.write(bits, 64);
			count -= 64;
		}
		if (count != 0L) {
			long bits = reader.readLong((int) count);
			writer.write(bits, (int) count);
		}
	}
	
	public static BitReader newBitReader(CharSequence chars) {
		if (chars == null) throw new IllegalArgumentException("null chars");
		return new CharBitReader(chars);
	}
	
	// package only
	
	static <B> Hasher<B> bitStoreHasher(HashSerializer<B> s) {
		return Hashing.murmur3Int().hasher(s);
	}

	// available via default BitStore method
	static BitStore newRangedView(BitStore store, int from, int to) {
		if (store == null) throw new IllegalArgumentException("null store");
		if (from < 0) throw new IllegalArgumentException();
		if (from > to) throw new IllegalArgumentException();
		if (to > store.size()) throw new IllegalArgumentException();
		return new AbstractBitStore() {

			@Override
			public int size() {
				return to - from;
			}

			@Override
			public boolean getBit(int index) {
				return store.getBit(adjIndex(index));
			}

			@Override
			public void setBit(int index, boolean value) {
				store.setBit(adjIndex(index), value);
			}
			
			@Override
			public void flipBit(int index) {
				store.flipBit(adjIndex(index));
			}
			
			@Override
			public long getBits(int position, int length) {
				return store.getBits(adjPosition(position), length);
			}
			
			@Override
			public boolean getThenSetBit(int index, boolean value) {
				return store.getThenSetBit(adjIndex(index), value);
			}

			@Override
			public void setStore(int position, BitStore that) {
				store.setStore(adjPosition(position), store);
			}
			
			@Override
			public BitWriter openWriter() {
				return store.openWriter(to);
			}
			
			@Override
			public BitWriter openWriter(int position) {
				return store.openWriter(adjPosition(position));
			}
			
			@Override
			public BitReader openReader() {
				return store.openReader(to);
			}
			
			@Override
			public BitReader openReader(int position) {
				return store.openReader(adjPosition(position));
			}
			
			@Override
			public BitWriter openWriter(Operation operation, int position) {
				return store.openWriter(operation, adjPosition(position));
			}

			@Override
			public boolean isMutable() {
				return store.isMutable();
			}

			private int adjIndex(int index) {
				if (index < 0) throw new IllegalArgumentException();
				index += from;
				if (index >= to) throw new IllegalArgumentException();
				return index;
			}

			private int adjPosition(int position) {
				if (position < 0) throw new IllegalArgumentException();
				position += from;
				if (position > to) throw new IllegalArgumentException();
				return position;
			}
		};
	}

	// available via default BitStore method
	static Number asNumber(BitStore store) {
		return new Number() {

			private static final long serialVersionUID = -2906430071162493968L;

			final int size = store.size();

			@Override
			public byte byteValue() {
				return (byte) store.getBits(0, Math.min(8, size));
			}

			@Override
			public short shortValue() {
				return (short) store.getBits(0, Math.min(16, size));
			}

			@Override
			public int intValue() {
				return (int) store.getBits(0, Math.min(32, size));
			}

			@Override
			public long longValue() {
				return store.getBits(0, Math.min(64, size));
			}

			@Override
			public float floatValue() {
				return store.toBigInteger().floatValue();
			}

			@Override
			public double doubleValue() {
				return store.toBigInteger().doubleValue();
			}

		};
	}

	//TODO further optimizations possible
	// available via default BitStore method
	static BitWriter newBitWriter(BitStore store, int position) {
		return newBitWriter(Operation.SET, store, position);
	}

	// available via default BitStore method
	static BitReader newBitReader(BitStore store, int position) {
		if (store == null) throw new IllegalArgumentException("null store");
		if (position < 0) throw new IllegalArgumentException();
		if (position > store.size()) throw new IllegalArgumentException();
		
		return new BitReader() {
			int pos = position;
			
			@Override
			public long getPosition() {
				return position - pos;
			}
			
			@Override
			public long setPosition(long newPosition) {
				if (newPosition < 0L) throw new IllegalArgumentException();
				if (newPosition >= position) {
					pos = 0;
					return position;
				}
				pos = position - (int) newPosition;
				return newPosition;
			}
			
			@Override
			public boolean readBoolean() throws BitStreamException {
				if (pos <= 0) throw new EndOfBitStreamException();
				return store.getBit(--pos);
			}
			
			@Override
			public int readBit() throws BitStreamException {
				return readBoolean() ? 1 : 0;
			}
			
			@Override
			public long readLong(int count) throws BitStreamException {
				if (count < 0) throw new IllegalArgumentException();
				if (count > 64) throw new IllegalArgumentException();
				pos -= count;
				if (pos < 0) throw new EndOfBitStreamException();
				return store.getBits(pos, count);
			}
			
			@Override
			public int read(int count) throws BitStreamException {
				if (count < 0) throw new IllegalArgumentException();
				if (count > 32) throw new IllegalArgumentException();
				pos -= count;
				if (pos < 0) throw new EndOfBitStreamException();
				return (int) store.getBits(pos, count);
			}
			
			@Override
			public int readUntil(boolean one) throws BitStreamException {
				//TODO efficient implementation needs next/previous bit support on BitStore
				return BitReader.super.readUntil(one);
			}
			
			@Override
			public BigInteger readBigInt(int count) throws BitStreamException {
				switch(count) {
				case 0 : return BigInteger.ZERO;
				case 1 : return readBoolean() ? BigInteger.ONE : BigInteger.ZERO;
				default :
					final int from = pos - count;
					if (from < 0) throw new EndOfBitStreamException();
					final int to = pos;
					pos = from;
					return store.range(from, to).toBigInteger();
				}
			}
		};
	}
	
	// available via default BitStore method
	static BitWriter newBitWriter(BitStore.Operation operation, BitStore store, int position) {
		if (store == null) throw new IllegalArgumentException("null store");
		if (position < 0) throw new IllegalArgumentException();
		if (position > store.size()) throw new IllegalArgumentException();

		switch(operation) {
		case SET: return new BitStoreWriter.Set(store, position);
		case AND: return new BitStoreWriter.And(store, position);
		case OR:  return new BitStoreWriter.Or (store, position);
		case XOR: return new BitStoreWriter.Xor(store, position);
		default:
			throw new IllegalStateException("unsupported operation");
		}
	}
	
	// available via default BitStore method
	static ListIterator<Integer> newListIterator(Matches matches, int position) {
		if (matches == null) throw new IllegalArgumentException("null matches");
		if (position < 0L) throw new IllegalArgumentException();
		//TODO consider restoring dedicated size accessor on matches
		if (position > matches.store().size()) throw new IllegalArgumentException();
		return new BitStorePositions(matches, position);
	}

	static int compareNumeric(BitStore a, BitStore b) {
		ListIterator<Integer> as = a.ones().positions(a.size());
		ListIterator<Integer> bs = b.ones().positions(b.size());
		while (true) {
			boolean ap = as.hasPrevious();
			boolean bp = bs.hasPrevious();
			if (ap && bp) {
				int ai = as.previous();
				int bi = bs.previous();
				if (ai == bi) continue;
				return ai > bi ? 1 : -1;
			} else {
				if (ap) return 1;
				if (bp) return -1;
				return 0;
			}
		}
		
	}

	//duplicated here to avoid dependencies
	static int gcd(int a, int b) {
		while (a != b) {
			if (a > b) {
				int na = a % b;
				if (na == 0) return b;
				a = na;
			} else {
				int nb = b % a;
				if (nb == 0) return a;
				b = nb;
			}
		}
		return a;
	}

	private Bits() { }

}