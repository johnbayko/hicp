package hicp;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HICPHeaderValue {
    private static final Logger LOGGER =
        Logger.getLogger( HICPHeaderValue.class.getName() );

    protected final static CharsetDecoder _decoder =
        StandardCharsets.UTF_8.newDecoder();

    protected final static CharsetEncoder _encoder =
        StandardCharsets.UTF_8.newEncoder();

    final public boolean isBinary;
    protected ByteBuffer _byteBuffer = null;

    protected String _string = null;
    protected byte[] _bytes = null;

    /** Construct an empty object. */
    public HICPHeaderValue() {
        _byteBuffer = null;
        isBinary = false;

        _string = "";
    }

    // TODO maybe should be byte[], and wrap in a ByteBuffer internally?
    public HICPHeaderValue(final ByteBuffer byteBuffer) {
        // Assume there is a string representation unless otherwise specified.
        this(byteBuffer, false);
    }

    public HICPHeaderValue(final ByteBuffer byteBuffer, final boolean newIsBinary) {
        _byteBuffer = byteBuffer;
        isBinary = newIsBinary;

        // This could be binary data with no string representation (e.g.
        // image), and calling code should be able to determine that from
        // header values, so only decode string when getString() is called.
    }

    public HICPHeaderValue(final String string) {
        _string = string;
        isBinary = false;

        // Should always be a binary representation for string.
        try {
            final CharBuffer charBuffer = CharBuffer.wrap(_string);
            _byteBuffer = _encoder.encode(charBuffer);
        } catch (CharacterCodingException ex) {
            // No valid byte representation, it stays null.
        }
    }

    public byte[] getBytes() {
        if (null == _byteBuffer) {
            return null;
        }
        // get() only works once without resetting, so only do it once.
        if (null == _bytes) synchronized(this) {
            _bytes = new byte[_byteBuffer.remaining()];
            _byteBuffer.get(_bytes);
        }
        return _bytes;
    }

    public String getString() {
        if (null == _string) synchronized(this) {
            if (null == _byteBuffer) {
                // No binary representation to try to convert.
                return null;
            }
            try {
                final CharBuffer charBuffer = _decoder.decode(_byteBuffer);
                _string = charBuffer.toString();
            } catch (CharacterCodingException ex) {
                // No string representation of this binary value, let _string
                // remain null, don't need to do anything else.
            }
        }
        return _string;
    }
}
