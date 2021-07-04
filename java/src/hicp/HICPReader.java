package hicp;

import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.message.HeaderEnum;
import hicp.message.Message;
import hicp.message.command.*;
import hicp.message.event.*;

public class HICPReader
{
    private static final Logger LOGGER =
        Logger.getLogger( HICPReader.class.getName() );

    protected final InputStream _in;

    protected boolean _isPreviousByte = false;
    protected int _previousByte = 0;
    protected int _avgTokenByteCnt = 16;

    protected static final Acceptor _headerNameAcceptor =
        new TokenAcceptor(':');

    protected static final Acceptor _separatorAcceptor =
        new SeparatorAcceptor();

    protected static final Acceptor _headerValueAcceptor =
        new TokenAcceptor();

    protected static final Acceptor _termCritAcceptor =
        new TokenAcceptor('=');

    protected static final Acceptor _termSeqAcceptor =
        new TermSeqAcceptor();

    protected final HICPHeaderValue EMPTY_HEADER_VALUE = new HICPHeaderValue();

    public HICPReader(InputStream in) {
        _in = in;
    }

    /**
        Read until end is reached, determined by Acceptor. End
        characters are consumed, but not returned as part of the token.
        There are no escapes in the input.
     */
    public HICPHeaderValue readToken(Acceptor acceptor, String eofString)
        throws IOException
    {
        return readToken(acceptor, eofString, false);
    }

    /**
        Read until end is reached, determined by Acceptor. End
        characters are consumed, but not returned as part of the token.
        If isEscapeProcessing is true, ESC characters indicate that the
        following character must always be accepted, resetting the
        acceptor in the process.
     */
    public HICPHeaderValue readToken(
        Acceptor acceptor, String eofString, boolean isEscapeProcessing
    )
        throws IOException
    {
        ByteBuffer readByteBuffer = ByteBuffer.allocate(_avgTokenByteCnt + 4);
        int inByte;
        TokenIndicator tokenIndicator = TokenIndicator.IS_PART;

        boolean isAfterEscape = false;
        acceptor.reset();
readLoop:
        for (;;) {
            inByte = readByte();

            if (-1 == inByte) {
                // End of file, no more input.
                if (0 == readByteBuffer.position()) {
                    // There no input, return null.
                    return null;
                } else {
                    // There was some input, treat as end of token so
                    // input so far can be processed.
                    break readLoop;
                }
            }
            if (0x1b == inByte) {
                // Escape, next character is always accepted.
                isAfterEscape = true;
            } else {
                try {
                    if (!readByteBuffer.hasRemaining()) {
                        // No more space, double buffer size before adding.
                        ByteBuffer newReadByteBuffer =
                            ByteBuffer.allocate(readByteBuffer.capacity() * 2);

                        // Flip read buffer from input to output.
                        readByteBuffer.flip();
                        newReadByteBuffer.put(readByteBuffer);

                        readByteBuffer = newReadByteBuffer;
                    }
                    if (isAfterEscape) {
                        // Character after escape must always be
                        // accepted. This also means that acceptor must
                        // start over.
                        tokenIndicator = TokenIndicator.IS_PART;
                        acceptor.reset();
                    } else {
                        tokenIndicator = acceptor.accept((byte)inByte);
                    }

                    if ( (TokenIndicator.IS_PART == tokenIndicator)
                      || (TokenIndicator.IS_END == tokenIndicator) )
                    {
                        // Byte is part of current token.
                        readByteBuffer.put((byte)inByte);
                    }
                } catch (BufferOverflowException ex) {
                    // Should never happen because available space is checked
                    // before adding bytes, but if it does don't try reading any
                    // more, just return what's been read.
                    break readLoop;
                }

                if (TokenIndicator.NOT_PART == tokenIndicator) {
                    // No more bytes for this token.
                    break readLoop;
                }
                if (TokenIndicator.IS_END == tokenIndicator) {
                    // No more bytes for this token. End bytes are not part
                    // of token, but had to be stored for parsing. Remove
                    // them before breaking out of read loop by setting read
                    // position back before end token
                    readByteBuffer
                        .position(
                            readByteBuffer.position() - acceptor.getEndLength()
                        );
                    break readLoop;
                }
                isAfterEscape = false;
            }
        } // for (;;)

        if (TokenIndicator.NOT_PART == tokenIndicator) {
            // Byte just read is part of next token.
            unreadByte(inByte);
        }

        _avgTokenByteCnt =
            (_avgTokenByteCnt + readByteBuffer.position() + 1) / 2;

        // Flip ByteBuffer from input to output.
        readByteBuffer.flip();
        return new HICPHeaderValue(readByteBuffer);
    }

