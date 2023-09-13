package hicp_client.gui;

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
import hicp.message.Message;
import hicp.message.TextAttributes;
import hicp.message.command.GUITextFieldInfo;
import hicp.message.event.EventInfo;
import hicp_client.text.AttributeTrackDocument;

public class TextFieldItem
    extends Item
    implements Positionable
{
    private static final Logger LOGGER =
        Logger.getLogger( TextFieldItem.class.getName() );

    protected final PositionInfo _positionInfo;

    protected final MessageExchange _messageExchange;

    protected JTextField _component;

    protected String _content = "";
    protected AttributeTrackDocument _document;
    protected TextAttributes _attributes;

    public TextFieldItem(
        Message m,
        MessageExchange messageExchange
    ) {
        super(m);
        _positionInfo = new PositionInfo(m);

        _messageExchange = messageExchange;
    }

    protected Item addInvoked(final Message addCmd) {
        final var commandInfo = addCmd.getCommandInfo();
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiTextFieldInfo = guiInfo.getGUITextFieldInfo();

        // TODO: If no width specified, use contents for default width.
        _component = new JTextField(guiTextFieldInfo.width);
        _document = new AttributeTrackDocument();
        _component.setDocument(_document);

        setContentInvoked(
            guiTextFieldInfo.content,
            guiTextFieldInfo.attributes
        );

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

        setEventsInvoked(guiTextFieldInfo.events);
        return this;
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
            final var changedEvent = new Message(EventInfo.Event.CHANGED);
            final var eventInfo = changedEvent.getEventInfo();
            final var itemInfo = eventInfo.getItemInfo();
            final var textFieldInfo = itemInfo.getTextFieldInfo();

            itemInfo.id = idString;
            if (hasContentChanged) {
                textFieldInfo.content = content;
            }
            if (hasAttributesChanged) {
                textFieldInfo.attributes = attributes;
            }
            _messageExchange.send(changedEvent);

            // Save for next event.
            _content = content;
            _attributes = attributes;  // Is a copy, document can't change this.
        }
    }

    public Component getComponent() {
        return _component;
    }

    public PositionInfo getPositionInfo() {
        return _positionInfo;
    }

    public int getGridBagAnchor() {
        return java.awt.GridBagConstraints.EAST;
    }

    public int getGridBagFill() {
        return java.awt.GridBagConstraints.HORIZONTAL;
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

    protected Item setEventsInvoked(final GUITextFieldInfo.EventsEnum events) {
        final boolean enabled =
            (null != events)
                ? (events == GUITextFieldInfo.EventsEnum.ENABLED)
                : true; // Default is enabled.

        if (_component.isEditable() != enabled) {
            _component.setEditable(enabled);
        }
        return this;
    }

    protected Item modifyInvoked(final Message modifyCmd) {
        final var commandInfo = modifyCmd.getCommandInfo();
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiTextFieldInfo = guiInfo.getGUITextFieldInfo();

        // See what's changed.
        if (null != guiTextFieldInfo.content) {
            final String modifyContent = guiTextFieldInfo.content;

            if (!modifyContent.equals(_component.getText())) {
                setContentInvoked(modifyContent, guiTextFieldInfo.attributes);
            }
        }
        if (guiTextFieldInfo.hasWidth) {
            if (guiTextFieldInfo.width != _component.getColumns()) {
                _component.setColumns(guiTextFieldInfo.width);
                // Resize?
            }
        }
        if (null != guiTextFieldInfo.attributes ) {
// handle attributes.
// To start, just log the string.
            final String modifyAttributes =
                guiTextFieldInfo.attributes.toString();
        }
        if (null != guiTextFieldInfo.events) {
            setEventsInvoked(guiTextFieldInfo.events);
        }
        // Changed parent ID is handled by Controller.
        return this;
    }
}

