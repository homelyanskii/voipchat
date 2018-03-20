package Server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class ServerChat {

    private static ArrayList<PrintWriter> arrayList;
    private static Connection c;
    private static Statement statement;
    private static PrintWriter writer;


    public static void main(String[] args) throws Exception {
        initServer();
    }

    private static void initServer() throws Exception {
        arrayList = new ArrayList<>();
        setDBConfig();
        try{
            ServerSocket serverSocket = new ServerSocket(3134);
            while (true){
                Socket socket = serverSocket.accept();
                System.out.println("Got user!");
                writer = new PrintWriter(socket.getOutputStream());
                InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String login = reader.readLine();
                saveUser(login);
                loadDB(writer);
                arrayList.add(writer);
                Thread thread = new Thread(new Listener(socket,login));
                thread.start();
            }
        } catch (Exception e) {}
    }

    private static void saveUser(String login) throws SQLException {

        String querySQL = "SELECT COUNT(`login`) FROM `logins` WHERE `login` LIKE '" + login +"'";
        ResultSet resultSet = statement.executeQuery(querySQL);
        resultSet.next();
        System.out.println(resultSet.getString(1));


        Date date = new Date();
        DateFormat datePattern = new SimpleDateFormat("yyyy-MM-dd");
        if (resultSet.getInt(1) == 0){
            querySQL = "INSERT INTO `logins` (`login`,`last_entry`) VALUES ('"+ login +"','"+ datePattern.format(date) +"')";
        } else {
            querySQL = "UPDATE `logins` SET `last_entry`=\""+ datePattern.format(date) +"\" WHERE `login` LIKE \""+ login +"\" ";
        }
        statement.executeUpdate(querySQL);

    }

    private static void tellEveryone(String msg) throws SQLException, ClassNotFoundException {
        int x = msg.indexOf(':');
        String login = msg.substring(0,x);
        msg = msg.substring(x + 1 , msg.length());

        saveDB(login,msg);

        Iterator<PrintWriter> iterator = arrayList.iterator();
        while (iterator.hasNext()){
            writer = iterator.next();
            writer.println(login + ": " + msg);
            writer.flush();
        }

    }

    private static void loadDB(PrintWriter writer) throws Exception {

        String querySQL = "SELECT `logins`.`login`,`chat`.`msg` FROM `logins`,`chat` WHERE `chat`.`loginID`=`logins`.`#userID`";
        ResultSet resultSet = statement.executeQuery(querySQL);

        while (resultSet.next()){
            writer.println(resultSet.getString("login")+ ": " + resultSet.getString("msg"));
            writer.flush();
        }
    }

    private static void saveDB(String login, String msg) throws SQLException, ClassNotFoundException {

        String querySQL = "SELECT `logins`.`#userID` FROM `logins` WHERE `logins`.`login` LIKE ";
        querySQL = querySQL + "'"+ login +"'";
        ResultSet resultSet = statement.executeQuery(querySQL);
        resultSet.next();
        Integer loginID = resultSet.getInt("#userID");

        querySQL = "INSERT INTO `chat` (`loginID`, `msg`) VALUES ('"+ loginID +"', '"+ msg +"');";
        statement.executeUpdate(querySQL);
//        querySQL = "INSERT INTO `logins` (`login`) VALUES ('"+ login +"');";
//        statement.executeUpdate(querySQL);
        //ResultSet resultSet = statement.executeQuery(querySQL);
        //SELECT `logins`.`login` FROM `logins`, `chat` WHERE `chat`.`loginID` = `logins`.`#userID`
    }

    private static void setDBConfig() throws ClassNotFoundException, SQLException {

        String url = "jdbc:mysql://localhost:3306/messenger";
        String login = "root";
        String pass = "";

        Class.forName("com.mysql.jdbc.Driver");
        c = DriverManager.getConnection(url, login, pass);
        statement = c.createStatement();

    }

    private static class Listener implements Runnable{

        BufferedReader bufferedReader;
        String login;

        Listener(Socket socket, String login){
            InputStreamReader inputStreamReader;
            try {
                inputStreamReader = new InputStreamReader(socket.getInputStream());
                bufferedReader = new BufferedReader(inputStreamReader);
                this.login = login;
                //System.out.println(bufferedReader.readLine());
            } catch (Exception e) {}
        }

        @Override
        public void run() {
            String msg;
            try {

                while ((msg = bufferedReader.readLine()) != null){
                    System.out.println(msg);

                    tellEveryone(msg);
                }
            }catch (Exception e){}
        }


    }
}