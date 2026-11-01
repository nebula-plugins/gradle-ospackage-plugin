/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core.header

import com.netflix.gradle.plugins.rpm.core.Util

import groovy.transform.CompileStatic

import java.lang.reflect.Array
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.nio.charset.StandardCharsets

/**
 * Common structure of the RPM signature and header sections: a 16 byte
 * header-header, an index of 16 byte entries and a data store.
 */
@CompileStatic
abstract class AbstractHeader {

    interface Tag {
        int NULL_ENTRY = 0
        int CHAR_ENTRY = 1
        int INT8_ENTRY = 2
        int INT16_ENTRY = 3
        int INT32_ENTRY = 4
        int INT64_ENTRY = 5
        int STRING_ENTRY = 6
        int BIN_ENTRY = 7
        int STRING_ARRAY_ENTRY = 8
        int I18NSTRING_ENTRY = 9
        int ASN1_ENTRY = 10
        int OPENPGP_ENTRY = 11

        int getCode()

        int getType()

        String getName()
    }

    protected static final int HEADER_HEADER_SIZE = 16
    protected static final int ENTRY_SIZE = 16
    // 0x8EADE801 does not fit in a signed int, so Groovy would make it a long without the cast.
    protected static final int MAGIC_WORD = (int) 0x8EADE801L

    protected final Map<Integer, Tag> tags = new HashMap<Integer, Tag>()
    protected final Map<Integer, Entry<?>> entries = new TreeMap<Integer, Entry<?>>()
    protected final Map<Entry<?>, Integer> pending = new LinkedHashMap<Entry<?>, Integer>()

    protected int startPos
    protected int endPos

    protected abstract boolean pad()

    /**
     * Reads the entire header contents for this channel and returns the number of entries
     * found.
     * @param input the ReadableByteChannel to read
     * @return the number read
     * @throws IOException there was an IO error
     */
    int read(ReadableByteChannel input) throws IOException {
        ByteBuffer header = Util.fill(input, HEADER_HEADER_SIZE)

        int magic = header.getInt()

        // Some RPMs carry a mangled header; skip a zero word and retry once.
        if (magic == 0) {
            header.compact()
            Util.fill(input, header)
            magic = header.getInt()
        }
        Util.check(MAGIC_WORD, magic)
        header.getInt()

        final ByteBuffer index = Util.fill(input, header.getInt() * ENTRY_SIZE)

        final int total = header.getInt()
        final int pad = pad() ? Util.round(total, 7) - total : 0
        final ByteBuffer data = Util.fill(input, total + pad)

        int count = 0
        while (index.remaining() >= ENTRY_SIZE) {
            readEntry(index.getInt(), index.getInt(), index.getInt(), index.getInt(), data)
            count++
        }
        return count
    }

    /**
     * Writes this header section to the provided channel at the current position and returns the
     * required padding. The caller is responsible for adding the padding immediately after
     * this data.
     * @param out the WritableByteChannel to output to
     * @return the padding required after the written data
     * @throws IOException there was an IO error
     */
    int write(WritableByteChannel out) throws IOException {
        final ByteBuffer header = getHeader()
        final ByteBuffer index = getIndex()
        final ByteBuffer data = getData(index)

        data.flip()
        int pad = pad() ? Util.round(data.remaining(), 7) - data.remaining() : 0
        header.putInt(data.remaining())
        Util.empty(out, header.flip())
        Util.empty(out, index.flip())
        Util.empty(out, data)
        return pad
    }

    int count() {
        return entries.size()
    }

