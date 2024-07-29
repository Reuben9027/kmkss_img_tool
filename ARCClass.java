import java.io.IOException;
import java.util.ArrayList;

public class ARCClass extends ByteManip {
    ArrayList<SCFClass> scfList = new ArrayList<>();

    public ARCClass(byte[] arr) {
        super(arr);
    }

    class SCFTemp {
        int nameAddress;
        int nameLen;

        int dataAddress;
        int dataLen;

        byte[] name;
        byte[] data;

        SCFClass convertToSCF() throws IOException {
            SCFClass scf = new SCFClass(this.data, new String(name, "SHIFT-JIS"));
            return scf;
        }

        void print() {
            try {

                System.out.println(new String(name, "SHIFT-JIS"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void getName() {
            this.name = construct(nameAddress, nameLen);
        }

        void getData() {
            this.data = construct(dataAddress, dataLen);
            // System.out.println(this.data.length);
        }

        public void compareNext(int nA, int dA) throws IOException {
            this.nameLen = nA - this.nameAddress - 1;
            this.dataLen = dA - this.dataAddress - 1;
            getName();
            getData();
        }

        public void compareNext(SCFTemp nextSCF) throws IOException {
            this.nameLen = nextSCF.nameAddress - this.nameAddress - 1;
            this.dataLen = nextSCF.dataAddress - this.dataAddress;
            getName();
            getData();
        }

        public SCFTemp(int nameAddress, int dataAddress) {
            this.nameAddress = nameAddress;
            this.dataAddress = dataAddress;
        }
    }

    public ArrayList<SCFClass> decode() {
        ArrayList<SCFTemp> tempList = new ArrayList<>();
        int fileSize = readMultipleByteReverse(this.cursor, 4);
        cursorJump(4);

        int fileNumber = readMultipleByteReverse(this.cursor, 4);
        cursorJump(4);

        for (int i = 0; i < fileNumber; i++) {
            int tempNameAddress = readMultipleByteReverse(this.cursor, 4);
            int tempDataAddress = readMultipleByteReverse(this.cursor + (fileNumber * 4), 4);

            System.out.printf("%d: %s | %s\n", i, Integer.toHexString(tempNameAddress),
                    Integer.toHexString(tempDataAddress));
            SCFTemp scfTemp = new SCFTemp(tempNameAddress, tempDataAddress);
            try {
                tempList.get(tempList.size() - 1).compareNext(scfTemp);
            } catch (Exception e) {
            }
            tempList.add(scfTemp);
            cursorJump(4);
        }

        try {
            tempList.get(tempList.size() - 1).compareNext(tempList.get(0).dataAddress, this.arr.length + 1);
        } catch (Exception e) {
            // TODO: handle exception
        }

        for (SCFTemp scfTemp : tempList) {

            try {
                this.scfList.add(scfTemp.convertToSCF());
            } catch (Exception e) {
                // TODO: handle exception
            }

        }

        return this.scfList;

    }

}
