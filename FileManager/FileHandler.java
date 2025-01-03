package FileManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.StringTokenizer;

interface InnerFilerHandler {

    byte readNextByte();

    public void setFileLength(int n);

    public void putByte(byte n);

    public void putByte(byte[] arr);

    byte[] readNBytes(int start, int length);

    byte[] readNBytes(int length);

    void encode(String path);

    void decode(String path);

    // TODO: add the undefined methods
}

class FileHandlerInstance extends FileHandler {

    FileHandlerInstance(String str) throws IOException {
        super(str);
    }

    FileHandlerInstance(int n) {
        super(n);
    }

    @Override
    public void encode(String path) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'encode'");
    }

    @Override
    public void decode(String path) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'decode'");
    }

}

public abstract class FileHandler implements InnerFilerHandler {
    private byte[] fileBytes;
    private int cursor = 0;
    private FileOutputStream outputStream = null;

    public FileHandler(String path) throws IOException {
        this.readBytes(path);
    }

    public FileHandler(int length) {
        setFileLength(length);
    }

    public static FileHandlerInstance getInstance(int n) {
        return new FileHandlerInstance(n);
    }

    public void setCursor(int n) {
        this.cursor = n;
    }

    public byte[] getFileBytes() {
        return this.fileBytes;
    }

    public int getCursor() {
        return this.cursor;
    }

    public FileOutputStream getOutputStream() {
        return this.outputStream;
    }

    // file methods
    public void setExport(String path) throws IOException {
        StringTokenizer strTok = new StringTokenizer(path, "/");
        String pathFolder = "";
        if (strTok.countTokens() != 1) {
            while (strTok.hasMoreTokens()) {
                pathFolder += strTok.nextToken() + "/";
                if (strTok.countTokens() == 1) {
                    break;
                }
            }
            File folder = new File(pathFolder);
            // folder.createNewFile();
            folder.mkdirs();
        }
        this.outputStream = new FileOutputStream(pathFolder + strTok.nextToken());
    }

    // WRITING bytes
    public void setFileLength(int n) {
        this.fileBytes = new byte[n];
    }

    public void putByteCyclic(byte n) {
        this.fileBytes[(this.cursor++)] = n;
        this.cursor %= this.fileBytes.length;
    }

    public void putByteCyclic(byte[] arr) {
        for (byte n : arr) {
            putByteCyclic(n);
        }
    }

    public void putByte(byte n) {
        this.fileBytes[(this.cursor++)] = n;
        // this.cursor %= this.fileBytes.length;
    }

    public void putByte(byte[] arr) {
        for (byte n : arr) {
            putByte(n);
        }
    }

    // READING bytes
    public static byte[] readArrNBytes(byte[] arr, int start, int length) {
        if (start + length > arr.length) {
            return null;
        }
        return Arrays.copyOfRange(arr, start, start + length);
    }

    final void readBytes(String pathString) throws IOException {
        Path path = Paths.get(pathString);
        fileBytes = Files.readAllBytes(path);
    }

    public byte readNextByte() {
        byte data = fileBytes[this.cursor++];
        // this.cursor %= this.fileBytes.length;
        return data;
    }

    public byte readNextByteCyclic() {
        byte data = fileBytes[this.cursor++];
        this.cursor %= this.fileBytes.length;
        return data;
    }

    public byte[] readNBytes(int start, int length) {
        return readArrNBytes(fileBytes, start, length);
    }

    public byte[] readNBytes(int length) {
        byte[] temp = this.readNBytes(this.cursor, length);
        this.cursor += length;
        return temp;
    }

    public byte[] readNBytesCycle(int start, int length) {
        byte[] temp = new byte[length];
        int end = start + length;
        for (int i = 0; i < length; i++) {
            int index = (start + i) % this.fileBytes.length;
            temp[i] = this.fileBytes[index];
        }
        return temp;
    }

    public byte[] readWriteNBytesCycle(int start, int length) {
        byte[] temp = new byte[length];
        int end = start + length;
        for (int i = 0; i < length; i++) {
            int index = (start + i) % this.fileBytes.length;
            temp[i] = this.fileBytes[index];
            this.putByteCyclic(temp[i]);
        }
        return temp;
    }

}
