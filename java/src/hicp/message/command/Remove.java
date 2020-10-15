package hicp.message.command;

import hicp.message.Message;

public class Remove
    extends AddModifyRemove
{
    public Remove(final String name, final int id) {
        super(name, id);
    }

    public Message newMessage() {
        return new Remove(_name, _id);
    }
}
