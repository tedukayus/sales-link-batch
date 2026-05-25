-- =====================================================
-- PostgreSQL PL/pgSQL プロシージャ
-- 売上情報連携バッチ処理
-- =====================================================

-- スキーマ作成（必要に応じて）
CREATE SCHEMA IF NOT EXISTS nsms_link_sales_pac;

-- プロシージャ: LINK_SALES_BAT_EXPORT
-- 入力パラメータ:
--   p_base_date: 基準日（YYYYMMDD形式）
--   p_journal_output_division: 仕訳出力区分
-- 出力パラメータ:
--   p_result_code: 実行結果（1:正常、0:異常）
--   p_process_count: 処理件数

CREATE OR REPLACE PROCEDURE nsms_link_sales_pac.link_sales_bat_export(
    IN p_base_date VARCHAR,
    IN p_journal_output_division VARCHAR,
    OUT p_result_code INTEGER,
    OUT p_process_count INTEGER
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_last_process_date DATE;
    v_pre_last_month DATE;
    v_current_month_end DATE;
    v_error_message VARCHAR;
    v_processed_rows INTEGER := 0;
BEGIN
    -- トランザクション開始
    BEGIN
        -- ==========================================
        -- 1. ワークテーブルのデータ削除
        -- ==========================================
        DELETE FROM nsms_journal_out_sales_h;
        DELETE FROM nsms_journal_out_sales_k;
        DELETE FROM nsms_journal_out_sales_s;
        DELETE FROM nsms_journal_work;
        DELETE FROM nsms_journal_target_slip_id_wk WHERE business_division = '1' AND slip_type = 'SALES';

        -- ==========================================
        -- 2. 各データ取得
        -- ==========================================
        
        -- 2-1. 売上連係回数を取得
        INSERT INTO nsms_journal_out_sales_h (
            issuing_org_code,
            link_business_code,
            link_creation_date,
            link_business_code_seq_no,
            link_count,
            contents,
            line_item_count
        )
        SELECT
            '0001',
            '00101',
            p_base_date,
            COALESCE(MAX(link_business_code_seq_no), 0) + 1,
            COALESCE(MAX(link_count), 0) + 1,
            '売上',
            0
        FROM nsms_journal_out_sales_h;

        -- 2-2. 売上連係データ取得と登録
        INSERT INTO nsms_journal_work (
            issuing_org_code,
            link_business_code,
            business_name,
            transaction_summary,
            data_type,
            debit_credit_division,
            account_code_1,
            account_code_2,
            account_code_3,
            account_code_4,
            account_code_5,
            account_code_6,
            responsibility_org_code,
            customer_code,
            amount,
            tax_classification_code
        )
        SELECT
            '0001',
            '00101',
            '売上',
            '売上',
            '2',
            CASE WHEN hd.sales_amount >= 0 THEN '2' ELSE '1' END,
            dt.account_code,
            '000',
            '000',
            '000',
            '000',
            '000',
            '0001',
            hd.customer_code,
            dt.sales_amount,
            '0'
        FROM nsms_sales_hd hd
        INNER JOIN nsms_sales_detail dt ON hd.sales_slip_id = dt.sales_slip_id
        WHERE hd.deleted_flag = '0'
            AND dt.deleted_flag = '0'
            AND hd.journal_output_flag = '0'
            AND hd.sales_date >= (SELECT MAX(key_value)::DATE + INTERVAL '1 day' FROM nsms_own_company WHERE key_code = 'PRE_LAST_MONTH')
            AND hd.sales_date <= (SELECT MAX(key_value)::DATE FROM nsms_own_company WHERE key_code = 'LAST_MONTH')
            AND (p_journal_output_division IS NULL OR hd.business_division_code LIKE SUBSTR(p_journal_output_division, 1, 3) || '%');

        GET DIAGNOSTICS v_processed_rows = ROW_COUNT;

        -- ==========================================
        -- 3. ファイル更新（ワークテーブル）
        -- ==========================================
        -- ワークテーブルのデータをヘッダテーブルに転送
        INSERT INTO nsms_journal_out_sales_h (
            issuing_org_code,
            link_business_code,
            link_creation_date,
            link_business_code_seq_no,
            link_count,
            contents,
            line_item_count
        )
        SELECT DISTINCT
            issuing_org_code,
            link_business_code,
            link_creation_date::VARCHAR,
            1,
            1,
            'Sales Data',
            COUNT(*)
        FROM nsms_journal_work
        GROUP BY issuing_org_code, link_business_code, link_creation_date;

        -- ==========================================
        -- 4. 仕訳対象データの伝票ID取得
        -- ==========================================
        INSERT INTO nsms_journal_target_slip_id_wk (
            slip_id,
            link_business_code,
            business_division,
            slip_type
        )
        SELECT DISTINCT
            hd.sales_slip_id,
            '00101',
            '1',
            'SALES'
        FROM nsms_sales_hd hd
        WHERE hd.deleted_flag = '0'
            AND hd.journal_output_flag = '0'
            AND hd.sales_date >= (SELECT MAX(key_value)::DATE + INTERVAL '1 day' FROM nsms_own_company WHERE key_code = 'PRE_LAST_MONTH')
            AND hd.sales_date <= (SELECT MAX(key_value)::DATE FROM nsms_own_company WHERE key_code = 'LAST_MONTH');

        -- 成功時の結果を設定
        p_result_code := 1;
        p_process_count := v_processed_rows;

        -- 正常終了ログ
        RAISE NOTICE 'LINK_SALES_BAT_EXPORT プロシージャが正常に完了しました。処理件数: %', v_processed_rows;

    EXCEPTION WHEN OTHERS THEN
        -- エラーハンドリング
        v_error_message := SQLERRM;
        RAISE NOTICE 'エラーが発生しました: %', v_error_message;
        
        -- ロールバック
        ROLLBACK;
        
        -- 異常結果を設定
        p_result_code := 0;
        p_process_count := 0;
    END;

END;
$$;

-- グラント権限設定（必要に応じて）
GRANT EXECUTE ON PROCEDURE nsms_link_sales_pac.link_sales_bat_export(VARCHAR, VARCHAR, OUT INTEGER, OUT INTEGER) TO bead;

COMMIT;
