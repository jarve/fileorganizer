package fi.iki.pj.filescanner;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Edustaa yhtä skannattujen tiedostojen tietuetta.
 */
public class FileRecord {

    private final String filePath;
    private final String fileName;
    private final long fileSize;
    private final String md5Checksum;
    private final LocalDateTime lastModified;

    /** Avain: "Hakemisto / Tag", Arvo: tekstiarvo */
    private final Map<String, String> exifTags = new LinkedHashMap<>();

    public FileRecord(String filePath, String fileName, long fileSize,
                      String md5Checksum, LocalDateTime lastModified) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.md5Checksum = md5Checksum;
        this.lastModified = lastModified;
    }

    public void addExifTag(String directory, String tagName, String value) {
        exifTags.put(directory + "\0" + tagName, value);
    }

    // Getterit
    public String getFilePath()        { return filePath; }
    public String getFileName()        { return fileName; }
    public long   getFileSize()        { return fileSize; }
    public String getMd5Checksum()     { return md5Checksum; }
    public LocalDateTime getLastModified() { return lastModified; }
    public Map<String, String> getExifTags() { return exifTags; }
}
