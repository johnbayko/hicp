package hicp;

import hicp.message.command.Command;
import hicp.message.command.CommandEnum;

public interface Controller {
    public void receivedMessage(CommandEnum ce, Command c);

    public void closed();
}
