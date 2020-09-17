package hicp.message.command;

import hicp.message.Message;

public class Remove
    extends AddModifyRemove
{
    public Remove(String name, int id) {
        super(name, id);
    }

    public Message newMessage() {
        return new Remove(getName(), getID());
    }
}
