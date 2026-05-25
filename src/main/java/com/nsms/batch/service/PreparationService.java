package com.nsms.batch.service;

import com.nsms.batch.config.DatabaseConfig;
import com.nsms.batch.util.DateUtil;
import com.nsms.batch.util.Logger;

import java.sql.*;
import java.time.LocalDate;

/**
 * 事前準備サービス
 * 基準日設定、月次実行済みチェック
 */
public class PreparationService {
    private static final Logger logger = new Logger(PreparationService.class);
    private final DatabaseConfig dbConfig;

    public PreparationService(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * 基準日を設定
     * @param targetMonth 対象年月（YYYYMM形式、未設定の場合はnull）
     * @return 基準日（YYYYMMDD形式）
     * @throws SQLException
     */
    public String setBaseDateAndValidate(String targetMonth) throws SQLException {
        logger.info("基準日設定と月次実行済みチェックを開始します");

        try (Connection conn = dbConfig.getConnection()) {
            String baseDate;

            if (targetMonth == null || targetMonth.isEmpty()) {
                // 自動実行：LAST_MONTH を取得
                baseDate = getLastMonthFromDb(conn);
                logger.info("自動実行モード - 基準日: " + baseDate);
            } else {
                // 手動実行：対象年月の月末日を基準日に設定
                baseDate = DateUtil.getLastDayOfMonth(targetMonth);
                logger.info("手動実行モード - 基準日: " + baseDate);
            }

            // 月次実行済みチェック
            validateMonthlyProcessing(conn, baseDate);

            logger.info("基準日設定と月次実行済みチェックが完了しました");
            return baseDate;
        }
    }

    /**
     * LAST_MONTH を取得
     */
    private String getLastMonthFromDb(Connection conn) throws SQLException {
        String query = "SELECT key_value FROM nsms_own_company WHERE key_code = 'LAST_MONTH'";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getString("key_value");
            } else {
                throw new SQLException("LAST_MONTH キーが見つかりません");
            }
        }
    }

    /**
     * 月次実行済みチェック
     * @param conn データベース接続
     * @param baseDate 基準日
     * @throws SQLException
     */
    private void validateMonthlyProcessing(Connection conn, String baseDate) throws SQLException {
        String lastProcessedMonth = getLastProcessedMonth(conn);
        
        // baseDate と lastProcessedMonth を比較
        // baseDate > lastProcessedMonth の場合、月次処理が未実行
        if (baseDate.compareTo(lastProcessedMonth) > 0) {
            String yearMonth = baseDate.substring(0, 6);
            String errorMessage = "対象年月" + yearMonth + "は月次締めが終了しておりません。";
            logger.error(errorMessage);
            throw new SQLException(errorMessage);
        }

        logger.info("月次実行済みチェック: 問題なし");
    }

    /**
     * 最終月次年月を取得
     */
    private String getLastProcessedMonth(Connection conn) throws SQLException {
        String query = "SELECT key_value FROM nsms_own_company WHERE key_code = 'LAST_MONTH'";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getString("key_value");
            } else {
                throw new SQLException("最終月次年月が見つかりません");
            }
        }
    }
}
