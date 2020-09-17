package hicp_client;

public interface HICP_Receiver {
    /**
        Process the bytes received from the server.
        Bytes may be frame codes, message headers, or data. Data may be
        any form, depends on the message header.
    */
    public void receive(int inChar);

    /**
        When the input stream has closed, this method is called.
     */
    public void closed();
}
