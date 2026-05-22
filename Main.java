import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.zip.*;

/**
 * Main entry point for the kmkss_img_tool â€? a PS2 visual novel extraction
 * and repacking utility for the kmkss game format (based on the AMG codec).
 *
 * <p>Supported operations:
 * <ol>
 *   <li>ARC â†? JSON + SCF temp files</li>
 *   <li>JSON folder â†? SCF files</li>
 *   <li>SCF folder â†? ARC</li>
 *   <li>(debug) raw SCF decode</li>
 *   <li>IMG/PAC â†? ARC</li>
 *   <li>ARC â†? IMG</li>
 * </ol>
 *
 * <p>All output folders are created automatically. After each operation the
 * results can optionally be zipped into a single archive for easy transport.
 *
 * <p><b>Usage:</b> Run the program, enter the operation number, supply the
 * requested path, then confirm whether you want a zip.
 *
 * @see ARCClass
 * @see SCFClass
 * @see PACClass
 */
public class Main {

    // -------------------------------------------------------------------------
    // Output directory constants
    // -------------------------------------------------------------------------

    /** Folder where extracted JSON files are written (operation 1). */
    static final String DIR_JSON_EXTRACT = "JSONextract";

    /** Folder where extracted raw SCF temp files are written (operation 1). */
    static final String DIR_SCF_EXTRACT  = "SCFextract";

    /** Folder where converted SCF files are written (operation 2). */
    static final String DIR_SCF_CONVERT  = "SCFconvert";

    /** Temporary file that records SCF entry order for ARC rebuilding. */
    static final String FILE_TMP_LIST    = "tmp.list";

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    /**
     * Application entry point.
     *
     * @param args command-line arguments (unused; all input is interactive)
     * @throws IOException if any file I/O operation fails fatally
     */
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        printMenu();

        int type = scanner.nextInt();
        scanner.nextLine(); // consume newline

        // Translation flag is enabled for all interactive operations
        SCFClass.toTranslate = true;

        switch (type) {
            case 1 -> runArcToJson(scanner);
            case 2 -> runJsonToScf(scanner);
            case 3 -> runScfToArc(scanner);
            case 4 -> runDebugDecode(scanner);
            case 5 -> runImgToArc(scanner);
            case 6 -> runArcToImg(scanner);
            default -> System.out.println("Unknown option. Exiting.");
        }

