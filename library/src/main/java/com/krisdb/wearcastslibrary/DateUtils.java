package com.krisdb.wearcastslibrary;

import android.content.Context;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {
    private static String dateFormat = "yyyy-MM-dd HH:mm:ss";

    public static Date ConvertDate(final String date) {
        return ConvertDate(date, dateFormat);
    }

    public static Date ConvertDate(final String date, final String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.ENGLISH);
        try {
            return sdf.parse(date);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String GetTime(final Date date)
    {
        final DateFormat time = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
        time.setTimeZone(TimeZone.getDefault());
        return time.format(date);
    }

    public static String FormatPositionTime(final int position) {
        long second = (position / 1000) % 60;
        long minute = (position / (1000 * 60)) % 60;
        long hour = (position / (1000 * 60 * 60)) % 24;

        if (hour < 1)
            return String.format(Locale.ENGLISH, "%d:%02d", minute, second);
        else
            return String.format(Locale.ENGLISH, "%d:%02d:%02d", hour, minute, second);

        //Date date = new Date(position);
        //DateFormat formatter = new SimpleDateFormat(position > 3600000 ? "h:mm:ss" : "mm:ss");

        //return formatter.format(date);
    }

    public static int getMilliseconds(String time)
    {
        try {
            final String[] tokens = time.split(":");
            final int secondsToMs = Integer.parseInt(tokens[tokens.length == 2 ? 1 : 2]) * 1000;
            final int minutesToMs = Integer.parseInt(tokens[tokens.length == 2 ? 0 : 1]) * 60000;
            final int hoursToMs = tokens.length == 2 ? 0 : Integer.parseInt(tokens[0]) * 3600000;

            return secondsToMs + minutesToMs + hoursToMs;
        }
        catch(Exception ignored){}

        return 0;
    }

    public static String GetDuration(final int duration)
    {
        final Date date = new Date(duration);
        SimpleDateFormat df = new SimpleDateFormat( duration >= 3600000 ? "h:mm:ss" : "mm:ss", Locale.ENGLISH);

        return df.format(date);
    }

    public static String FormatDate(final String date) {
        return FormatDate(date, "EEE, dd MMM yyyy HH:mm:ss zzz", dateFormat);
    }

    public static String FormatDate(final String dateStr, String patternFrom, String patternTo) {
        try {
            SimpleDateFormat dateParser = new SimpleDateFormat(patternFrom, Locale.ENGLISH);
            SimpleDateFormat dateFormatter = new SimpleDateFormat(patternTo, Locale.ENGLISH);
            Date date = dateParser.parse(dateStr);

            return dateFormatter.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static int minutesDiff(final Date earlierDate, final Date laterDate) {
        return (int) ((laterDate.getTime() - earlierDate.getTime()) / 60000);
    }

    public static String GetDisplayDate(final Context ctx, final String date) {
        return GetDisplayDate(ctx, date, dateFormat);
    }

    public static String GetDisplayDate(final Context ctx, final String date, final String format)
    {
        final Date itemDateTime = ConvertDate(date, format);

        if (itemDateTime == null) return "";

        if (isToday(itemDateTime))
            return ctx.getString(R.string.today);

        if (isYesterday(itemDateTime))
            return ctx.getString(R.string.yesterday);

        Calendar calItem = Calendar.getInstance(Locale.ENGLISH);
        calItem.setTime(itemDateTime);

        Calendar calToday = Calendar.getInstance(Locale.ENGLISH);
        calToday.setTime(new Date());

        if (calItem.get(Calendar.YEAR) == calToday.get(Calendar.YEAR))
            return new SimpleDateFormat("MMM dd", Locale.ENGLISH).format(itemDateTime);

        return new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH).format(itemDateTime);
    }

    public static long daysBetween(Calendar startDate, Calendar endDate)
    {
        Calendar date = (Calendar)startDate.clone();
        long daysBetween = 0;

        while (date.before(endDate))
        {
            date.add(Calendar.DAY_OF_MONTH, 1);
            daysBetween++;
        }

        return daysBetween;
    }

    private static boolean isSameDay(final Date date1, final Date date2)
    {
        final Calendar cal1 = Calendar.getInstance(Locale.ENGLISH);
        cal1.setTime(date1);

        final Calendar cal2 = Calendar.getInstance(Locale.ENGLISH);
        cal2.setTime(date2);

        return isSameDay(cal1, cal2);
    }

    public static String GetDate()
    {
        Calendar c = Calendar.getInstance(Locale.ENGLISH);

        SimpleDateFormat df = new SimpleDateFormat(dateFormat, Locale.ENGLISH);
        return df.format(c.getTime());
    }

    public static boolean isSameDay(final Calendar cal1, final Calendar cal2)
    {
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }

    public static long TimeSince(final Date date)
    {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);

        return  (date.getTime() - cal.getTime().getTime()) / 1000;
    }

    public static boolean isYesterday(final Date date)
    {
        final Calendar itemDate = Calendar.getInstance(Locale.ENGLISH);
        itemDate.setTime(date);

        final Calendar yesterday = Calendar.getInstance(Locale.ENGLISH);
        yesterday.add(Calendar.DATE, -1);

        return isSameDay(itemDate, yesterday);
    }

    public static boolean isToday(final Date date) {
        return isSameDay(date, Calendar.getInstance(Locale.ENGLISH).getTime());
    }
}