    /**
        Skip until end is reached, determined by Acceptor. End
        characters are consumed.
     */
    public void skipToken(Acceptor acceptor)
        throws IOException
    {
        int inByte;
        TokenIndicator tokenIndicator = TokenIndicator.IS_PART;

        acceptor.reset();
readLoop:
        for (;;) {
            inByte = readByte();

            if (-1 == inByte) {
                // End of file, no more input. Treat as end of token.
                return;
            }
            tokenIndicator = acceptor.accept((byte)inByte);

            if ( (TokenIndicator.NOT_PART == tokenIndicator)
              || (TokenIndicator.IS_END == tokenIndicator) )
            {
                // No more bytes for this token.
                break readLoop;
            }
        } // for (;;)

        if (TokenIndicator.NOT_PART == tokenIndicator) {
            // Byte just read is part of next token.
            unreadByte(inByte);
        }
    }

    protected int readByte()
        throws IOException
    {
        if (_isPreviousByte) {
            _isPreviousByte = false;
            return _previousByte;
        } else {
            return _in.read();
        }
    }

    protected void unreadByte(int inByte)
    {
        _isPreviousByte = true;
        _previousByte = inByte;
    }

    public HICPHeader readHeader()
        throws IOException
    {
        final String headerName;
        HICPHeaderValue headerValue = EMPTY_HEADER_VALUE;
        {
            final HICPHeaderValue headerNameToken =
                readToken(_headerNameAcceptor, null);

            if (null == headerNameToken) {
                // End of file.
                return null;
            }
            headerName = headerNameToken.getString();
        }
        if ("".equals(headerName)) {
            // Blank line - no name or field.
            return new HICPHeader();
        }
        {
            final HICPHeaderValue separatorToken =
                readToken(_separatorAcceptor, "");

            if ("".equals(separatorToken.getString())) {
                // No separator, no header value. For now, leave value as "".
            } else if ("::".equals(separatorToken.getString())) {
                final String terminationCriterion =
                    readToken(_termCritAcceptor, "").getString();
                if ("length".equals(terminationCriterion)) {
                    // Skip the "=".
                    readByte();
                    // Read the rest of the line and parse an integer
                    // out of it.
                    final String lengthString =
                        readToken(_headerValueAcceptor, "0").getString();
                    try {
                        final int length = Integer.parseInt(lengthString);

                        // EOL should have been accepted already (no
                        // unread bytes waiting for readByte()), so
                        // just fill a buffer from the input stream.
                        final byte[] valueBytes = new byte[length];

                        _in.read(valueBytes);

                        final ByteBuffer valueBuffer =
                            ByteBuffer.wrap(valueBytes);
                        headerValue =
                            new HICPHeaderValue(valueBuffer);

                        // Read final EOL and discard.
                        skipToken(_headerValueAcceptor);

                    } catch (NumberFormatException ex) {
                        // No valid length, leave header value as null
                        // default.  Don't try and read the body because
                        // there's no way to know how long it is.
                    }
                } else if ("boundary".equals(terminationCriterion)) {
                    // Skip the "=".
                    readByte();

                    byte[] termSeqBytes =
                        readToken(_termSeqAcceptor, "").getBytes();

                    if ( (2 <= termSeqBytes.length)
                      && ('\r' == termSeqBytes[0])
                      && ('\n' == termSeqBytes[1])
                    ) {
                        /*
                           Special case of termination sequence
                           beginning with CR LF, means the next line has
                           to be appended to it.
                           E.g:
                             abc:: boundary=\r\n
                             --\r\n
                             one\r\n
                             two\r\n
                             --\r\n
                           First read is: "\r\n"
                           Second read is "--\r\n"
                           Termination sequence is "\r\n--\r\n".
                           Value is "one\r\ntwo" (no terminating "\r\n")
                         */
                        final byte[] appendTermSeqBytes =
                            readToken(_termSeqAcceptor, "").getBytes();
                        final byte[] newTermSeqBytes =
                            new byte[
                                termSeqBytes.length + appendTermSeqBytes.length
                            ];
                        System.arraycopy(
                            termSeqBytes, 0,
                            newTermSeqBytes, 0,
                            termSeqBytes.length
                        );
                        System.arraycopy(
                            appendTermSeqBytes, 0,
                            newTermSeqBytes, termSeqBytes.length,
                            appendTermSeqBytes.length
                        );
                        termSeqBytes = newTermSeqBytes;
                    }
                    headerValue =
                        readToken(
                            new BoundaryAcceptor(termSeqBytes), ""
                        );
                } else {
                    // Not valid termination criterion. Should skip to
                    // end, if not at EOL (true if "=" was the last
                    // character read, _isPreviousByte will be true).
                    if (_isPreviousByte) {
                        skipToken(_headerValueAcceptor);
                    }
                }
            } else if (":".equals(separatorToken.getString())) {
                headerValue = readToken(_headerValueAcceptor, "");
            } else {
                // No separator, skip to the end of the line.
                skipToken(_headerValueAcceptor);
            }
        }
        return new HICPHeader(headerName, headerValue);
    }

