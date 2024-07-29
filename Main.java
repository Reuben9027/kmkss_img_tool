import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        Scanner s = new Scanner(System.in);

        System.out.println("Enter Parameter: ");
        System.out.println("1 : ARC file to JSON");
        System.out.println("2 : JSON to SCFs");
        System.out.println("3 : SCFs to ARC:");
        System.out.println("4 //debug dont use:");
        System.out.println("5 : IMG|PAC to ARC");
        System.out.println("6 : ARC to IMG");

        int type = s.nextInt();
        String dir;
        s.nextLine();

        SCFClass.toTranslate = true;

        switch (type) {
            case 1: // arc to json
                System.out.println("Enter ARC File:");
                dir = s.nextLine();
                Path path = Paths.get(dir);
                byte[] arr = Files.readAllBytes(path);

                ARCClass arc = new ARCClass(arr);
                ArrayList<SCFClass> scfList = arc.decode();

                File tmpList = new File("tmp.list");
                FileWriter writer = new FileWriter(tmpList);

                for (SCFClass scf : scfList) {
                    scf.decode();
                    scf.maakeJson();
                    writer.append(scf.fileName + "\n");

                }
                writer.flush();
                writer.close();
                System.out.println("Check JSONextract & SCFextract");

                break;
            case 2: // json to scf
                System.out.println("Enter JSON Folder:");
                dir = s.nextLine();
                File file = new File(dir);
                File[] fileArr = file.listFiles();

                for (File file2 : fileArr) {
                    try {
                        SCFClass.jsonToSCF(file2, file2.getName().replace(".json", ""));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("Check SCFconvert");
                break;

            // scf to arc
            case 3:
                System.out.println("Enter SCF folder:");
                dir = s.nextLine();
                String dirtmp = "tmp.list";
                BufferedReader reader = new BufferedReader(new FileReader(dirtmp));

                file = new File(dir);
                int folderLen = file.listFiles().length;

                File[] folderFiles = new File[folderLen];
                for (int i = 0; i < folderLen; i++) {
                    folderFiles[i] = new File(dir + "\\" + reader.readLine() + ".scf");
                }

                SCFClass dummy = new SCFClass(null);

                try {
                    dummy.buildArc(folderFiles);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.out.println("Check SCF_to_ARC.ARC");
                break;

            case 4: // dont mind this
                dir = s.nextLine();
                path = Paths.get(dir);

                byte[] jsonDebug = Files.readAllBytes(path);
                SCFClass scfClass = new SCFClass(jsonDebug);
                scfClass.decode();

                break;
            case 5: // img || pac to arc
                System.out.println("Enter IMG|PAC File:");
                dir = s.nextLine();
                path = Paths.get(dir);

                byte[] pacFile = Files.readAllBytes(path);

                PACClass packClass = new PACClass(pacFile);
                packClass.decode();

                System.out.println("Check IMG_to_ARC.ARC");
                break;

            case 6: // arc to img
                System.out.println("Enter ARC File:");
                dir = s.nextLine();
                path = Paths.get(dir);

                pacFile = Files.readAllBytes(path);

                packClass = new PACClass(pacFile);

                Duration dur = Duration.ZERO;
                Instant begin = Instant.now();
                packClass.encode();
                dur = Duration.between(begin, Instant.now());
                System.out.println(dur.getSeconds());

                System.out.println("Check ARC_to_IMG.IMG");
                break;
            default:
                break;
        }
    }
}