import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;

/**
 * Base utility class providing bit-manipulation helpers.
 *
 * <p>This layer exists solely for helper methods that do not depend on
 * the byte-array state carried by {@link ByteManip}.
 */
abstract class ByteTool {

    /**
     * Reverses the bit order of an integer up to its highest set bit.
     *
     * <p>Example: {@code bitReverse(0b1011)} returns {@code 0b101} (3-bit
     * reversal of the 4-bit value â€? note that only {@code bitCount(n)} bits
     * are iterated).
     *
     * @param n the value whose bits should be reversed
     * @return the bit-reversed result
     */
    int bitReverse(int n) {
        int temp  = 0;
        int count = Integer.bitCount(n);
        for (int i = 0; i < count; i++) {
            temp += (n >> (count - i)) & 1;
        }
        return temp;
    }
}

/**
 * Core byte-array reader used by all file-format classes.
 *
 * <p>Wraps a raw {@code byte[]} together with a mutable {@link #cursor}
 * position and exposes typed read methods. All format-specific classes
 * ({@link ARCClass}, {@link SCFClass}, {@link PACClass}) extend this class
 * and use its helpers to walk through binary data without managing offsets
 * manually.
 *
 * <h2>Cursor semantics</h2>
 * <ul>
 *   <li>{@link #cursorJump(int)} advances the cursor by {@code i} bytes and
 *       returns the <em>new</em> cursor position.</li>
 *   <li>{@link #cursorGetByte()} reads the byte at the <em>current</em> cursor
 *       position as an unsigned int without moving the cursor.</li>
 *   <li>{@link #jumpGetByte(int)} is a combined advance-then-read convenience.</li>
 * </ul>
 */
public abstract class ByteManip extends ByteTool {

    /** The raw binary data being parsed. */
    byte[] arr;

    /** Current read position within {@link #arr}. */
    int cursor = 0;

    /**
     * Constructs a {@code ByteManip} wrapping the supplied byte array.
     *
     * @param arr the raw binary data; may be {@code null} for dummy instances
     *            used only to call static-like helpers such as
     *            {@link SCFClass#buildArc(java.io.File[])}
     */
    public ByteManip(byte[] arr) {
        this.arr = arr;
    }

    /**
     * Copies {@code end} bytes starting at offset {@code o} from {@link #arr}
     * into {@code arrDes}, starting at index 0 of {@code arrDes}.
     *
     * @param arrDes destination buffer; must have length >= {@code end}
     * @param o      source start offset in {@link #arr}
     * @param end    number of bytes to copy
     */
    void copy(byte[] arrDes, int o, int end) {
        int j = 0;
        for (int i = o; i < (o + end); i++) {
            arrDes[j] = this.arr[i];
            j++;
        }
    }

    /**
     * Reads {@code size} bytes from {@link #arr} at offset {@code loc} and
     * interprets them as a little-endian (reversed) unsigned integer.
     *
     * <p>For example, the bytes {@code [0x01, 0x00, 0x00, 0x00]} at {@code loc}
     * with {@code size=4} return {@code 1}.
     *
     * @param loc  starting byte offset in {@link #arr}
     * @param size number of bytes to read (1??4)
     * @return the decoded unsigned integer value
     */
    int readMultipleByteReverse(int loc, int size) {
        String temp = "";
        for (int i = 0; i < size; i++) {
            int    tempInt = Byte.toUnsignedInt(this.arr[i + loc]);
            String tempStr = Integer.toHexString(tempInt);
            if (tempStr.length() <= 1) {
                tempStr = "0" + tempStr;
            }
            temp = tempStr + temp;
        }
        return HexFormat.fromHexDigits(temp);
    }

    /**
     * Reads {@code size} bytes from {@link #arr} at offset {@code loc} and
     * interprets them as a big-endian unsigned integer.
     *
     * @param loc  starting byte offset in {@link #arr}
     * @param size number of bytes to read
     * @return the decoded unsigned integer value
     */
    int readMultipleByte(int loc, int size) {
        int temp = 0;
        for (int i = 0; i < size; i++) {
            temp += (temp << i * 8) + this.arr[loc + i];
        }
        return temp;
    }

    /**
     * Advances the cursor by {@code i} bytes, then returns the byte at the
     * new cursor position as an unsigned int.
     *
     * @param i number of bytes to advance before reading
     * @return unsigned value of {@code arr[cursor]} after the jump
     */
    int jumpGetByte(int i) {
        cursorJump(i);
        return cursorGetByte();
    }

    /**
     * Returns the byte at the current cursor position as an unsigned int
     * without moving the cursor.
     *
     * @return unsigned value of {@code arr[cursor]}
     */
    int cursorGetByte() {
        return Byte.toUnsignedInt(this.arr[this.cursor]);
    }

    /**
     * Advances the cursor by {@code i} bytes and returns the new position.
     *
     * @param i number of bytes to advance
     * @return updated cursor position
     */
    int cursorJump(int i) {
        this.cursor += i;
        return this.cursor;
    }


    /**
     * @return returns the current location of the cursor
     */
    int getCursorLocation(){
        return this.cursor;
    }

    /**
     * Allocates a new byte array of length {@code y} and copies {@code y} bytes
     * from {@link #arr} starting at offset {@code x} into it.
     *
     * @param x source start offset
     * @param y number of bytes to copy (also the length of the returned array)
     * @return a freshly allocated byte array containing the copied bytes
     */
    byte[] construct(int x, int y) {
        byte[] temp = new byte[y];
        copy(temp, x, y);
        return temp;
    }


    static byte[] writeIntLE(int n){
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(n);

        byte[] bytes = buffer.array();
        return bytes;
    }

    static byte[] writeShortLE(short n){
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort(n);

        byte[] bytes = buffer.array();
        return bytes;
    }


}
