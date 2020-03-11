import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

public class ClientGUI extends JFrame implements ActionListener, ListSelectionListener, Thread.UncaughtExceptionHandler, SocketThreadListener, MouseListener {

    private static final int WIDTH = 600;
    private static final int HEIGHT = 300;
    private String nickname = "";
    private boolean renaming = false;

    private final JTextArea log = new JTextArea();
    private final JPanel panelTop = new JPanel(new GridLayout(2, 3));
    //    private final JTextField tfIPAddress = new JTextField("95.84.209.91");
    private final JTextField tfIPAddress = new JTextField("127.0.0.1");

    private final JTextField tfPort = new JTextField("8189");
    private final JCheckBox cbAlwaysOnTop = new JCheckBox("Always on top");
    private final JTextField tfLogin = new JTextField("ivan");
    private final JPasswordField tfPassword = new JPasswordField("123");
    private final JButton btnLogin = new JButton("Login");
    private final JButton btnRegister = new JButton("Register");

    private final JPanel panelBottom = new JPanel(new BorderLayout());
    private final JButton btnDisconnect = new JButton("<html><b>Disconnect</b></html>");
    private final JTextField tfMessage = new JTextField();
    private final JButton btnSend = new JButton("Send");

    private final JList<String> userList = new JList<>();
    private boolean shownIoErrors = false;
    private SocketThread socketThread;
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss: ");
    private final String WINDOW_TITLE = "Chat";
    private final String REGISTRATION_FORM_TITLE = "Register new User";
    private final String CHANGE_NICKNAME_FORM_TITLE = "Changing Nickname";
    private final String CHANGE_NICKNAME_BUTTON_TEXT = "Change Nickname";
    private final String REGISTER_NEW_USER_BUTTON_TEXT = "Register";
    private ModalDialog registrationFrom = new ModalDialog(this, REGISTRATION_FORM_TITLE, true, REGISTER_NEW_USER_BUTTON_TEXT);
    private ModalDialog renamingForm = new ModalDialog(this, CHANGE_NICKNAME_FORM_TITLE, true, CHANGE_NICKNAME_BUTTON_TEXT);

    private ClientGUI() {
        Thread.setDefaultUncaughtExceptionHandler(this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setTitle(WINDOW_TITLE);
        setSize(WIDTH, HEIGHT);
        log.setEditable(false);
        log.setLineWrap(true);
        JScrollPane scrollLog = new JScrollPane(log);
        JScrollPane scrollUser = new JScrollPane(userList);
        scrollUser.setPreferredSize(new Dimension(100, 0));
        btnLogin.setPreferredSize(new Dimension(100, 0));
        btnRegister.setPreferredSize(new Dimension(100, 0));
        cbAlwaysOnTop.addActionListener(this);
        btnSend.addActionListener(this);
        tfMessage.addActionListener(this);
        btnLogin.addActionListener(this);
        btnRegister.addActionListener(this);
        btnDisconnect.addActionListener(this);
        userList.addListSelectionListener(this);
        userList.addMouseListener(this);
        panelBottom.setVisible(false);

        panelTop.add(tfIPAddress);
        panelTop.add(tfPort);
        panelTop.add(tfLogin);
        panelTop.add(tfPassword);
        panelTop.add(cbAlwaysOnTop);
        panelTop.add(btnLogin);
        panelTop.add(btnRegister);
        panelBottom.add(btnDisconnect, BorderLayout.WEST);
        panelBottom.add(tfMessage, BorderLayout.CENTER);
        panelBottom.add(btnSend, BorderLayout.EAST);

        add(scrollLog, BorderLayout.CENTER);
        add(scrollUser, BorderLayout.EAST);
        add(panelTop, BorderLayout.NORTH);
        add(panelBottom, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void connect() {
        try {
            Socket socket = new Socket(tfIPAddress.getText(), Integer.parseInt(tfPort.getText()));
            socketThread = new SocketThread(this, "Client", socket);
        } catch (IOException e) {
            showException(Thread.currentThread(), e);
        }
    }

    public void register() {

        registrationFrom.tfLogin.setText(tfLogin.getText());
        registrationFrom.tfNickname.setText(tfLogin.getText());
        registrationFrom.tfPassword.setText(String.valueOf(tfPassword.getPassword()));
        registrationFrom.setVisible(true);

    }

    public void rename() {
        renaming = true;
        renamingForm.tfNickname.setText(tfLogin.getText());
        renamingForm.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() { // Event Dispatching Thread
                new ClientGUI();
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == cbAlwaysOnTop) {
            setAlwaysOnTop(cbAlwaysOnTop.isSelected());
        } else if (src == btnSend || src == tfMessage) {
            sendMessage();
        } else if (src == btnLogin) {
            connect();
        } else if (src == btnDisconnect) {
            socketThread.close();
        } else if (src == btnRegister) {
            register();
        } else {
            throw new RuntimeException("Unknown source: " + src);
        }
    }

    private void sendMessage() {

        String msg = tfMessage.getText();
        String username = tfLogin.getText();
        if ("".equals(msg)) return;
        tfMessage.setText(null);
        tfMessage.requestFocusInWindow();
        socketThread.sendMessage(Library.getTypeBcastClient(msg));
        //wrtMsgToLogFile(msg, username);

    }

    private void wrtMsgToLogFile(String msg, String username) {
        try (FileWriter out = new FileWriter("log.txt", true)) {
            out.write(username + ": " + msg + "\n");
            out.flush();
        } catch (IOException e) {
            if (!shownIoErrors) {
                shownIoErrors = true;
                showException(Thread.currentThread(), e);
            }
        }
    }

    private void putLog(String msg) {
        if ("".equals(msg)) return;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.append(msg + "\n");
                log.setCaretPosition(log.getDocument().getLength());
            }
        });
    }

