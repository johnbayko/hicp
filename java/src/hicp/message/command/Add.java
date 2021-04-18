package hicp.message.command;

import hicp.HICPHeader;
import hicp.message.Message;

public class Add
    extends AddModify
{
    public final static String MODE = "mode";
    public final static String PRESENTATION = "presentation";

    public String mode = null;
    public String presentation = null;

    public Add(final String name) {
        super(name);
    }

    protected boolean readField(HICPHeader hicpHeader) {
        if (super.readField(hicpHeader)) {
            return true;
        }

        // Extract recognized fields.
        if (MODE.equals(hicpHeader.name)) {
            mode = hicpHeader.value.getString();
            return true;
        } else if (PRESENTATION.equals(hicpHeader.name)) {
            presentation = hicpHeader.value.getString();
            return true;
        }

        return false;
    }
}