    public HeaderMap readHeaderMap()
        throws IOException
    {
        final HeaderMap headerMap = new HeaderMap();
        for (;;) {
            final HICPHeader header = readHeader();

            if ((null == header) || (null == header.name)) {
                // End of headers, end of message.
                return headerMap;
            }
            final HeaderEnum headerEnum = HeaderEnum.getEnum(header.name);

            if (null != headerEnum) {
                headerMap.put(headerEnum, header);
            }
        }
    }

    public Message newCommand(final HeaderMap headerMap)
        throws IOException
    {
        final String cmdHeader = headerMap.getString(HeaderEnum.COMMAND);
        if (null == cmdHeader) {
            // No actual command.
            return null;
        }
        final CommandEnum command = CommandEnum.getEnum(cmdHeader);
        switch (command) {
          // TODO Message clss hierarcy will be unified to just Message with
          // usage specific info objects, which will make this mess go away.
          // But it's needed until then.
          case ADD:
            {
                final HICPHeader categoryHeader =
                    headerMap.getHeader(HeaderEnum.CATEGORY);
                if (null == categoryHeader) {
                    // No category.
                    return null;
                }
                final ItemCommand.CategoryEnum category =
                    ItemCommand.CategoryEnum.getEnum(
                        categoryHeader.value.getString()
                    );
                switch (category) {
                  case TEXT:
                    return new Message(command.name, headerMap);
                  case GUI:
                    return new Add(command.name).addHeaders(headerMap);
                  default:
                    return null;
                }
            }
          case MODIFY:
            {
                final HICPHeader categoryHeader =
                    headerMap.getHeader(HeaderEnum.CATEGORY);
                if (null == categoryHeader) {
                    // No category.
                    return null;
                }
                final ItemCommand.CategoryEnum category =
                    ItemCommand.CategoryEnum.getEnum(
                        categoryHeader.value.getString()
                    );
                switch (category) {
                  case TEXT:
                    return new Message(command.name, headerMap);
                  case GUI:
                    return new Modify(command.name).addHeaders(headerMap);
                  default:
                    return null;
                }
            }
          case AUTHENTICATE:
          case REMOVE:
          case DISCONNECT:
            return new Message(command.name, headerMap);
          // Is there a warning if an enum switch is missing an item?
          // If not, add a default here.
        }
        return null;
    }

