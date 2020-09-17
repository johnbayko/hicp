package hicp;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;

public class HICPHeaderValue {
    protected final ByteBuffer _byteBuffer;
    protected final CharsetDecoder _decoder;

    protected byte[] _bytes = null;
    protected String _string = null;

    /** Construct an empty object. */
    public HICPHeaderValue() {
        _bytes = new byte[0];
        _string = "";

        _byteBuffer = null;
        _decoder = null;
    }

    public HICPHeaderValue(ByteBuffer byteBuffer, CharsetDecoder decoder) {
        _byteBuffer = byteBuffer;
        _decoder = decoder;
    }

    public byte[] getBytes() {
        if (null == _bytes) {
            _bytes = new byte[_byteBuffer.position()];
            System.arraycopy(_byteBuffer.array(), 0, _bytes, 0, _bytes.length);
        }
        return _bytes;
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