    /**
     * Allocates the header structure header, prefilled with the magic data and the
     * index count. The total data section size still has to be appended once known.
     * @return a buffer containing the header
     */
    protected ByteBuffer getHeader() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_HEADER_SIZE)
        buffer.putInt(MAGIC_WORD)
        buffer.putInt(0)
        buffer.putInt(count())
        return buffer
    }

    /**
     * Allocates the buffer that will hold the index entries.
     * @return a buffer sized for the index
     */
    protected ByteBuffer getIndex() throws IOException {
        return ByteBuffer.allocate(count() * ENTRY_SIZE)
    }

    /**
     * Builds the data section. Each entry writes its index record into the provided
     * index buffer and its data into the returned buffer. The first entry (the region
     * tag) is written last so that its data trails the other entries, as RPM expects.
     * @param index ByteBuffer of the index
     * @return the data section
     */
    protected ByteBuffer getData(final ByteBuffer index) throws IOException {
        int offset = 0
        final List<ByteBuffer> buffers = new LinkedList<ByteBuffer>()
        final Iterator<Integer> i = entries.keySet().iterator()

        index.position(16)
        final Entry<?> first = entries.get(i.next())
        Entry<?> entry = null
        try {
            while (i.hasNext()) {
                entry = entries.get(i.next())
                offset = writeData(buffers, index, entry, offset)
            }
            index.position(0)
            offset = writeData(buffers, index, first, offset)
            index.position(index.limit())
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Error while writing '" + entry + "'.", e)
        }

        ByteBuffer data = ByteBuffer.allocate(offset)
        for (ByteBuffer buffer : buffers) {
            data.put(buffer)
        }
        return data
    }

    protected int writeData(final Collection<ByteBuffer> buffers, final ByteBuffer index, final Entry<?> entry, int offset) {
        final int shift = entry.getOffset(offset) - offset
        if (shift > 0) buffers.add(ByteBuffer.allocate(shift))
        offset += shift

        final int size = entry.size()
        final ByteBuffer buffer = ByteBuffer.allocate(size)
        entry.index(index, offset)
        if (entry.ready()) {
            entry.write(buffer)
            buffer.flip()
        } else {
            pending.put(entry, offset)
        }
        buffers.add(buffer)
        return offset + size
    }

    void writePending(final FileChannel channel) {
        for (Entry<?> entry : pending.keySet()) {
            try {
                ByteBuffer data = ByteBuffer.allocate(entry.size())
                entry.write(data)
                channel.position(Lead.LEAD_SIZE + HEADER_HEADER_SIZE + count() * ENTRY_SIZE + pending.get(entry))
                Util.empty(channel, data.flip())
            } catch (Exception e) {
                throw new RuntimeException("Error writing pending entry '" + entry.getTag() + "'.", e)
            }
        }
    }

    Map<Entry<?>, Integer> getPending() {
        return pending
    }

    void removeEntry(final Entry<?> entry) {
        entries.remove(entry.getTag())
    }

    Entry<?> getEntry(final Tag tag) {
        return getEntry(tag.getCode())
    }

    Entry<?> getEntry(final int tag) {
        return entries.get(tag)
    }

    @SuppressWarnings('unchecked')
    Entry<String[]> createEntry(Tag tag, CharSequence value) {
        Entry<String[]> entry = (Entry<String[]>) createEntry(tag.getCode(), tag.getType(), 1)
        entry.setValues([value.toString()] as String[])
        return entry
    }

    @SuppressWarnings('unchecked')
    Entry<int[]> createEntry(Tag tag, int value) {
        Entry<int[]> entry = (Entry<int[]>) createEntry(tag.getCode(), tag.getType(), 1)
        entry.setValues([value] as int[])
        return entry
    }

    /**
     * This is the main entry point through which entries are created from the builder code for
     * types other than String.
     * @param tag the Tag identifying the type of header this is bound for
     * @param values the values to be stored in the entry.
     * @throws ClassCastException if the type of values is not compatible with the type
     * required by tag
     * @return a header entry of the appropriate type, with the supplied values set in it.
     */
    @SuppressWarnings('unchecked')
    public <T> Entry<T> createEntry(Tag tag, T values) {
        Entry<T> entry = (Entry<T>) createEntry(tag.getCode(), tag.getType(), lengthOf(values))
        entry.setValues(values)
        return entry
    }

    /**
     * Creates or appends to an entry. This is useful for header types which may have
     * multiple components of each tag, as changelogs do.
     * @param tag the Tag identifying the type of header this is bound for
     * @param values the values to be stored in or appended to the entry.
     * @throws ClassCastException if the type of values is not compatible with the
     * type required by tag
     * @return a header entry of the appropriate type, with the supplied values
     * set in or appended to it.
     */
    @SuppressWarnings('unchecked')
    public <T> Entry<T> addOrAppendEntry(Tag tag, T values) {
        Entry<T> entry = (Entry<T>) addOrAppendEntry(tag.getCode(), tag.getType(), lengthOf(values))
        T existingValues = entry.getValues()
        if (existingValues == null) {
            entry.setValues(values)
        } else {
            int oldSize = Array.getLength(existingValues)
            int newSize = lengthOf(values)
            Class<?> elementType = existingValues.getClass().getComponentType()
            T newValues = (T) Array.newInstance(elementType, oldSize + newSize)
            System.arraycopy(existingValues, 0, newValues, 0, oldSize)
            System.arraycopy(values, 0, newValues, oldSize, newSize)
            entry.setValues(newValues)
        }
        return entry
    }

    @SuppressWarnings('unchecked')
    public <T> Entry<T> createEntry(Tag tag, int type, T values) {
        Entry<T> entry = (Entry<T>) createEntry(tag.getCode(), type, lengthOf(values))
        entry.setValues(values)
        return entry
    }

    @SuppressWarnings('unchecked')
    public <T> Entry<T> createEntry(int tag, int type, T values) {
        Entry<T> entry = (Entry<T>) createEntry(tag, type, lengthOf(values))
        entry.setValues(values)
        return entry
    }

    /**
     * Adds a pending entry to this header. This entry will have the correctly sized buffer allocated, but
     * will not be written until the caller writes a value and then invokes {@link #writePending} on this
     * object.
     * @param tag the tag
     * @param count the count
     * @return the entry added
     */
    Entry<?> addEntry(Tag tag, int count) {
        return createEntry(tag.getCode(), tag.getType(), count)
    }

    Entry<?> readEntry(final int tag, final int type, final int offset, final int count, final ByteBuffer data) {
        final Entry<?> entry = createEntry(tag, type, count)
        final ByteBuffer buffer = data.duplicate()
        buffer.position(offset)
        entry.read(buffer)
        entry.setOffset(offset)
        return entry
    }

    Entry<?> createEntry(final int tag, final int type, final int count) {
        final Entry<?> entry = createEntry(type)
        entry.setTag(tag)
        entry.setCount(count)
        entries.put(tag, entry)
        return entry
    }

    Entry<?> addOrAppendEntry(final int tag, final int type, final int count) {
        Entry<?> entry = entries.get(tag)
        if (entry == null) {
            entry = createEntry(type)
            entry.setTag(tag)
            entry.setCount(count)
        } else {
            entry.incCount(count)
        }
        entries.put(tag, entry)
        return entry
    }

    protected Entry<?> createEntry(int type) {
        switch (type) {
            case Tag.NULL_ENTRY:
                return new NullEntry()
            case Tag.CHAR_ENTRY:
                return new CharEntry()
            case Tag.INT8_ENTRY:
                return new Int8Entry()
            case Tag.INT16_ENTRY:
                return new Int16Entry()
            case Tag.INT32_ENTRY:
                return new Int32Entry()
            case Tag.INT64_ENTRY:
                return new Int64Entry()
            case Tag.STRING_ENTRY:
                return new StringEntry()
            case Tag.BIN_ENTRY:
                return new BinEntry()
            case Tag.STRING_ARRAY_ENTRY:
                return new StringArrayEntry()
            case Tag.I18NSTRING_ENTRY:
                return new I18NStringEntry()
            default:
                throw new IllegalStateException("Unknown entry type '" + type + "'.")
        }
    }

    int getEndPos() {
        return endPos
    }

    void setEndPos(int endPos) {
        this.endPos = endPos
    }

    int getStartPos() {
        return startPos
    }

    void setStartPos(int startPos) {
        this.startPos = startPos
    }

    protected static void requireType(Object values, Class<?> type) {
        if (!type.isInstance(values)) {
            throw new ClassCastException(values.getClass().getName() + ' cannot be cast to ' + type.getName())
        }
    }

    private static int lengthOf(Object values) {
        return values.getClass().isArray() ? Array.getLength(values) : 1
    }

    interface Entry<T> {
        void setTag(int tag)

        void setSize(int size)

        void setCount(int count)

        void incCount(int count)

        void setOffset(int offset)

        void setValues(T values)

        T getValues()

        int getTag()

        int getType()

        int getOffset(int offset)

        int size()

        boolean ready()

        void read(ByteBuffer buffer)

        void write(ByteBuffer buffer)

        void index(ByteBuffer buffer, int position)
    }

    abstract class AbstractEntry<T> implements Entry<T> {
        protected int size
        protected int tag
        protected int count
        protected int offset
        protected T values

        void setTag(Tag tag) { this.tag = tag.getCode() }

        void setTag(int tag) { this.tag = tag }

        void setSize(int size) { this.size = size }

        void setCount(int count) { this.count = count }

        void incCount(int count) { this.count += count }

        void setOffset(int offset) { this.offset = offset }

        /**
         * Fails fast if Tag and T are not compatible.
         * @throws ClassCastException if the type of values is not compatible with the type
         * required by tag.type()
         */
        protected abstract void typeCheck(Object values)

        /**
         * @throws ClassCastException if the type of values is not compatible with the type
         * required by tag.type()
         */
        void setValues(T values) {
            if (values.getClass().isArray()) {
                typeCheck(values)
            }
            this.values = values
        }

        T getValues() { return values }

        int getTag() { return tag }

        int getOffset(int offset) { return offset }

        /**
         * Returns true if this entry is ready to write, indicated by the presence of
         * a set of values.
         */
        boolean ready() { return values != null }

        /** Returns the data type of this entry. */
        abstract int getType()

        /**
         * Returns the size this entry will need in the provided data buffer to write
         * its contents, corrected for any trailing zeros to fill to a boundary.
         */
        abstract int size()

        /** Reads this entry's value from the provided buffer using the set count. */
        abstract void read(final ByteBuffer buffer)

        /** Writes this entry's values into the provided data buffer. */
        abstract void write(final ByteBuffer data)

        /** Writes the index entry into the provided buffer at the current position. */
        void index(final ByteBuffer index, final int position) {
            index.putInt(tag).putInt(getType()).putInt(position).putInt(count)
        }

        String toString() {
            StringBuilder builder = new StringBuilder()
            Map<Integer, Tag> knownTags = AbstractHeader.this.tags
            if (knownTags.containsKey(tag)) builder.append(knownTags.get(tag).getName())
            // what Object.toString() returns; a plain super.toString() call would not bind to Object here
            else builder.append(getClass().getName()).append('@').append(Integer.toHexString(hashCode()))
            builder.append('[tag=').append(tag)
            builder.append(',type=').append(getType())
            builder.append(',count=').append(count)
            builder.append(',size=').append(size())
            builder.append(',offset=').append(offset)
            builder.append(']')
            return builder.toString()
        }
    }

    class NullEntry extends AbstractEntry<Object> {
        int getType() { return Tag.NULL_ENTRY }

        int size() { return 0 }

        void read(final ByteBuffer buffer) {}

        void write(final ByteBuffer data) {}

        @Override
        protected void typeCheck(Object values) {}
    }

    class CharEntry extends AbstractEntry<byte[]> {
        int getType() { return Tag.CHAR_ENTRY }

        int size() { return count }

        void read(final ByteBuffer buffer) {
            byte[] values = new byte[count]
            for (int x = 0; x < count; x++) values[x] = buffer.get()
            setValues(values)
        }

        void write(final ByteBuffer data) {
            for (byte c : (byte[]) values) data.put(c)
        }

        String toString() {
            StringBuilder builder = new StringBuilder(super.toString())
            for (byte c : (byte[]) values) builder.append((int) c)
            builder.append('\n\t')
            return builder.toString()
        }

        @Override
        protected void typeCheck(Object values) { AbstractHeader.requireType(values, byte[].class) }
    }

    class Int8Entry extends AbstractEntry<byte[]> {
        int getType() { return Tag.INT8_ENTRY }

        int size() { return count }

        void read(final ByteBuffer buffer) {
            byte[] values = new byte[count]
            for (int x = 0; x < count; x++) values[x] = buffer.get()
            setValues(values)
        }

        void write(final ByteBuffer data) {
            for (byte b : (byte[]) values) data.put(b)
        }

        @Override
        protected void typeCheck(Object values) { AbstractHeader.requireType(values, byte[].class) }

        String toString() {
            StringBuilder builder = new StringBuilder(super.toString())
            builder.append('\n\t')
            for (byte b : (byte[]) values) builder.append((int) b).append(', ')
            return builder.toString()
        }
    }

    class Int16Entry extends AbstractEntry<short[]> {
        int getOffset(int offset) { return Util.round(offset, 1) }

        int getType() { return Tag.INT16_ENTRY }

        int size() { return count * Short.BYTES }

        void read(final ByteBuffer buffer) {
            short[] values = new short[count]
            for (int x = 0; x < count; x++) values[x] = buffer.getShort()
            setValues(values)
        }

        void write(final ByteBuffer data) {
            for (short s : (short[]) values) data.putShort(s)
        }

        String toString() {
            StringBuilder builder = new StringBuilder(super.toString())
            builder.append('\n\t')
            for (short s : (short[]) values) builder.append(s & 0xFFFF).append(', ')
            return builder.toString()
        }

        @Override
        protected void typeCheck(Object values) { AbstractHeader.requireType(values, short[].class) }
    }

    class Int32Entry extends AbstractEntry<int[]> {
        int getOffset(int offset) { return Util.round(offset, 3) }

        int getType() { return Tag.INT32_ENTRY }

        int size() { return count * Integer.BYTES }

        void read(final ByteBuffer buffer) {
            int[] values = new int[count]
            for (int x = 0; x < count; x++) values[x] = buffer.getInt()
            setValues(values)
        }

        void write(final ByteBuffer data) {
            for (int i : (int[]) values) data.putInt(i)
        }

        String toString() {
            StringBuilder builder = new StringBuilder(super.toString())
            builder.append('\n\t')
            for (int i : (int[]) values) builder.append(i).append(', ')
            return builder.toString()
        }

        @Override
        protected void typeCheck(Object values) { AbstractHeader.requireType(values, int[].class) }
    }

    class Int64Entry extends AbstractEntry<long[]> {
        int getOffset(int offset) { return Util.round(offset, 7) }

        int getType() { return Tag.INT64_ENTRY }

        int size() { return count * Long.BYTES }

        void read(final ByteBuffer buffer) {
            long[] values = new long[count]
            for (int x = 0; x < count; x++) values[x] = buffer.getLong()
            setValues(values)
        }

        void write(final ByteBuffer data) {
            for (long l : (long[]) values) data.putLong(l)
        }

        String toString() {
            StringBuilder builder = new StringBuilder(super.toString())
            builder.append('\n\t')
            for (long l : (long[]) values) builder.append(l).append(', ')
            return builder.toString()
        }

        @Override
        protected void typeCheck(Object values) { AbstractHeader.requireType(values, long[].class) }
    }

    /**
     * According to early documentation it should be illegal for this type of
     * entry to store more than one string value, but other recent documents
     * indicate that this may no longer be the case.
     */
    class StringEntry extends AbstractEntry<String[]> {
        int getType() { return Tag.STRING_ENTRY }

        int size() {
            if (size != 0) return size
            for (String s : (String[]) values) size += StandardCharsets.UTF_8.encode(s).remaining() + 1
            return size
        }

        void read(final ByteBuffer buffer) {
            String[] values = new String[count]
            for (int x = 0; x < count; x++) {
                int length = 0
                while (buffer.get(buffer.position() + length) != (byte) 0) length++

                final ByteBuffer slice = buffer.slice()
                buffer.position(buffer.position() + length + 1)
                slice.limit(length)
                values[x] = StandardCharsets.UTF_8.decode(slice).toString()
            }
            setValues(values)
        }

        void write(final ByteBuffer data) {
            for (String s : (String[]) values) data.put(StandardCharsets.UTF_8.encode(s)).put((byte) 0)
        }

        String toString() {
            StringBuilder builder = new StringBuilder(super.toString())
            if (values != null) {
                for (String s : (String[]) values) {
                    builder.append('\n\t')
                    builder.append(s)
                }
            }
            return builder.toString()
        }

        @Override
        protected void typeCheck(Object values) { AbstractHeader.requireType(values, String[].class) }
    }

    class BinEntry extends AbstractEntry<byte[]> {
        int getType() { return Tag.BIN_ENTRY }

        int size() { return count }

        void read(final ByteBuffer buffer) {
            byte[] values = new byte[count]
            buffer.get(values)
            setValues(values)
        }

        void write(final ByteBuffer data) {
            data.put((byte[]) values)
        }

        String toString() {
            StringBuilder builder = new StringBuilder(super.toString())
            if (values != null) {
                builder.append('\n')
                Util.dump((byte[]) values, builder)
            }
            return builder.toString()
        }

        @Override
        protected void typeCheck(Object values) { AbstractHeader.requireType(values, byte[].class) }
    }

    class StringArrayEntry extends StringEntry {
        int getType() { return Tag.STRING_ARRAY_ENTRY }
    }

    class I18NStringEntry extends StringEntry {
        int getType() { return Tag.I18NSTRING_ENTRY }
    }

    String toString() {
        StringBuilder builder = new StringBuilder()
        builder.append('Start Header ( ').append(getClass()).append(')').append('\n')
        int count = 0
        for (Integer tag : entries.keySet()) {
            builder.append(count++).append(': ').append(entries.get(tag)).append('\n')
        }
        return builder.toString()
    }
}