    private void showException(Thread t, Throwable e) {
        String msg;
        StackTraceElement[] ste = e.getStackTrace();
        if (ste.length == 0)
            msg = "Empty Stacktrace";
        else {
            msg = "Exception in " + t.getName() + " " +
                    e.getClass().getCanonicalName() + ": " +
                    e.getMessage() + "\n\t at " + ste[0];
        }
        JOptionPane.showMessageDialog(null, msg, "Exception", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        showException(t, e);
        System.exit(1);
    }

    /**
     * Socket thread listener methods
     */


    @Override
    public void onSocketStart(SocketThread thread, Socket socket) {
        putLog("Start");
    }

    @Override
    public void onSocketStop(SocketThread thread) {
        panelBottom.setVisible(false);
        panelTop.setVisible(true);
        setTitle(WINDOW_TITLE);
        userList.setListData(new String[0]);
        nickname = "";
    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        panelBottom.setVisible(true);
        panelTop.setVisible(false);
        String login = tfLogin.getText();
        String password = new String(tfPassword.getPassword());
        if ("".equals(nickname)) {
            thread.sendMessage(Library.getAuthRequest(login, password));
        } else {

            thread.sendMessage(Library.getRegRequest(login, password, nickname));
        }

    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String msg) {
        handleMessage(msg);
    }

    @Override
    public void onSocketException(SocketThread thread, Exception exception) {
        // showException(thread, exception);
    }

    private void handleMessage(String msg) {
        String[] arr = msg.split(Library.DELIMITER);
        String msgType = arr[0];
        switch (msgType) {
            case Library.AUTH_ACCEPT:
                nickname = arr[1];
                setTitle(WINDOW_TITLE + " entered with nickname: " + nickname);
                registrationFrom.setVisible(false);
                break;
            case Library.AUTH_DENIED:

                putLog(msg.replace(Library.DELIMITER, " "));

                break;
            case Library.REG_DENIED:
                putLog(msg.replace(Library.DELIMITER, " "));
                registrationFrom.errordiag.showMessageDialog(registrationFrom, msg.replace(Library.DELIMITER, " "), "Error", 2);
//                throw new RuntimeException("Current user already exist: " + msg);

            case Library.MSG_FORMAT_ERROR:
                putLog(msg);
                socketThread.close();
                break;
            case Library.TYPE_BROADCAST:
                putLog(DATE_FORMAT.format(Long.parseLong(arr[1])) +
                        arr[2] + ": " + arr[3]);
                break;
            case Library.USER_LIST:
                String users = msg.substring(Library.USER_LIST.length() +
                        Library.DELIMITER.length());
                Vector<String> userVector = new Vector<>();
                userVector.addAll(Arrays.asList(users.split(Library.DELIMITER)));
                if (userVector.size() > 1) {
                    Collections.sort(userVector);
                    userVector.remove(userVector.indexOf(nickname));
                    userVector.insertElementAt(nickname, 0);
                }
                userList.setListData(userVector);
                break;
            case Library.REN_ACCEPT:
                nickname = renamingForm.nickname;
                renamingForm.setVisible(false);
                putLog("Your nickname was changed to " + nickname);
                setTitle(WINDOW_TITLE + " entered with nickname: " + nickname);
                break;

            case Library.REN_DENIED:
                putLog(msg.replace(Library.DELIMITER, " "));
                renamingForm.errordiag.showMessageDialog(renamingForm, msg.replace(Library.DELIMITER, " "), "Error", 2);
                break;

            default:
                throw new RuntimeException("Unknown message type: " + msg);
        }
    }

    /**
     * MouseEvent Listener methods
     */


    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        if (SwingUtilities.isRightMouseButton(mouseEvent)) {
            userList.setSelectedIndex(userList.locationToIndex(mouseEvent.getPoint()));
            if (userList.getSelectedValue().equals(nickname)) doPop(mouseEvent);
        }

    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {

    }

    private void doPop(MouseEvent mouseEvent) {
        PopUp menu = new PopUp("Изменить ник");
        menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());

    }

