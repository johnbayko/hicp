package hv;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.lang.NumberFormatException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import hicp_client.Params;
import hv.text.WholeNumberDocument;

public class HICP_ParamsFrame
    extends javax.swing.JFrame
{
    private static final Logger LOGGER =
        Logger.getLogger( HICP_ParamsFrame.class.getName() );

    final protected JTextField _address;
    final protected JTextField _port;

    final protected JTextField _username;
    final protected JPasswordField _password;
    final protected JTextField _application;

    final protected JButton _connectButton;
    final protected JButton _disconnectButton;
    final protected JButton _quitButton;

    public HICP_ParamsFrame() {
        // Set up GUI
	{
	    final Container contentPane = this.getContentPane();
	    contentPane.setLayout(new BorderLayout());

            {
                final GridBagLayout centreLayout = new GridBagLayout();
                final GridBagConstraints centreLC = new GridBagConstraints();
                final JPanel centrePanel = new JPanel(centreLayout);

                centreLC.fill = GridBagConstraints.BOTH;
                centreLC.gridx = 0;
                centreLC.gridy = GridBagConstraints.RELATIVE;

                {
                    final GridBagLayout connectLayout = new GridBagLayout();
                    final GridBagConstraints connectLC = new GridBagConstraints();
                    final JPanel connectPanel = new JPanel(connectLayout);

                    connectLC.ipadx = 4;
                    connectPanel.setBorder(
                        BorderFactory.createTitledBorder("Connect")
                    );

                    connectLC.anchor = GridBagConstraints.EAST;
                    connectLC.fill = GridBagConstraints.NONE;
                    connectLC.gridx = 0;
                    connectLC.weightx = 0.0;
                    connectLC.gridy = 0;
                    connectPanel.add(new JLabel("Host"), connectLC);

                    _address = new JTextField(30);
                    connectLC.anchor = GridBagConstraints.WEST;
                    connectLC.fill = GridBagConstraints.HORIZONTAL;
                    connectLC.gridx = 1;
                    connectLC.weightx = 1.0;
                    connectPanel.add(_address, connectLC);

                    // Port number - edit as integer.
                    connectLC.anchor = GridBagConstraints.EAST;
                    connectLC.fill = GridBagConstraints.NONE;
                    connectLC.gridx = 0;
                    connectLC.weightx = 0.0;
                    connectLC.gridy++;
                    connectPanel.add(new JLabel("Port"), connectLC);

                    _port = new JTextField();
                    final var portDocument = new WholeNumberDocument();
                    _port.setDocument(portDocument);
                    connectLC.anchor = GridBagConstraints.WEST;
                    connectLC.fill = GridBagConstraints.HORIZONTAL;
                    connectLC.gridx = 1;
                    connectLC.weightx = 1.0;
                    connectPanel.add(_port, connectLC);

                    centrePanel.add(connectPanel, centreLC);
                }
                {
                    final GridBagLayout hicpLayout = new GridBagLayout();
                    final GridBagConstraints hicpLC = new GridBagConstraints();
                    final JPanel hicpPanel = new JPanel(hicpLayout);

                    hicpLC.ipadx = 4;
                    hicpPanel.setBorder(
                        BorderFactory.createTitledBorder("HICP")
                    );

                    hicpLC.anchor = GridBagConstraints.EAST;
                    hicpLC.fill = GridBagConstraints.NONE;
                    hicpLC.gridx = 0;
                    hicpLC.weightx = 0.0;
                    hicpLC.gridy = 0;
                    hicpPanel.add(new JLabel("User"), hicpLC);

                    _username = new JTextField(30);
                    _username.setText("user1");  // debug
                    hicpLC.anchor = GridBagConstraints.WEST;
                    hicpLC.fill = GridBagConstraints.HORIZONTAL;
                    hicpLC.gridx = 1;
                    hicpLC.weightx = 1.0;
                    hicpPanel.add(_username, hicpLC);

                    // Password - display as *.
                    hicpLC.anchor = GridBagConstraints.EAST;
                    hicpLC.fill = GridBagConstraints.NONE;
                    hicpLC.gridx = 0;
                    hicpLC.weightx = 0.0;
                    hicpLC.gridy++;
                    hicpPanel.add(new JLabel("Password"), hicpLC);

                    _password = new JPasswordField(30);
                    _password.setText("password1");  // debug
                    hicpLC.anchor = GridBagConstraints.WEST;
                    hicpLC.fill = GridBagConstraints.HORIZONTAL;
                    hicpLC.gridx = 1;
                    hicpLC.weightx = 1.0;
                    hicpPanel.add(_password, hicpLC);

                    hicpLC.anchor = GridBagConstraints.EAST;
                    hicpLC.fill = GridBagConstraints.NONE;
                    hicpLC.gridx = 0;
                    hicpLC.weightx = 0.0;
                    hicpLC.gridy++;
                    hicpPanel.add(new JLabel("Application"), hicpLC);

                    _application = new JTextField(30);
                    hicpLC.anchor = GridBagConstraints.WEST;
                    hicpLC.fill = GridBagConstraints.HORIZONTAL;
                    hicpLC.gridx = 1;
                    hicpLC.weightx = 1.0;
                    hicpPanel.add(_application, hicpLC);

                    centrePanel.add(hicpPanel, centreLC);
                }

                contentPane.add(centrePanel, BorderLayout.CENTER);
            }
	    {
	        final JPanel southPanel = new JPanel(new BorderLayout());
		final JPanel buttonPanel = new JPanel(new FlowLayout());

                _connectButton = new JButton("Connect");
		buttonPanel.add(_connectButton);

                _disconnectButton = new JButton("Disconnect");
                _disconnectButton.setEnabled(false);
		buttonPanel.add(_disconnectButton);

                _quitButton = new JButton("Quit");
		buttonPanel.add(_quitButton);

	        southPanel.add(buttonPanel, BorderLayout.EAST);
	        contentPane.add(southPanel, BorderLayout.SOUTH);
	    }
	}

	this.pack();
	this.setSize(this.getPreferredSize());
    }

    public void setConnectParams(ConnectParams connectParams) {
        _address.setText(connectParams.address);
        _port.setText(Integer.toString(connectParams.port));
    }

    public void fillConnectParams(ConnectParams connectParams) {
	// Populate _connectParams from GUI fields.
        connectParams.address = _address.getText();

        try {
            connectParams.port = Integer.parseInt(_port.getText());
        } catch (NumberFormatException ex) {
            // Document should ensure text is always be parseble.
            // If some sort of error, don't change port, but set text to
            // port being used.
            _port.setText(Integer.toString(connectParams.port));
        }
    }

    public void setHICP_Params(Params hicpParams) {
        _username.setText(hicpParams.username);
        _password.setText(hicpParams.password);
        _application.setText(hicpParams.application);
    }

    public void fillHICP_Params(Params hicpParams) {
	// Populate _hicpParams from GUI fields.
        hicpParams.username = _username.getText();

        // Java docs say Strings are not secure. Doesn't say why, so
        // I'll use a String anyway.
        hicpParams.password = new String(_password.getPassword());

        hicpParams.application = _application.getText();
    }

    public void addConnectListener(ActionListener l) {
        // Add to connect button.
	_connectButton.addActionListener(l);
    }

    public void removeConnectListener(ActionListener l) {
        // Remove from connect button.
	_connectButton.removeActionListener(l);
    }

    public void setConnectEnabled(boolean b) {
        _connectButton.setEnabled(b);
    }

    public void addDisconnectListener(ActionListener l) {
        // Add to connect button.
	_disconnectButton.addActionListener(l);
    }

    public void removeDisconnectListener(ActionListener l) {
        // Remove from connect button.
	_disconnectButton.removeActionListener(l);
    }

    public void setDisconnectEnabled(boolean b) {
        _disconnectButton.setEnabled(b);
    }

    public void addQuitListener(ActionListener l) {
        // Add to quit button.
	_quitButton.addActionListener(l);
    }

    public void removeQuitListener(ActionListener l) {
        // Remove from quit button.
	_quitButton.removeActionListener(l);
    }
}
