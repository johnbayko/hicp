package hicp.message.command;

import hicp.message.Message;

public class Modify
    extends AddModify
{
    public Modify() {
        super(Command.MODIFY_STR, Command.MODIFY_ID);
    }

    public Message newMessage() {
        return new Modify();
    }
}