    @Override
    public void valueChanged(ListSelectionEvent e) {

    }

    /**
     * PopUpMenu Class
     */

    private class PopUp extends JPopupMenu {
        private final JMenuItem nickChange = new JMenuItem("Change your nick");

        public PopUp(String label) {
            super(label);
            nickChange.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    rename();
                }
            });
            this.add(nickChange);
        }
    }

    /**
     * Register Dialog Window
     */
    public class ModalDialog extends JDialog implements ActionListener {
        private static final int R_WIDTH = 600;
        private static final int R_HEIGHT = 115;
        private static final int N_WIDTH = 600;
        private static final int N_HEIGHT = 100;
        private final JLabel lLogin = new JLabel("Input Login:");
        private final JTextField tfLogin = new JTextField();
        private final JLabel lNickname = new JLabel("Input Nickname:");
        private final JTextField tfNickname = new JTextField();
        private final JLabel lPassword = new JLabel("Input Password:");
        private final JPasswordField tfPassword = new JPasswordField();
        private final JPanel panelTop;
        private final JPanel panelBottom;
        private final JButton btnAccept = new JButton();
        private final ClientGUI parent;
        private boolean renaming = false;
        private boolean register = false;
        private String nickname;
        private JOptionPane errordiag = new JOptionPane();

        /**
         * Register new user
         */

        public ModalDialog(Frame owner, String title, boolean modal, String btnName) {
            super(owner, title, modal);
            parent = (ClientGUI) owner;
            setLocationRelativeTo(null);

            panelTop = new JPanel();
            panelBottom = new JPanel();
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            Thread.setDefaultUncaughtExceptionHandler((Thread.UncaughtExceptionHandler) owner);
            btnAccept.setText(btnName);
            panelBottom.add(btnAccept);

            if (btnName.equals(REGISTER_NEW_USER_BUTTON_TEXT)) {
                register = true;
                setSize(R_WIDTH, R_HEIGHT);
                panelTop.setLayout(new GridLayout(2, 3));
                panelBottom.setLayout(new GridLayout(1, 1));
                panelTop.add(lLogin);
                panelTop.add(lPassword);
                panelTop.add(lNickname);
                panelTop.add(tfLogin);
                panelTop.add(tfPassword);
                panelTop.add(tfNickname);
            } else if (btnName.equals(CHANGE_NICKNAME_BUTTON_TEXT)) {
                renaming = true;
                setSize(N_WIDTH, N_HEIGHT);
                panelTop.setLayout(new GridLayout(1, 2));
                panelBottom.setLayout(new GridLayout(1, 1));
                panelTop.add(lNickname);
                panelTop.add(tfNickname);
            }

            add(panelTop, BorderLayout.NORTH);
            add(panelBottom, BorderLayout.SOUTH);
            btnAccept.addActionListener(this);
        }

        public String getNickname() {
            return nickname;
        }

        /**
         * Changing NickName         *
         */


        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            Object src = actionEvent.getSource();
            if (src == btnAccept) {
                if (register) {
                    parent.tfLogin.setText(tfLogin.getText());
                    parent.tfPassword.setText(String.valueOf(tfPassword.getPassword()));
                    parent.nickname = tfNickname.getText();
                    parent.connect();

                } else if (renaming) {
                    String nick = tfNickname.getText();
                    if (nick != null && !"".equals(nick)) {
                        if (!nick.equals(parent.nickname))
                            nickname = nick;
                        parent.socketThread.sendMessage(Library.getRenaimingRequest(nickname));
//                    } else new ErrorDialog(parent, "Nickname не заполнен");
                    } else errordiag.showMessageDialog(parent, "Nickname can't be empty", "Error", 2);

                }
            }
        }
    }



}