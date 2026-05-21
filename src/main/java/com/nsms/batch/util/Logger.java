package com.nsms.batch.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ログ出力ユーティリティ
 */
public class Logger {
    private static final String LOG_LEVEL_DEBUG = "DEBUG";
    private static final String LOG_LEVEL_INFO = "INFO";
    private static final String LOG_LEVEL_WARN = "WARN";
    private static final String LOG_LEVEL_ERROR = "ERROR";
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final Class<?> clazz;

    public Logger(Class<?> clazz) {
        this.clazz = clazz;
    }

    /**
     * DEBUG レベルのログを出力
     * @param message メッセージ
     */
    public void debug(String message) {
        log(LOG_LEVEL_DEBUG, message, null);
    }

    /**
     * INFO レベルのログを出力
     * @param message メッセージ
     */
    public void info(String message) {
        log(LOG_LEVEL_INFO, message, null);
    }

    /**
     * WARN レベルのログを出力
     * @param message メッセージ
     */
    public void warn(String message) {
        log(LOG_LEVEL_WARN, message, null);
    }

    /**
     * ERROR レベルのログを出力
     * @param message メッセージ
     * @param exception 例外
     */
    public void error(String message, Exception exception) {
        log(LOG_LEVEL_ERROR, message, exception);
    }

    /**
     * ERROR レベルのログを出力（例外なし）
     * @param message メッセージ
     */
    public void error(String message) {
        log(LOG_LEVEL_ERROR, message, null);
    }

    /**
     * ログを出力
     * @param level ログレベル
     * @param message メッセージ
     * @param exception 例外
     */
    private void log(String level, String message, Exception exception) {
        String timestamp = SDF.format(new Date());
        String className = clazz.getSimpleName();

        System.out.println(String.format("[%s] %s [%s] %s", timestamp, level, className, message));

        if (exception != null) {
            StringWriter sw = new StringWriter();
            exception.printStackTrace(new PrintWriter(sw));
            System.err.println(sw.toString());
        }
    }
}
