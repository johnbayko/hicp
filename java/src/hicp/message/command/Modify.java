package hicp.message.command;

import hicp.message.Message;

public class Modify
    extends AddModify
{
    public Modify(final String name, final int id) {
        super(name, id);
    }

    public Message newMessage() {
        return new Modify(_name, _id);
    }
}
