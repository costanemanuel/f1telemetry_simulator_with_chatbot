package com.telemetry;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.File;
import java.io.PrintWriter;

public class DatabaseManager {

    // locatia fisierului unde tinem baza de date (se face automat in folderul proiectului)
    private static final String URL = "jdbc:sqlite:telemetry_data.db";

    public static void initializeDatabase() {
        // query-ul cu care construim tabelul doar daca nu a fost facut deja la o rulare anterioara
        String createTableSQL = "CREATE TABLE IF NOT EXISTS lap_times ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "driver_name TEXT NOT NULL,"
                + "lap_number INTEGER NOT NULL,"
                + "sector_1_ms INTEGER,"
                + "sector_2_ms INTEGER,"
                + "sector_3_ms INTEGER,"
                + "total_time_ms INTEGER,"
                + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ");";

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {

            // trimitem scriptul la executie
            stmt.execute(createTableSQL);
            System.out.println("baza de date a fost initializata cu succes");

        } catch (SQLException e) {
            System.out.println("eroare la crearea bazei de date: " + e.getMessage());
        }
    }

    public static void insertLap(String driverName, int lapNumber, long s1, long s2, long s3, long totalTime) {
        // facem un template de insert cu semne de intrebare ca sa fie clean si safe
        String insertSQL = "INSERT INTO lap_times(driver_name, lap_number, sector_1_ms, sector_2_ms, sector_3_ms, total_time_ms) VALUES(?,?,?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            // legam variabilele pe care le-am primit din simulator de template-ul de mai sus
            pstmt.setString(1, driverName);
            pstmt.setInt(2, lapNumber);
            pstmt.setLong(3, s1);
            pstmt.setLong(4, s2);
            pstmt.setLong(5, s3);
            pstmt.setLong(6, totalTime);

            // executam efectiv scrierea randului nou in tabel
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("eroare la salvarea turului: " + e.getMessage());
        }
    }

    public static void exportToCSV(File file) {
        String query = "SELECT * FROM lap_times ORDER BY id ASC";

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query);
             PrintWriter pw = new PrintWriter(file)) {

            // scriem "capul de tabel" in fisierul csv (prima linie din excel)
            pw.println("ID,Driver,Lap,Sector 1 (ms),Sector 2 (ms),Sector 3 (ms),Total Time (ms),Timestamp");

            // iteram rand cu rand prin rezultatele primite din baza de date
            while (rs.next()) {
                StringBuilder row = new StringBuilder();
                row.append(rs.getInt("id")).append(",");
                row.append(rs.getString("driver_name")).append(",");
                row.append(rs.getInt("lap_number")).append(",");
                row.append(rs.getLong("sector_1_ms")).append(",");
                row.append(rs.getLong("sector_2_ms")).append(",");
                row.append(rs.getLong("sector_3_ms")).append(",");
                row.append(rs.getLong("total_time_ms")).append(",");
                row.append(rs.getString("timestamp"));

                // printam randul asamblat in fisier cu linie noua
                pw.println(row.toString());
            }
            System.out.println("Datele au fost exportate cu succes!");
        } catch (Exception e) {
            System.out.println("eroare la exportul csv: " + e.getMessage());
        }
    }
}