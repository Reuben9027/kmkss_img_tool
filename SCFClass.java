//D:\Amagami\cpk\n0fre_me03A.scf

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

abstract class SCFAbstract extends ByteManip {

    public SCFAbstract(byte[] arr) {
        super(arr);
        // TODO Auto-generated constructor stub
    }

    interface InnerSCF {
        public void print();

        public JSONObject printJson();

        public int groupNo();

        public int getType();
    }

}

public class SCFClass extends SCFAbstract {
    static boolean toTranslate = false;
    ArrayList<InnerSCF> scfList = new ArrayList<>();
    String fileName = "";
    byte[] unknown;

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

    JSONObject convert(String name, JSONArray jsonArr) {
        JSONObject temp = new JSONObject();
        temp.put(name, jsonArr);
        return temp;
    }

    public void maakeJson() {
        try {
            if (this.fileName.equals("BG_Picture")) {
                System.out.println();
            }

            String scfFile = String.format("%s.json", this.fileName);
            FileWriter file = new FileWriter("JSONextract\\" + scfFile);

            JSONArray[] arrayJSON = new JSONArray[5];
            String[] arrayName = { "Labels", "Variables", "Blocks", "Entries", "Unknown" };

            for (int i = 0; i < 5; i++) {
                arrayJSON[i] = new JSONArray();
                if (i == 1 || i == 2) {
                    arrayJSON[i].add(new JSONArray());
                    arrayJSON[i].add(new JSONArray());
                }

            }

            for (InnerSCF b : this.scfList) {
                int tempType = b.getType();

                JSONArray tempArrayJson = arrayJSON[b.getType()];
                if (tempType == 1 || tempType == 2) {
                    int group = b.groupNo();
                    JSONArray groupTemp = (JSONArray) tempArrayJson.get(group);
                    groupTemp.add(b.printJson());
                    continue;
                }
                // arrayJSON[b.getType()].get(tempType)
                arrayJSON[b.getType()].add(b.printJson());
            }

            JSONObject tempJson = new JSONObject();

            JSONObject unknownJsonObject = new JSONObject();
            for (int i = 0; i < 3; i++) {
                unknownJsonObject.put(i, Byte.toUnsignedInt(unknown[i]));
            }
            arrayJSON[4].add(unknownJsonObject);

            for (int i = 0; i < 5; i++) {
                tempJson.put(arrayName[i], arrayJSON[i]);
            }

            file.append(tempJson.toString());
            file.flush();
            file.close();
        } catch (Exception e) {

        }

    }

    public SCFClass(byte temp[], String name) {
        super(temp);
        this.fileName = name;
    }

    public SCFClass(byte temp[]) {
        super(temp);
    }

    abstract class ScfPrototype implements InnerSCF {
        JSONObject jsonData = new JSONObject();

        String name;
        String name64;
        int index;

        public ScfPrototype(int ind, byte[] str) {
            this.index = ind;
            this.name64 = Base64.getEncoder().encodeToString(str);
            this.name = new String(str, StandardCharsets.UTF_8);
        }
    }

    class SCFLabel extends ScfPrototype {

        @Override
        public void print() {
            System.out.printf("LabelHeader #%d: %s\n", this.index, this.name);
        }

        public SCFLabel(int ind, byte[] str) {
            super(ind, str);
        }

        @Override
        public JSONObject printJson() {
            this.jsonData.put("name64", this.name64);
            this.jsonData.put("name", this.name);
            this.jsonData.put("index", "" + this.index);
            return this.jsonData;
        }

        @Override
        public int getType() {
            return 0;
        }

