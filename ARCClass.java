import java.io.IOException;
import java.util.ArrayList;

/**
 * Parses and decodes ARC container files used by the kmkss PS2 visual novel.
 *
 * <p>An ARC file is a flat, uncompressed archive. Its binary layout is:
 * <pre>
 *   [4 bytes] total file size      (little-endian)
 *   [4 bytes] number of entries N  (little-endian)
 *   [N * 4]   name address table   (little-endian offsets into name section)
 *   [N * 4]   data address table   (little-endian offsets into data section)
 *   [name section]  NUL-terminated Shift-JIS strings
 *   [data section]  raw SCF file payloads (no padding between entries)
 * </pre>
 *
 * <p>Call {@link #decode()} to parse the raw bytes supplied at construction
 * time. The returned list of {@link SCFClass} objects is in the same order as
 * the original ARC entry table.
 *
 * @see SCFClass
 * @see ByteManip
 */
public class ARCClass extends ByteManip {

    /** Decoded SCF entries, populated by {@link #decode()}. */
    ArrayList<SCFClass> scfList = new ArrayList<>();

    /**
     * Constructs an ARCClass from raw ARC file bytes.
     *
     * @param arr the complete bytes of the ARC file
     */
    public ARCClass(byte[] arr) {
        super(arr);
    }

    // -------------------------------------------------------------------------
    // Inner class — transient parsing state for one directory entry
    // -------------------------------------------------------------------------

    /**
     * Transient holder for the raw address/length data of a single ARC entry
     * before it is converted to a fully decoded {@link SCFClass}.
     *
     * <p>Name and data lengths are not stored explicitly in the ARC header;
     * they are inferred by comparing adjacent entry addresses via the two
     * {@code compareNext} overloads.
     */
    class SCFTemp {

        /** Byte offset of this entry's name string in the ARC name section. */
        int nameAddress;

        /** Number of bytes in the name string (excludes the NUL terminator). */
        int nameLen;

        /** Byte offset of this entry's SCF payload in the ARC data section. */
        int dataAddress;

        /** Number of bytes in the SCF payload. */
        int dataLen;

        /** Raw name bytes (Shift-JIS encoded). */
        byte[] name;

        /** Raw SCF payload bytes. */
        byte[] data;

        /**
         * Constructs an SCFTemp with the given name and data base addresses.
         *
         * @param nameAddress offset into the ARC name section
         * @param dataAddress offset into the ARC data section
         */
        public SCFTemp(int nameAddress, int dataAddress) {
            this.nameAddress = nameAddress;
            this.dataAddress = dataAddress;
        }

        /**
         * Converts this temporary holder into a fully constructed {@link SCFClass}.
         *
         * @return a new SCFClass initialised with this entry's data and name
         * @throws IOException if the name cannot be decoded as Shift-JIS
         */
        SCFClass convertToSCF() throws IOException {
            return new SCFClass(this.data, new String(name, "SHIFT-JIS"));
        }

        /**
         * Prints the Shift-JIS decoded name of this entry to standard output
         * (used for debug tracing during parsing).
         */
        void print() {
            try {
                System.out.println(new String(name, "SHIFT-JIS"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /** Reads {@link #nameLen} bytes from the ARC buffer into {@link #name}. */
        void getName() {
            this.name = construct(nameAddress, nameLen);
        }

        /** Reads {@link #dataLen} bytes from the ARC buffer into {@link #data}. */
        void getData() {
            this.data = construct(dataAddress, dataLen);
        }

        /**
         * Calculates name/data lengths for the <em>last</em> entry in the
         * table by comparing against the first data address and the total
         * array length (sentinel values supplied by the caller).
         *
         * @param nA sentinel address marking the end of the name section
         * @param dA sentinel address marking the end of the data section
         * @throws IOException if name/data cannot be read
         */
        public void compareNext(int nA, int dA) throws IOException {
            this.nameLen = nA - this.nameAddress - 1;
            this.dataLen = dA - this.dataAddress - 1;
            getName();
            getData();
        }

        /**
         * Calculates name/data lengths by comparing against the addresses of
         * the immediately following entry in the directory table.
         *
         * @param nextSCF the next entry in the directory
         * @throws IOException if name/data cannot be read
         */
        public void compareNext(SCFTemp nextSCF) throws IOException {
            this.nameLen = nextSCF.nameAddress - this.nameAddress - 1;
            this.dataLen = nextSCF.dataAddress - this.dataAddress;
            getName();
            getData();
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Decodes the ARC binary data provided at construction time.
     *
     * <p>The method walks the entry directory, resolves name/data lengths by
     * comparing consecutive address pairs, and converts each raw entry into
     * a {@link SCFClass} instance.
     *
     * @return an ordered list of decoded {@link SCFClass} objects corresponding
     *         to the entries in the ARC file
     */
    public ArrayList<SCFClass> decode() {
        ArrayList<SCFTemp> tempList = new ArrayList<>();

        // Header: total file size and entry count
        int fileSize   = readMultipleByteReverse(this.cursor, 4);
        cursorJump(4);
        int fileNumber = readMultipleByteReverse(this.cursor, 4);
        cursorJump(4);

        // Read the parallel name-address / data-address tables
        for (int i = 0; i < fileNumber; i++) {
            int tempNameAddress = readMultipleByteReverse(this.cursor, 4);
            int tempDataAddress = readMultipleByteReverse(this.cursor + (fileNumber * 4), 4);

            System.out.printf("%d: name=0x%s | data=0x%s%n",
                    i,
                    Integer.toHexString(tempNameAddress),
                    Integer.toHexString(tempDataAddress));

            SCFTemp scfTemp = new SCFTemp(tempNameAddress, tempDataAddress);

            // Fill lengths of the previous entry now that we have the next addresses
            try {
                tempList.get(tempList.size() - 1).compareNext(scfTemp);
            } catch (Exception ignored) {
                // First iteration: no previous entry to update
            }

            tempList.add(scfTemp);
            cursorJump(4);
        }

        // Finalise the last entry using the first data address as the name sentinel
        // and the total array length as the data sentinel
        try {
            tempList.get(tempList.size() - 1)
                    .compareNext(tempList.get(0).dataAddress, this.arr.length + 1);
        } catch (Exception ignored) {
            // Empty ARC or single-entry edge case
        }

        // Convert each SCFTemp into a proper SCFClass
        for (SCFTemp scfTemp : tempList) {
            try {
                this.scfList.add(scfTemp.convertToSCF());
            } catch (Exception ignored) {
                // Conversion errors are silently skipped to allow partial extraction
            }
        }

        return this.scfList;
    }
}
