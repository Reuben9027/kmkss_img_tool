import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Handles decoding (IMG/PAC → ARC) and encoding (ARC → IMG) using a custom
 * LZSS-variant compression scheme found in kmkss PS2 image containers.
 *
 * <h2>Compression format overview</h2>
 * <p>Compressed data is divided into 8-entry blocks. Each block starts with
 * a 1-byte header whose bits indicate whether the corresponding data unit is
 * a literal byte ({@code bit=1}) or a back-reference ({@code bit=0}):
 * <ul>
 *   <li><b>Literal</b> — 1 byte of raw data copied directly to output.</li>
 *   <li><b>Back-reference</b> — 2-byte token encoding a sliding-window offset
 *       and match length. The 2048-byte (256×8) ring buffer is pre-filled with
 *       zero bytes; the window start is {@code 0x7DE}.</li>
 * </ul>
 *
 * <p>The 2-byte reference token layout (little-endian word):
 * <pre>
 *   bits [7:0]   → buffer offset within the 256-byte page (low byte)
 *   bits [12:8]  → match length − 3 (5 bits)
 *   bits [15:13] → page selector (3 bits → 256*sid + offset = buffer index)
 * </pre>
 *
 * @see ARCClass
 * @see ByteManip
 */
public class PACClass extends ByteManip {

    /**
     * Constructs a PACClass wrapping the raw IMG/PAC or ARC bytes.
     *
     * @param arr the complete binary content of the source file
     */
    public PACClass(byte[] arr) {
        super(arr);
    }

    // -------------------------------------------------------------------------
    // Static helper — token encoding
    // -------------------------------------------------------------------------

    /**
     * Encodes a sliding-window back-reference into the 2-byte token format.
     *
     * @param n    buffer index of the match start (0 – 2047)
     * @param size match length − 3 (0 – 31); stored in bits 12:8 of the token
     * @return the packed 16-bit token value
     */
    static int toHBlockHeader(int n, int size) {
        int sid    = n / 256;
        int offset = n % 256;
        int r = sid << 13;
        r += offset;
        r += size << 8;
        return r;
    }

    // -------------------------------------------------------------------------
    // Inner class — hash-assisted sliding-window search
    // -------------------------------------------------------------------------

    /**
     * Maintains a per-byte-value index over the sliding window to allow
     * fast candidate lookup during encoding.
     *
     * <p>Each {@code BufferEntry} tracks which window positions currently
     * hold a given byte value via a {@code boolean[]} presence array and an
     * ordered {@code ArrayList} of positions (used for round-robin iteration).
     */
    class BufferEntry {
        int cursor = 0;

        /** {@code hash[i] == true} iff window position {@code i} holds this value. */
        boolean[] hash = new boolean[256 * 8];

        /** Ordered list of window positions holding this byte value. */
        ArrayList<Integer> memList = new ArrayList<>();

        /**
         * Advances the round-robin cursor and returns the next candidate position.
         *
         * @return the next window position for this byte value
         */
        int next() {
            cursor++;
            cursor %= memList.size();
            return memList.get(cursor);
        }

        /**
         * Pre-fills the first {@code num} window positions with value 0x00.
         * Called once during buffer initialisation to set up the zero-filled
         * sliding window region.
         *
         * @param num number of positions to mark as containing 0x00
         */
        void fillZero(int num) {
            for (int i = 0; i < num; i++) {
                memList.add(i);
                hash[i] = true;
            }
        }

        public BufferEntry() {}
    }

    // -------------------------------------------------------------------------
    // Inner class — sliding window with O(1) lookup
    // -------------------------------------------------------------------------

    /**
     * A fixed-size sliding window backed by a per-byte-value index for fast
     * match searching during encoding.
     *
     * <p>When a position in the buffer is overwritten, the old byte's index is
     * updated to remove that position and the new byte's index gains it,
     * keeping all lookups O(candidates) rather than O(bufferSize).
     */
    class FindReference {

        /** The actual ring-buffer bytes. */
        byte[] buffer;

        /** Per-byte-value index; {@code bufferEntries[v]} lists positions holding value {@code v}. */
        BufferEntry[] bufferEntries;

        /**
         * Writes {@code len} bytes from {@code value} into the buffer starting
         * at {@code index}, wrapping around via modulo.
         *
         * @param index  start position in the ring buffer
         * @param value  source bytes
         * @param len    number of bytes to write
         */
        void set(int index, byte[] value, int len) {
            for (int i = 0; i < len; i++) {
                set((index + i) % buffer.length, value[i]);
            }
        }

