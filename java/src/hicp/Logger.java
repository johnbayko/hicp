package hicp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
    Simple exception logger.
 */
public interface Logger {
    public void log(String msg, Exception t);

    public void log(Exception t);

    public void log(String msg);

    // Message without timestamp.
    public void add(String msg);

    // Remove the log file.
    public void removeLog();
}
