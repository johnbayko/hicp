package hicp.message.command;

import hicp.HICPHeader;
import hicp.message.Message;

public class Add
    extends AddModify
{
    public final static String HEIGHT = "height";
    public final static String MODE = "mode";
    public final static String PRESENTATION = "presentation";
    public final static String WIDTH = "width";

    public String height = null;
    public String mode = null;
    public String presentation = null;
    public String width = null;

    public Add(final String name) {
        super(name);
    }

    protected boolean readField(HICPHeader hicpHeader) {
        if (super.readField(hicpHeader)) {
            return true;
        }

        // Extract recognized fields.
        if (HEIGHT.equals(hicpHeader.name)) {
            height = hicpHeader.value.getString();
            return true;
        } else if (MODE.equals(hicpHeader.name)) {
            mode = hicpHeader.value.getString();
            return true;
        } else if (PRESENTATION.equals(hicpHeader.name)) {
            presentation = hicpHeader.value.getString();
            return true;
        } else if (WIDTH.equals(hicpHeader.name)) {
            width = hicpHeader.value.getString();
            return true;
        }

        return false;
    }
}
