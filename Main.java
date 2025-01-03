import java.io.IOException;
import java.util.*;

import FileManager.BitUtils;
import FileManager.ByteUtils;
import FileManager.FileHandler;
import FileManager.ArcManager.ARCtoSCF;
import FileManager.ImgManager.IMGtoPAC;

public class Main {
    public static void main(String[] args) throws IOException {
        Scanner s = new Scanner(System.in);

        // byte[] arr = { 'a', 'b', 0, 0, 0, 'a' };

        // System.out.println(new String(arr));

        // IMGtoPAC test1 = new IMGtoPAC(s.nextLine());
        // test1.decode(s.nextLine());

        ARCtoSCF test2 = new ARCtoSCF(s.nextLine());
        test2.decode(s.nextLine());

    }
}