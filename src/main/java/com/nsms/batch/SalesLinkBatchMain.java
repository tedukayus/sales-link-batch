package com.nsms.batch;

import com.nsms.batch.config.DatabaseConfig;
import com.nsms.batch.service.*;
import com.nsms.batch.util.Logger;

import java.io.IOException;
import java.sql.SQLException;

/**
 * 売上情報連携バッチプログラム
 * メインクラス（エントリーポイント）
 * 
 * 実行方法：
 * - 自動実行: java SalesLinkBatchMain
 * - 手動実行: java SalesLinkBatchMain 202605
 * - 手動実行+区分指定: java SalesLinkBatchMain 202605 001
 */
public class SalesLinkBatchMain {
    private static final Logger logger = new Logger(SalesLinkBatchMain.class);

    public static void main(String[] args) {
        logger.info("=====================================================");
        logger.info("売上情報連携バッチプログラムを開始します");
        logger.info("=====================================================");

        try {
            // 1. 入力パラメータを取得
            String targetMonth = null;              // 対象年月��YYYYMM形式）
            String journalOutputDivision = null;    // 仕訳出力区分

            if (args.length > 0) {
                targetMonth = args[0];
                logger.info("引数：対象年月 = " + targetMonth);
            }

            if (args.length > 1) {
                journalOutputDivision = args[1];
                logger.info("引数：仕訳出力区分 = " + journalOutputDivision);
            }

            // 2. データベース接続を初期化
            DatabaseConfig dbConfig = new DatabaseConfig();

            // 3. 事前準備処理：基準日設定、月次実行済みチェック
            logger.info("\n[ステップ1] 事前準備処理");
            PreparationService preparationService = new PreparationService(dbConfig);
            String baseDate = preparationService.setBaseDateAndValidate(targetMonth);
            logger.info("基準日が設定されました: " + baseDate);

            // 4. システム連係処理：PL/pgSQL プロシージャ実行
            logger.info("\n[ステップ2] システム連係処理");
            ProcedureExecutionService procedureService = new ProcedureExecutionService(dbConfig);
            int resultCode = procedureService.executeLinkSalesBatExport(baseDate, journalOutputDivision);

            // 5. 結果判定
            if (resultCode != 1) {
                logger.error("システム連係処理が異常終了しました。処理を中断します。");
                System.exit(1);
            }

            // 6. CSV出力処理
            logger.info("\n[ステップ3] CSV出力処理");
            CsvExportService csvExportService = new CsvExportService(dbConfig);
            csvExportService.exportCsvFiles(baseDate);

            // 7. 売上伝票テーブル更新
            logger.info("\n[ステップ4] 売上伝票テーブル更新");
            SalesSlipUpdateService slipUpdateService = new SalesSlipUpdateService(dbConfig);
            slipUpdateService.updateSalesSlips();

            logger.info("\n=====================================================");
            logger.info("売上情報連携バッチプログラムが正常に完了しました");
            logger.info("=====================================================");

        } catch (SQLException e) {
            logger.error("データベース処理エラーが発生しました", e);
            logger.error("\n=====================================================");
            logger.error("バッチプログラムが異常終了しました");
            logger.error("=====================================================");
            System.exit(1);
        } catch (IOException e) {
            logger.error("ファイル処理エラーが発生しました", e);
            logger.error("\n=====================================================");
            logger.error("バッチプログラムが異常終了しました");
            logger.error("=====================================================");
            System.exit(1);
        } catch (Exception e) {
            logger.error("予期しないエラーが発生しました", e);
            logger.error("\n=====================================================");
            logger.error("バッチプログラムが異常終了しました");
            logger.error("=====================================================");
            System.exit(1);
        }
    }
}
