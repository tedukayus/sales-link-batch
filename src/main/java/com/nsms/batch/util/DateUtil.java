package com.nsms.batch.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * 日付処理ユーティリティ
 */
public class DateUtil {
    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 対象年月の月末日を取得（YYYYMMDD形式）
     * @param yearMonth 対象年月（YYYYMM形式）
     * @return 月末日（YYYYMMDD形式）
     */
    public static String getLastDayOfMonth(String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth, YEAR_MONTH_FORMAT);
        LocalDate lastDay = ym.atEndOfMonth();
        return lastDay.format(DATE_FORMAT);
    }

    /**
     * 指定日付を次の日付に
     * @param date 日付（YYYYMMDD形式）
     * @return 翌日（YYYYMMDD形式）
     */
    public static String getNextDay(String date) {
        LocalDate currentDate = LocalDate.parse(date, DATE_FORMAT);
        LocalDate nextDate = currentDate.plusDays(1);
        return nextDate.format(DATE_FORMAT);
    }

    /**
     * 対象年月から前月の月末日を取得
     * @param yearMonth 対象年月（YYYYMM形式）
     * @return 前月末日（YYYYMMDD形式）
     */
    public static String getPreviousMonthEnd(String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth, YEAR_MONTH_FORMAT);
        LocalDate previousMonthEnd = ym.minusMonths(1).atEndOfMonth();
        return previousMonthEnd.format(DATE_FORMAT);
    }
}
