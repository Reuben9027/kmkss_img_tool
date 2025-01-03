package FileManager;

import java.nio.ByteBuffer;

public class ByteUtils {
    public static byte[] longToBytes(long x) {
        byte[] temp = new byte[Long.BYTES];

        for (int i = 0; i < temp.length; i++) {
            temp[i] = (byte) ((x >>> (i * Byte.SIZE)) & 0xff);
        }
        return temp;
    }

    public static long bytesToLong(byte[] bytes) {
        long temp = 0;
        for (int i = 0; i < bytes.length; i++) {
            temp |= (bytes[i] & 0xff) << (i * Byte.SIZE);
        }
        return temp;
    }

    public static byte[] shortToBytes(short x) {
        byte[] temp = new byte[Short.BYTES];

        for (int i = 0; i < temp.length; i++) {
            temp[i] = (byte) ((x >>> (i * Byte.SIZE)) & 0xff);
        }
        return temp;
    }

    public static short bytesToShort(byte[] bytes) {
        short temp = 0;
        for (int i = 0; i < bytes.length; i++) {
            temp |= (bytes[i] & 0xff) << (i * Byte.SIZE);
        }
        return temp;
    }

    public static byte[] intToBytes(int x) {
        byte[] temp = new byte[Integer.BYTES];
        for (int i = 0; i < temp.length; i++) {
            temp[i] = (byte) ((x >>> (i * Byte.SIZE)) & 0xff);
        }
        return temp;
    }

    public static int bytesToInt(byte[] bytes) {
        int temp = 0;
        for (int i = 0; i < bytes.length; i++) {
            int debug = (bytes[i] & 0xff) << (i * Byte.SIZE);
            temp |= debug;
        }
        return temp;
    }

}
