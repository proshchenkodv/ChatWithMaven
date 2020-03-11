import java.sql.*;

public class SqlClient {

    private static Connection connection;
    private static Statement statement;

    synchronized static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:chat-server/src/main/resources/chat.db");
            statement = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized static String getNickname(String login, String password) {
        String query = String.format("select nickname from users where login='%s' and password='%s'", login, password);
        try (ResultSet set = statement.executeQuery(query)) {
            if (set.next())
                return set.getString(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
    synchronized static Boolean isBusyNickame(String nickname) {
        String query = String.format("select nickname from users where nickname='%s'", nickname);
        try (ResultSet set = statement.executeQuery(query)) {
            if (set.next())
                return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }
    synchronized static Boolean isBusyLogin(String login) {
        String query = String.format("select login from users where login='%s'", login);
        try (ResultSet set = statement.executeQuery(query)) {
            if (set.next())
                return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    synchronized static int addNewUser(String login, String password, String nickname) {
        String query = String.format("insert into users(login, password, nickname) values('%s','%s','%s')", login,password,nickname);
        try  {
            return statement.executeUpdate(query);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static int setNickname(String oldNick,String newNick) {
        String query = String.format("UPDATE users set nickname='%s' where nickname='%s'", newNick, oldNick);
        try  {
            return statement.executeUpdate(query);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
