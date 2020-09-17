package hicp;

public class HICPHeader
{
    public final String name;
    public final HICPHeaderValue value;

    public HICPHeader(String newName, HICPHeaderValue newValue) {
        name = newName;
        value = newValue;
    }
}
