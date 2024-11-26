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
        // 로그 표시 영역
        t_log = new JTextArea();
        t_log.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(t_log);
        add(scrollPane, BorderLayout.CENTER);

        // 버튼 패널
        JPanel buttonPanel = new JPanel();
        b_start = new JButton("서버 시작");
        b_stop = new JButton("서버 중지");
        b_stop.setEnabled(false);

        b_start.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startServer();
            }
        });

        b_stop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopServer();
            }
        });

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
                serverSocket = new ServerSocket(12345); // 포트 번호는 필요에 따라 변경 가능
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
        private DataInputStream in;
        private DataOutputStream out;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                appendLog("클라이언트 연결 오류: " + e.getMessage());
            }
        }

        public void run() {
            try {
                // 클라이언트로부터 이름 받기
                clientName = in.readUTF();
                appendLog(clientName + "님이 연결되었습니다.");

                // 이후 클라이언트와의 통신 처리 (현재는 없음)

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
                clients.remove(this);
            } catch (IOException e) {
                appendLog("연결 종료 오류: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        new ServerGUI();
    }
}
