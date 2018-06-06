package Server;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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
import java.util.Timer;
import java.util.TimerTask;

public class ServerChat {

    private static ArrayList<PrintWriter> arrayList;
    private static Statement statement;
    private static PrintWriter writer;
    private static ObservableList<String> connectedList = FXCollections.observableArrayList();
    private static Timer timer = new Timer();

    public static void main(String[] args) throws Exception {
        initServer();
    }

    private static void initServer() throws Exception {
        arrayList = new ArrayList<>();

        setDBConfig();
        try {
            ServerSocket serverSocket = new ServerSocket(3134);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Got user!");
                writer = new PrintWriter(socket.getOutputStream());
                InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String login = reader.readLine();
                saveUser(login);
                //loadDB(writer);
                arrayList.add(writer);

                Thread thread = new Thread(new Listener(socket, login));
                thread.start();
                /*timer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        for ( String s: connectedList) {
                                            for (PrintWriter anArrayList : arrayList) {
                                                anArrayList.println("#>" + s);
                                                anArrayList.flush();
                                            }
                                        }
                                    }
                                }, 5000);
                */
            }
        } catch (Exception ignored) {}
    }

    private static void addUserList(String login) {
        for (PrintWriter anArrayList : arrayList) {
            anArrayList.println("#>" + login);
            anArrayList.flush();
        }
    }

    private static void saveUser(String login) throws SQLException {

        String querySQL = "SELECT COUNT(`login`) FROM `logins` WHERE `login` LIKE '" + login +"'";
        ResultSet resultSet = statement.executeQuery(querySQL);
        resultSet.next();


        Date date = new Date();
        DateFormat datePattern = new SimpleDateFormat("yyyy-MM-dd");
        if (resultSet.getInt(1) == 0){
            querySQL = "INSERT INTO `logins` (`login`,`last_entry`) VALUES ('"+ login +"','"+ datePattern.format(date) +"')";
        } else {
            querySQL = "UPDATE `logins` SET `last_entry`=\""+ datePattern.format(date) +"\" WHERE `login` LIKE \""+ login +"\" ";
        }
        statement.executeUpdate(querySQL);

    }

    private static void tellEveryone(String login, String msg) throws SQLException, ClassNotFoundException {

        //saveDB(login,msg);

        for (PrintWriter anArrayList : arrayList) {
            anArrayList.println(login + ": " + msg);
            anArrayList.flush();
        }
    }

    private static void loadDB(PrintWriter writer) throws Exception {

        String querySQL = "SELECT `logins`.`login`, `chat`.`msg` FROM `logins` INNER JOIN `chat` ON `logins`.`#userID` = `chat`.`loginID` ORDER BY `chat`.`#msgID` ";
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
        Date date = new Date();
        DateFormat datePattern = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        querySQL = "INSERT INTO `chat` (`loginID`, `msg`, `msgDate`) VALUES ('"+ loginID +"', '"+ msg +"', '"
                + datePattern.format(date) +"')";
        statement.executeUpdate(querySQL);
    }

    private static void setDBConfig() throws ClassNotFoundException, SQLException {

        String url = "jdbc:mysql://localhost:3307/messenger";
        String login = "root";
        String pass = "";

        Class.forName("com.mysql.jdbc.Driver");
        Connection c = DriverManager.getConnection(url, login, pass);
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
                for (String s: connectedList){
                    writer.println("#>" + s);
                    writer.flush();
                }
                connectedList.add(login);
                addUserList(login);

            } catch (Exception e) { e.printStackTrace();}
        }

        @Override
        public void run() {
            String msg;
            try {

                while ((msg = bufferedReader.readLine()) != null){
                    System.out.println(msg);
                    tellEveryone(login, msg);
                }
            }catch (Exception e){}
        }
    }
}