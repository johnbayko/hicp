package hicp_client.text;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

import hicp.message.TextAttributes;

public class AttributeTrackDocument
    extends javax.swing.text.PlainDocument
{
    // Just tracks HICP attributes, because JTextField does not actually
    // support any, but HICP wants them tracked and returned based on edits
    // anyway.
    private TextAttributes textAttributes;

    public void insertString(int offset, String str, AttributeSet a)
        throws BadLocationException
    {
        if (null != textAttributes) {
            textAttributes.insert(offset, str.length());
        }
        super.insertString(offset, str, a);
    }


    public void remove(int offset, int len)
        throws BadLocationException
    {
        if (null != textAttributes) {
            textAttributes.remove(offset, len);
        }
        super.remove(offset, len);
    }

    public void setTextAttributes(final TextAttributes newTextAttributes) {
        textAttributes = newTextAttributes;
    }

    public void setTextAttributes(final String attributesStr) {
        textAttributes = new TextAttributes(attributesStr);
    }

    // Return a copy of internal TextAttributes, because it needs to be mutable
    // and changes that get out of sync with text component would be bad.
    public TextAttributes getTextAttributes() {
        return new TextAttributes(textAttributes);
    }
}
