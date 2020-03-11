import org.apache.log4j.Logger;

import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Vector;

public class ChatServer implements ServerSocketThreadListener, SocketThreadListener {

    private final ChatServerListener listener;
    private ServerSocketThread server;
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss: ");
    final static Logger logger = Logger.getLogger(ChatServer.class);

    public static final int ACTIVITYTIMEOUT = 1200;
    private Vector<SocketThread> clients = new Vector<>();
    private Thread checkActivity;


    public ChatServer(ChatServerListener listener) {
        this.listener = listener;
    }

    public void start(int port) {
        if (server != null && server.isAlive()) {
            System.out.println("Server is already started");
            logger.warn("Trying to start server, while its already started");
        }

        else {
            server = new ServerSocketThread(this, "Server", port, 2000);
            checkActivity = new Thread() {
                synchronized void check() {
                    ClientThread client;
                    for (int i = 0; i < clients.size(); i++) {
                        client = (ClientThread) clients.get(i);
                        if (!client.isAuthorized() && (System.currentTimeMillis() - client.getCreateTime()) > ACTIVITYTIMEOUT) {
                            client.sendMessage(Library.AUTH_DENIED + Library.DELIMITER + "Your connection was closed");
                            clients.get(i).close();
                            clients.remove(i);
                        }
                    }
                }

                @Override
                public void run() {

                    while (!isInterrupted()) {
                        if (!clients.isEmpty()) {
                            try {
                                check();
                                sleep(1000);
                            } catch (InterruptedException e) {
                                putLog("Поток проверки не смог уснуть " + e.getMessage());
                                logger.error("Поток проверки не смог уснуть " + e.getMessage());
                            }
                        }
                    }

                }
            };
            checkActivity.start();
        }

    }

    public void stop() {
        if (server == null || !server.isAlive()) {
            putLog("Server is not running");
            logger.warn("Server is not running");
        } else {
            server.interrupt();
        }
    }

    private void putLog(String msg) {
        msg = DATE_FORMAT.format(System.currentTimeMillis()) +
                Thread.currentThread().getName() + ": " + msg;
        listener.onChatServerMessage(msg);
    }

    /**
     * Server methods
     */


    @Override
    public void onServerStart(ServerSocketThread thread) {
        putLog("Server thread started");
        logger.info("Server thread started");
        SqlClient.connect();
    }

    @Override
    public void onServerStop(ServerSocketThread thread) {
        putLog("Server thread stopped");
        logger.info("Server thread stopped");
        SqlClient.disconnect();
        for (int i = 0; i < clients.size(); i++) {
            clients.get(i).close();
        }

    }

    @Override
    public void onServerSocketCreated(ServerSocketThread thread, ServerSocket server) {
        putLog("Server socket created");
        logger.info("Server socket created");


    }

    @Override
    public void onServerTimeout(ServerSocketThread thread, ServerSocket server) {
//        putLog("Server timeout");

    }

    @Override
    public void onSocketAccepted(ServerSocketThread thread, ServerSocket server, Socket socket) {
        putLog("Client connected");
        logger.info("Client connected from "+ socket.getInetAddress() + ":" + socket.getPort());
        String name = "SocketThread " + socket.getInetAddress() + ":" + socket.getPort();
        new ClientThread(this, name, socket);

    }

    @Override
    public void onServerException(ServerSocketThread thread, Throwable exception) {
        exception.printStackTrace();
    }

    /**
     * Socket methods
     */


    @Override
    public synchronized void onSocketStart(SocketThread thread, Socket socket) {
        putLog("Socket created");
        logger.info("Socket created");

    }

    @Override
    public synchronized void onSocketStop(SocketThread thread) {
        ClientThread client = (ClientThread) thread;
        clients.remove(thread);

        if (client.isAuthorized() && !client.isReconnecting()) {
            sendToAuthClients(Library.getTypeBroadcast("Server",
                    client.getNickname() + " disconnected"));
        } else if (client.isRegistering()) {
            putLog("Неудачная попытка регистрации " + client.getSocket().toString());
            logger.warn("Неудачная попытка регистрации " + client.getSocket().toString());
        } else if (client.isRenaming()) {
            putLog("Неудачная попытка изменения ника " + client.getSocket().toString());
            logger.warn("Неудачная попытка изменения ника " + client.getSocket().toString());
        } else {
            putLog("Неудачная попытка входа на " + client.getSocket().toString() + ".\nСокет был закрыт по таймауту;");
            logger.warn("Неудачная попытка входа на " + client.getSocket().toString() + ".\nСокет был закрыт по таймауту;");
        }


        sendToAuthClients(Library.getUserList(getUsers()));
    }

