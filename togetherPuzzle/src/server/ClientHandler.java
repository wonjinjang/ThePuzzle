package server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader reader;
    BufferedWriter writer;
    String userId;

    private PuzzleServer puzzleServer;

    public ClientHandler(Socket socket, String userId, PuzzleServer puzzleServer) {
        this.socket = socket;
        this.userId = userId;
        this.puzzleServer = puzzleServer;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            // ID 먼저 할당 통지
            sendMessage("ASSIGN_ID::" + userId);
            puzzleServer.appendLog(userId + " 연결됨");
            puzzleServer.sendCurrentUserList(this);

            // 퍼즐이 이미 시작된 상태라면 현재 상태 전송
            puzzleServer.sendCurrentPuzzleStateToClient(this);

        } catch (IOException e) {
            puzzleServer.appendLog("클라이언트 연결 오류: " + e.getMessage());
        }
    }

    public void sendMessage(String msg) {
        try {
            writer.write(msg + "\n");
            writer.flush();
        } catch (IOException e) {
            puzzleServer.appendLog("메시지 전송 오류: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                puzzleServer.appendLog(userId + " 메시지: " + message);
                puzzleServer.handleClientMessage(this, message);
            }
        } catch (IOException e) {
            puzzleServer.appendLog(userId + " 통신 오류: " + e.getMessage());
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
            puzzleServer.appendLog("연결 종료 오류: " + e.getMessage());
        } finally {
            puzzleServer.removeClient(this);
            puzzleServer.appendLog(userId + " 연결 종료");
        }
    }

    public String getUserId() {
        return userId;
    }
}
