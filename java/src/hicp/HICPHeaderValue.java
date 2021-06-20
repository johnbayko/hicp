package hicp;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;

public class HICPHeaderValue {
    protected final ByteBuffer _byteBuffer;
    protected final CharsetDecoder _decoder;

    protected String _string = null;

    /** Construct an empty object. */
    public HICPHeaderValue() {
        _byteBuffer = null;
        _decoder = null;

        _string = "";
    }

    public HICPHeaderValue(ByteBuffer byteBuffer, CharsetDecoder decoder) {
        _byteBuffer = byteBuffer;
        _decoder = decoder;

        // For some reason I've forgotten, can't decode string here, though
        // it looks like byte buffer has the bytes needed. So decode string
        // when getString() is called.
    }

    public byte[] getBytes() {
        final byte[] bytes = new byte[_byteBuffer.remaining()];
        _byteBuffer.get(bytes);
        return bytes;
    }

    public String getString() {
        if (null == _string) {
            if (null != _decoder) {
                try {
                    final CharBuffer charBuffer = _decoder.decode(_byteBuffer);
                    _string = charBuffer.toString();
                } catch (CharacterCodingException ex) {
                    // Let _string remain null, don't need to do
                    // anything else.
                }
            }
        }
        return _string;
    }
}
