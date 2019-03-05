package com.wk.middleware.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Created by jince on 2018/12/25.
 */
public final class DateUtil {
    public static final String FMT_YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    public static final String FMT_YYYY_MM_DD = "yyyy-MM-dd";

    private DateUtil() {
    }

    public static String toStr(Date date, String format) {

        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate().format(DateTimeFormatter.ofPattern(format));
        } else {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(format));
        }
    }

    public static String toStr(LocalDate date, String format) {
        return date.format(DateTimeFormatter.ofPattern(format));
    }

    public static String now(String format) {

        return format.contains("HH") ? LocalDateTime.now().format(DateTimeFormatter.ofPattern(format)) :
                LocalDate.now().format(DateTimeFormatter.ofPattern(format));
    }

    public static Date toDate(String str, String format) {


        if (format.contains("HH")) {
            LocalDateTime localDateTime = LocalDateTime.parse(str, DateTimeFormatter.ofPattern(format));
            return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        } else {
            LocalDate localDate = LocalDate.parse(str, DateTimeFormatter.ofPattern(format));
            return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
    }

    /**
     * date1是否大于等于date2
     *
     * @param date1
     * @param date2
     * @return
     */
    public static boolean gte(Date date1, Date date2) {
        return date1.equals(date2) || date1.after(date2);
    }
}
