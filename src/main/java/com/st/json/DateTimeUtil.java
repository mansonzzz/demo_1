package com.st.json;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.*;

public class DateTimeUtil {
	public static final DateTimeFormatter ISO8601DateTimeformatter;
	public static final DateTimeFormatter ISO8601DateTimeformatterNoMS;
	public static final DateTimeFormatter UTCDateTimeformatter;
	public static final DateTimeFormatter standardDateTimeFormatter;
	public static final DateTimeFormatter dateFormatter;
	public static final DateTimeFormatter dateFormatterNoDash;
	private static final DateTimeParser[] dateTimeParsers;
	private static final DateTimeFormatter dateTimeFormatter;

	public DateTimeUtil() {
	}

	public static DateTime parseDateTime(String text) {
		return dateTimeFormatter.parseDateTime(text);
	}

	static {
		DateTimeZone.setDefault(DateTimeZone.forOffsetHours(8));
		ISO8601DateTimeformatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		ISO8601DateTimeformatterNoMS = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ");
		UTCDateTimeformatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss zzz");
		standardDateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
		dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
		dateFormatterNoDash = DateTimeFormat.forPattern("yyyyMMdd");
		dateTimeParsers = new DateTimeParser[]{ISO8601DateTimeformatter.getParser(), ISO8601DateTimeformatterNoMS.getParser(), UTCDateTimeformatter.getParser(), standardDateTimeFormatter.getParser(), dateFormatter.getParser(), dateFormatterNoDash.getParser()};
		dateTimeFormatter = (new DateTimeFormatterBuilder()).append((DateTimePrinter)null, dateTimeParsers).toFormatter();
	}
}
