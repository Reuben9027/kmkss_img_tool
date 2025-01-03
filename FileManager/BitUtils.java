package FileManager;

public class BitUtils {
    // Least Significant First
    // is this some sort of obfuscation????
    public static byte readBitmask(byte n, int start, int end) {
        return (byte) ((n >>> start) & ((0xFF) >>> (8 - ((1 + end) - start))));
    }

    public static short readBitmask(short n, int start, int end) {
        return (short) ((n >>> start) & ((0xFFFF) >>> (16 - ((1 + end) - start))));
    }

    public static int readBitmask(int n, int start, int end) {
        int k = ((0xffffffff) >>> (32 - ((1 + end) - start)));
        return (int) ((n >>> start) & k);
    }

    public static long readBitmask(long n, int start, int end) {
        return (long) ((n >>> start) & ((~0L) >>> (64 - ((1 + end) - start))));
    }
}
