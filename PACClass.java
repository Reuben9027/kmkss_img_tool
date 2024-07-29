import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class PACClass extends ByteManip {

    public PACClass(byte[] arr) {
        super(arr);
    }

    static int toHBlockHeaer(int n, int size) {
        int sid = n / 256;
        int offset = n % 256;

        int r = sid << 13;
        r += offset;
        r += size << 8;
        return r;
    }

    class BufferEntry {
        int cursor = 0;
        boolean[] hash = new boolean[256 * 8];
        ArrayList<Integer> memList = new ArrayList<>();

        int next() {
            cursor++;
            cursor %= memList.size();
            return memList.get(cursor);
        }

        void fillZero(int num) {
            for (int i = 0; i < num; i++) {
                memList.add((Integer) i);
                hash[i] = true;
            }
        }

        public BufferEntry() {
        }
    }

    class FindReference {
        byte[] buffer;
        BufferEntry[] bufferEntries;

        void set(int index, byte[] value, int len) {
            for (int i = 0; i < len; i++) {
                set((index + i) % buffer.length, value[i]);
            }
        }

        void set(int index, byte value) {
            int oldValue = Byte.toUnsignedInt(buffer[index]);
            bufferEntries[oldValue].memList.remove((Integer) index);
            bufferEntries[oldValue].hash[index] = false;

            int newValue = Byte.toUnsignedInt(value);
            bufferEntries[newValue].memList.add((Integer) index);
            bufferEntries[newValue].hash[index] = true;
            buffer[index] = value;
        }

        int find(byte[] sequence, int startLine) {
            BufferEntry first = bufferEntries[Byte.toUnsignedInt(sequence[0])];
            if (first.memList.size() == 0) {
                return -1;
            }
            first.cursor = -1;
            int ref = first.next();
            int ref2 = ref;
            do {
                int t = 1;
                for (int i = 1; i < sequence.length; i++) {
                    int p = (ref2 + i) % buffer.length;
                    if (startLine == p) {
                        continue;
                    }
                    boolean value = bufferEntries[Byte.toUnsignedInt(sequence[i])].hash[p];
                    if (value) {
                        t++;
                        continue;
                    }
                    break;
                }
                if (t == sequence.length) {
                    first.cursor--;
                    return ref2;
                }

                ref2 = first.next();
            } while (ref != ref2);

            return -1;
        }

        public FindReference(int bufferSize) {
            buffer = new byte[bufferSize];
            bufferEntries = new BufferEntry[256];
            for (int i = 0; i < 256; i++) {
                bufferEntries[i] = new BufferEntry();
            }

            bufferEntries[0].fillZero(bufferSize);
        }
    }

    class ReferenceBlock {
        byte[] array = new byte[34];
        int size = 0;
        int dataLen = 0;
        int dataHeader = 0;

        byte[] getByLength() {
            return Arrays.copyOfRange(this.array, 0, size);
        }

        void clear() {
            for (int i = 0; i < 34; i++) {
                this.array[i] = 0;
            }
            this.size = 0;
            this.dataLen = 0;
            this.dataHeader = 0;
        }

        void add(int num, boolean t) {
            int numInt = num;

            dataLen++;
            if (!t) {

                array[size] = (byte) num;
                size++;
                dataHeader += 1 << dataLen - 1;
                return;
            }
            array[size] = (byte) (numInt % 0x100);
            array[size + 1] = (byte) (numInt >> 8);
            // System.out.println((numInt % 0x100) + " " + (numInt >> 8));
            size += 2;
        }

        public ReferenceBlock() {
            array[0] = -1;
        }

    }

    public void encode() throws IOException {
        FileOutputStream outputStream = new FileOutputStream("ARC_to_IMG.IMG");
        FindReference buffer = new FindReference(256 * 8);
        int bufStart = 0x7de;
        ReferenceBlock outputBlock = new ReferenceBlock();

        byte[] unpackedData = Arrays.copyOfRange(this.arr, this.cursor, 4);
        outputStream.write(unpackedData);

        int len = 3;
        int oldRef = 0;
        while (this.cursor < this.arr.length) {
            byte[] value = Arrays.copyOfRange(this.arr, this.cursor, this.cursor + len);
            int ref = buffer.find(value, bufStart);

            if (ref != -1 && len <= 34) {
                oldRef = ref;
                len++;
                continue;
            } else if (len != 3) {
                outputBlock.add(toHBlockHeaer(oldRef, value.length - 4), true);

                for (int i = 0; i < value.length - 1; i++) {
                    buffer.set(bufStart, value[i]);
                    bufStart += 1;
                    bufStart %= 256 * 8;
                }

                oldRef = 0;
                len = 3;
                cursorJump(value.length - 1);
            } else {
                outputBlock.add(value[0], false);
                buffer.set(bufStart, value[0]);
                bufStart++;
                bufStart %= 256 * 8;
                cursorJump(1);
            }

            if (outputBlock.dataLen >= 8) {
                outputStream.write(outputBlock.dataHeader);
                outputStream.write(outputBlock.getByLength());
                outputBlock.clear();
            }
        }
        outputStream.write(outputBlock.dataHeader);
        outputStream.write(outputBlock.getByLength());
        outputBlock.clear();

        outputStream.close();
    }

    public void decode() throws IOException {
        FileOutputStream outputStream = new FileOutputStream("ING_to_ARC.ARC");

        byte buffer[] = new byte[256 * 8];
        int bufStart = 0x7de;
        // int unpackedData = readMultipleByteReverse(this.cursor, 4);
        cursorJump(4);
        try {

            while (this.cursor < arr.length) {
                int blockHeader = cursorGetInt();
                cursorJump(1);
                // System.out.println(arr.length + " / " + this.cursor);

                for (int i = 0; i < 8; i++) {
                    int bitValue = (blockHeader >> i) & 1;

                    if (bitValue == 0) {
                        int ref = readMultipleByteReverse(this.cursor, 2);

                        int offset = (ref % (1 << 8));
                        int size = ((ref >> 8) % (1 << 5)) + 3;
                        int sid = (ref >> 13);

                        int refCursor = (sid * 256) + offset;

                        for (int o = 0; o < size; o++) {
                            byte value = buffer[(refCursor + o) % buffer.length];

                            outputStream.write(value);

                            buffer[bufStart] = value;
                            bufStart = (bufStart + 1) % (256 * 8);
                        }

                        cursorJump(2);
                    } else {
                        byte value = this.arr[this.cursor];
                        outputStream.write(value);

                        buffer[bufStart] = value;
                        bufStart = (bufStart + 1) % (256 * 8);

                        cursorJump(1);
                    }

                }
            }
        } catch (Exception e) {
            System.err.println(bufStart);
            e.printStackTrace();
        }
        outputStream.flush();
        outputStream.close();

    }

}
