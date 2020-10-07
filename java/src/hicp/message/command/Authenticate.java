package hicp.message.command;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import hicp.HICPHeader;
import hicp.HICPReader;
import hicp.message.Message;

public class Authenticate
    extends Command
{
    public final static String METHOD = "method";
    public final static String PASSWORD = "password";

    public String method = null;
    public String password = null;

    private Set<String> allMethods = null;

    public Authenticate() {
        super(Command.AUTHENTICATE_STR, Command.AUTHENTICATE_ID);
    }

    public Message newMessage() {
        return new Authenticate();
    }

    public void write(Writer out)
        throws IOException
    {
    }

    public void read(HICPReader in)
        throws IOException
    {
        try {
readLoop:   for (;;) {
                final HICPHeader hicpHeader = in.readHeader();
                if (null == hicpHeader.name) {
                    break readLoop;
                }

                // Extract recognized fields.
                if (METHOD.equals(hicpHeader.name)) {
                    method = hicpHeader.value.getString();

                    // Extract available methods separated by ",",
                    // discard spaces
                    allMethods = Set.of(method.trim().split("\\s*,\\s*"));
                } else if (PASSWORD.equals(hicpHeader.name)) {
                    password = hicpHeader.value.getString();
                }
            }
        } catch (NullPointerException ex) {
            // Unexpected end of input - not really an error, so just
            // quietly return with whatever was read.
        }
    }

    public void clear() {
        method = null;
        password = null;
    }

    public boolean hasMethod(final String method) {
        return allMethods.contains(method);
    }
}
