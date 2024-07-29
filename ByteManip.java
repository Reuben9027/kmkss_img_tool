
import java.util.HexFormat;

abstract class ByteTool {
    int bitReverse(int n) {
        int temp = 0;
        int count = Integer.bitCount(n);

        for (int i = 0; i < count; i++) {
            temp += (n >> (count - i)) & 1;
        }
        return temp;
    }

}

public abstract class ByteManip extends ByteTool {
    byte[] arr;
    int cursor = 0;

    public ByteManip(byte[] arr) {
        this.arr = arr;
    }

    void copy(byte[] arrDes, int o, int end) {
        // arrDes = Arrays.copyOfRange(this.arr, o, o)

        int j = 0;
        for (int i = o; i < (o + end); i++) {
            arrDes[j] = this.arr[i];
            j++;
        }
    }

    int readMultipleByteReverse(int loc, int size) {
        String temp = "";
        for (int i = 0; i < size; i++) {
            int tempInt = Byte.toUnsignedInt(this.arr[i + loc]);
            String tempStr = Integer.toHexString(tempInt);
            if (tempStr.length() <= 1) {
                tempStr = "0" + tempStr;
            }

            temp = tempStr + temp;
        }
        int result = HexFormat.fromHexDigits(temp);
        return result;
    }

    int readMultipleByte(int loc, int size) {
        int temp = 0;
        for (int i = 0; i < size; i++) {
            temp += (temp << i * 8) + this.arr[loc + i];
        }
        return temp;
    }

    int jumpGet(int i) {
        cursorJump(i);
        return cursorGetInt();
    }

    int cursorGetInt() {
        return Byte.toUnsignedInt(this.arr[this.cursor]);
    }

    int cursorJump(int i) {
        this.cursor += i;
        return this.cursor;
    }

    byte[] construct(int x, int y) {
        byte[] temp = new byte[y];
        copy(temp, x, y);
        return temp;
    }

}