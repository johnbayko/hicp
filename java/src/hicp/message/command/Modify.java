package hicp.message.command;

import hicp.message.Message;

public class Modify
    extends AddModify
{
    public Modify(String name, int id) {
        super(name, id);
    }

    public Message newMessage() {
        return new Modify(getName(), getID());
    }
}
