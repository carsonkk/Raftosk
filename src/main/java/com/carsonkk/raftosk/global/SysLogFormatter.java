package main.java.com.carsonkk.raftosk.global;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

// Provides custom formatting for log messages
public class SysLogFormatter extends Formatter{
    //region Private Members

    private static final DateFormat dateFormat = new SimpleDateFormat("mm:ss.SSS");

    //endregion

    //region Public Methods

    public String format(LogRecord record) {
        String sourceClassName = "main.java.com.carsonkk.raftosk.";

        StringBuilder stringBuilder = new StringBuilder(2048);
        stringBuilder.append("[").append(record.getThreadID()).append("] ");
        stringBuilder.append("(").append(dateFormat.format(new Date(record.getMillis()))).append(") ");
        stringBuilder.append(record.getSourceClassName().substring(sourceClassName.length())).append(".");
        stringBuilder.append(record.getSourceMethodName()).append("(): ");
        stringBuilder.append("[").append(record.getLevel()).append("] ");
        stringBuilder.append(formatMessage(record));
        stringBuilder.append(System.lineSeparator());

        return stringBuilder.toString();
    }

    //endregion
}
