/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.buffer;

import org.easymock.EasyMock;
import org.junit.Test;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ScatteringByteChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static io.netty.buffer.Unpooled.*;
import static org.junit.Assert.*;

/**
 * Tests channel buffers
 */
@SuppressWarnings("ZeroLengthArrayAllocation")
public class UnpooledTest {

    @Test
    public void testCompositeWrappedBuffer() {
        ByteBuf header = buffer(12);
        ByteBuf payload = buffer(512);

        header.writeBytes(new byte[12]);
        payload.writeBytes(new byte[512]);

        ByteBuf buffer = wrappedBuffer(header, payload);

        assertEquals(12, header.readableBytes());
        assertEquals(512, payload.readableBytes());

        assertEquals(12 + 512, buffer.readableBytes());
        assertEquals(2, buffer.nioBufferCount());

        buffer.release();
    }

    @Test
    public void testHashCode() {
        Map<byte[], Integer> map = new LinkedHashMap<byte[], Integer>();
        map.put(new byte[0], 1);
        map.put(new byte[] { 1 }, 32);
        map.put(new byte[] { 2 }, 33);
        map.put(new byte[] { 0, 1 }, 962);
        map.put(new byte[] { 1, 2 }, 994);
        map.put(new byte[] { 0, 1, 2, 3, 4, 5 }, 63504931);
        map.put(new byte[] { 6, 7, 8, 9, 0, 1 }, (int) 97180294697L);
        map.put(new byte[] { -1, -1, -1, (byte) 0xE1 }, 1);

        for (Entry<byte[], Integer> e: map.entrySet()) {
            assertEquals(
                    e.getValue().intValue(),
                    BufUtil.hashCode(wrappedBuffer(e.getKey())));
        }
    }

    @Test
    public void testEquals() {
        ByteBuf a, b;

        // Different length.
        a = wrappedBuffer(new byte[] { 1  });
        b = wrappedBuffer(new byte[] { 1, 2 });
        assertFalse(BufUtil.equals(a, b));

        // Same content, same firstIndex, short length.
        a = wrappedBuffer(new byte[] { 1, 2, 3 });
        b = wrappedBuffer(new byte[] { 1, 2, 3 });
        assertTrue(BufUtil.equals(a, b));

        // Same content, different firstIndex, short length.
        a = wrappedBuffer(new byte[] { 1, 2, 3 });
        b = wrappedBuffer(new byte[] { 0, 1, 2, 3, 4 }, 1, 3);
        assertTrue(BufUtil.equals(a, b));

        // Different content, same firstIndex, short length.
        a = wrappedBuffer(new byte[] { 1, 2, 3 });
        b = wrappedBuffer(new byte[] { 1, 2, 4 });
        assertFalse(BufUtil.equals(a, b));

        // Different content, different firstIndex, short length.
        a = wrappedBuffer(new byte[] { 1, 2, 3 });
        b = wrappedBuffer(new byte[] { 0, 1, 2, 4, 5 }, 1, 3);
        assertFalse(BufUtil.equals(a, b));

        // Same content, same firstIndex, long length.
        a = wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        b = wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        assertTrue(BufUtil.equals(a, b));

        // Same content, different firstIndex, long length.
        a = wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        b = wrappedBuffer(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}, 1, 10);
        assertTrue(BufUtil.equals(a, b));

        // Different content, same firstIndex, long length.
        a = wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        b = wrappedBuffer(new byte[] { 1, 2, 3, 4, 6, 7, 8, 5, 9, 10 });
        assertFalse(BufUtil.equals(a, b));