        /**
         * Writes a single byte into the buffer at {@code index}, updating the
         * per-value index to reflect the change.
         *
         * @param index buffer position to update
         * @param value new byte value
         */
        void set(int index, byte value) {
            int oldValue = Byte.toUnsignedInt(buffer[index]);
            bufferEntries[oldValue].memList.remove((Integer) index);
            bufferEntries[oldValue].hash[index] = false;

            int newValue = Byte.toUnsignedInt(value);
            bufferEntries[newValue].memList.add(index);
            bufferEntries[newValue].hash[index] = true;
            buffer[index] = value;
        }

        /**
         * Searches the sliding window for the longest occurrence of
         * {@code sequence} starting at any position other than {@code startLine}.
         *
         * @param sequence  the byte sequence to search for
         * @param startLine the current write head (must not be used as a match start)
         * @return the buffer index where the match starts, or {@code -1} if not found
         */
        int find(byte[] sequence, int startLine) {
            BufferEntry first = bufferEntries[Byte.toUnsignedInt(sequence[0])];
            if (first.memList.isEmpty()) return -1;

            first.cursor = -1;
            int ref  = first.next();
            int ref2 = ref;

            do {
                int matchLen = 1;
                for (int i = 1; i < sequence.length; i++) {
                    int p = (ref2 + i) % buffer.length;
                    if (startLine == p) continue; // skip write-head position
                    if (bufferEntries[Byte.toUnsignedInt(sequence[i])].hash[p]) {
                        matchLen++;
                    } else {
                        break;
                    }
                }
                if (matchLen == sequence.length) {
                    first.cursor--;
                    return ref2;
                }
                ref2 = first.next();
            } while (ref != ref2);

            return -1;
        }

        /**
         * Initialises a {@code FindReference} with a zero-filled buffer of
         * the given size and builds the initial per-value index.
         *
         * @param bufferSize size of the ring buffer in bytes (typically 256 * 8)
         */
        public FindReference(int bufferSize) {
            buffer        = new byte[bufferSize];
            bufferEntries = new BufferEntry[256];
            for (int i = 0; i < 256; i++) {
                bufferEntries[i] = new BufferEntry();
            }
            // All positions start with value 0x00
            bufferEntries[0].fillZero(bufferSize);
        }
    }

    // -------------------------------------------------------------------------
    // Inner class — 8-entry output block
    // -------------------------------------------------------------------------

    /**
     * Accumulates up to 8 literal/reference data units together with their
     * 1-byte flag header before flushing to the output stream.
     *
     * <p>Each call to {@link #add(int, boolean)} appends one unit:
     * <ul>
     *   <li>{@code t=false} — literal byte, 1 byte of payload, flag bit set to 1.</li>
     *   <li>{@code t=true}  — back-reference, 2 bytes of payload, flag bit set to 0.</li>
     * </ul>
     * The flag header is built LSB-first; bit {@code i} corresponds to the
     * {@code i}-th unit added.
     */
    class ReferenceBlock {
        /** Raw payload bytes (up to 34 = 8 refs × 2 bytes + alignment). */
        byte[] array = new byte[34];

        /** Current write position in {@link #array}. */
        int size = 0;

        /** Number of data units added so far (max 8). */
        int dataLen = 0;

        /** Accumulated flag byte: bit {@code i}=1 means unit {@code i} is a literal. */
        int dataHeader = 0;

        /** Returns a copy of {@link #array} trimmed to the bytes actually written. */
        byte[] getByLength() {
            return Arrays.copyOfRange(this.array, 0, size);
        }

        /** Resets all fields so this block can be reused for the next 8-unit group. */
        void clear() {
            for (int i = 0; i < 34; i++) this.array[i] = 0;
            this.size       = 0;
            this.dataLen    = 0;
            this.dataHeader = 0;
        }

        /**
         * Appends a data unit to this block.
         *
         * @param num the data value: a raw byte for literals, a packed 2-byte
         *            reference token for back-references
         * @param t   {@code true} if this is a back-reference (2-byte token),
         *            {@code false} for a literal byte
         */
        void add(int num, boolean t) {
            dataLen++;
            if (!t) {
                // Literal: 1 byte payload, set corresponding flag bit
                array[size] = (byte) num;
                size++;
                dataHeader += 1 << (dataLen - 1);
                return;
            }
            // Back-reference: 2-byte little-endian token, flag bit stays 0
            array[size]     = (byte) (num % 0x100);
            array[size + 1] = (byte) (num >> 8);
            size += 2;
        }

