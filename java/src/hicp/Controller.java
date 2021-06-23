package hicp;

import hicp.message.Message;

public interface Controller {
    public void receivedMessage(Message m);

    public void closed();
}
