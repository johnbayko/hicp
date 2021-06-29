package hicp;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.HICPHeader;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public class HICPWriter
{
    private static final Logger LOGGER =
        Logger.getLogger( HICPWriter.class.getName() );

    public static final byte[] EOL = {'\r', '\n'};
    protected final OutputStream _out;

    public HICPWriter(final OutputStream out) {
        _out = out;
    }

    public HICPWriter writeString(final String s)
        throws IOException
    {
        final HICPHeaderValue writeHV = new HICPHeaderValue(s);
        _out.write(writeHV.getBytes());
        return this;
    }

    public HICPWriter writeHeader(final HICPHeader header)
        throws IOException
    {
        writeString(header.name);

        // If string or bytes contain EOL, then must be multiline. Either with
        // a boundary or length specifier. Boundary is more human readable if
        // it's a string.
        final HICPHeaderValue value = header.value;
        if (value.isBinary) {
            // Use "length" specifier.
            final byte[] bytes = value.getBytes();
            writeString(":: length=" + bytes.length);
            _out.write(bytes);
        } else {
            final String valueStr = value.getString();
            if (-1 == valueStr.indexOf("\r\n")) {
                // Value can be sent on a single line.
                writeString(": " + valueStr);
            } else {
                // Value has a CR LF, then send in multiple lines usnr
                // "boundary".
                writeString(":: boundary=\r\n--\r\n");

                // Escape boundary ("\r\n--"), and single ESC.
                final String esc_value =
                    valueStr.replace("\033", "\033\033");
                    valueStr.replace("\r\n--", "\033\r\n--");
                writeString(valueStr);

                // Write out terminator sequence
                writeString("\r\n--");
            }
        }
        _out.write(EOL);
        return this;
    }

    public HICPWriter writeMessage(final Message m)
        throws IOException
    {
        final Map<HeaderEnum, HICPHeader> headerMap = m.getHeaders();
        // First header has to be event or command, so get and remove that
        // before iterating over other headers.
        {
            final HICPHeader h = headerMap.remove(HeaderEnum.EVENT);
            if (null != h) {
                writeHeader(h);
            }
        }
        // What if there are both? Problem exists elsewhere, just output both
        // here.
        {
            final HICPHeader h = headerMap.remove(HeaderEnum.COMMAND);
            if (null != h) {
                writeHeader(h);
            }
        }
        for (HICPHeader h : headerMap.values()) {
            writeHeader(h);
        }
        // End with blank line.
        _out.write(EOL);
        return this;
    }
}

