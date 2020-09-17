package hicp_client;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;

public class InputThread
    extends Thread
{
    protected final Reader _in;
    protected final HICP_Receiver _receiver;

    public InputThread(InputStream in, HICP_Receiver receiver) {
        _in = new InputStreamReader(in);
        _receiver = receiver;
    }

    public void run() {
        // Simplest case, pass each byte to receiver.
        try {
readLoop:   for (;;) {
                final int inChar = _in.read();  // 1 character
                if (-1 == inChar) {
                    break readLoop;
                }
                _receiver.receive(inChar);
            }
        } catch (IOException ex) {
            // Quietly quit the loop.
            // Log?
        }
        _receiver.closed();
    }

    public void close() {
        // Set some sort of flag or signal to stop this thread.
        _receiver.closed();
    }
}

