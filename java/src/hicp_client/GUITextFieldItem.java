package hicp_client;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JTextField;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;

import hicp.MessageExchange;
import hicp.message.TextAttributes;
import hicp.message.command.Add;
import hicp.message.command.Modify;
import hicp.message.event.Changed;
import hicp.message.event.Event;
import hicp.message.event.EventEnum;
import hicp_client.text.AttributeTrackDocument;

public class GUITextFieldItem
    extends GUIItem
{
    private static final Logger LOGGER =
        Logger.getLogger( GUIItem.class.getName() );

    protected final MessageExchange _messageExchange;

    protected JTextField _component;
    protected String _content = "";
    protected AttributeTrackDocument _document;
    protected TextAttributes _attributes;

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
            _document = new AttributeTrackDocument();
            _component.setDocument(_document);

            setContentInvoked(_addCmd.content, _addCmd.textAttributes);

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

            // Text editing enable/disable.
            {
                // Default is enable.
                final String eventsValue =
                    (null != _addCmd.events) ? _addCmd.events : Add.ENABLED;

                setEventsInvoked(eventsValue);
            }
        }
    }

    protected void sendChangedEventIfEdited() {
        if (null == _component) {
            // Maybe focus changed because of shutdown, so no change
            // event.
            return;
        }
        final String content =
            _component.getText();
        final boolean hasContentChanged =
            !content.equals(_content);

        final TextAttributes attributes =
            _document.getTextAttributes();
        final boolean hasAttributesChanged =
            (null != attributes) && !attributes.equals(_attributes);

        if (hasContentChanged || hasAttributesChanged) {
            // Content has changed.
            // Send a changed event with this object's ID
            // and the new content.
            Changed changedEvent = (Changed)EventEnum.CHANGED.newEvent();
            
            changedEvent.id = idString;
            if (hasContentChanged) {
                changedEvent.content = content;
            }
            if (hasAttributesChanged) {
                changedEvent.attributes = attributes;
            }
            _messageExchange.send(changedEvent);

            // Save for next event.
            _content = content;
            _attributes = attributes;  // Is a copy, document can't change this.
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

    protected void setContentInvoked(
        String content,
        final TextAttributes textAttributes
    ) {
        // Make sure content is valid.
        if (null == content) {
            content = "";
        }
        // Remove all non-printable (control) characters.
        final Matcher nonPrintableMatcher =
            nonPrintablePattern.matcher(content);
        content = nonPrintableMatcher.replaceAll("");

        // Replacement text doesn't need old text attributes.
        _document.setTextAttributes((TextAttributes)null);
        _component.setText(content);
        _content = content;

        // This keeps track of attributes associated with content string, but
        // document could have text changed events fired, so set the text
        // attributes after the text has been set.
        // TextAttributes is mutable, so make copies (one tor the document, one
        // original to check later if any have changed.
        if (null != textAttributes) {
            _document.setTextAttributes(new TextAttributes(textAttributes));
        }
        _attributes = new TextAttributes(textAttributes);
    }

    public void dispose() {
        super.dispose();
        _component = null;
    }

    protected GUIItem setEventsInvoked(final String eventsValue) {
        final boolean enabled = eventsValue.equals(Add.ENABLED);

        if (_component.isEditable() != enabled) {
            _component.setEditable(enabled);
        }
        return this;
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
            // See what's changed.
            if (null != _modifyCmd.content) {
                final String modifyContent = _modifyCmd.content;

                if (!modifyContent.equals(_component.getText())) {
                    setContentInvoked(modifyContent, _modifyCmd.textAttributes);
                }
            }
            if (null != _modifyCmd.attributes ) {
                final String modifyAttributes = _modifyCmd.attributes;

// handle attributes.
// To start, just log the string.
log("modifyAttributes: " + modifyAttributes);  // debug
            }
            if (null != _modifyCmd.events) {
                setEventsInvoked(_modifyCmd.events);
            }
            // Changed parent ID is handled by Controller.
        }
    }
}

