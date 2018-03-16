package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientChat {

    private static JTextField mainTextField;
    private static JTextArea mainTextArea;
    private static String login;
    private static BufferedReader reader;
    private static PrintWriter writer;

    public static void main(String[] args) throws Exception, IOException {
        initChat();
    }

    public static void initChat(){

        login = JOptionPane.showInputDialog("Введите логин: ");
        JFrame mainFrame = new JFrame("Client");
        mainFrame.setResizable(false);
        mainFrame.setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        mainTextArea = new JTextArea(16,36);
        mainTextArea.setLineWrap(true);
        mainTextArea.setEditable(false);
        mainTextArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(mainTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mainTextField = new JTextField(29);
        JButton sendButton = new JButton("Post");

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String msg = login + ":" + mainTextField.getText();
                writer.println(msg);
                writer.flush();
                mainTextField.setText("");
                mainTextField.requestFocus();
            }
        });
        panel.add(scrollPane);
        panel.add(mainTextField);
        panel.add(sendButton);
        setSocket();

//        writer.println(login);
//        writer.flush();

        Thread thread = new Thread(new Listener());
        thread.start();

        mainFrame.getContentPane().add(BorderLayout.CENTER, panel);
        mainFrame.setSize(400,360);
        mainFrame.setVisible(true);
        mainFrame.setDefaultCloseOperation(mainFrame.EXIT_ON_CLOSE);
    }

    private static void setSocket(){
        try {
            Socket socket = new Socket("127.0.0.1",3134);
            InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
            reader = new BufferedReader(inputStreamReader);
            writer = new PrintWriter(socket.getOutputStream());
        } catch (Exception e) {}
    }

    private static class Listener implements Runnable{
        @Override
        public void run() {
            String msg;
            try {
                while ((msg = reader.readLine()) != null){
                    mainTextArea.append(msg + "\n");
                }
            } catch (Exception e) {}
        }
    }
}
