package com.islamiclifestyle.testproject;

import android.os.Build;

import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.chrono.HijrahChronology;
import java.time.chrono.HijrahDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateConverter {

    public static String convertToHijriDate(String englishDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date date = sdf.parse(englishDate);

            UmmalquraCalendar ummalquraCalendar = new UmmalquraCalendar();
            ummalquraCalendar.setTime(date);

            int year = ummalquraCalendar.get(Calendar.YEAR);
            int month = ummalquraCalendar.get(Calendar.MONTH) + 1;
            int day = ummalquraCalendar.get(Calendar.DAY_OF_MONTH);

            return String.format(Locale.US, "%04d-%02d-%02d", year, month, day);



        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
