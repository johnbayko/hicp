package hv;

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
public class Logger
    implements hicp.Logger
{
    protected final DateFormat _timestampFormat =
        new SimpleDateFormat("'['yyyy-MM-dd HH:mm:ss.SSSS']'");
    protected final File _logFile;

    public Logger(String logFileName) {
        this(new File(logFileName));
    }

    public Logger(File logFile) {
        // Cheap null check.
        logFile.getClass();
        
        _logFile = logFile;
        log("Start");
    }
    
    public void log(Exception t) {
        log(null, t);
    }

    public void log(String msg, Exception t) {
        log(msg);
        PrintWriter logWriter = null;
        try {
            // Open stream.
            logWriter = newWriter();
            // Write throwable msg
            logWriter.println(t.getMessage());
            // Write stack trace.
            t.printStackTrace(logWriter);
        } catch (IOException ex) {
            // This is the exception logger. If execution gets here, no
            // way to log an exception, so just give up.
            return;
        } finally {
            // Close stream.
            if (null != logWriter) try {
                logWriter.close();
            } catch (Exception ex) {
                // Ignore exception.
            }
        }
    }

    public void log(String msg) {
        PrintWriter logWriter = null;
        try {
            // Open stream.
            logWriter = newWriter();
            // Write timestamp.
            logWriter.println(_timestampFormat.format(new Date()));
            // Write message.
            if (null != msg) {
                logWriter.println(msg);
            }
        } catch (IOException ex) {
            // This is the exception logger. If execution gets here, no
            // way to log an exception, so just give up.
            return;
        } finally {
            // Close stream.
            if (null != logWriter) try {
                logWriter.close();
            } catch (Exception ex) {
                // Ignore exception.
            }
        }
    }

    // Message without timestamp.
    public void add(String msg) {
        PrintWriter logWriter = null;
        try {
            // Open stream.
            logWriter = newWriter();
            // Write message.
            logWriter.println(msg);
        } catch (IOException ex) {
            // This is the exception logger. If execution gets here, no
            // way to log an exception, so just give up.
            return;
        } finally {
            // Close stream.
            if (null != logWriter) try {
                logWriter.close();
            } catch (Exception ex) {
                // Ignore exception.
            }
        }
    }

    protected PrintWriter newWriter()
        throws IOException
    {
        return new PrintWriter(
                new BufferedWriter(new FileWriter(_logFile, true))
            );
    }

    public void removeLog() {
        // If this fails, well, can't do anything about it, so just
        // ignore the result. But log an error to alert user.
        if (!_logFile.delete()) {
            log(null, new Exception("Could not delete log file."));
        }
    }
}
