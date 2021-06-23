package hicp;

import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.message.event.Event;

public class HICPWriter
{
    private static final Logger LOGGER =
        Logger.getLogger( HICPWriter.class.getName() );

    protected final OutputStream _out;

    public HICPWriter(final OutputStream out) {
        _out = out;
    }

    public HICPWriter writeEvent(final Event event) {
        return this;
    }
}

