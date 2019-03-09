import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;

public class untar {
    public static void main(String args[]) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: untar l|x <input archive> [output directory]");
        } else {
            String command = args[0];
            if (args.length < 2) {
                System.out.println("Missing input archive.");
            } else {
                File archive = new File(args[1]);
                switch (command) {
                    case "l":
                        list(archive);
                        break;
                    case "x":
                        String outputDirectoryArg = ".";
                        if (args.length > 2) {
                            outputDirectoryArg = args[2];
                        }
                        File outputDirectory = new File(outputDirectoryArg);
                        uncompress(archive, outputDirectory);
                        break;
                    default:
                        System.out.println("Unknown command.");
                }
            }
        }
    }

    private static void list(File archive) throws IOException {
        TarArchiveInputStream tarInput = openArchive(archive);

        while (true) {
            TarArchiveEntry tarEntry = tarInput.getNextTarEntry();
            if (tarEntry == null) {
                break;
            }
            String entryName = tarEntry.getName();
            if (tarEntry.isSymbolicLink()) {
                String linkName = tarEntry.getLinkName();
                System.out.println("Symbolic link \"" + linkName + "\" to \"" + entryName + "\"");
            } else if (tarEntry.isDirectory()) {
                System.out.println("Directory \"" + entryName + "\"");
            } else if (tarEntry.isFile()) {
                System.out.println("File \"" + entryName + "\"");
            } else {
                throw new IOException("Unknown archive entry type.");
            }
        }
        tarInput.close();
    }

    private static void uncompress(File archive, File outputDirectory) throws IOException {
        TarArchiveInputStream tarInput = openArchive(archive);

        outputDirectory.mkdirs();

        while (true) {
            TarArchiveEntry tarEntry = tarInput.getNextTarEntry();
            if (tarEntry == null) {
                break;
            }
            String entryName = tarEntry.getName();
            if (tarEntry.isSymbolicLink()) {
                String linkName = tarEntry.getLinkName();
                System.out.println("Linking \"" + linkName + "\" to \"" + entryName + "\"...");
                File outputFile = new File(outputDirectory, entryName);
                File parentDirectory = outputFile.getParentFile();
                File linkFile = new File(parentDirectory, linkName);
                Files.createSymbolicLink(outputFile.toPath(), linkFile.toPath().toAbsolutePath());
            } else if (tarEntry.isDirectory()) {
                System.out.println("Creating \"" + entryName + "\"...");
                File outputFile = new File(outputDirectory, entryName);
                outputFile.mkdirs();
            } else if (tarEntry.isFile()) {
                System.out.println("Unpacking \"" + entryName + "\"...");
                File outputFile = new File(outputDirectory, entryName);
                File parentDirectory = outputFile.getParentFile();
                parentDirectory.mkdirs();
                outputFile.createNewFile();
                IOUtils.copy(tarInput, new FileOutputStream(outputFile));
            } else {
                throw new IOException("Unknown archive entry type.");
            }
        }
        tarInput.close();
    }

    private static TarArchiveInputStream openArchive(File archive) throws IOException {
        InputStream input = new BufferedInputStream(new FileInputStream(archive));
        if (archive.getName().endsWith(".gz")) {
            input = new GzipCompressorInputStream(input);
        }
        return new TarArchiveInputStream(input);
    }
}