    @Override
    public synchronized void onSocketReady(SocketThread thread, Socket socket) {
        clients.add(thread);
    }

    @Override
    public synchronized void onReceiveString(SocketThread thread, Socket socket, String msg) {
        ClientThread client = (ClientThread) thread;
        if (client.isAuthorized()) {
            handleAuthMessage(client, msg);
        } else {
            handleNonAuthMessage(client, msg);
        }
    }

    private void handleNonAuthMessage(ClientThread client, String msg) {
        String[] arr = msg.split(Library.DELIMITER);
        if (!arr[0].equals(Library.REG_REQUEST) && !arr[0].equals(Library.AUTH_REQUEST)) {
            client.msgFormatError(msg);
            return;
        }
        String login = arr[1];
        String password = arr[2];
        String nickname = SqlClient.getNickname(login, password);
        if (nickname == null) {
            if (arr[0].equals(Library.REG_REQUEST)) {
                String nick = arr[3];
                if (SqlClient.isBusyLogin(login)) {
                    client.regFail("Login is busy");
                } else if (SqlClient.isBusyNickame(nick)) {
                    client.regFail("Nickname is busy");
                } else {
                    if(SqlClient.addNewUser(login, password, arr[3])>0) {
                        client.authAccept(nick);
                        sendToAuthClients(Library.getTypeBroadcast("Server", nick + " connected"));
                        sendToAuthClients(Library.getUserList(getUsers()));
                    }
                    else {
                        putLog("Failed Register new user attemp at "+client);
                        logger.warn("Failed Register new user attemp at "+client);
                    }
                }
            } else {
                putLog("Invalid login attempt: " + login);
                logger.warn("Invalid login attempt: " + login);
                client.authFail();
                return;
            }
        } else {
            if (arr[0].equals(Library.REG_REQUEST)) {
                client.regFail("User with login " + login + " already exist");
                return;
            } else {
                ClientThread oldClient = findClientByNickname(nickname);
                client.authAccept(nickname);
                if (oldClient == null) {
                    sendToAuthClients(Library.getTypeBroadcast("Server", nickname + " connected"));
                } else {
                    oldClient.reconnect();
                    clients.remove(oldClient);
                }

                sendToAuthClients(Library.getUserList(getUsers()));

            }
        }
    }

    private void handleAuthMessage(ClientThread client, String msg) {
        String[] arr = msg.split(Library.DELIMITER);
        String msgType = arr[0];
        switch (msgType) {
            case Library.TYPE_BCAST_CLIENT:
                sendToAuthClients(Library.getTypeBroadcast(
                        client.getNickname(), arr[1]));
                break;
            case Library.RENAMING_REQUEST:
                if (SqlClient.isBusyNickame(arr[1])) client.renFail("Nickname is busy");
                else {
                    if (SqlClient.setNickname(client.getNickname(), arr[1])!=0) {
                        client.renAccept(arr[1]);
                        sendToAuthClients(Library.getUserList(getUsers()));
                    } else client.renFail("Something goes wrong with DB");
                }
                break;
            default:
                client.sendMessage(Library.getMsgFormatError(msg));
        }
    }


    // launch4j

    private void sendToAuthClients(String msg) {
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.isAuthorized()) continue;
            client.sendMessage(msg);
        }
    }

    @Override
    public synchronized void onSocketException(SocketThread thread, Exception exception) {
        exception.printStackTrace();
    }

    private String getUsers() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.isAuthorized()) continue;
            sb.append(client.getNickname()).append(Library.DELIMITER);
        }
        return sb.toString();
    }

    private synchronized ClientThread findClientByNickname(String nickname) {
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.isAuthorized()) continue;
            if (client.getNickname().equals(nickname))
                return client;
        }
        return null;
    }

}
