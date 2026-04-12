package tn.esprit.tournamentmodule.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyBDConnexion {

    private static final String HOST     = "localhost";
    private static final String PORT     = "3306";
    private static final String DATABASE = "clutchx_db";
    private static final String USER     = "root";
    private static final String PASSWORD = "";  // ← set your MySQL password here

    private static final String URL =
        "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static MyBDConnexion instance;
    private Connection connection;

    private MyBDConnexion() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("[DB] Connected to " + DATABASE);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver not found: " + e.getMessage(), e);
        } catch (SQLException e) {
            throw new RuntimeException("Connection failed: " + e.getMessage(), e);
        }
    }

    public static synchronized MyBDConnexion getInstance() {
        if (instance == null) instance = new MyBDConnexion();
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed())
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException("Re-connect failed: " + e.getMessage(), e);
        }
        return connection;
    }

    public void close() {
        try { if (connection != null && !connection.isClosed()) connection.close(); }
        catch (SQLException ignored) {}
        instance = null;
    }
}
