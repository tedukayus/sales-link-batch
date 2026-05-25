package com.nsms.batch.config;

import com.nsms.batch.util.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * PostgreSQL データベース接続設定
 */
public class DatabaseConfig {
    private static final Logger logger = new Logger(DatabaseConfig.class);
    
    private static final String DB_HOST = "localhost";
    private static final int DB_PORT = 5432;
    private static final String DB_NAME = "BEADDB_MUT";
    private static final String DB_USER = "bead";
    private static final String DB_PASSWORD = "bead";
    private static final String JDBC_URL = String.format(
        "jdbc:postgresql://%s:%d/%s", DB_HOST, DB_PORT, DB_NAME
    );

    static {
        try {
            Class.forName("org.postgresql.Driver");
            logger.info("PostgreSQL JDBC ドライバをロードしました");
        } catch (ClassNotFoundException e) {
            logger.error("PostgreSQL JDBC ドライバのロードに失敗しました", e);
        }
    }

    /**
     * データベース接続を取得
     * @return Connection オブジェクト
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
            logger.info("データベースに接続しました");
            return conn;
        } catch (SQLException e) {
            logger.error("データベース接続に失敗しました", e);
            throw e;
        }
    }
}
