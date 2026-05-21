package com.nsms.batch.service;

import com.nsms.batch.config.DatabaseConfig;
import com.nsms.batch.util.CsvFileWriter;
import com.nsms.batch.util.FileManager;
import com.nsms.batch.util.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private static final String OUTPUT_FOLDER_PATH = "C:\\temp";

    public CsvExportService(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * CSV ファイルを出力
     * @param baseDate 基準日（YYYYMMDD形式）
     * @param journalOutputDivision 仕訳出力区分
     * @throws SQLException
     * @throws IOException
     */
    public void exportCsvFiles(String baseDate, String journalOutputDivision) throws SQLException, IOException {
        logger.info("CSV ファイル出力処理を開始します");

        try (Connection conn = dbConfig.getConnection()) {
            // 出力先フォルダパスを取得
            Map<String, String> folderPaths = getFolderPaths(conn);

            // ヘッダデータを取得
            List<Map<String, Object>> headerList = getHeaderData(conn);

            for (Map<String, Object> header : headerList) {
                String issuingOrgCode = header.get("issuing_org_code").toString();
                String linkBusinessCode = header.get("link_business_code").toString();
                String businessName = header.get("contents").toString();

                logger.info("ビジネスコード: " + linkBusinessCode + ", 業務名: " + businessName);

                // フォルダを作成
                String businessFolder = linkBusinessCode + "　" + businessName;
                createBusinessFolders(folderPaths, businessFolder);

                // 処理中フォルダのファイルをチェック・削除
                cleanupTempFolder(folderPaths, businessFolder);

                // 連係用フォルダ内のファイルをバックアップフォルダに移動
                backupExistingFiles(folderPaths, businessFolder);

                // ヘッダ CSV ファイルを作成
                createHeaderCsvFile(conn, folderPaths, header, businessFolder);

                // 共通・出納 CSV ファイルを作成
                createCommonCsvFiles(conn, folderPaths, header, businessFolder);

                // 会計 CSV ファイルを作成
                createAccountingCsvFiles(conn, folderPaths, header, businessFolder);

                // 処理中フォルダから連係用フォルダへ移動
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
     * ヘッダ CSV ファイルを作成
     */
    private void createHeaderCsvFile(Connection conn, Map<String, String> folderPaths, 
                                     Map<String, Object> header, String businessFolder) throws IOException {
        String linkBusinessCode = header.get("link_business_code").toString();
        int seqNo = (int) header.get("link_business_code_seq_no");
        
        String fileName = linkBusinessCode + "_h_" + seqNo + ".csv";
        String filePath = folderPaths.get("LINK_FOLDER_PATH_SALES_TEMP") + File.separator + 
                         businessFolder + File.separator + fileName;

        List<String> headers = Arrays.asList(
            "発行組織コード", "連係業務コード", "連係作成年月日", "連係業務コード追番",
            "連係回数", "内容", "出納情報件数"
        );

        List<String> data = Arrays.asList(
            header.get("issuing_org_code").toString(),
            header.get("link_business_code").toString(),
            header.get("link_creation_date").toString(),
            header.get("link_business_code_seq_no").toString(),
            header.get("link_count").toString(),
            header.get("contents").toString(),
            header.get("line_item_count").toString()
        );

        CsvFileWriter.writeHeader(filePath, headers);
        CsvFileWriter.appendData(filePath, data);

        logger.info("ヘッダ CSV ファイルを作成しました: " + filePath);
    }

    /**
     * 共通・出納 CSV ファイルを作成
     */
    private void createCommonCsvFiles(Connection conn, Map<String, String> folderPaths,
                                      Map<String, Object> header, String businessFolder) throws SQLException, IOException {
        String query = "SELECT * FROM nsms_journal_out_sales_s ORDER BY file_number";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            List<String> headers = Arrays.asList(
                "発行組織コード", "連係業務コード", "連係作成年月日", "連係業務コード追番",
                "連係回数", "連係NO", "処理区分", "会計整理年月", "取引年月日", "備考",
                "エントリ金額", "会計整理情報件数", "ファイル番号"
            );

            int currentFileNumber = -1;
            String currentFileName = null;
            String currentFilePath = null;

            while (rs.next()) {
                int fileNumber = rs.getInt("file_number");

                if (fileNumber != currentFileNumber) {
                    currentFileNumber = fileNumber;
                    String linkBusinessCode = header.get("link_business_code").toString();
                    int seqNo = (int) header.get("link_business_code_seq_no");
                    
                    currentFileName = linkBusinessCode + "_s_" + seqNo + ".csv";
                    currentFilePath = folderPaths.get("LINK_FOLDER_PATH_SALES_TEMP") + File.separator + 
                                     businessFolder + File.separator + currentFileName;

                    // ヘッダを書き込み
                    CsvFileWriter.writeHeader(currentFilePath, headers);
                }

                List<String> data = Arrays.asList(
                    rs.getString("issuing_org_code"),
                    rs.getString("link_business_code"),
                    rs.getString("link_creation_date"),
                    rs.getString("link_business_code_seq_no"),
                    rs.getString("link_count"),
                    rs.getString("link_number"),
                    rs.getString("process_division"),
                    rs.getString("accounting_settlement_month"),
                    rs.getString("transaction_date"),
                    rs.getString("remarks"),
                    rs.getString("entry_amount"),
                    rs.getString("accounting_settlement_info_count"),
                    rs.getString("file_number")
                );

                CsvFileWriter.appendData(currentFilePath, data);
            }
        }
    }

    /**
     * 会計 CSV ファイルを作成
     */
    private void createAccountingCsvFiles(Connection conn, Map<String, String> folderPaths,
                                         Map<String, Object> header, String businessFolder) throws SQLException, IOException {
        String query = "SELECT * FROM nsms_journal_out_sales_k ORDER BY file_number";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            List<String> headers = Arrays.asList(
                "発行組織コード", "連係業務コード", "連係作成年月日", "連係業務コード追番",
                "連係回数", "連係NO", "仕訳NO", "細目NO", "貸借区分", "科目コード1",
                "科目コード2", "科目コード3", "科目コード4", "科目コード5", "科目コード6",
                "責任組織1コード", "取引先コード", "税込額", "消費税額", "金額",
                "消費税区分コード", "ファイル番号"
            );

            int currentFileNumber = -1;
            String currentFileName = null;
            String currentFilePath = null;

            while (rs.next()) {
                int fileNumber = rs.getInt("file_number");

                if (fileNumber != currentFileNumber) {
                    currentFileNumber = fileNumber;
                    String linkBusinessCode = header.get("link_business_code").toString();
                    int seqNo = (int) header.get("link_business_code_seq_no");
                    
                    currentFileName = linkBusinessCode + "_k_" + seqNo + ".csv";
                    currentFilePath = folderPaths.get("LINK_FOLDER_PATH_SALES_TEMP") + File.separator + 
                                     businessFolder + File.separator + currentFileName;

                    CsvFileWriter.writeHeader(currentFilePath, headers);
                }

                List<String> data = new ArrayList<>();
                for (String header_name : headers) {
                    data.add(rs.getString(header_name) != null ? rs.getString(header_name) : "");
                }

                CsvFileWriter.appendData(currentFilePath, data);
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
