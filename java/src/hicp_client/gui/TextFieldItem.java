package hicp_client.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JTextField;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hicp.MessageExchange;
import hicp.message.AttributeListInfo;
import hicp.message.Message;
import hicp.message.command.ContentInfo;
import hicp.message.command.CommandInfo;
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
    protected boolean _is_editing = false;

    protected String _content = "";
    protected AttributeTrackDocument _document;
    protected AttributeListInfo _attributeListInfo;

    public TextFieldItem(
        CommandInfo commandInfo,
        MessageExchange messageExchange
    ) {
        super(commandInfo);
        _positionInfo = new PositionInfo(commandInfo);

        _messageExchange = messageExchange;
    }

    protected Item add(final CommandInfo commandInfo) {
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiTextFieldInfo = guiInfo.getGUITextFieldInfo();

        // TODO: If no width specified, use contents for default width.
        _component = new JTextField(guiTextFieldInfo.width);
        _document = new AttributeTrackDocument();
        _component.setDocument(_document);

        // Add can only send set action with full content, so getSetInfo() is
        // valid.
        if (guiTextFieldInfo.hasContent()) {
            final var contentInfo = guiTextFieldInfo.getContentInfo();
            final var setInfo = contentInfo.getSetInfo();
            setContent(setInfo);
        }
        if (guiTextFieldInfo.hasAttributes()) {
            final var attributeListInfo =
                guiTextFieldInfo.getAttributeListInfo();
            _document.setAttributeListInfo(attributeListInfo);
            // Save copy nobody else can access for checking if attributes
            // changed later.
            _attributeListInfo = _document.getAttributeListInfo();
        }

// Is this needed if there's a focus listener?
        _component.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    _is_editing = false;
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
                    _is_editing = false;
                    sendChangedEventIfEdited();
                }
            }
        );
        _component.addKeyListener(
            new KeyListener() {
                public void keyPressed(KeyEvent e) {
                    _is_editing = true;
                }
                public void keyTyped(KeyEvent e) { }
                public void keyReleased(KeyEvent e) { }
            }
        );
        // Can mouse based changes happen (e.g select, paste) happen without
        // triggering one of these events?
        // Might need to add DocumentListener to _document (includes attribute
        // changes).

        setEvents(guiTextFieldInfo.events);
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

        final var attributeListInfo =
            _document.getAttributeListInfo();
        final boolean hasAttributesChanged =
            ( (null != attributeListInfo)
           && !attributeListInfo.equals(_attributeListInfo) );

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
                textFieldInfo.setAttributeListInfo(attributeListInfo);
            }
            _messageExchange.send(changedEvent);

            // Save for next event.
            _content = content;
            _attributeListInfo = attributeListInfo;
        }
    }

    protected void sendChangedEvent() {
        // Send a changed event with this object's ID
        // and current content.
        final var changedEvent = new Message(EventInfo.Event.CHANGED);
        final var eventInfo = changedEvent.getEventInfo();
        final var itemInfo = eventInfo.getItemInfo();
        final var textFieldInfo = itemInfo.getTextFieldInfo();

        itemInfo.id = idString;
        textFieldInfo.content = _content;
        textFieldInfo.setAttributeListInfo(_attributeListInfo);

        _messageExchange.send(changedEvent);
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

    final static Pattern nonPrintablePattern = Pattern.compile("\\p{Cntrl}");

    protected void setContent(
        final ContentInfo.SetInfo setInfo
    ) {
        // Make sure content is valid.
        String content = setInfo.text;
        if (null == content) {
            content = "";
        }
        // Remove all non-printable (control) characters.
        final Matcher nonPrintableMatcher =
            nonPrintablePattern.matcher(content);
        content = nonPrintableMatcher.replaceAll("");

        // Replacement text doesn't need old text attributes.
        _document.setAttributeListInfo((AttributeListInfo)null);

        _component.setText(content);
        _content = content;
    }

    protected void addContent(
        final ContentInfo.AddInfo addInfo
    ) {
        // Make sure content is valid.
        String addText = addInfo.text;
        if (null == addText) {
            addText = "";
        }
        // Remove all non-printable (control) characters.
        final Matcher nonPrintableMatcher =
            nonPrintablePattern.matcher(addText);
        addText = nonPrintableMatcher.replaceAll("");

        // JTextComponent doesn't have a way to insert text, so have to take
        // existing text, edit, and replace it.
        // This shows up in the document as a delete all, then add new. This
        // will erase existing attributes, so disable attribute updates, set
        // the new text, and then expand attribute ranges where text was
        // inserted.
        _document.setUpdateAttributes(false);

        final String oldText = _component.getText();
        final StringBuilder changeText = new StringBuilder(oldText);
        changeText.insert(addInfo.position, addText);
        final String newText = changeText.toString();
        _component.setText(newText);

        _document.insertForAttributes(addInfo.position, addText);

        _document.setUpdateAttributes(true);
        _content = newText;
    }

    protected void deleteContent(
        final ContentInfo.DeleteInfo deleteInfo
    ) {
        // JTextComponent doesn't have a way to delete text, so have to take
        // existing text, edit, and replace it.
        // This shows up in the document as a delete all, then add new. This
        // will erase existing attributes, so disable attribute updates, set
        // the new text, and then expand attribute ranges where text was
        // inserted.
        _document.setUpdateAttributes(false);

        final String oldText = _component.getText();
        final StringBuilder changeText = new StringBuilder(oldText);
        changeText.delete(
            deleteInfo.position,
            deleteInfo.position + deleteInfo.length
        );
        final String newText = changeText.toString();
        _component.setText(newText);

        _document.removeForAttributes(deleteInfo);

        _document.setUpdateAttributes(true);
        _content = newText;
    }

    public void dispose() {
        super.dispose();
        _component = null;
    }

    protected Item setEvents(final GUITextFieldInfo.EventsEnum events) {
        final boolean enabled =
            (null != events)
                ? (events == GUITextFieldInfo.EventsEnum.ENABLED)
                : true; // Default is enabled.

        if (_component.isEditable() != enabled) {
            // If this disables editing, it should restore the text to what it
            // was before editing started (in component and document). Check if
            // it does.
            _component.setEditable(enabled);
        }
        return this;
    }

    protected Item modify(final CommandInfo commandInfo) {
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiTextFieldInfo = guiInfo.getGUITextFieldInfo();

        // See what's changed.
        if (!_is_editing) {
            if (guiTextFieldInfo.hasContent()) {
                final var contentInfo = guiTextFieldInfo.getContentInfo();

                switch (contentInfo.action) {
                  case SET:
                    // Content can be replaced when editable, but not while user
                    // is editing.
                    {
                        final var setInfo = contentInfo.getSetInfo();
                        setContent(setInfo);
                    }
                    break;
                  case ADD:
                    // Content can not be modified when editable.
                    {
                        final var addInfo = contentInfo.getAddInfo();
                        if (null != addInfo) {
                            addContent(addInfo);
                        }
                    }
                    break;
                  case DELETE:
                    // Content can not be modified when editable.
                    {
                        final var deleteInfo = contentInfo.getDeleteInfo();
                        if (null != deleteInfo) {
                           deleteContent(deleteInfo);
                        }
                    }
                    break;
                  // Could be null, I guess.
                  default:
                    break;
                }
            }
            if (guiTextFieldInfo.hasAttributes()) {
                final var attributeListInfo =
                    guiTextFieldInfo.getAttributeListInfo();
                if (attributeListInfo.hasAttributes()) {
                    _document.modifyAttributeListInfo(attributeListInfo);
                    // Save copy nobody else can access for checking if
                    // attributes changed later.
                    _attributeListInfo = _document.getAttributeListInfo();
                }
            }
        } else {
            if ( guiTextFieldInfo.hasContent()
              || guiTextFieldInfo.hasAttributes()
            ) {
                // Reject the modify command, and send a changed event back with
                // the current values.
                sendChangedEvent();
            }
        }
        if (guiTextFieldInfo.hasWidth()) {
            if (guiTextFieldInfo.width != _component.getColumns()) {
                _component.setColumns(guiTextFieldInfo.width);
                // Resize?
            }
        }
        if (null != guiTextFieldInfo.events) {
            setEvents(guiTextFieldInfo.events);
        }
        // Changed parent ID is handled by Controller.
        return this;
    }
}

