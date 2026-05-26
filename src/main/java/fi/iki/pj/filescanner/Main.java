package fi.iki.pj.filescanner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Ohjelman käynnistyspiste.
 *
 * Käyttö:
 *   java -jar filescanner.jar <hakemisto> [db-host] [db-port] [db-nimi] [db-käyttäjä] [db-salasana]
 *
 * Oletusarvot:
 *   db-host     = localhost
 *   db-port     = 3306
 *   db-nimi     = filescanner
 *   db-käyttäjä = filescanner
 *   db-salasana = filescanner
 *
 * Ympäristömuuttujat (ylikirjoittavat oletukset, CLI-parametrit ylikirjoittavat nämä):
 *   FS_DB_HOST, FS_DB_PORT, FS_DB_NAME, FS_DB_USER, FS_DB_PASSWORD
 */
public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        // --- Hakemisto ---
        Path scanDir = Paths.get(args[0]);
        if (!Files.isDirectory(scanDir)) {
            System.err.println("Virhe: '" + args[0] + "' ei ole hakemisto tai sitä ei löydy.");
            System.exit(2);
        }

        // --- Tietokanta-asetukset: ensin ympäristömuuttujat, sitten CLI ---
        String dbHost = env("FS_DB_HOST",     "localhost");
        int    dbPort = Integer.parseInt(env("FS_DB_PORT", "3306"));
        String dbName = env("FS_DB_NAME",     "filescanner");
        String dbUser = env("FS_DB_USER",     "filescanner");
        String dbPass = env("FS_DB_PASSWORD", "filescanner");

        if (args.length > 1) dbHost = args[1];
        if (args.length > 2) dbPort = Integer.parseInt(args[2]);
        if (args.length > 3) dbName = args[3];
        if (args.length > 4) dbUser = args[4];
        if (args.length > 5) dbPass = args[5];

        System.out.println("=".repeat(50));
        System.out.println("  File Scanner v1.0");
        System.out.println("=".repeat(50));
        System.out.printf("Hakemisto : %s%n", scanDir.toAbsolutePath());
        System.out.printf("Tietokanta: %s:%d/%s%n", dbHost, dbPort, dbName);
        System.out.println();

        try {
            // 1. Alusta tietokantayhteys
            Database db = new Database(dbHost, dbPort, dbName, dbUser, dbPass);
            db.initSchema();

            // 2. Skannaa tiedostot
            System.out.println("Skannataan hakemistoa...");
            FileScanner scanner = new FileScanner();
            List<FileRecord> records = scanner.scan(scanDir);
            scanner.printSummary();
            System.out.println();

            // 3. Kirjoita tietokantaan
            System.out.println("Kirjoitetaan tietokantaan...");
            DatabaseWriter writer = new DatabaseWriter(db);
            writer.writeAll(records);

            System.out.println();
            System.out.println("✓ Valmis!");

        } catch (Exception e) {
            System.err.println("Kriittinen virhe: " + e.getMessage());
            e.printStackTrace();
            System.exit(3);
        }
    }

    private static String env(String key, String defaultValue) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }

    private static void printUsage() {
        System.out.println("""
            Käyttö:
              java -jar filescanner.jar <hakemisto> [host] [portti] [db-nimi] [käyttäjä] [salasana]

            Esimerkki:
              java -jar filescanner.jar /home/kuvat localhost 3306 filescanner root salainen

            Ympäristömuuttujat (vaihtoehto CLI-parametreille):
              FS_DB_HOST, FS_DB_PORT, FS_DB_NAME, FS_DB_USER, FS_DB_PASSWORD
            """);
    }
}
