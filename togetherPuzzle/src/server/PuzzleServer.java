package server;

import common.ProtocolConstants;
import common.Utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.Base64;
import java.util.List;

public class PuzzleServer {
    private Vector<ClientHandler> clients;
    private JTextArea t_log;
    private byte[] puzzleImageData;
    private int puzzlePieceCount;
    private int rowCount, colCount;

    private String currentTurn = "user1";
    private boolean[] piecePlaced;
    private Map<Integer, Point> piecePositions;

    private static final int BOARD_SIZE = 500;
    private int actualWidth, actualHeight;

    private boolean puzzleStarted = false;
    private List<String> storedPuzzlePieces = new ArrayList<>(); // "index::base64Data" 형식으로 저장

    public PuzzleServer(Vector<ClientHandler> clients, JTextArea t_log) {
        this.clients = clients;
        this.t_log = t_log;
    }

    public synchronized void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            t_log.append(msg + "\n");
            t_log.setCaretPosition(t_log.getDocument().getLength());
        });
    }

    public synchronized void broadcast(String msg) {
        for (ClientHandler ch : clients) {
            ch.sendMessage(msg);
        }
    }

    public synchronized void removeClient(ClientHandler ch) {
        clients.remove(ch);
        broadcastUserList();
    }

    public synchronized void broadcastUserList() {
        StringBuilder userList = new StringBuilder(ProtocolConstants.USER_LIST);
        for (ClientHandler ch : clients) {
            userList.append(ch.getUserId()).append(",");
        }
        if (userList.length() > 11) {
            userList.setLength(userList.length() - 1);
        }
        broadcast(userList.toString());
    }

    public void sendCurrentUserList(ClientHandler ch) {
        StringBuilder userList = new StringBuilder(ProtocolConstants.USER_LIST);
        for (ClientHandler c : clients) {
            userList.append(c.getUserId()).append(",");
        }
        if (userList.length() > 11) {
            userList.setLength(userList.length() - 1);
        }
        ch.sendMessage(userList.toString());
    }

    public void sendTurnInfo() {
        broadcast(ProtocolConstants.TURN + currentTurn);
    }

    public void sendOriginalImage(ClientHandler ch) {
        if (puzzleImageData == null) return;
        String originalBase64 = Base64.getEncoder().encodeToString(puzzleImageData);
        if (ch == null) {
            broadcast(ProtocolConstants.SHOW_ORIGINAL_IMAGE + originalBase64);
        } else {
            ch.sendMessage(ProtocolConstants.SHOW_ORIGINAL_IMAGE + originalBase64);
        }
    }

    // PUZZLE_SETUP::pieceCount::actualWidth::actualHeight
    public void sendPuzzleSetup(ClientHandler ch) {
        if (puzzleImageData == null) return;
        String msg = ProtocolConstants.PUZZLE_SETUP + puzzlePieceCount + "::" + actualWidth + "::" + actualHeight;
        if (ch == null) {
            broadcast(msg);
        } else {
            ch.sendMessage(msg);
        }
    }

    private void initPuzzleState() {
        rowCount = (int)Math.sqrt(puzzlePieceCount);
        colCount = rowCount;
        piecePlaced = new boolean[puzzlePieceCount];
        piecePositions = new HashMap<>();
        List<Integer> indices = new ArrayList<>();
        for (int i=0; i<puzzlePieceCount; i++) indices.add(i);
        Collections.shuffle(indices);
        for (int i=0; i<puzzlePieceCount; i++) {
            piecePositions.put(indices.get(i), new Point(i,0));
        }
    }

    private boolean allPiecesPlaced() {
        for (boolean b : piecePlaced) {
            if (!b) return false;
        }
        return true;
    }

    private void nextTurn() {
        List<String> userIds = new ArrayList<>();
        for (ClientHandler ch : clients) {
            userIds.add(ch.getUserId());
        }
        if (userIds.isEmpty()) return;

        int idx = userIds.indexOf(currentTurn);
        if (idx == -1) {
            currentTurn = userIds.get(0);
            return;
        }
        idx = (idx + 1) % userIds.size();
        currentTurn = userIds.get(idx);
    }

    private void handleTryPiece(String userId, int pieceIndex, int row, int col) {
        if (!userId.equals(currentTurn)) {
            return;
        }

        int correctRow = pieceIndex / colCount;
        int correctCol = pieceIndex % colCount;
        if (correctRow == row && correctCol == col) {
            piecePlaced[pieceIndex] = true;
            broadcast("MOVE_PIECE::" + pieceIndex + "::" + row + "::" + col + "::success");
            if (!allPiecesPlaced()) {
                nextTurn();
                sendTurnInfo();
            } else {
                broadcast(ProtocolConstants.GAME_END);
            }
        } else {
            Point orig = piecePositions.get(pieceIndex);
            broadcast("MOVE_PIECE::" + pieceIndex + "::" + orig.x + "::" + orig.y + "::failure");
            broadcast("Server::틀렸습니다!");
            nextTurn();
            sendTurnInfo();
        }
    }

    private void generatePuzzlePieces() throws IOException {
        InputStream is = new ByteArrayInputStream(puzzleImageData);
        BufferedImage original = ImageIO.read(is);

        float wPerCol = (float)BOARD_SIZE / colCount;
        float hPerRow = (float)BOARD_SIZE / rowCount;
        int pieceWidth = Math.round(wPerCol);
        int pieceHeight = Math.round(hPerRow);
        actualWidth = pieceWidth * colCount;
        actualHeight = pieceHeight * rowCount;

        BufferedImage scaled = Utils.scaleImage(original, actualWidth, actualHeight);

        List<Integer> pieceOrder = new ArrayList<>();
        for (int i = 0; i < puzzlePieceCount; i++) {
            pieceOrder.add(i);
        }
        Collections.shuffle(pieceOrder);

        storedPuzzlePieces.clear();
        for (int index : pieceOrder) {
            int r = index / colCount;
            int c = index % colCount;
            BufferedImage piece = scaled.getSubimage(c * pieceWidth, r * pieceHeight, pieceWidth, pieceHeight);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(piece, "jpg", baos);
            baos.flush();
            String base64Data = Base64.getEncoder().encodeToString(baos.toByteArray());
            baos.close();
            storedPuzzlePieces.add(index + "::" + base64Data);
        }
    }

    private void broadcastPuzzlePieces() {
        for (String pieceData : storedPuzzlePieces) {
            broadcast(ProtocolConstants.PUZZLE_PIECE + pieceData);
        }
    }

    // 특정 클라이언트에게 현재 퍼즐 조각 재전송
    private void sendPuzzlePiecesToClient(ClientHandler ch) {
        for (String pieceData : storedPuzzlePieces) {
            ch.sendMessage(ProtocolConstants.PUZZLE_PIECE + pieceData);
        }
    }

    public synchronized void handleClientMessage(ClientHandler ch, String message) {
        if (message.startsWith(ProtocolConstants.PUZZLE_INFO)) {
            String[] parts = message.split("::", 3);
            if (parts.length == 3) {
                puzzlePieceCount = Integer.parseInt(parts[1]);
                puzzleImageData = Base64.getDecoder().decode(parts[2]);
                appendLog("퍼즐 이미지 및 조각 정보 수신");
                initPuzzleState();
            }
        } else if (message.equals(ProtocolConstants.START_PUZZLE)) {
            appendLog("퍼즐 시작 요청 수신");
            puzzleStarted = true;
            sendOriginalImage(null);
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    generatePuzzlePieces();
                    sendPuzzleSetup(null);
                    broadcastPuzzlePieces();
                    sendTurnInfo();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } else if (message.startsWith(ProtocolConstants.TRY_PIECE)) {
            String[] parts = message.split("::");
            if (parts.length == 4) {
                int pIndex = Integer.parseInt(parts[1]);
                int r = Integer.parseInt(parts[2]);
                int c = Integer.parseInt(parts[3]);
                handleTryPiece(ch.getUserId(), pIndex, r, c);
            }
        } else {
            broadcast(ch.getUserId() + "::" + message);
        }
    }

    // 퍼즐 시작 후 새로 접속한 클라이언트에게 현재 상태 전달
    public void sendCurrentPuzzleStateToClient(ClientHandler ch) {
        if (puzzleStarted && puzzleImageData != null) {
            sendOriginalImage(ch);
            sendPuzzleSetup(ch);
            sendPuzzlePiecesToClient(ch);
            sendTurnInfo();
        }
    }
}
