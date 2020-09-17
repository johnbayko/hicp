package hicp_client;

public interface Monitor {
    /**
        Signal that an open request has been made.
     */
//    public void hicpOpen(/* something... */);

    /**
        Signal that HICP is connected.
     */
    public void connected();

    /**
        Signal that HICP is disconnected.
     */
    public void disconnected();

    /**
        Handle an exception from the HICP library.
     */
    public void exception(String msg, Exception ex);
}
