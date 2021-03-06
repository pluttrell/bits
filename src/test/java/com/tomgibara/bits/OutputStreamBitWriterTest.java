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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.tomgibara.bits.BitReader;
import com.tomgibara.bits.BitWriter;
import com.tomgibara.bits.ByteBasedBitWriter;
import com.tomgibara.bits.InputStreamBitReader;
import com.tomgibara.bits.OutputStreamBitWriter;

public class OutputStreamBitWriterTest extends AbstractByteBasedBitWriterTest {

	@Override
	ByteBasedBitWriter newBitWriter(long size) {
		return new Writer(new ByteArrayOutputStream((int) (size + 7) / 8));
	}

	@Override
	BitReader bitReaderFor(BitWriter writer) {
		return new InputStreamBitReader(new ByteArrayInputStream(getWrittenBytes(writer)));
	}

	@Override
	byte[] getWrittenBytes(BitWriter writer) {
		Writer w = (Writer) writer;
		return w.out.toByteArray();
	}

	private static class Writer extends OutputStreamBitWriter {

		final ByteArrayOutputStream out;

		Writer(ByteArrayOutputStream out) {
			super(out);
			this.out = out;
		}

	}

}
