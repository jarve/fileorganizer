package fi.iki.pj.filescanner;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Hallinnoi tietokantayhteyttä ja alustaa skeeman.
 */
public class Database {

    private final String url;
    private final String user;
    private final String password;

    public Database(String host, int port, String dbName, String user, String password) {
        this.url = String.format(
            "jdbc:mariadb://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Europe/Helsinki",
            host, port, dbName
        );
        this.user = user;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Luo taulut jos niitä ei vielä ole olemassa.
     */
    public void initSchema() throws SQLException {
        String createFiles = """
            CREATE TABLE IF NOT EXISTS files (
                id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                file_path     VARCHAR(2048) NOT NULL,
                file_name     VARCHAR(512)  NOT NULL,
                file_size     BIGINT        NOT NULL,
                md5_checksum  CHAR(32)      NOT NULL,
                last_modified DATETIME      NOT NULL,
                scanned_at    DATETIME      NOT NULL DEFAULT NOW(),
                UNIQUE KEY uq_file_path (file_path(512))
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

        String createExif = """
            CREATE TABLE IF NOT EXISTS exif_data (
                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                file_id     BIGINT       NOT NULL,
                directory   VARCHAR(128) NOT NULL,
                tag_name    VARCHAR(256) NOT NULL,
                tag_value   TEXT,
                FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
                INDEX idx_file_id (file_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createFiles);
            stmt.execute(createExif);
            System.out.println("✓ Tietokantaskeema alustettu.");
        }
    }
}