        scanner.close();
    }

    // -------------------------------------------------------------------------
    // Menu
    // -------------------------------------------------------------------------

    /** Prints the interactive operation menu to standard output. */
    private static void printMenu() {
        System.out.println("=== kmkss_img_tool ===");
        System.out.println("Select operation:");
        System.out.println("  1 : ARC file     -> JSON + SCF temp files");
        System.out.println("  2 : JSON folder  -> SCF files");
        System.out.println("  3 : SCF folder   -> ARC file");
        System.out.println("  4 : (debug) raw SCF byte decode -- do not use in production");
        System.out.println("  5 : IMG/PAC file -> ARC file");
        System.out.println("  6 : ARC file     -> IMG file");
        System.out.print("Enter number: ");
    }

    // -------------------------------------------------------------------------
    // Operation 1 - ARC -> JSON + SCF temp files
    // -------------------------------------------------------------------------

    /**
     * Operation 1: Decodes an ARC container into individual JSON script files
     * and raw SCF temp files.
     *
     * <p>Output directories ({@value #DIR_JSON_EXTRACT} and
     * {@value #DIR_SCF_EXTRACT}) are created if they do not exist. A
     * {@value #FILE_TMP_LIST} index file is also written so that operation 3
     * can rebuild the ARC in the correct order.
     *
     * @param scanner the active console scanner for reading user input
     * @throws IOException if the ARC file cannot be read or outputs cannot be written
     */
    private static void runArcToJson(Scanner scanner) throws IOException {
        ensureDirectories(DIR_JSON_EXTRACT, DIR_SCF_EXTRACT);

        System.out.println("Enter path to ARC file:");
        String dir = scanner.nextLine();

        byte[] arcBytes = Files.readAllBytes(Paths.get(dir));
        ARCClass arc = new ARCClass(arcBytes);
        ArrayList<SCFClass> scfList = arc.decode();

        // Write the ordered name list used by operation 3
        try (FileWriter writer = new FileWriter(FILE_TMP_LIST)) {
            for (SCFClass scf : scfList) {
                scf.decode();
                scf.maakeJson();
                writer.append(scf.fileName).append("\n");
            }
        }

        System.out.println("Done. Check " + DIR_JSON_EXTRACT + " and " + DIR_SCF_EXTRACT + ".");
        offerZip(scanner, DIR_JSON_EXTRACT, DIR_SCF_EXTRACT);
    }

    // -------------------------------------------------------------------------
    // Operation 2 - JSON folder -> SCF files
    // -------------------------------------------------------------------------

    /**
     * Operation 2: Converts a folder of JSON script files back into binary SCF
     * files ready for repacking.
     *
     * <p>The output directory ({@value #DIR_SCF_CONVERT}) is created if it
     * does not exist. Files that fail to convert are logged to stderr and
     * skipped so that a single bad entry does not abort the batch.
     *
     * @param scanner the active console scanner for reading user input
     */
    private static void runJsonToScf(Scanner scanner) {
        ensureDirectories(DIR_SCF_CONVERT);

        System.out.println("Enter path to JSON folder:");
        String dir = scanner.nextLine();

        File[] files = new File(dir).listFiles();
        if (files == null || files.length == 0) {
            System.err.println("No files found in: " + dir);
            return;
        }

        for (File file : files) {
            try {
                SCFClass.jsonToSCF(file, file.getName().replace(".json", ""));
            } catch (Exception e) {
                System.err.println("Failed to convert: " + file.getName());
                e.printStackTrace();
            }
        }

        System.out.println("Done. Check " + DIR_SCF_CONVERT + ".");
        offerZip(scanner, DIR_SCF_CONVERT);
    }

    // -------------------------------------------------------------------------
    // Operation 3 - SCF folder -> ARC
    // -------------------------------------------------------------------------

    /**
     * Operation 3: Repacks a folder of SCF files into a single ARC container.
     *
     * <p>File order is determined by {@value #FILE_TMP_LIST}, which must have
     * been generated by operation 1. The resulting archive is written to
     * {@code SCF_to_ARC.ARC} in the working directory.
     *
     * @param scanner the active console scanner for reading user input
     * @throws IOException if the SCF folder or tmp.list cannot be read, or the
     *                     output ARC cannot be written
     */
    private static void runScfToArc(Scanner scanner) throws IOException {
        System.out.println("Enter path to SCF folder:");
        String dir = scanner.nextLine();

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_TMP_LIST))) {
            File folder = new File(dir);
            File[] folderFiles = folder.listFiles();
            if (folderFiles == null) {
                System.err.println("Folder is empty or does not exist: " + dir);
                return;
            }

            File[] orderedFiles = new File[folderFiles.length];
            for (int i = 0; i < folderFiles.length; i++) {
                String name = reader.readLine();
                if (name == null) {
                    System.err.println("tmp.list has fewer entries than files in folder.");
                    break;
                }
                orderedFiles[i] = new File(dir + File.separator + name + ".scf");
            }

            SCFClass dummy = new SCFClass(null);
            try {
                dummy.buildArc(orderedFiles);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("Done. Output: SCF_to_ARC.ARC");
        offerZip(scanner, "SCF_to_ARC.ARC");
    }

    // -------------------------------------------------------------------------
    // Operation 4 - Debug decode
    // -------------------------------------------------------------------------

    /**
     * Operation 4 (debug): Reads a raw SCF file and runs the byte-level decoder.
     *
     * <p><b>Do not use this in normal operation.</b> It is kept for developer
     * diagnostics only and may produce incomplete or garbled output.
     *
     * @param scanner the active console scanner for reading user input
     * @throws IOException if the SCF file cannot be read
     */
    private static void runDebugDecode(Scanner scanner) throws IOException {
        System.out.println("[DEBUG] Enter path to SCF file:");
        String dir = scanner.nextLine();

        byte[] scfBytes = Files.readAllBytes(Paths.get(dir));
        SCFClass scfClass = new SCFClass(scfBytes);
        scfClass.decode();

        System.out.println("[DEBUG] Decode complete.");
    }

    // -------------------------------------------------------------------------
    // Operation 5 - IMG/PAC -> ARC
    // -------------------------------------------------------------------------

    /**
     * Operation 5: Decodes an IMG or PAC container file into an ARC archive.
     *
     * <p>The result is written to {@code ING_to_ARC.ARC} (legacy filename kept
     * for compatibility with downstream tooling).
     *
     * @param scanner the active console scanner for reading user input
     * @throws IOException if the input file cannot be read or the output cannot
     *                     be written
     */
    private static void runImgToArc(Scanner scanner) throws IOException {
        System.out.println("Enter path to IMG/PAC file:");
        String dir = scanner.nextLine();

        byte[] pacBytes = Files.readAllBytes(Paths.get(dir));
        PACClass packClass = new PACClass(pacBytes);
        packClass.decode();

        System.out.println("Done. Output: IMG_to_ARC.ARC");
        offerZip(scanner, "IMG_to_ARC.ARC");
    }

    // -------------------------------------------------------------------------
    // Operation 6 - ARC -> IMG
    // -------------------------------------------------------------------------

    /**
     * Operation 6: Encodes an ARC container back into a compressed IMG file.
     *
     * <p>Elapsed encoding time is printed to standard output on completion.
     * The result is written to {@code ARC_to_IMG.IMG}.
     *
     * @param scanner the active console scanner for reading user input
     * @throws IOException if the ARC file cannot be read or the IMG cannot be
     *                     written
     */
    private static void runArcToImg(Scanner scanner) throws IOException {
        System.out.println("Enter path to ARC file:");
        String dir = scanner.nextLine();

        byte[] arcBytes = Files.readAllBytes(Paths.get(dir));
        PACClass packClass = new PACClass(arcBytes);

        Instant begin = Instant.now();
        packClass.encode();
        Duration dur = Duration.between(begin, Instant.now());

        System.out.printf("Done in %d second(s). Output: ARC_to_IMG.IMG%n", dur.getSeconds());
        offerZip(scanner, "ARC_to_IMG.IMG");
    }

    // -------------------------------------------------------------------------
    // Utility helpers
    // -------------------------------------------------------------------------

    /**
     * Creates one or more output directories if they do not already exist.
     *
     * @param dirs directory paths to create
     */
    private static void ensureDirectories(String... dirs) {
        for (String d : dirs) {
            File f = new File(d);
            if (!f.exists()) {
                f.mkdirs();
            }
        }
    }

    /**
     * Prompts the user whether to bundle the listed paths into a zip archive.
     *
     * <p>If the user answers {@code y} (case-insensitive), all matching files
     * and directories are zipped into {@code output.zip} in the working directory.
     * Existing {@code output.zip} files are overwritten.
     *
     * @param scanner the active console scanner for reading user input
     * @param paths   file or directory paths to include in the zip
     */
    private static void offerZip(Scanner scanner, String... paths) {
        System.out.print("Zip the output? (y/n): ");
        String answer = scanner.nextLine().trim();
        if (!answer.equalsIgnoreCase("y")) {
            return;
        }

        String zipName = "output.zip";
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipName))) {
            for (String path : paths) {
                File f = new File(path);
                if (f.isDirectory()) {
                    zipDirectory(f, f.getName(), zos);
                } else if (f.exists()) {
                    zipFile(f, f.getName(), zos);
                } else {
                    System.err.println("Skipping missing path: " + path);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to create zip: " + e.getMessage());
            return;
        }

        System.out.println("Zipped to: " + zipName);
    }

    /**
     * Recursively adds a directory and all its contents to a zip stream.
     *
     * @param dir        the directory to add
     * @param parentPath the path prefix used inside the zip entry
     * @param zos        the target zip output stream
     * @throws IOException if a file cannot be read or the zip entry cannot be written
     */
    private static void zipDirectory(File dir, String parentPath, ZipOutputStream zos)
            throws IOException {
        File[] children = dir.listFiles();
        if (children == null) return;

        for (File child : children) {
            String entryPath = parentPath + "/" + child.getName();
            if (child.isDirectory()) {
                zipDirectory(child, entryPath, zos);
            } else {
                zipFile(child, entryPath, zos);
            }
        }
    }

    /**
     * Adds a single file to a zip stream.
     *
     * @param file      the file to add
     * @param entryName the name/path of the entry inside the zip
     * @param zos       the target zip output stream
     * @throws IOException if the file cannot be read or the zip entry cannot be written
     */
    private static void zipFile(File file, String entryName, ZipOutputStream zos)
            throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            zos.putNextEntry(new ZipEntry(entryName));
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
            zos.closeEntry();
        }
    }
}
