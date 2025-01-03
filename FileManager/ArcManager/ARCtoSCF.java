package FileManager.ArcManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Queue;

import FileManager.ByteUtils;
import FileManager.FileHandler;

class SCF {
    String name;

    int nameReference = -1;
    int dataReference = -1;

    FileHandler fileHandler = FileHandler.getInstance(0);

    SCF(int nR, int dR) {
        this.nameReference = nR;
        this.dataReference = dR;
    }

    void setExport(String path) throws IOException {
        this.fileHandler.setExport(path + this.name + ".scf");
    }

    FileOutputStream getOutputStream() {
        return fileHandler.getOutputStream();
    }

}

public class ARCtoSCF extends FileHandler {

    public ARCtoSCF(String str) throws IOException {
        super(str);
    }

    byte[] getBytesUntil(int start, int size, int delim) {
        byte[] arr = new byte[size];
        int i = 0;
        this.setCursor(start);
        while ((arr[i++] = this.readNextByte()) != 0) {
        }
        return arr;
    }

    @Override
    public void encode(String path) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'encode'");
    }

    @Override
    public void decode(String path) {
        int length = ByteUtils.bytesToInt(this.readNBytes(4));
        int totalFiles = ByteUtils.bytesToInt(this.readNBytes(4));
        File folderPath = new File(path);

        Deque<SCF> fileDeque = new ArrayDeque<SCF>();
        for (int i = 0; i < totalFiles; i++) {
            int dataReference = ByteUtils.bytesToInt(this.readNBytes(this.getCursor() + (totalFiles * 4), 4));
            int nameReference = ByteUtils.bytesToInt(this.readNBytes(4));
            fileDeque.add(new SCF(nameReference, dataReference));
        }

        SCF head = fileDeque.peekFirst();

        for (int i = 0; i < totalFiles; i++) {
            SCF currentSCF = fileDeque.poll();
            SCF nextSCF = fileDeque.peekFirst();

            int nameStart = currentSCF.nameReference;
            int nameEnd = head.dataReference;

            int dataStart = currentSCF.dataReference;
            int dataEnd = length;

            if (nextSCF != null) {
                nameStart = currentSCF.nameReference;
                nameEnd = nextSCF.nameReference;

                dataStart = currentSCF.dataReference;
                dataEnd = nextSCF.dataReference;
            }

            int nameLength = nameEnd - nameStart;
            int dataLength = dataEnd - dataStart;

            currentSCF.name = new String(this.readNBytes(nameStart, nameLength - 1));
            byte[] data = this.readNBytes(dataStart, dataLength);
            try {
                currentSCF.setExport(path);
                currentSCF.getOutputStream().write(data);
                currentSCF.getOutputStream().flush();
                currentSCF.getOutputStream().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
