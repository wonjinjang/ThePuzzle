package server;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class ServerGUI extends JFrame {
    private JButton b_start, b_stop;
    private JTextArea t_log;
    private ServerSocket serverSocket;
    private Vector<ClientHandler> clients = new Vector<>();
    private boolean isRunning = false;

    private PuzzleServer puzzleServer;

    public ServerGUI() {
        super("TogetherPuzzle Server");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        buildGUI();
        setSize(400, 300);
        setVisible(true);

        puzzleServer = new PuzzleServer(clients, t_log);
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
                puzzleServer.appendLog("서버가 시작되었습니다.");

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    String userId;

                    int clientNumber = clients.size() + 1;
                    switch (clientNumber) {
                        case 1: userId = "user1"; break;
                        case 2: userId = "user2"; break;
                        case 3: userId = "user3"; break;
                        case 4: userId = "user4"; break;
                        default: userId = "unknown"; break;
                    }

                    ClientHandler handler = new ClientHandler(clientSocket, userId, puzzleServer);
                    clients.add(handler);
                    handler.start();
                    puzzleServer.broadcastUserList();
                }
            } catch (IOException e) {
                if (isRunning) {
                    puzzleServer.appendLog("서버 오류: " + e.getMessage());
                }
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
            puzzleServer.appendLog("서버가 중지되었습니다.");
        } catch (IOException e) {
            puzzleServer.appendLog("서버 중지 오류: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new ServerGUI();
    }
}
