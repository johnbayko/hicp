package hv;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import hicp_client.Controller;
import hicp_client.Monitor;
import hicp_client.Params;
import hicp_client.Session;

public class HV {
    private static final Logger LOGGER =
        Logger.getLogger( HV.class.getName() );

    final protected HICP_ParamsFrame _hicp_ParamsFrame;
    protected Socket _socket = null;
    protected Controller _hicp = null;

    final protected Monitor _hicpMonitor =
        new Monitor() {
            /**
                Signal that an open request has been made.
             */
//          public void open(/* something... */);

            /**
                Signal that HICP is connected.
             */
            public void connected() {
                _hicp_ParamsFrame.setConnectEnabled(false);
                _hicp_ParamsFrame.setDisconnectEnabled(true);
            }

            /**
                Signal that HICP is disconnected.
             */
            public void disconnected() {
                _hicp.dispose();

                try {
                    _socket.close();
                } catch (IOException ex) {
                    // Don't care at this point.
                }
                _socket = null;

                _hicp = null;
                LOGGER.log(Level.FINE, "HICP closed");  // debug

                _hicp_ParamsFrame.setConnectEnabled(true);
                _hicp_ParamsFrame.setDisconnectEnabled(false);
                LOGGER.log(Level.FINE, "HICP Disconnected");  // debug
            }

            /**
                Handle an exception from the HICP component.
             */ 
            public void exception(String message, Exception ex) {
                JOptionPane.showMessageDialog(
                    _hicp_ParamsFrame,
                    message,
                    "HICP error",
                    JOptionPane.WARNING_MESSAGE
                );
                LOGGER.log(Level.WARNING, message, ex);
            }
        };

    private void configLogger(String pkg) {
        final Logger parentLogger = Logger.getLogger(pkg);

        parentLogger.setLevel(Level.ALL);

        java.util.logging.Handler h;
        try {
            h = new java.util.logging.FileHandler(pkg + ".log");
        } catch (java.io.IOException ex) {
            System.out.println("Exception: " + ex.toString());

            h = new java.util.logging.ConsoleHandler();
        }
        h.setFormatter(new java.util.logging.SimpleFormatter());
        parentLogger.addHandler(h);
    }

    public HV() {
        configLogger("hv");
        configLogger("hicp");
        configLogger("hicp_client");
        LOGGER.log(Level.FINE, "Started");

        _hicp_ParamsFrame = new HICP_ParamsFrame();
        _hicp_ParamsFrame.setVisible(true);

        // Add connect and quit listeners.
        _hicp_ParamsFrame.addConnectListener(_connectListener);
        _hicp_ParamsFrame.addDisconnectListener(_disconnectListener);
        _hicp_ParamsFrame.addQuitListener(_quitListener);
    }

    final ActionListener _connectListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            try {
                final Params hp =  new Params();
                _hicp_ParamsFrame.fillHICP_Params(hp);

                final ConnectParams cp = new ConnectParams();
                _hicp_ParamsFrame.fillConnectParams(cp);

                if ( (null == cp.address) || "".equals(cp.address) ) {
                    cp.address = "localhost";
                }
                _socket = new Socket(cp.address, cp.port);
                
                // Construct HICP session.
                final Session hicpSession =
                    new Session(
                        hp,
                        _socket.getInputStream(),
                        _socket.getOutputStream()
                    );

                // Make HICP
                _hicp = new Controller(hicpSession, _hicpMonitor/*, LOGGER*/);
                _hicp.connect();
            } catch (UnknownHostException ex) {
                _hicp = null;

                final String msg = "Couldn't open socket";

                JOptionPane.showMessageDialog(
                    _hicp_ParamsFrame,
                    msg + ":\n" + ex.getMessage(),
                    "HICP error",
                    JOptionPane.WARNING_MESSAGE
                );
                LOGGER.log(Level.WARNING, msg, ex);
            } catch (IOException ex) {
                _hicp = null;

                final String msg = "Exception opening socket";

                JOptionPane.showMessageDialog(
                    _hicp_ParamsFrame,
                    msg + ":\n" + ex.getMessage(),
                    "HICP exception",
                    JOptionPane.ERROR_MESSAGE
                );
                LOGGER.log(Level.WARNING, msg, ex);
            }
        }
    };

    final ActionListener _disconnectListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            _hicp.disconnect();
        }
    };

    final ActionListener _quitListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            _hicp_ParamsFrame.removeConnectListener(_connectListener);
            _hicp_ParamsFrame.removeQuitListener(_quitListener);
            _hicp_ParamsFrame.dispose();

            // Close _hicp if opened
            if (null != _hicp) {
                _hicp.dispose();
                _hicp = null;
            }

            System.exit(0);
        }
    };

    public static void main(String[] args) {
        final HV hv = new HV();
    }
}