        // Different content, different firstIndex, long length.
        a = wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        b = wrappedBuffer(new byte[] { 0, 1, 2, 3, 4, 6, 7, 8, 5, 9, 10, 11 }, 1, 10);
        assertFalse(BufUtil.equals(a, b));
    }

    @Test
    public void testCompare() {
        List<ByteBuf> expected = new ArrayList<ByteBuf>();
        expected.add(wrappedBuffer(new byte[] { 1 }));
        expected.add(wrappedBuffer(new byte[] { 1, 2 }));
        expected.add(wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8,  9, 10 }));
        expected.add(wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8,  9, 10, 11, 12 }));
        expected.add(wrappedBuffer(new byte[] { 2 }));
        expected.add(wrappedBuffer(new byte[] { 2, 3 }));
        expected.add(wrappedBuffer(new byte[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 }));
        expected.add(wrappedBuffer(new byte[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13 }));
        expected.add(wrappedBuffer(new byte[] { 2, 3, 4 }, 1, 1));
        expected.add(wrappedBuffer(new byte[] { 1, 2, 3, 4 }, 2, 2));
        expected.add(wrappedBuffer(new byte[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 }, 1, 10));
        expected.add(wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 }, 2, 12));
        expected.add(wrappedBuffer(new byte[] { 2, 3, 4, 5 }, 2, 1));
        expected.add(wrappedBuffer(new byte[] { 1, 2, 3, 4, 5 }, 3, 2));
        expected.add(wrappedBuffer(new byte[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 }, 2, 10));
        expected.add(wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 }, 3, 12));

        for (int i = 0; i < expected.size(); i ++) {
            for (int j = 0; j < expected.size(); j ++) {
                if (i == j) {
                    assertEquals(0, BufUtil.compare(expected.get(i), expected.get(j)));
                } else if (i < j) {
                    assertTrue(BufUtil.compare(expected.get(i), expected.get(j)) < 0);
                } else {
                    assertTrue(BufUtil.compare(expected.get(i), expected.get(j)) > 0);
                }
            }
        }
    }

    @Test
    public void shouldReturnEmptyBufferWhenLengthIsZero() {
        assertSame(EMPTY_BUFFER, wrappedBuffer(new byte[0]));
        assertSame(EMPTY_BUFFER, wrappedBuffer(new byte[8], 0, 0));
        assertSame(EMPTY_BUFFER, wrappedBuffer(new byte[8], 8, 0));
        assertSame(EMPTY_BUFFER, wrappedBuffer(ByteBuffer.allocateDirect(0)));
        assertSame(EMPTY_BUFFER, wrappedBuffer(EMPTY_BUFFER));
        assertSame(EMPTY_BUFFER, wrappedBuffer(new byte[0][]));
        assertSame(EMPTY_BUFFER, wrappedBuffer(new byte[][] { new byte[0] }));
        assertSame(EMPTY_BUFFER, wrappedBuffer(new ByteBuffer[0]));
        assertSame(EMPTY_BUFFER, wrappedBuffer(new ByteBuffer[] { ByteBuffer.allocate(0) }));
        assertSame(EMPTY_BUFFER, wrappedBuffer(ByteBuffer.allocate(0), ByteBuffer.allocate(0)));
        assertSame(EMPTY_BUFFER, wrappedBuffer(new ByteBuf[0]));
        assertSame(EMPTY_BUFFER, wrappedBuffer(new ByteBuf[] { buffer(0) }));
        assertSame(EMPTY_BUFFER, wrappedBuffer(buffer(0), buffer(0)));

        assertSame(EMPTY_BUFFER, copiedBuffer(new byte[0]));
        assertSame(EMPTY_BUFFER, copiedBuffer(new byte[8], 0, 0));
        assertSame(EMPTY_BUFFER, copiedBuffer(new byte[8], 8, 0));
        assertSame(EMPTY_BUFFER, copiedBuffer(ByteBuffer.allocateDirect(0)));
        assertSame(EMPTY_BUFFER, copiedBuffer(EMPTY_BUFFER));
        assertSame(EMPTY_BUFFER, copiedBuffer(new byte[0][]));
        assertSame(EMPTY_BUFFER, copiedBuffer(new byte[][] { new byte[0] }));
        assertSame(EMPTY_BUFFER, copiedBuffer(new ByteBuffer[0]));
        assertSame(EMPTY_BUFFER, copiedBuffer(new ByteBuffer[] { ByteBuffer.allocate(0) }));
        assertSame(EMPTY_BUFFER, copiedBuffer(ByteBuffer.allocate(0), ByteBuffer.allocate(0)));
        assertSame(EMPTY_BUFFER, copiedBuffer(new ByteBuf[0]));
        assertSame(EMPTY_BUFFER, copiedBuffer(new ByteBuf[] { buffer(0) }));
        assertSame(EMPTY_BUFFER, copiedBuffer(buffer(0), buffer(0)));
    }

    @Test
    public void testCompare2() {
        assertTrue(BufUtil.compare(
                wrappedBuffer(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}),
                wrappedBuffer(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00}))
                > 0);

        assertTrue(BufUtil.compare(
                wrappedBuffer(new byte[]{(byte) 0xFF}),
                wrappedBuffer(new byte[]{(byte) 0x00}))
                > 0);
    }

    @Test
    public void shouldAllowEmptyBufferToCreateCompositeBuffer() {
        ByteBuf buf = wrappedBuffer(
                EMPTY_BUFFER,
                wrappedBuffer(new byte[16]).order(LITTLE_ENDIAN),
                EMPTY_BUFFER);
        try {
            assertEquals(16, buf.capacity());
        } finally {
            buf.release();
        }
    }

    @Test
    public void testWrappedBuffer() {
        assertEquals(16, wrappedBuffer(ByteBuffer.allocateDirect(16)).capacity());

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                wrappedBuffer(new byte[][] { new byte[] { 1, 2, 3 } }));

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                wrappedBuffer(
                        new byte[] { 1 },
                        new byte[] { 2 },
                        new byte[] { 3 }));

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                wrappedBuffer(new ByteBuf[] {
                        wrappedBuffer(new byte[] { 1, 2, 3 })
                }));

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                wrappedBuffer(
                        wrappedBuffer(new byte[] { 1 }),
                        wrappedBuffer(new byte[] { 2 }),
                        wrappedBuffer(new byte[] { 3 })));

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                wrappedBuffer(new ByteBuffer[] {
                        ByteBuffer.wrap(new byte[] { 1, 2, 3 })
                }));

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                wrappedBuffer(
                        ByteBuffer.wrap(new byte[] { 1 }),
                        ByteBuffer.wrap(new byte[] { 2 }),
                        ByteBuffer.wrap(new byte[] { 3 })));
    }

    @Test
    public void testCopiedBuffer() {
        assertEquals(16, copiedBuffer(ByteBuffer.allocateDirect(16)).capacity());

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                copiedBuffer(new byte[][] { new byte[] { 1, 2, 3 } }));

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                copiedBuffer(
                        new byte[] { 1 },
                        new byte[] { 2 },
                        new byte[] { 3 }));

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                copiedBuffer(new ByteBuf[] {
                        wrappedBuffer(new byte[] { 1, 2, 3 })
                }));

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                copiedBuffer(
                        wrappedBuffer(new byte[] { 1 }),
                        wrappedBuffer(new byte[] { 2 }),
                        wrappedBuffer(new byte[] { 3 })));

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                copiedBuffer(new ByteBuffer[] {
                        ByteBuffer.wrap(new byte[] { 1, 2, 3 })
                }));

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                copiedBuffer(
                        ByteBuffer.wrap(new byte[] { 1 }),
                        ByteBuffer.wrap(new byte[] { 2 }),
                        ByteBuffer.wrap(new byte[] { 3 })));
    }

    @Test
    public void testHexDump() {
        assertEquals("", BufUtil.hexDump(EMPTY_BUFFER));

        assertEquals("123456", BufUtil.hexDump(wrappedBuffer(
                new byte[]{
                        0x12, 0x34, 0x56
                })));

        assertEquals("1234567890abcdef", BufUtil.hexDump(wrappedBuffer(
                new byte[]{
                        0x12, 0x34, 0x56, 0x78,
                        (byte) 0x90, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF
                })));
    }

    @Test
    public void testSwapMedium() {
        assertEquals(0x563412, BufUtil.swapMedium(0x123456));
        assertEquals(0x80, BufUtil.swapMedium(0x800000));
    }

    @Test
    public void testUnmodifiableBuffer() throws Exception {
        ByteBuf buf = unmodifiableBuffer(buffer(16));

        try {
            buf.discardReadBytes();
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            buf.setByte(0, (byte) 0);
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            buf.setBytes(0, EMPTY_BUFFER, 0, 0);
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            buf.setBytes(0, new byte[0], 0, 0);
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            buf.setBytes(0, ByteBuffer.allocate(0));
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            buf.setShort(0, (short) 0);
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            buf.setMedium(0, 0);
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            buf.setInt(0, 0);
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            buf.setLong(0, 0);
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            buf.setBytes(0, EasyMock.createMock(InputStream.class), 0);
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            buf.setBytes(0, EasyMock.createMock(ScatteringByteChannel.class), 0);
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testWrapSingleInt() {
        ByteBuf buffer = copyInt(42);
        assertEquals(4, buffer.capacity());
        assertEquals(42, buffer.readInt());
        assertFalse(buffer.isReadable());
    }

    @Test
    public void testWrapInt() {
        ByteBuf buffer = copyInt(1, 4);
        assertEquals(8, buffer.capacity());
        assertEquals(1, buffer.readInt());
        assertEquals(4, buffer.readInt());
        assertFalse(buffer.isReadable());

        assertEquals(0, Unpooled.copyInt(null).capacity());
        assertEquals(0, Unpooled.copyInt(new int[0]).capacity());
    }

    @Test
    public void testWrapSingleShort() {
        ByteBuf buffer = copyShort(42);
        assertEquals(2, buffer.capacity());
        assertEquals(42, buffer.readShort());
        assertFalse(buffer.isReadable());
    }

    @Test
    public void testWrapShortFromShortArray() {
        ByteBuf buffer = copyShort(new short[]{1, 4});
        assertEquals(4, buffer.capacity());
        assertEquals(1, buffer.readShort());
        assertEquals(4, buffer.readShort());
        assertFalse(buffer.isReadable());

        assertEquals(0, Unpooled.copyShort((short[]) null).capacity());
        assertEquals(0, Unpooled.copyShort(new short[0]).capacity());
    }

    @Test
    public void testWrapShortFromIntArray() {
        ByteBuf buffer = copyShort(1, 4);
        assertEquals(4, buffer.capacity());
        assertEquals(1, buffer.readShort());
        assertEquals(4, buffer.readShort());
        assertFalse(buffer.isReadable());

        assertEquals(0, Unpooled.copyShort((int[]) null).capacity());
        assertEquals(0, Unpooled.copyShort(new int[0]).capacity());
    }

    @Test
    public void testWrapSingleMedium() {
        ByteBuf buffer = copyMedium(42);
        assertEquals(3, buffer.capacity());
        assertEquals(42, buffer.readMedium());
        assertFalse(buffer.isReadable());
    }

    @Test
    public void testWrapMedium() {
        ByteBuf buffer = copyMedium(1, 4);
        assertEquals(6, buffer.capacity());
        assertEquals(1, buffer.readMedium());
        assertEquals(4, buffer.readMedium());
        assertFalse(buffer.isReadable());

        assertEquals(0, Unpooled.copyMedium(null).capacity());
        assertEquals(0, Unpooled.copyMedium(new int[0]).capacity());
    }

    @Test
    public void testWrapSingleLong() {
        ByteBuf buffer = copyLong(42);
        assertEquals(8, buffer.capacity());
        assertEquals(42, buffer.readLong());
        assertFalse(buffer.isReadable());
    }

    @Test
    public void testWrapLong() {
        ByteBuf buffer = copyLong(1, 4);
        assertEquals(16, buffer.capacity());
        assertEquals(1, buffer.readLong());
        assertEquals(4, buffer.readLong());
        assertFalse(buffer.isReadable());

        assertEquals(0, Unpooled.copyLong(null).capacity());
        assertEquals(0, Unpooled.copyLong(new long[0]).capacity());
    }

    @Test
    public void testWrapSingleFloat() {
        ByteBuf buffer = copyFloat(42);
        assertEquals(4, buffer.capacity());
        assertEquals(42, buffer.readFloat(), 0.01);
        assertFalse(buffer.isReadable());
    }

    @Test
    public void testWrapFloat() {
        ByteBuf buffer = copyFloat(1, 4);
        assertEquals(8, buffer.capacity());
        assertEquals(1, buffer.readFloat(), 0.01);
        assertEquals(4, buffer.readFloat(), 0.01);
        assertFalse(buffer.isReadable());

        assertEquals(0, Unpooled.copyFloat(null).capacity());
        assertEquals(0, Unpooled.copyFloat(new float[0]).capacity());
    }

    @Test
    public void testWrapSingleDouble() {
        ByteBuf buffer = copyDouble(42);
        assertEquals(8, buffer.capacity());
        assertEquals(42, buffer.readDouble(), 0.01);
        assertFalse(buffer.isReadable());
    }

    @Test
    public void testWrapDouble() {
        ByteBuf buffer = copyDouble(1, 4);
        assertEquals(16, buffer.capacity());
        assertEquals(1, buffer.readDouble(), 0.01);
        assertEquals(4, buffer.readDouble(), 0.01);
        assertFalse(buffer.isReadable());

        assertEquals(0, Unpooled.copyDouble(null).capacity());
        assertEquals(0, Unpooled.copyDouble(new double[0]).capacity());
    }

    @Test
    public void testWrapBoolean() {
        ByteBuf buffer = copyBoolean(true, false);
        assertEquals(2, buffer.capacity());
        assertTrue(buffer.readBoolean());
        assertFalse(buffer.readBoolean());
        assertFalse(buffer.isReadable());

        assertEquals(0, Unpooled.copyBoolean(null).capacity());
        assertEquals(0, Unpooled.copyBoolean(new boolean[0]).capacity());

    }
}
