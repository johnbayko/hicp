package hicp_client;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JTextField;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;

import hicp.MessageExchange;
import hicp.message.command.Add;
import hicp.message.command.Modify;
import hicp.message.event.Event;

public class GUITextFieldItem
    extends GUIItem
{
    protected final MessageExchange _messageExchange;

    protected JTextField _component;
    protected String _content = "";

    public GUITextFieldItem(
        Add addCmd,
        TextItem textItem,  // Doesn't apply to text fields.
        MessageExchange messageExchange
    ) {
        super(addCmd);

        _messageExchange = messageExchange;

        SwingUtilities.invokeLater(
            new RunNew(addCmd)
        );
    }

    class RunNew
        implements Runnable
    {
        protected final Add _addCmd;

        public RunNew(Add addCmd)
        {
            _addCmd = addCmd;
        }

        public void run()
        {
            _component = new JTextField();

            setContentInvoked(_addCmd.content);

// Is this needed if there's a focus listener?
            _component.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        sendChangedEventIfEdited();
                    }
                }
            );
            _component.addFocusListener(
                new FocusListener() {
                    public void focusGained(FocusEvent e) {
                        // Don't do anything, no editing has happened
                        // yet.
                    }
                    public void focusLost(FocusEvent e) {
                        sendChangedEventIfEdited();
                    }
                }
            );

        }
    }

    protected void sendChangedEventIfEdited() {
        if (null == _component) {
            // Maybe focus changed because of shutdown, so no change
            // event.
            return;
        }
        final String content = _component.getText();
        if (!content.equals(_content)) {
            // Content has changed.
            // Send a changed event with this object's ID
            // and the new content.
            hicp.message.event.Changed changedEvent =
                (hicp.message.event.Changed)Event.CHANGED.newMessage();
            
            changedEvent.id = idString;
            changedEvent.content = content;
log("changedEvent.content \"" + changedEvent.content + "\"");  // debug
        
            _messageExchange.send(changedEvent);

            _content = content;
        }
    }

    protected Component getComponent() {
        return _component;
    }

    protected int getGridBagAnchor() {
        return java.awt.GridBagConstraints.EAST;
    }

    protected int getGridBagFill() {
        return java.awt.GridBagConstraints.HORIZONTAL;
    }

    /**
        Called in non-GUI thread.
     */
    protected GUIItem setText(String text) {
        return this;
    }

    /**
        Called in GUI thread.
     */
    protected GUIItem setTextInvoked(String text) {
        return this;
    }

    /**
        Called in GUI thread.
     */
    final static Pattern nonPrintablePattern = Pattern.compile("\\p{Cntrl}");

    protected void setContentInvoked(String content) {
        // Make sure content is valid.
        if (null == content) {
            content = "";
        }
        // Remove all non-printable (control) characters.
        final Matcher nonPrintableMatcher =
            nonPrintablePattern.matcher(content);
        content = nonPrintableMatcher.replaceAll("");

        _component.setText(content);
        _content = content;
    }

    public void dispose() {
        _component = null;
    }

    public GUIItem modify(Modify modifyCmd, TextItem textItem) {
        SwingUtilities.invokeLater(
            new RunModify(modifyCmd)
        );

        return this;
    }

    class RunModify
        implements Runnable
    {
        protected final Modify _modifyCmd;

        public RunModify(Modify modifyCmd) {
            _modifyCmd = modifyCmd;
        }

        public void run() {
log("Modifying textfield, component is "
    + _component.getClass().getName()
);  // debug
            // See what's changed.
            {
                final String modifyContent =
                    (null != _modifyCmd.content ) ? _modifyCmd.content : "";

                if (!modifyContent.equals(_component.getText())) {
                    setContentInvoked(modifyContent);
                }
            }
            {
                final String modifyAttributes =
                    (null != _modifyCmd.attributes ) ?  _modifyCmd.attributes : "";

// handle attributes.
// To start, just log the string.
log("modifyAttributes: " + modifyAttributes);  // debug
            }

            // Enable/disable?

            // Changed parent ID is handled by Controller.
        }
    }
}

