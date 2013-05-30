package net.johnsonlau.tool;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateTime {
	public static final String getUtcDateTimeString() {
		Date date = new Date();
		SimpleDateFormat dateFormatGmt = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormatGmt.format(date);
	}
	
	public static final String getUtcDateTimeString(Date date) {
		SimpleDateFormat dateFormatGmt = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormatGmt.format(date);
	}
	
	public static final int getUtcSecondTimestamp(){
		return (int)(new Date().getTime() / 1000);
	}
	
	public static final long getUtcMilliSecondTimestamp(){
		return new Date().getTime();
	}
}