        /** Initialises the block with a placeholder header byte. */
        public ReferenceBlock() {
            array[0] = -1;
        }
    }

    // -------------------------------------------------------------------------
    // Public encode / decode
    // -------------------------------------------------------------------------

    /**
     * Encodes (compresses) the ARC data in {@link #arr} into a compressed IMG
     * file using the custom LZSS-variant scheme.
     *
     * <p>The first 4 bytes of the input are treated as an uncompressed header
     * and are written verbatim. The remainder is compressed block by block.
     * Output is written to {@code ARC_to_IMG.IMG} in the working directory.
     *
     * @throws IOException if the output file cannot be created or written
     */
    public void encode() throws IOException {
        FileOutputStream outputStream = new FileOutputStream("ARC_to_IMG.IMG");
        FindReference    buffer       = new FindReference(256 * 8);
        int bufStart = 0x7de;

        ReferenceBlock outputBlock = new ReferenceBlock();

        // Write the 4-byte uncompressed header verbatim
        byte[] header = Arrays.copyOfRange(this.arr, this.cursor, 4);
        outputStream.write(header);

        int len    = 3;  // minimum match length
        int oldRef = 0;

        while (this.cursor < this.arr.length) {
            byte[] value = Arrays.copyOfRange(this.arr, this.cursor, this.cursor + len);
            int ref = buffer.find(value, bufStart);

            if (ref != -1 && len <= 34) {
                // Match extended — keep trying a longer sequence
                oldRef = ref;
                len++;
                continue;
            } else if (len != 3) {
                // Longest match found — emit reference token
                outputBlock.add(toHBlockHeader(oldRef, value.length - 4), true);
                for (int i = 0; i < value.length - 1; i++) {
                    buffer.set(bufStart, value[i]);
                    bufStart = (bufStart + 1) % (256 * 8);
                }
                oldRef = 0;
                len    = 3;
                cursorJump(value.length - 1);
            } else {
                // No match — emit literal
                outputBlock.add(value[0], false);
                buffer.set(bufStart, value[0]);
                bufStart = (bufStart + 1) % (256 * 8);
                cursorJump(1);
            }

            if (outputBlock.dataLen >= 8) {
                outputStream.write(outputBlock.dataHeader);
                outputStream.write(outputBlock.getByLength());
                outputBlock.clear();
            }
        }

        // Flush the final (possibly partial) block
        outputStream.write(outputBlock.dataHeader);
        outputStream.write(outputBlock.getByLength());
        outputBlock.clear();
        outputStream.close();
    }

    /**
     * Decodes (decompresses) the IMG/PAC data in {@link #arr} into a raw ARC
     * file using the custom LZSS-variant scheme.
     *
     * <p>The first 4 bytes of the input are skipped (uncompressed header).
     * Compressed blocks are processed until the end of input. Output is written
     * to {@code ING_to_ARC.ARC} in the working directory.
     *
     * @throws IOException if the output file cannot be created or written
     */
    public void decode() throws IOException {
        FileOutputStream outputStream = new FileOutputStream("ING_to_ARC.ARC");
        byte[] buffer = new byte[256 * 8];
        int bufStart  = 0x7de;

        cursorJump(4); // skip 4-byte uncompressed header

        try {
            while (this.cursor < arr.length) {
                int blockHeader = cursorGetInt();
                cursorJump(1);

                for (int i = 0; i < 8; i++) {
                    int bitValue = (blockHeader >> i) & 1;

                    if (bitValue == 0) {
                        // Back-reference: 2-byte token
                        int ref    = readMultipleByteReverse(this.cursor, 2);
                        int offset = ref % (1 << 8);
                        int size   = ((ref >> 8) % (1 << 5)) + 3;
                        int sid    = ref >> 13;
                        int refCursor = (sid * 256) + offset;

                        for (int o = 0; o < size; o++) {
                            byte value = buffer[(refCursor + o) % buffer.length];
                            outputStream.write(value);
                            buffer[bufStart] = value;
                            bufStart = (bufStart + 1) % (256 * 8);
                        }
                        cursorJump(2);
                    } else {
                        // Literal byte
                        byte value = this.arr[this.cursor];
                        outputStream.write(value);
                        buffer[bufStart] = value;
                        bufStart = (bufStart + 1) % (256 * 8);
                        cursorJump(1);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Decode error at bufStart=" + bufStart);
            e.printStackTrace();
        }

        outputStream.flush();
        outputStream.close();
    }
}
