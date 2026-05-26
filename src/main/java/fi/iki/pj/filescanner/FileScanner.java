package fi.iki.pj.filescanner;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.Directory;
import com.drew.metadata.Tag;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Käy hakemistopuun läpi rekursiivisesti ja rakentaa FileRecord-oliot.
 */
public class FileScanner {

    /** Tiedostopäätteet joista EXIF-tietoja yritetään lukea */
    private static final Set<String> EXIF_EXTENSIONS = Set.of(
        ".jpg", ".jpeg", ".tif", ".tiff", ".png", ".webp", ".heic", ".heif", ".cr2", ".nef", ".arw"
    );

    private int totalFiles = 0;
    private int exifFiles  = 0;
    private int errors     = 0;

    /**
     * Skannaa annettu hakemisto rekursiivisesti.
     *
     * @param rootDir skannattava juurihakemisto
     * @return lista FileRecord-olioista
     */
    public List<FileRecord> scan(Path rootDir) throws IOException {
        List<FileRecord> records = new ArrayList<>();

        Files.walkFileTree(rootDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                try {
                    FileRecord record = processFile(file, attrs);
                    records.add(record);
                    totalFiles++;

                    if (!record.getExifTags().isEmpty()) {
                        exifFiles++;
                    }

                    if (totalFiles % 100 == 0) {
                        System.out.printf("  Käsitelty %d tiedostoa...%n", totalFiles);
                    }
                } catch (Exception e) {
                    errors++;
                    System.err.printf("  ✗ Virhe tiedostossa %s: %s%n", file, e.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                errors++;
                System.err.printf("  ✗ Ei pääsyä: %s (%s)%n", file, exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });

        return records;
    }

    private FileRecord processFile(Path file, BasicFileAttributes attrs) throws Exception {
        String absPath     = file.toAbsolutePath().toString();
        String fileName    = file.getFileName().toString();
        long   fileSize    = attrs.size();
        String md5         = computeMd5(file);
        LocalDateTime lastMod = LocalDateTime.ofInstant(
            attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault()
        );

        FileRecord record = new FileRecord(absPath, fileName, fileSize, md5, lastMod);

        // EXIF vain tuetuille kuvatiedostoille
        if (isExifCandidate(fileName)) {
            readExif(file.toFile(), record);
        }

        return record;
    }

    private boolean isExifCandidate(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) return false;
        return EXIF_EXTENSIONS.contains(fileName.substring(dot).toLowerCase());
    }

    private String computeMd5(Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[8192];
        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            int read;
            while ((read = fis.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void readExif(File file, FileRecord record) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    String value = tag.getDescription();
                    if (value != null && !value.isBlank()) {
                        record.addExifTag(directory.getName(), tag.getTagName(), value);
                    }
                }
                // Kirjataan myös mahdolliset virheet
                if (directory.hasErrors()) {
                    for (String error : directory.getErrors()) {
                        System.err.printf("  EXIF-virhe (%s): %s%n", file.getName(), error);
                    }
                }
            }
        } catch (Exception e) {
            // Ei pakoteta — tiedosto voi olla korruptoitunut
            System.err.printf("  ✗ EXIF-luku epäonnistui (%s): %s%n", file.getName(), e.getMessage());
        }
    }

    public void printSummary() {
        System.out.println("─".repeat(50));
        System.out.printf("Skannaus valmis:%n");
        System.out.printf("  Tiedostoja yhteensä : %d%n", totalFiles);
        System.out.printf("  EXIF-tietoja löytyi : %d tiedostosta%n", exifFiles);
        System.out.printf("  Virheitä            : %d%n", errors);
    }
}
