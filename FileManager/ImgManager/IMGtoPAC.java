package FileManager.ImgManager;

import java.io.IOException;
import java.util.Arrays;

import FileManager.BitUtils;
import FileManager.ByteUtils;
import FileManager.FileHandler;

public class IMGtoPAC extends IMGClass {
    FileHandler buffer = FileHandler.getInstance(256 * 8);

    public IMGtoPAC(String path) throws IOException {
        super(path);
        this.buffer.setCursor(0x7de);
    }

    @Override
    public void encode(String path) {
        throw new UnsupportedOperationException("Unimplemented method 'encode'");
    }

    @Override
    public void decode(String path) {
        int cursorCounter = 0;
        int length = 0;

        // Arrays.fill(this.buffer.getFileBytes(), (byte) 0);
        try {
            this.setExport(path);
        } catch (Exception e) {
        }

        try {
            // add outputPath
            length = ByteUtils.bytesToInt(this.readNBytes(4));
            cursorCounter = 4;
            // this.getOutputStream().write(ByteUtils.intToBytes(length));

            while (true) {
                byte blochHeader = this.readNextByte();
                for (int i = 0; i < 8; i++) {
                    if (this.getCursor() == 2752) {
                        System.out.println("jj");
                    }

                    boolean isReference = BitUtils.readBitmask(blochHeader, i, i) == 0;
                    if (isReference) {
                        short dataHeader = ByteUtils.bytesToShort(this.readNBytes(2));

                        int offset = BitUtils.readBitmask(dataHeader, 0, 7);
                        int size = BitUtils.readBitmask(dataHeader, 8, 12) + 3;
                        int sectionId = BitUtils.readBitmask(dataHeader, 13, 15);

                        int index = (sectionId * 256) + offset;

                        byte[] data = this.buffer.readWriteNBytesCycle(index, size);

                        // this.buffer.putByte(data);
                        this.getOutputStream().write(data);// putdata to outputfile

                        cursorCounter += size;
                        continue;
                    }

                    byte data = this.readNextByte();
                    this.buffer.putByteCyclic(data);
                    this.getOutputStream().write(data);
                    cursorCounter += 1;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("END");
            System.err.println();
        }

        System.out.println(cursorCounter + " " + length);
        try {
            this.getOutputStream().flush();
            this.getOutputStream().close();
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

}
