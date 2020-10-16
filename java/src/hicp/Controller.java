package hicp;

import hicp.message.command.Command;

public interface Controller {
    public void receivedMessage(Command m);

    public void closed();
}
