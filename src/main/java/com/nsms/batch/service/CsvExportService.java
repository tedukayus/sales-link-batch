package com.nsms.batch.service;

import com.nsms.batch.config.DatabaseConfig;
import com.nsms.batch.util.CsvFileWriter;
import com.nsms.batch.util.FileManager;
import com.nsms.batch.util.Logger;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * CSV 出力サービス
 * ワークテーブルから CSV ファイルを生成して出力
 */
public class CsvExportService {
    private static final Logger logger = new Logger(CsvExportService.class);
    private final DatabaseConfig dbConfig;

    public CsvExportService(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * CSV ファイルを出力
     * @param baseDate 基準日（YYYYMMDD形式）
     * @throws SQLException
     * @throws IOException
     */
    public void exportCsvFiles(String baseDate) throws SQLException, IOException {
        logger.info("CSV ファイル出力処理を開始します");

        try (Connection conn = dbConfig.getConnection()) {
            // 出力先フォルダパスを取得
            Map<String, String> folderPaths = getFolderPaths(conn);

            // ヘッダデータを取得
            List<Map<String, Object>> headerList = getHeaderData(conn);

            for (Map<String, Object> header : headerList) {
                String linkBusinessCode = header.get("link_business_code").toString();
                String businessName = header.get("contents").toString();

                logger.info("ビジネスコード: " + linkBusinessCode + ", 業務名: " + businessName);

                // フォルダを作成
                String businessFolder = linkBusinessCode + "　" + businessName;
                createBusinessFolders(folderPaths, businessFolder);

                // 処理中フォルダをクリーンアップ
                cleanupTempFolder(folderPaths, businessFolder);

                // バックアップ処理
                backupExistingFiles(folderPaths, businessFolder);

                // ファイル移動
                moveFilesToLiveFolder(folderPaths, businessFolder);
            }

            logger.info("CSV ファイル出力処理が完了しました");
        } catch (SQLException | IOException e) {
            logger.error("CSV ファイル出力処理中にエラーが発生しました", e);
            throw e;
        }
    }

    /**
     * 出力先フォルダパスを取得
     */
    private Map<String, String> getFolderPaths(Connection conn) throws SQLException {
        Map<String, String> paths = new HashMap<>();
        String query = "SELECT key_code, key_value FROM nsms_own_company " +
                       "WHERE key_code IN ('LINK_FOLDER_PATH_SALES', 'LINK_FOLDER_PATH_SALES_TEMP', 'LINK_FOLDER_PATH_SALES_BK')";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                paths.put(rs.getString("key_code"), rs.getString("key_value"));
            }
        }

        return paths;
    }

    /**
     * ヘッダデータを取得
     */
    private List<Map<String, Object>> getHeaderData(Connection conn) throws SQLException {
        List<Map<String, Object>> headerList = new ArrayList<>();
        String query = "SELECT issuing_org_code, link_business_code, link_creation_date, " +
                       "link_business_code_seq_no, link_count, contents, line_item_count " +
                       "FROM nsms_journal_out_sales_h";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("issuing_org_code", rs.getString("issuing_org_code"));
                map.put("link_business_code", rs.getString("link_business_code"));
                map.put("link_creation_date", rs.getString("link_creation_date"));
                map.put("link_business_code_seq_no", rs.getInt("link_business_code_seq_no"));
                map.put("link_count", rs.getInt("link_count"));
                map.put("contents", rs.getString("contents"));
                map.put("line_item_count", rs.getInt("line_item_count"));
                headerList.add(map);
            }
        }

        return headerList;
    }

    /**
     * ビジネスごとのフォルダを作成
     */
    private void createBusinessFolders(Map<String, String> folderPaths, String businessFolder) throws IOException {
        for (String path : folderPaths.values()) {
            String fullPath = path + File.separator + businessFolder;
            FileManager.createFolder(fullPath);
        }
    }

    /**
     * 処理中フォルダをクリーンアップ
     */
    private void cleanupTempFolder(Map<String, String> folderPaths, String businessFolder) throws IOException {
        String tempFolder = folderPaths.get("LINK_FOLDER_PATH_SALES_TEMP") + File.separator + businessFolder;
        File[] files = FileManager.listFiles(tempFolder);

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    FileManager.deleteFile(file.getAbsolutePath());
                }
            }
        }
    }

    /**
     * 連係用フォルダの既存ファイルをバックアップ
     */
    private void backupExistingFiles(Map<String, String> folderPaths, String businessFolder) throws IOException {
        String liveFolder = folderPaths.get("LINK_FOLDER_PATH_SALES") + File.separator + businessFolder;
        String backupFolder = folderPaths.get("LINK_FOLDER_PATH_SALES_BK") + File.separator + businessFolder;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        File[] files = FileManager.listFiles(liveFolder);
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String newFileName = timestamp + "_" + file.getName();
                    String targetPath = backupFolder + File.separator + newFileName;
                    FileManager.moveFile(file.getAbsolutePath(), targetPath);
                }
            }
        }
    }

    /**
     * 処理中フォルダのファイルを連係用フォルダに移動
     */
    private void moveFilesToLiveFolder(Map<String, String> folderPaths, String businessFolder) throws IOException {
        String tempFolder = folderPaths.get("LINK_FOLDER_PATH_SALES_TEMP") + File.separator + businessFolder;
        String liveFolder = folderPaths.get("LINK_FOLDER_PATH_SALES") + File.separator + businessFolder;

        File[] files = FileManager.listFiles(tempFolder);
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String targetPath = liveFolder + File.separator + file.getName();
                    FileManager.moveFile(file.getAbsolutePath(), targetPath);
                    logger.info("ファイルを移動しました: " + file.getName());
                }
            }
        }
    }
}