        @Override
        public int groupNo() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'groupNo'");
        }

    }

    class SCFVariables extends ScfPrototype {
        int id;
        int group;

        @Override
        public int getType() {
            return 1;
        }

        public SCFVariables(int ind, int id, byte[] str, int group) {
            super(ind, str);
            this.id = id;
            this.group = group;
        }

        public SCFVariables(int ind, byte[] str) {
            super(ind, str);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void print() {
            System.out.printf("Variable: id = %d, , index = %d , name: %s\n", this.id, this.index, this.name);
        }

        @Override
        public JSONObject printJson() {
            this.jsonData.put("name64", this.name64);
            this.jsonData.put("id", this.id);
            this.jsonData.put("index", this.index);
            this.jsonData.put("name", this.name);
            return this.jsonData;
        }

        @Override
        public int groupNo() {
            return this.group;
        }

    }

    class SCFBlock extends ScfPrototype {
        int dataLen;
        int isMain;
        String data;
        int group;

        public SCFBlock(int ind, byte[] str) {
            super(ind, str);
        }

        public SCFBlock(int ind, byte[] str, int dataLen, int isMain, byte[] data, int group) {
            super(ind, str);
            this.dataLen = dataLen;
            this.isMain = isMain;
            this.data = Base64.getEncoder().encodeToString(data);
            this.group = group;
        }

        @Override
        public void print() {
            System.out.printf("Block: isMain = %d, dataLen = %d, index = %d , name: %s\n", this.isMain, this.dataLen,
                    this.index, this.name);
            System.out.printf("Data: %s\n", this.data);
        }

        @Override
        public JSONObject printJson() {
            this.jsonData.put("name64", this.name64);
            this.jsonData.put("index", this.index);
            this.jsonData.put("dataLen", this.dataLen);
            this.jsonData.put("isMain", this.isMain);
            this.jsonData.put("name", this.name);
            this.jsonData.put("xdata", this.data);

            return this.jsonData;
        }

        @Override
        public int getType() {
            return 2;
        }

        @Override
        public int groupNo() {
            return this.group;
        }

    }

    abstract class SCFEntryList implements InnerSCF {
        JSONObject jsonData = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        ArrayList<SCFEntry> list = new ArrayList<>();
        int index;
        int type;
        String dataText = "";

    }

    class SCFEntry extends SCFEntryList {

        @Override
        public int getType() {
            return 3;
        }

        public SCFEntry(int ind, int type) {
            this.type = type;
            this.index = ind;
            if (type == 8) {
                this.decode();
                return;
            }

            switch (type) {
                case 0:
                case 2:
                case 4:
                    cursorJump(1);
                    break;
                case 1:
                case 3:
                    int data = 0;

                    for (int i = 0; i < 4; i++) {
                        int temp = jumpGet(1) << (8 * i);
                        data += temp;
                    }
                    this.dataText = Integer.toHexString(data);
                    cursorJump(1);
                    break;
                default:
                    int strLen = jumpGet(1);
                    byte[] str = construct(cursorJump(2), strLen);

                    try {
                        this.dataText = new String(str, "SHIFT-JIS");
                        ;
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                    cursorJump(strLen);
                    break;
            }
        }

        public void decode() {
            cursorJump(1);
            int entryLen = readMultipleByteReverse(cursor, 2);
            cursorJump(2);
            for (int i = 0; i < entryLen; i++) {
                int entryType = cursorGetInt();
                SCFEntry entry = new SCFEntry(i, entryType);
                this.list.add(entry);
            }
        }

        @Override
        public void print() {
            System.out.printf("Entry: index = %d, type = %d , text = %s \n", this.index, this.type, this.dataText);
            for (InnerSCF temp : list) {
                temp.print();
            }
        }

        @Override
        public JSONObject printJson() {
            // lol tf is this
            this.jsonData.put("index", this.index);
            this.jsonData.put("type", this.type);

            for (InnerSCF temp : list) {
                this.jsonArray.add(temp.printJson());
            }

            if (this.type == 8) {
                this.jsonData.put("elements", this.jsonArray);
            } else {
                this.jsonData.put("xdata", this.dataText);
                if (this.type == 5) {
                    this.jsonData.put("tdata", "ＮｏＤａｔａ");
                    this.jsonData.put("toTranslate", 0);
                }

            }
            return this.jsonData;
        }

        @Override
        public int groupNo() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'groupNo'");
        }

    }

    public void decode() {
        try {
            File file = new File("SCFextract\\" + fileName + ".scfTemp");
            FileOutputStream s = new FileOutputStream(file);
            s.write(this.arr);
        } catch (Exception e) {
            // TODO: handle exception
        }

        if (this.fileName.equals("BG_Picture")) {
            System.out.println();
        }

        String scfCheck = new String(Arrays.copyOfRange(arr, 0, 3));

        if (!scfCheck.equals(scfCheck)) {
            System.out.println("ERROR: NOT SCF filename: " + fileName);
        }
        this.cursor = 3;

        this.unknown = Arrays.copyOfRange(arr, this.cursor, this.cursor + 3);
        this.cursor = 6;

        for (int i = 0; i < 2; i++) {
            int strLen = cursorGetInt();
            // System.out.println(this.cursor);
            byte[] temp = construct(cursorJump(2), strLen);
            SCFLabel label = new SCFLabel(i, temp);

            /*
             * if (i == 0) {
             * this.fileName = label.name;
             * }
             */

            scfList.add(label);
            // labelList.add(label);
            cursorJump(strLen);
        }

        for (int j = 0; j < 2; j++) {
            int varLen = cursorGetInt();
            cursorJump(2);
            for (int i = 0; i < varLen; i++) {
                int id = cursorGetInt();
                int strLen = Byte.toUnsignedInt(this.arr[cursorJump(1)]);
                SCFVariables currVariable = new SCFVariables(i, id,
                        construct(cursorJump(2), strLen), j);

                scfList.add(currVariable);
                cursorJump(strLen);
            }
        }

        for (int j = 0; j < 2; j++) {
            int blockLen = readMultipleByteReverse(this.cursor, 2);
            cursorJump(2);
            for (int i = 0; i < blockLen; i++) {
                int strLen = cursorGetInt();
                byte[] str = construct(cursorJump(2), strLen);
                int isMain = Byte.toUnsignedInt(arr[cursorJump(strLen)]);

                int dataLen = readMultipleByteReverse(cursorJump(2), 2);
                byte[] data = null;
                try {
                    data = construct(cursorJump(2), dataLen);
                } catch (Exception e) {
                    System.out.println(fileName + " " + new String(str));
                    System.out.println("blyat" + dataLen);
                }

                SCFBlock block = new SCFBlock(i, str, dataLen, isMain, data, j);
                scfList.add(block);
                cursorJump(dataLen);
            }
        }

        int cursorSave = this.cursor;

        int entrySize = cursorGetInt();
        entrySize += jumpGet(1) << 8;

        cursorJump(1);
        try {
            for (int i = 0; i < entrySize; i++) {
                SCFEntry entry = new SCFEntry(i, cursorGetInt());
                // entryList.add(entry);
                scfList.add(entry);
            }
        } catch (Exception e) {
            System.out.println(this.fileName + " Something is wrong " + entrySize + " Cursor Entry: " + cursorSave);
            System.out.println(this.fileName + " Size: " + this.arr.length + " cursor: " + this.cursor);
            e.printStackTrace();
            // TODO: handle exception
        }
    }

    static void jsonToSCF(File json, String dir) throws IOException, ParseException {
        FileOutputStream temp = new FileOutputStream("SCFconvert\\" + dir + ".scf");

        temp.write(("SCF").getBytes());

        // + the another 3 bytes

        JSONParser parser = new JSONParser();
        FileReader reader = new FileReader(json);
        JSONObject mainJson = (JSONObject) parser.parse(reader);

        JSONArray unknownArray = (JSONArray) mainJson.get("Unknown");
        JSONObject unknownTemp = (JSONObject) unknownArray.get(0);
        for (int i = 0; i < 3; i++) {
            temp.write(((Long) unknownTemp.get("" + i)).intValue());
        }

        JSONArray labelArray = (JSONArray) mainJson.get("Labels");
        // there are 2 labels
        for (int i = 0; i < 2; i++) {
            JSONObject objTemp = (JSONObject) labelArray.get(i);
            String name = (String) objTemp.get("name64");
            byte nameBytes[] = Base64.getDecoder().decode(name);

            temp.write(nameBytes.length);
            temp.write(0);
            temp.write(nameBytes);
        }

        JSONArray varArray2 = (JSONArray) mainJson.get("Variables");

        for (int j = 0; j < 2; j++) {
            JSONArray varArray = (JSONArray) varArray2.get(j);

            temp.write(varArray.size());
            temp.write(0);
            for (int i = 0; i < varArray.size(); i++) {
                JSONObject objTemp = (JSONObject) varArray.get(i);
                String name = (String) objTemp.get("name64");
                byte nameBytes[] = Base64.getDecoder().decode(name);

                int test = ((Long) objTemp.get("id")).intValue();

                System.out.println(test);

                temp.write(test);
                temp.write(nameBytes.length);
                temp.write(0);
                temp.write(nameBytes);
            }
        }

        // blcoks
        JSONArray blockArray2 = (JSONArray) mainJson.get("Blocks");

        for (int j = 0; j < 2; j++) {
            JSONArray blockArray = (JSONArray) blockArray2.get(j);
            temp.write(blockArray.size());
            temp.write(0);
            for (int i = 0; i < blockArray.size(); i++) {
                JSONObject objTemp = (JSONObject) blockArray.get(i);
                String name = (String) objTemp.get("name64");
                byte nameBytes[] = Base64.getDecoder().decode(name);
                // byte[] nameJIS = name.getBytes("SHIFT-JIS");

                // temp.write(0);
                temp.write(nameBytes.length);
                temp.write(0);
                temp.write(nameBytes);
                temp.write(((Long) objTemp.get("isMain")).intValue());
                temp.write(0);

                String xdata = (String) objTemp.get("xdata");
                byte[] data = Base64.getDecoder().decode(xdata);

                for (int o = 0; o < 2; o++) {
                    temp.write((data.length >> (8 * o)) % 0x100);
                }

                // temp.write(0);
                temp.write(data);
            }
        }

        JSONArray entriesArray = (JSONArray) mainJson.get("Entries");
        temp.write(entriesArray.size() % (0x100));
        temp.write(entriesArray.size() >> 8);

        for (int i = 0; i < entriesArray.size(); i++) {
            JSONObject objTemp = (JSONObject) entriesArray.get(i);
            jsonToSCFEntry(temp, objTemp);
        }

    }

    static void jsonToSCFEntry(FileOutputStream temp, JSONObject objTemp) throws IOException {
        int type = ((Long) objTemp.get("type")).intValue();
        temp.write(type);

        if (type == 8) {

            JSONArray tempArray = (JSONArray) objTemp.get("elements");

            for (int i = 0; i < 2; i++) {
                temp.write((tempArray.size() >> (8 * i)) % 0x100);
            }
            // temp.write(tempArray.size());
            // temp.write(0);
            for (int i = 0; i < tempArray.size(); i++) {
                jsonToSCFEntry(temp, (JSONObject) tempArray.get(i));
            }
            return;
        }

        String keyTranslate = "xdata";
        if (type == 5) {
            int toTranslate = ((Long) objTemp.get("toTranslate")).intValue();
            if (toTranslate == 1)
                keyTranslate = "tdata";

        }

        String xdataValue = (String) objTemp.get(keyTranslate);

        byte[] xdataJIS = xdataValue.getBytes("SHIFT-JIS");
        int xdataLen = xdataValue.length();

        switch (type) {
            case 0:
            case 2:
            case 4:
                // emp.write(type);
                break;

            case 1:
            case 3:
                // int val = Integer.parseUnsignedInt(xdataValue, xdataLen)
                int value = Integer.parseUnsignedInt(xdataValue, 16);
                for (int i = 0; i < 4; i++) {
                    temp.write((value >> (8 * i)) % 0x100);
                }
                break;

            default:

                temp.write(xdataJIS.length);

                temp.write(0);
                temp.write(xdataJIS);
                break;

        }
    }

    class ArcTemp {
        int nameAddress;
        int dataAddress;

        public ArcTemp(int nameAddress, int dataAddress) {
            this.dataAddress = dataAddress;
            this.nameAddress = nameAddress;
        }

    }

    void reversePut(FileOutputStream file, int number) throws IOException {
        for (int i = 0; i < 4; i++) {
            file.write(((number >> (i * 8)) % 0x100));
            // System.out.print(((number >> i) % 0x100) + " ");
        }
        // System.out.println();
    }

    void buildArc(File[] fileArray) throws IOException {
        ArrayList<ArcTemp> arcList = new ArrayList<>();
        int nameLen = 0;
        int dataLen = 0;

        int fileNumber = fileArray.length;
        int referenceLen = (fileNumber * 4 * 2);

        try {
            for (File file : fileArray) {
                arcList.add(new ArcTemp(nameLen, dataLen));
                System.out.println(file.getAbsolutePath());
                byte[] temp = Files.readAllBytes(Paths.get(file.getAbsolutePath()));

                dataLen += temp.length;
                nameLen += file.getName().replace(".scf", "").length() + 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        int fileLen = referenceLen + dataLen + nameLen + 8;

        int nameOffset = referenceLen + 8;
        int dataOffset = nameOffset + nameLen;

        File arcFile = new File("SCF_to_ARC.arc");
        FileOutputStream outputStream = new FileOutputStream(arcFile);
        reversePut(outputStream, fileLen);
        reversePut(outputStream, fileNumber);

        System.out.println(fileLen + " " + fileNumber);

        // put name references
        for (int i = 0; i < fileNumber; i++) {
            ArcTemp temp = arcList.get(i);
            reversePut(outputStream, nameOffset + temp.nameAddress);
        }

        // put data references
        for (int i = 0; i < fileNumber; i++) {
            ArcTemp temp = arcList.get(i);
            reversePut(outputStream, dataOffset + temp.dataAddress);
        }

        for (File file : fileArray) {
            byte[] temp = file.getName().replace(".scf", "").getBytes();
            outputStream.write(temp);
            outputStream.write(0);
        }

        for (File file : fileArray) {
            Path path = Paths.get(file.getAbsolutePath());
            byte[] temp = Files.readAllBytes(path);
            outputStream.write(temp);
        }

    }

}