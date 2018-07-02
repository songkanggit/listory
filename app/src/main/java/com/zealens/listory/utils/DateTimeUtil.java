package com.zealens.listory.utils;

import org.jetbrains.annotations.NonNls;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by songkang on 2018/3/28.
 */

public class DateTimeUtil {
    public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public static SimpleDateFormat sdf_d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static Date string2Date(String date, SimpleDateFormat sdf_t) {
        Date d = null;
        try {
            d = sdf_t.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return d;
    }

    public static String timeStampToDateString(@NonNls String timeStamp, SimpleDateFormat dateFormate) {
        Date d = new Date(Long.valueOf(timeStamp));
        return dateFormate.format(d);
    }

    public static Date addDays(Date date, int days)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days);
        return cal.getTime();
    }
}

