// ServerGUI.java

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Vector;

public class ServerGUI extends JFrame {
    private JButton b_start, b_stop;
    private JTextArea t_log;
    private ServerSocket serverSocket;
    private Vector<ClientHandler> clients = new Vector<>();
    private boolean isRunning = false;

    public ServerGUI() {
        super("TogetherPuzzle Server");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        buildGUI();
        setSize(400, 300);
        setVisible(true);
    }

    private void buildGUI() {
        t_log = new JTextArea();
        t_log.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(t_log);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        b_start = new JButton("서버 시작");
        b_stop = new JButton("서버 중지");
        b_stop.setEnabled(false);

        b_start.addActionListener(e -> startServer());
        b_stop.addActionListener(e -> stopServer());

        buttonPanel.add(b_start);
        buttonPanel.add(b_stop);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void startServer() {
        isRunning = true;
        b_start.setEnabled(false);
        b_stop.setEnabled(true);
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(12345);
                appendLog("서버가 시작되었습니다.");

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket);
                    clients.add(handler);
                    handler.start();
                }
            } catch (IOException e) {
                appendLog("서버 오류: " + e.getMessage());
            }
        }).start();
    }

    private void stopServer() {
        isRunning = false;
        b_start.setEnabled(true);
        b_stop.setEnabled(false);
        try {
            for (ClientHandler client : clients) {
                client.closeConnection();
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            appendLog("서버가 중지되었습니다.");
        } catch (IOException e) {
            appendLog("서버 중지 오류: " + e.getMessage());
        }
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            t_log.append(message + "\n");
            t_log.setCaretPosition(t_log.getDocument().getLength());
        });
    }

    private class ClientHandler extends Thread {
        private Socket socket;
        private DataOutputStream out;
        private DataInputStream in;
        //private String clientName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                out = new DataOutputStream(socket.getOutputStream());
                in = new DataInputStream(socket.getInputStream());
            } catch (IOException e) {
                appendLog("클라이언트 연결 오류: " + e.getMessage());
            }
        }

        private void broadcast(String message) {
            for (ClientHandler client : clients) {
                try {
                    client.out.writeUTF(message);
                    client.out.flush();
                } catch (IOException e) {
                    appendLog("메시지 전송 오류: " + e.getMessage());
                }
            }
        }

        @Override
        public void run() {
            try {
                String message;
                //clientName = in.readUTF();
                
                // 클라이언트로부터 메시지 수신
                while ((message = in.readUTF()) != null) {
                    appendLog("클라이언트 메시지: " + message);
                    broadcast(message); // 다른 클라이언트에 메시지 전송
                }
            } catch (IOException e) {
                appendLog("클라이언트 통신 오류: " + e.getMessage());
            } finally {
                closeConnection();
            }
        }

        public void closeConnection() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
                appendLog("클라이언트 연결이 종료되었습니다.");
                clients.remove(this); // 클라이언트 목록에서 제거
            } catch (IOException e) {
                appendLog("연결 종료 오류: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        new ServerGUI();
    }
}