    public Event newEvent(final HeaderMap headerMap)
        throws IOException
    {
        final HICPHeader h = headerMap.getHeader(HeaderEnum.EVENT);
        if (null == h) {
            // No actual event.
            return null;
        }
        final EventEnum e = EventEnum.getEnum(h.value.getString());
        switch (e) {
          case AUTHENTICATE:
            return new hicp.message.event.Authenticate(e.messageName, headerMap);
          // Is there a warning if an enum switch is missing an item?
          // If not, add a default here.
        }
        return null;
    }

    public Message readMessage()
        throws IOException
     {
        final HeaderMap headerMap = readHeaderMap();
        {
            final Message command = newCommand(headerMap);
            if (null != command) {
                return command;
            }
        }
        {
            final Event event = newEvent(headerMap);
            if (null != event) {
                return event;
            }
        }
        return null;
     }
}

class TokenIndicator {
    public static final TokenIndicator IS_PART = new TokenIndicator();
    public static final TokenIndicator IS_END = new TokenIndicator();
    public static final TokenIndicator NOT_PART = new TokenIndicator();
    public static final TokenIndicator IS_INCOMPLETE = new TokenIndicator();
}

abstract class Acceptor {
    protected Acceptor() {
    }

    public abstract TokenIndicator accept(byte inByte);

    public abstract void reset();

    public abstract int getEndLength();
}

/** Can be ": " or ":: ". This could be more specific, but for now will
   also accept ":::: ". */
class SeparatorAcceptor
    extends Acceptor
{
    private static final Logger LOGGER =
        Logger.getLogger( SeparatorAcceptor.class.getName() );

    protected static final int MATCH_NONE = 1;
    protected static final int MATCH_COLON = 2;
    protected static final int MATCH_SPACE = 3;

    protected int _state = MATCH_NONE;

    public SeparatorAcceptor() {
    }

    public TokenIndicator accept(byte inByte) {
        switch (_state) {
          case MATCH_NONE:
            if (':' == inByte) {
                _state = MATCH_COLON;
                return TokenIndicator.IS_PART;
            } else {
                return TokenIndicator.NOT_PART;
            }
          case MATCH_COLON:
            if (':' == inByte) {
                return TokenIndicator.IS_PART;
            } else if (' ' == inByte) {
                _state = MATCH_SPACE;
                return TokenIndicator.IS_END;
            } else {
                return TokenIndicator.NOT_PART;
            }
          case MATCH_SPACE:
          default:
            return TokenIndicator.NOT_PART;
        }
    }

    public void reset() {
        _state = MATCH_NONE;
    }

    public int getEndLength() {
        return 1;
    }
}

class TokenAcceptor
    extends Acceptor
{
    private static final Logger LOGGER =
        Logger.getLogger( TokenAcceptor.class.getName() );

    protected static final int MATCH_NONE = 1;
    protected static final int MATCH_CR = 2;
    protected static final int MATCH_LF = 3;

    protected int _state = MATCH_NONE;

    protected final char _separator;
    protected final boolean _excludeSeparator;

    public TokenAcceptor() {
        _separator = '\0';
        _excludeSeparator = false;
    }

    public TokenAcceptor(char separator) {
        _separator = separator;
        _excludeSeparator = true;
    }

    public TokenIndicator accept(byte inByte) {
        // Exclude separator in all states.
        if (_excludeSeparator && (_separator == (char)inByte)) {
            return TokenIndicator.NOT_PART;
        }

        switch (_state) {
          case MATCH_NONE:
            if ('\r' == (char)inByte) {
                _state = MATCH_CR;
            }
            return TokenIndicator.IS_PART;

          case MATCH_CR:
            if ('\n' == (char)inByte) {
                _state = MATCH_LF;
                return TokenIndicator.IS_END;
            }
            return TokenIndicator.IS_PART;

          default:
            if (Character.isISOControl((char)inByte)) {
                return TokenIndicator.NOT_PART;
            } else {
                return TokenIndicator.IS_PART;
            }
        }
    }

    public void reset() {
        _state = MATCH_NONE;
    }

    public int getEndLength() {
        return 2;
    }
}

