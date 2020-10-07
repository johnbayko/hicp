package hicp.message.command;

import hicp.message.Message;

public class Add
    extends AddModify
{
    public Add() {
        super(Command.ADD_STR, Command.ADD_ID);
    }

    public Message newMessage() {
        return new Add();
    }
}
