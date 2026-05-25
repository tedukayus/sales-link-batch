package com.nsms.batch.service;

import com.nsms.batch.config.DatabaseConfig;
import com.nsms.batch.util.Logger;

import java.sql.*;

/**
 * PL/pgSQL プロシージャ実行サービス
 */
public class ProcedureExecutionService {
    private static final Logger logger = new Logger(ProcedureExecutionService.class);
    private final DatabaseConfig dbConfig;

    public ProcedureExecutionService(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * LINK_SALES_BAT_EXPORT プロシージャを実行
     * @param baseDate 基準日（YYYYMMDD形式）
     * @param journalOutputDivision 仕訳出力区分
     * @return 処理結果（1:正常、0:異常）
     * @throws SQLException
     */
    public int executeLinkSalesBatExport(String baseDate, String journalOutputDivision) throws SQLException {
        logger.info("LINK_SALES_BAT_EXPORT プロシージャを実行します");
        logger.info("基準日: " + baseDate + ", 仕訳出力区分: " + journalOutputDivision);

        try (Connection conn = dbConfig.getConnection()) {
            // プロシージャ呼び出し SQL
            String sql = "{ CALL nsms_link_sales_pac.link_sales_bat_export(?, ?, ?, ?) }";

            try (CallableStatement stmt = conn.prepareCall(sql)) {
                // 入力パラメータ
                stmt.setString(1, baseDate);
                stmt.setString(2, journalOutputDivision);

                // 出力パラメータ
                stmt.registerOutParameter(3, Types.INTEGER);     // 実行結果
                stmt.registerOutParameter(4, Types.INTEGER);     // 処理件数

                // プロシージャ実行
                stmt.execute();

                // 出力パラメータを取得
                int resultCode = stmt.getInt(3);
                int processCount = stmt.getInt(4);

                logger.info("プロシージャ実行完了 - 結果コード: " + resultCode + ", 処理件数: " + processCount);

                if (resultCode == 1) {
                    logger.info("処理は正常に完了しました");
                } else {
                    logger.error("処理中にエラーが発生しました - 結果コード: " + resultCode);
                }

                return resultCode;
            }
        } catch (SQLException e) {
            logger.error("プロシージャ実行中にエラーが発生しました", e);
            throw e;
        }
    }
}
