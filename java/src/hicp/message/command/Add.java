package hicp.message.command;

import hicp.Logger;
import hicp.message.Message;

public class Add
    extends AddModify
{
    public Add(String name, int id) {
        super(name, id);
    }

    public Message newMessage() {
        return new Add(getName(), getID());
    }
}
