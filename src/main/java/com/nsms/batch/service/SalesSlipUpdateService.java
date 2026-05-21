package com.nsms.batch.service;

import com.nsms.batch.config.DatabaseConfig;
import com.nsms.batch.util.Logger;

import java.sql.*;

/**
 * 売上伝票テーブル更新サービス
 * 仕訳処理完了後の売上伝票テーブルを更新
 */
public class SalesSlipUpdateService {
    private static final Logger logger = new Logger(SalesSlipUpdateService.class);
    private final DatabaseConfig dbConfig;

    public SalesSlipUpdateService(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * 売上伝票テーブルを更新
     * @throws SQLException
     */
    public void updateSalesSlips() throws SQLException {
        logger.info("売上伝票テーブルの更新を開始します");

        try (Connection conn = dbConfig.getConnection()) {
            // 【仕訳】対象伝票ID保持ワークテーブルから対象伝票を取得
            String selectQuery = "SELECT slip_id, link_business_code FROM nsms_journal_target_slip_id_wk " +
                                 "WHERE business_division = '1' AND slip_type = 'SALES'";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(selectQuery)) {

                while (rs.next()) {
                    String slipId = rs.getString("slip_id");
                    String linkBusinessCode = rs.getString("link_business_code");
                    
                    // 売上伝票の仕訳出力フラグを「1：処理済み」に更新
                    updateJournalOutputFlag(conn, slipId);
                    
                    logger.info("売上伝票を更新しました - slipId: " + slipId);
                }
            }

            logger.info("売上伝票テーブルの更新が完了しました");
        } catch (SQLException e) {
            logger.error("売上伝票テーブルの更新中にエラーが発生しました", e);
            throw e;
        }
    }

    /**
     * 仕訳出力フラグを更新
     * @param conn データベース接続
     * @param slipId 伝票ID
     * @throws SQLException
     */
    private void updateJournalOutputFlag(Connection conn, String slipId) throws SQLException {
        String updateQuery = "UPDATE nsms_sales_hd SET journal_output_flag = '1' WHERE sales_slip_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
            stmt.setString(1, slipId);
            stmt.executeUpdate();
        }
    }
}
