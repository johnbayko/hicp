package hicp.message.command;

import hicp.message.Message;

public class Remove
    extends AddModifyRemove
{
    public Remove() {
        super(Command.REMOVE_STR, Command.REMOVE_ID);
    }

    public Message newMessage() {
        return new Remove();
    }
}
