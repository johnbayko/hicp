package hicp_client.gui.selection;

import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;

import hicp.MessageExchange;
import hicp.message.command.Add;
import hicp.message.command.Modify;
import hicp_client.gui.Item;
import hicp_client.text.TextLibrary;

public class ToggleItem
    extends Item
{
    private static final Logger LOGGER =
        Logger.getLogger( ScrollItem.class.getName() );

    protected final TextLibrary _textLibrary;
    protected final MessageExchange _messageExchange;

    protected Component _component;

    public ToggleItem(
        Add addCmd,
        TextLibrary textLibrary,
        MessageExchange messageExchange
    ) {
        super(addCmd);

        _textLibrary = textLibrary;
        _messageExchange = messageExchange;
    }

    protected Item addInvoked(final Add addCmd) {
        final ModeEnum mode = ModeEnum.getEnum(addCmd.mode);

        switch (mode) {
          case SINGLE:
            _component = new JLabel("radio selection list");  // debug
            break;
          case MULTIPLE:
            _component = new JLabel("checkbox selection list");  // debug
            break;
        }

        return this;
    }

    protected Component getComponent() {
        return _component;
    }

    protected int getGridBagAnchor() {
        if (hicp.TextDirection.RIGHT == _parent.getHorizontalTextDirection()) {
            return java.awt.GridBagConstraints.WEST;
        } else {
            return java.awt.GridBagConstraints.EAST;
        }
    }

    protected int getGridBagFill() {
        return java.awt.GridBagConstraints.NONE;
    }

    public void dispose() {
        super.dispose();
        _component = null;
    }

    protected Item modifyInvoked(final Modify modifyCmd) {
        // See what's changed.
        // Changed parent ID is handled by Controller.
        return this;
    }
}