/*
    Terminal sequance is "\r\n" (CR LF).
 */
class TermSeqAcceptor
    extends Acceptor
{
    private static final Logger LOGGER =
        Logger.getLogger( TermSeqAcceptor.class.getName() );

    protected static final int MATCH_NONE = 1;
    protected static final int MATCH_CR = 2;
    protected static final int MATCH_LF = 3;

    protected int _state = MATCH_NONE;

    public TermSeqAcceptor() {
    }

    public TokenIndicator accept(byte inByte) {
        switch (_state) {
          case MATCH_NONE:
            if ('\r' == (char)inByte) {
                _state = MATCH_CR;
            }
            return TokenIndicator.IS_PART;

          case MATCH_CR:
            if ('\n' == (char)inByte) {
                _state = MATCH_LF;
            }
            return TokenIndicator.IS_END;

          case MATCH_LF:
            return TokenIndicator.NOT_PART;

          default:
            return TokenIndicator.NOT_PART;
        }
    }

    public void reset() {
        _state = MATCH_NONE;
    }

    public int getEndLength() {
        return 0;
    }
}

class BoundaryAcceptor
    extends Acceptor
{
    private static final Logger LOGGER =
        Logger.getLogger( BoundaryAcceptor.class.getName() );

    protected int _matchIdx = 0;

    final protected byte[] _termSeq;
    final protected int[] _failIdx;

    public BoundaryAcceptor(byte[] termSeq) {
        _termSeq = termSeq;
        if (0 == termSeq.length) {
            // No boundary, so nothing to accept.
            _failIdx = null;
            return;
        }

        // Build the _failIdx values. This is a Knuth-Morris-Pratt state
        // machine. Briefly, consider pattern "ababc" and text
        // "abababc".
        // The pattern will have a matched part and an unchecked part:
        // "abab""c".
        // The text will have a checked part and an unchecked part:
        // "abab""abc".
        // At that point, the match fails, so the matched part of the
        // pattern should be rolled back two positions, to "ab""abc", so
        // that nothing will be missed. Now the unchecked part of the
        // pattern ("abc") will match the unchecked text ("abc").
        // This code computes the indices used for rolling back the
        // matches when a mismatch occurs and stores them in the
        // _failIdx array.
        _failIdx = new int[_termSeq.length];
        _failIdx[0] = -1;
        for (int scanFailIdx = 1;scanFailIdx < _failIdx.length;scanFailIdx++) {
            int prevFailIdx = _failIdx[scanFailIdx - 1];
findFailIdxLoop:
            for (;;) {
                if (-1 == prevFailIdx) {
                    break findFailIdxLoop;
                }
                if (_termSeq[prevFailIdx] == _termSeq[scanFailIdx - 1])
                {
                    break findFailIdxLoop;
                }
                prevFailIdx = _failIdx[prevFailIdx];
            }
            _failIdx[scanFailIdx] = prevFailIdx + 1;
        }
    }

    public TokenIndicator accept(final byte inByte) {
        if (0 == _termSeq.length) {
            // No sequence to accept.
            return TokenIndicator.IS_END;
        }
        while (_matchIdx > -1) {
            if (inByte == _termSeq[_matchIdx]) {
                // Matched a byte in termination sequence.
                _matchIdx++;
                if (_matchIdx < _termSeq.length) {
                    // More bytes left to match in termination sequence.
                    return TokenIndicator.IS_PART;
                } else {
                    // Last byte of termination sequence.
                    return TokenIndicator.IS_END;
                }
            } else {
                // Didn't match, adjust _matchIdx for next try. See
                // comments in constructor for details.
                _matchIdx = _failIdx[_matchIdx];
            }
        }
        // Start over at beginning of termination sequence.
        _matchIdx = 0;
        return TokenIndicator.IS_PART;
    }

    public void reset() {
        _matchIdx = 0;
    }

    public int getEndLength() {
        return _termSeq.length;
    }
}

