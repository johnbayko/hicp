package hicp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.channels.ClosedByInterruptException;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.message.Message;
import hicp.message.command.Command;

/**
    Starts a thread which converts characters from an input stream into
    message. Another thread can retrieve messages as needed. Also sends
    messages to an output stream.
 */
public class MessageExchange
    extends Thread
{
    private static final Logger LOGGER =
        Logger.getLogger( MessageExchange.class.getName() );

    protected HICPReader _in;
    protected Writer _out;
    protected Controller _controller;

    /**
        If the Java runtime doesn't support the UTF8 encoding, the
        calling code ought to know.
     */
    public MessageExchange(
        InputStream in, OutputStream out, Controller controller//, Logger logger
    )
        throws UnsupportedEncodingException
    {
        _in = new HICPReader(in);
        _out = new OutputStreamWriter(out, "UTF8");
        _controller = controller;

        // Start this up as a thread to read messages.
        this.start();
    }

    public void run() {
        try {
readLoop:   while (null != _in) {
                // Read first header.
                final HICPHeader firstHeader = _in.readHeader();
                if (null == firstHeader) { 
                    // Connecton closed.
log("null == firstHeader");  // debug
                    break readLoop;
                }
                
                // If line is blank or not a header (.name is null),
                // don't do anything.
                if (null != firstHeader.name) {
if (null == firstHeader.value) {  // debug
    log("null == firstHeader.value");  // debug
}  // debug
                    final Message templateMessage =
                        Command.getMessage(firstHeader.value.getString());

                    if (null != templateMessage) {
                        // Make a duplicate - more of the same message may
                        // arrive before this is processed.
                        final Message inMessage =
                            templateMessage.newMessage();

                        inMessage.read(_in);

                        _controller.receivedMessage(inMessage);
                    }
                }
            }
        } catch (IOException ex) {
            // Quietly quit the loop.
        }
        _controller.closed();
    }

    public synchronized MessageExchange send(Message m) {
        if (null == m) {
            return this;
        }
        try {
            m.write(_out);
        } catch (IOException ex) {
            log(ex);
        }

        return this;
    }

    public MessageExchange dispose() {
        _in = null;
        _out = null;

        // Interrupt this thread so that the read loop stops.
        this.interrupt();

        return this;
    }

    // Utility
    private void log(Exception ex) {
        LOGGER.log(Level.WARNING, ex.toString());
    }

    private void log(String msg) {
        LOGGER.log(Level.FINE, msg);
    }
}
