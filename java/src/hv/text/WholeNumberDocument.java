package hv.text;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

// Can do this with Pattern and p.matcher(modifiedString).
public class WholeNumberDocument
    extends javax.swing.text.PlainDocument
{
    public void insertString(int offs, String str, AttributeSet a)
        throws BadLocationException
    {
        if (0 == str.length()) {
            return;
        }
        final StringBuilder validStr = new StringBuilder();

        str.codePoints()
            .filter(checkChar -> Character.isDigit(checkChar))
            .forEachOrdered(validChar -> validStr.appendCodePoint(validChar));

        if (0 == validStr.length()) {
            return;
        }
        super.insertString(offs, validStr.toString(), a);
    }
}

