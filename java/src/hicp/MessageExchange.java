package hicp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.message.Message;

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
    protected HICPWriter _out;
    protected Writer _outWriter;  // TODO being replaced by HICPWriter above.
    protected Controller _controller;

    /*
        Written by send (GUI event thread), read by input thread (this).
     */
    protected Message lastMessage;

    /**
        If the Java runtime doesn't support the UTF8 encoding, the
        calling code ought to know.
     */
    public MessageExchange(
        InputStream in, OutputStream out, Controller controller
    )
        throws UnsupportedEncodingException
    {
        _in = new HICPReader(in);
        _out = new HICPWriter(out);
        _outWriter = new OutputStreamWriter(out, "UTF8");
        _controller = controller;

        // Start this up as a thread to read messages.
        this.start();
    }

    public void run() {
        try {
            while (null != _in) {
                final Message message = _in.readMessage();

                if (null != message) { 
                    _controller.receivedMessage(message);
                }
            }
        } catch (IOException ex) {
            // Quietly quit the loop.
        }
        _controller.closed();
    }

    // TODO remove this when all messages are switched over.
    final static Set<Class<? extends hicp.message.Message>> useHICPWriter =
        Set.of(
            hicp.message.event.Authenticate.class,
            hicp.message.event.Connect.class
        );
    /*
        Typically called from GUI event thread.
     */
    public synchronized MessageExchange send(Message m) {
        if (null == m) {
            return this;
        }
        try {
            if ( useHICPWriter.contains(m.getClass()) ) {
                _out.writeMessage(m);
            } else {
                m.write(_outWriter);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, ex.toString());
        }
        lastMessage = m;

        return this;
    }

    public Message getLastMessage() {
        return lastMessage;
    }

    public MessageExchange dispose() {
        _in = null;
        _out = null;
        _outWriter = null;

        // Interrupt this thread so that the read loop stops.
        this.interrupt();

        return this;
    }
}
