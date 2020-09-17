package hicp_client;

import java.io.InputStream;
import java.io.OutputStream;

public class Session {
    public Params params = null;
    public InputStream in = null;
    public OutputStream out = null;

    public Session(
        Params newParams, InputStream newIn, OutputStream newOut
    ) {
        params = newParams;
        in = newIn;
        out = newOut;
    }
}
