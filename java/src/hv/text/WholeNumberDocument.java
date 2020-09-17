package hv.text;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

// Can do this with Pattern and p.matcher(modifiedString).
public class WholeNumberDocument
    extends javax.swing.text.PlainDocument
{
    protected char[] _validChars = null;

    public void insertString(int offs, String str, AttributeSet a)
        throws BadLocationException
    {
        if (str.length() == 0) {
            return;
        }
        final char[] source = str.toCharArray();

        if ((_validChars == null) || (_validChars.length < source.length)) {
            _validChars = new char[source.length];
        }

        int validCharIdx = 0;
        for (int sourceIdx = 0;sourceIdx < source.length; sourceIdx++) {
            if (Character.isDigit(source[sourceIdx])) {
                _validChars[validCharIdx++] = source[sourceIdx];
            }

        }
        super.insertString(offs, new String(_validChars, 0, validCharIdx), a);
    }
}

