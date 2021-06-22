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

    protected ByteBuffer _byteBuffer = null;

    protected String _string = null;

    /** Construct an empty object. */
    public HICPHeaderValue() {
        _byteBuffer = null;

        _string = "";
    }

    public HICPHeaderValue(final ByteBuffer byteBuffer) {
        _byteBuffer = byteBuffer;

        // For some reason I've forgotten, can't decode string here, though
        // it looks like byte buffer has the bytes needed. So decode string
        // when getString() is called.
    }

    public HICPHeaderValue(final String string) {
        _string = string;

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
        final byte[] bytes = new byte[_byteBuffer.remaining()];
        _byteBuffer.get(bytes);
        return bytes;
    }

    public String getString() {
        if (null == _string) {
            try {
                final CharBuffer charBuffer = _decoder.decode(_byteBuffer);
                _string = charBuffer.toString();
            } catch (CharacterCodingException ex) {
                // Let _string remain null, don't need to do
                // anything else.
            }
        }
        return _string;
    }
}
