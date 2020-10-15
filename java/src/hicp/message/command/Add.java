package hicp.message.command;

import hicp.message.Message;

public class Add
    extends AddModify
{
    public Add(final String name, final int id) {
        super(name, id);
    }

    public Message newMessage() {
        return new Add(_name, _id);
    }
}
