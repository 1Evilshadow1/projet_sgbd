package tn.esprit;

import java.sql.Connection;
import java.sql.DriverManager;

public class DB {
    private static Connection conn = null;

    public static Connection getConn() {
        if (conn == null) {
            try {
                Class.forName("oracle.jdbc.driver.OracleDriver");
                conn = DriverManager.getConnection(
                    "jdbc:oracle:thin:@localhost:1521:XE", // change si besoin
                    "SYSTEM",     // mets ton utilisateur Oracle ici
                    "1590"  // mets ton mot de passe ici
                );
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        return conn;
    }
}