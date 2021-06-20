package hicp.message.command;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

import hicp.HICPHeader;
import hicp.HICPReader;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public class Authenticate
    extends Command
{
    public final static String METHOD = "method";
    public final static String PASSWORD = "password";

    public String method = null;
    public String password = null;

    private Set<String> allMethods = null;

    public Authenticate(final String name) {
        super(name);
    }

    public void write(Writer out)
        throws IOException
    {
    }

    public Message addHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        super.addHeaders(headerMap);
        for (final HeaderEnum h : headerMap.keySet()) {
            final HICPHeader v = headerMap.get(h);
            switch (h) {
              case METHOD:
                method = v.value.getString();

                // Extract available methods separated by ",",
                // discard spaces
                allMethods = Set.of(method.trim().split("\\s*,\\s*"));
                break;
              case PASSWORD:
                password = v.value.getString();
                break;
            }
        }
        return this;
    }

    public boolean hasMethod(final String method) {
        return allMethods.contains(method);
    }
}
