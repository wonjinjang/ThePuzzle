import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
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
                    String userId;
    
                    // 클라이언트 연결 순서에 따라 user1, user2, user3, user4 할당
                    int clientNumber = clients.size() + 1;
                    switch (clientNumber) {
                        case 1:
                            userId = "user1";
                            break;
                        case 2:
                            userId = "user2";
                            break;
                        case 3:
                            userId = "user3";
                            break;
                        case 4:
                            userId = "user4";
                            break;
                        default:
                            userId = "unknown"; // 4명을 초과할 경우 처리
                            break;
                    }
    
                    ClientHandler handler = new ClientHandler(clientSocket, userId);
                    clients.add(handler); // 클라이언트를 목록에 추가

                    // 클라이언트 핸들러 시작
                    handler.start();

                    // 모든 클라이언트에게 사용자 목록 전송
                    broadcastUserList();
                }
            } catch (IOException e) {
                if (isRunning) {
                    appendLog("서버 오류: " + e.getMessage());
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

    private void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                try {
                    client.writer.write(message + "\n");
                    client.writer.flush();
                } catch (IOException e) {
                    appendLog("메시지 전송 오류: " + e.getMessage());
                }
            }
        }
    }

    private void broadcastUserList() {
        synchronized (clients) {
            StringBuilder userList = new StringBuilder("USER_LIST::");
            for (ClientHandler client : clients) {
                userList.append(client.userId).append(",");
            }
            // 마지막 콤마 제거
            if (userList.length() > 11) {
                userList.setLength(userList.length() - 1);
            }
            broadcast(userList.toString());
        }
    }

    private class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader reader;
        private BufferedWriter writer;
        private String userId;

        public ClientHandler(Socket socket, String userId) {
            this.socket = socket;
            this.userId = userId;
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                appendLog(userId + " 연결됨");

                // 새로운 클라이언트에게 현재 사용자 목록 전송
                sendCurrentUserList();
            } catch (IOException e) {
                appendLog("클라이언트 연결 오류: " + e.getMessage());
            }
        }

        private void sendCurrentUserList() {
            synchronized (clients) {
                StringBuilder userList = new StringBuilder("USER_LIST::");
                for (ClientHandler client : clients) {
                    userList.append(client.userId).append(",");
                }
                // 마지막 콤마 제거
                if (userList.length() > 11) {
                    userList.setLength(userList.length() - 1);
                }
                try {
                    writer.write(userList.toString() + "\n");
                    writer.flush();
                } catch (IOException e) {
                    appendLog("사용자 목록 전송 오류: " + e.getMessage());
                }
            }
        }

        @Override
        public void run() {
            try {
                String message;
                while ((message = reader.readLine()) != null) {
                    appendLog(userId + " 메시지: " + message);
                    broadcast(String.format("%s::%s", userId, message));
                }
            } catch (IOException e) {
                appendLog(userId + " 통신 오류: " + e.getMessage());
            } finally {
                closeConnection();
            }
        }

        public void closeConnection() {
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                appendLog("연결 종료 오류: " + e.getMessage());
            } finally {
                synchronized (clients) {
                    clients.remove(this);
                    broadcastUserList(); // 클라이언트 연결 종료 시 사용자 목록 업데이트
                }
                appendLog(userId + " 연결 종료");
            }
        }
    }

    public static void main(String[] args) {
        new ServerGUI(); 
    }    
}
