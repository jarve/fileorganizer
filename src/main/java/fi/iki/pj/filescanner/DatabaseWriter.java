package fi.iki.pj.filescanner;

import java.sql.*;
import java.util.List;
import java.util.Map;

/**
 * Kirjoittaa FileRecord-oliot MariaDB-tietokantaan.
 * Käyttää UPSERT-logiikkaa: jos tiedostopolku löytyy jo, päivittää tietueen.
 */
public class DatabaseWriter {

    private final Database database;

    public DatabaseWriter(Database database) {
        this.database = database;
    }

    /**
     * Tallentaa listan tietueita tietokantaan yhdessä transaktiossa.
     */
    public void writeAll(List<FileRecord> records) throws SQLException {
        int inserted = 0;
        int updated  = 0;
        int failed   = 0;

        String upsertFile = """
            INSERT INTO files (file_path, file_name, file_size, md5_checksum, last_modified, scanned_at)
            VALUES (?, ?, ?, ?, ?, NOW())
            ON DUPLICATE KEY UPDATE
                file_name     = VALUES(file_name),
                file_size     = VALUES(file_size),
                md5_checksum  = VALUES(md5_checksum),
                last_modified = VALUES(last_modified),
                scanned_at    = NOW()
            """;

        String getFileId = "SELECT id FROM files WHERE file_path = ?";

        String deleteExif = "DELETE FROM exif_data WHERE file_id = ?";

        String insertExif = """
            INSERT INTO exif_data (file_id, directory, tag_name, tag_value)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psFile   = conn.prepareStatement(upsertFile);
                 PreparedStatement psGetId  = conn.prepareStatement(getFileId);
                 PreparedStatement psDel    = conn.prepareStatement(deleteExif);
                 PreparedStatement psExif   = conn.prepareStatement(insertExif)) {

                for (FileRecord rec : records) {
                    try {
                        // 1. UPSERT tiedostorivi
                        psFile.setString(1, truncate(rec.getFilePath(), 2048));
                        psFile.setString(2, truncate(rec.getFileName(), 512));
                        psFile.setLong  (3, rec.getFileSize());
                        psFile.setString(4, rec.getMd5Checksum());
                        psFile.setObject(5, rec.getLastModified());
                        int affected = psFile.executeUpdate();

                        // 2. Hae tietueen id
                        psGetId.setString(1, truncate(rec.getFilePath(), 2048));
                        long fileId;
                        try (ResultSet rs = psGetId.executeQuery()) {
                            if (!rs.next()) throw new SQLException("Tietuetta ei löydy upsert-operaation jälkeen");
                            fileId = rs.getLong(1);
                        }

                        // 3. Poista vanhat EXIF-tiedot ja lisää uudet
                        psDel.setLong(1, fileId);
                        psDel.executeUpdate();

                        Map<String, String> exifTags = rec.getExifTags();
                        if (!exifTags.isEmpty()) {
                            for (Map.Entry<String, String> entry : exifTags.entrySet()) {
                                String[] parts = entry.getKey().split("\0", 2);
                                psExif.setLong  (1, fileId);
                                psExif.setString(2, truncate(parts[0], 128));
                                psExif.setString(3, truncate(parts[1], 256));
                                psExif.setString(4, entry.getValue());
                                psExif.addBatch();
                            }
                            psExif.executeBatch();
                        }

                        if (affected == 1) inserted++;
                        else               updated++;

                    } catch (SQLException e) {
                        failed++;
                        System.err.printf("  ✗ DB-virhe tiedostolle %s: %s%n",
                            rec.getFilePath(), e.getMessage());
                    }
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }

        System.out.println("─".repeat(50));
        System.out.printf("Tietokantaan kirjoitettu:%n");
        System.out.printf("  Uusia tietueita   : %d%n", inserted);
        System.out.printf("  Päivitettyjä      : %d%n", updated);
        System.out.printf("  Epäonnistuneita   : %d%n", failed);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
