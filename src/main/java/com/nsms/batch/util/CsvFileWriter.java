package com.nsms.batch.util;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;

/**
 * CSV ファイル生成ユーティリティ（Shift_JIS 対応）
 */
public class CsvFileWriter {
    private static final Logger logger = new Logger(CsvFileWriter.class);
    private static final Charset CHARSET = Charset.forName("Shift_JIS");

    /**
     * CSV ヘッダを書き込み
     * @param filePath ファイルパス
     * @param headers ヘッダリスト
     * @throws IOException
     */
    public static void writeHeader(String filePath, List<String> headers) throws IOException {
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), CHARSET))) {
            writer.println(convertToCsv(headers));
            logger.info("CSV ヘッダを書き込みました: " + filePath);
        }
    }

    /**
     * CSV データを追記
     * @param filePath ファイルパス
     * @param data データリスト
     * @throws IOException
     */
    public static void appendData(String filePath, List<String> data) throws IOException {
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(filePath, true), CHARSET))) {
            writer.println(convertToCsv(data));
        }
    }

    /**
     * CSV 形式に変換（カンマ区切り）
     * @param data データリスト
     * @return CSV 文字列
     */
    private static String convertToCsv(List<String> data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.size(); i++) {
            String value = data.get(i);
            if (value != null) {
                // ダブルクォートで囲んで、内部のダブルクォートはエスケープ
                sb.append('"').append(value.replace("\"", "\\\"")).append('"');
            } else {
                sb.append('"').append('"');
            }
            if (i < data.size() - 1) {
                sb.append(',');
            }
        }
        return sb.toString();
    }
}
