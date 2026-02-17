package com.nexora.bank.Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDB {
    private String url = "jdbc:mysql://localhost:3306/projetpidev";
    private String user = "root";
    private String password = "";

    public Connection conn ;
    private static MyDB instance;

    public static MyDB getInstance() {
        if (instance == null) {
            instance = new MyDB();
        }
        return instance;
    }

    public Connection getConn() {
        return conn;
    }

    public MyDB() {
        try {
            this.conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connection réussi avec succes" );

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
