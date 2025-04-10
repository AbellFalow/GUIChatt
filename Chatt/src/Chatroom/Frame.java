package Chatroom;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Frame extends JFrame implements ActionListener, Runnable {

    JPanel panel = new JPanel();
    JButton topButton = new JButton("Connect");
    JTextField bottomTextField = new JTextField();
    JTextArea leftTextField = new JTextArea();
    JTextArea rightTextField = new JTextArea();
    JScrollPane leftScrollPane = new JScrollPane(leftTextField);
    JScrollPane rightScrollPane = new JScrollPane(rightTextField);

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private boolean done;


    Frame() {

        panel.setLayout(new BorderLayout());
        bottomTextField.setPreferredSize(new Dimension(200, 40));
        leftScrollPane.setPreferredSize(new Dimension(588, 40));
        rightScrollPane.setPreferredSize(new Dimension(196, 40));
        topButton.setPreferredSize(new Dimension(200, 40));

        add(panel);
        panel.add(topButton, BorderLayout.NORTH);
        topButton.addActionListener(this);
        panel.add(bottomTextField, BorderLayout.SOUTH);
        bottomTextField.addActionListener(this);
        panel.add(leftScrollPane, BorderLayout.WEST);



        panel.add(rightScrollPane, BorderLayout.EAST);

        setSize(800, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

    }

    public static void main(String[] args) {
        Frame frame = new Frame();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == topButton) {
            if (topButton.getText().equals("Connect")) {
                Thread clientThread = new Thread(this);
                clientThread.start();
                String name = JOptionPane.showInputDialog("What is your name?");
                out.println(name);
                topButton.setText("Disconnect");
            } else if (topButton.getText().equals("Disconnect")) {
                out.println("/quit");
                shutdown();
                topButton.setText("Connect");
                leftTextField.setText("");
                rightTextField.setText("");
            }
        }

        if (e.getSource() == bottomTextField) {
            String input = bottomTextField.getText();
            out.println(input);
            bottomTextField.setText("");
        }
    }

    @Override
    public void run() {
        try {
            client = new Socket("localhost", 9999);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            InputHandler inHandler = new InputHandler();
            Thread t = new Thread(inHandler);
            t.start();

            String inMessage;
            while ((inMessage = in.readLine()) != null) {
                if (inMessage.startsWith("/users:")) {
                    String userList = inMessage.substring(7);
                    rightTextField.setText(userList.replace(",", "\n"));
                } else {
                    leftTextField.append(inMessage + "\n");
                }
            }
        } catch (IOException e) {
            shutdown();
        }
    }


    public void shutdown() {
        done = true;
        try {
            in.close();
            out.close();
            if (!client.isClosed()) {
                client.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    class InputHandler implements Runnable {

        @Override
        public void run() {
            try {
                BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
                while (!done) {
                    String message = inReader.readLine();
                    if (message.equals("/quit")) {
                        out.println(message);
                        inReader.close();
                        shutdown();
                    } else {
                        out.println(message);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }
    }
}

