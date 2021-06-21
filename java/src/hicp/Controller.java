package hicp;

import hicp.message.command.Command;
import hicp.message.command.CommandEnum;

public interface Controller {
    public void receivedMessage(Command c);

    public void closed();
}
