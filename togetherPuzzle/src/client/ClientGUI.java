package client;

import common.ProtocolConstants;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.Base64;

public class ClientGUI extends JFrame {
    private JTextField t_pieceCount;
    private JButton b_selectFile;
    private JButton b_start;
    private JLabel imageLabel;

    private CardLayout cardLayout;
    private JPanel mainPanel;

    private JTextField t_name;
    private JTextField t_port;

    private JPanel chatAreaPanel;
    private JPanel participantsPanel;
    private JTextField chatInput;
    private JButton b_send;

    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;
    private String userName;
    private int serverPort;

    private String currentTurn = null;
    private String myUserId = null;

    private byte[] selectedImageData = null;
    private ImageIcon originalIcon = null;

    private int pieceCount;
    private int rowCount, colCount;
    private boolean puzzleSetupDone = false;
    private JPanel topBoardPanel;
    private JPanel bottomPiecesPanel;
    private JPanel puzzleContainerPanel;
    private JPanel imagePanel;

    private ImageIcon[] receivedPiecesArray;
    private List<PieceLabel> pieceLabels = new ArrayList<>();

    private int actualWidth, actualHeight;

    private class PieceLabel extends JLabel {
        int pieceIndex;
        Point originalPos;
        boolean dragging = false;
        int dragOffsetX, dragOffsetY;

        public PieceLabel(ImageIcon icon, int pieceIndex) {
            super(icon);
            this.pieceIndex = pieceIndex;

            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (currentTurn == null || myUserId == null || !myUserId.equals(currentTurn)) return;
                    dragging = true;
                    dragOffsetX = e.getX();
                    dragOffsetY = e.getY();
                    getParent().setComponentZOrder(PieceLabel.this,0);
                }
                public void mouseReleased(MouseEvent e) {
                    if (!dragging) return;
                    dragging = false;
                    Point p = SwingUtilities.convertPoint(PieceLabel.this, e.getPoint(), topBoardPanel);
                    Rectangle rect = topBoardPanel.getBounds();
                    if (rect.contains(p)) {
                        int cellWidth = rect.width / colCount;
                        int cellHeight = rect.height / rowCount;
                        int relX = p.x;
                        int relY = p.y;
                        int r = relY / cellHeight;
                        int c = relX / cellWidth;
                        sendMessageToServer("TRY_PIECE::"+pieceIndex+"::"+r+"::"+c);
                    } else {
                        setLocation(originalPos);
                    }
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    if (!dragging) return;
                    int x = getX() + e.getX() - dragOffsetX;
                    int y = getY() + e.getY() - dragOffsetY;
                    setLocation(x,y);
                }
            });
        }
    }

    public ClientGUI() {
        super("TogetherPuzzle");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        buildGUI();
        setSize(900, 600);
        setVisible(true);
    }

    private void buildGUI() {
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        JPanel connectPanel = new JPanel(new BorderLayout());
        JPanel fieldsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JLabel nameLabel = new JLabel("이름: ");
        t_name = new JTextField(15);

        JLabel portLabel = new JLabel("포트 번호: ");
        t_port = new JTextField(5);
        t_port.setText("12345");

        fieldsPanel.add(nameLabel);
        fieldsPanel.add(t_name);
        fieldsPanel.add(portLabel);
        fieldsPanel.add(t_port);

        JButton b_startGame = new JButton("시작하기");
        connectPanel.add(fieldsPanel, BorderLayout.CENTER);
        connectPanel.add(b_startGame, BorderLayout.SOUTH);

        JPanel gamePanel = new JPanel(new BorderLayout());
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel l_pieceCount = new JLabel("조각 개수: ");
        t_pieceCount = new JTextField(10);
        leftPanel.add(l_pieceCount);
        leftPanel.add(t_pieceCount);

        b_selectFile = new JButton("파일 선택");
        b_selectFile.addActionListener(e -> selectFile());

        topPanel.add(leftPanel, BorderLayout.WEST);
        topPanel.add(b_selectFile, BorderLayout.EAST);
        gamePanel.add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        imagePanel = new JPanel(new BorderLayout());
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);
        JScrollPane imageScrollPane = new JScrollPane(imageLabel);
        imagePanel.add(imageScrollPane, BorderLayout.CENTER);

        JPanel chatPanel = new JPanel(new BorderLayout());
        chatAreaPanel = new JPanel();
        chatAreaPanel.setLayout(new BoxLayout(chatAreaPanel, BoxLayout.Y_AXIS));
        JScrollPane chatScrollPane = new JScrollPane(chatAreaPanel);
        JPanel chatInputPanel = new JPanel(new BorderLayout());
        chatInput = new JTextField();

        b_send = new JButton("↑");
        b_send.setFont(new Font("Arial", Font.BOLD, 20));
        b_send.setPreferredSize(new Dimension(60, 30));
        b_send.setFocusPainted(false);
        b_send.setBorder(BorderFactory.createLineBorder(new Color(220, 130, 150), 2));
        b_send.setBackground(new Color(255, 182, 193));
        b_send.setForeground(Color.WHITE);
        b_send.setOpaque(true);
        b_send.setContentAreaFilled(true);

        participantsPanel = new JPanel();
        participantsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        participantsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        participantsPanel.setPreferredSize(new Dimension(800, 70));
        participantsPanel.setBackground(new Color(255, 220, 230));

        chatPanel.add(participantsPanel, BorderLayout.NORTH);
        chatInputPanel.add(chatInput, BorderLayout.CENTER);
        chatInputPanel.add(b_send, BorderLayout.EAST);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, imagePanel, chatPanel);
        splitPane.setDividerLocation(600);
        centerPanel.add(splitPane, BorderLayout.CENTER);
        gamePanel.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        b_start = new JButton("시작!");
        bottomPanel.add(b_start);
        gamePanel.add(bottomPanel, BorderLayout.SOUTH);

        mainPanel.add(connectPanel, "connect");
        mainPanel.add(gamePanel, "game");
        add(mainPanel);

        b_startGame.addActionListener(e -> {
            userName = t_name.getText().trim();
            String portText = t_port.getText().trim();
            if (userName.isEmpty()) {
                JOptionPane.showMessageDialog(ClientGUI.this, "이름을 입력하세요.");
                return;
            }
            if (portText.isEmpty()) {
                JOptionPane.showMessageDialog(ClientGUI.this, "포트 번호를 입력하세요.");
                return;
            }
            try {
                serverPort = Integer.parseInt(portText);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(ClientGUI.this, "유효한 포트 번호를 입력하세요.");
                return;
            }

            if (connectToServer()) {
                cardLayout.show(mainPanel, "game");
                startChatReceiver();
            } else {
                JOptionPane.showMessageDialog(ClientGUI.this, "서버에 연결할 수 없습니다.");
            }
        });

        b_send.addActionListener(e -> sendMessage());
        chatInput.addActionListener(e -> sendMessage());

        b_start.addActionListener(e -> {
            if (selectedImageData == null) {
                JOptionPane.showMessageDialog(this, "이미지를 선택하세요.");
                return;
            }
            String pieceCountText = t_pieceCount.getText().trim();
            if (pieceCountText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "조각 개수를 입력하세요.");
                return;
            }
            int tempCount;
            try {
                tempCount = Integer.parseInt(pieceCountText);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "유효한 조각 개수를 입력하세요.", "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int sqrtVal = (int)Math.sqrt(tempCount);
            if (sqrtVal * sqrtVal != tempCount) {
                JOptionPane.showMessageDialog(this, "조각 개수는 완전제곱수여야 합니다. 예: 4, 9, 16 ...", "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            pieceCount = tempCount;
            sendPuzzleInfo(pieceCount);
            sendMessageToServer("START_PUZZLE");
        });
    }

    private boolean connectToServer() {
        try {
            socket = new Socket("localhost", serverPort);
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer.write(userName + "님이 참가하였습니다.\n");
            writer.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void sendMessage() {
        String message = chatInput.getText().trim();
        if (message.isEmpty()) return;
        try {
            writer.write(userName + ":" + message + "\n");
            writer.flush();
            chatInput.setText("");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "메시지 전송 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendMessageToServer(String msg) {
        try {
            writer.write(msg + "\n");
            writer.flush();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "서버 전송 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startChatReceiver() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    final String message = line;
                    handleMessage(message);
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "서버와의 연결이 끊어졌습니다.", "오류", JOptionPane.ERROR_MESSAGE));
            }
        });
    }

    private void handleMessage(String message) {
        if (message.startsWith(ProtocolConstants.USER_LIST)) {
            String[] users = message.substring(ProtocolConstants.USER_LIST.length()).split(",");
            updateParticipants(users);
        } else if (message.startsWith("ASSIGN_ID::")) {
            myUserId = message.substring("ASSIGN_ID::".length());
        } else if (message.startsWith(ProtocolConstants.SHOW_ORIGINAL_IMAGE)) {
            String base64Data = message.substring(ProtocolConstants.SHOW_ORIGINAL_IMAGE.length());
            byte[] imageData = Base64.getDecoder().decode(base64Data);
            showOriginalImage(imageData);
        } else if (message.startsWith(ProtocolConstants.PUZZLE_PIECE)) {
            String pieceData = message.substring(ProtocolConstants.PUZZLE_PIECE.length());
            String[] pParts = pieceData.split("::", 2);
            int idx = Integer.parseInt(pParts[0]);
            String base64Data = pParts[1];
            byte[] imageData = Base64.getDecoder().decode(base64Data);
            storePuzzlePiece(idx, imageData);
            checkAndSetupPieces();
        } else if (message.startsWith(ProtocolConstants.PUZZLE_SETUP)) {
            String[] parts = message.split("::");
            pieceCount = Integer.parseInt(parts[1]);
            actualWidth = Integer.parseInt(parts[2]);
            actualHeight = Integer.parseInt(parts[3]);
            handlePuzzleSetup();
            checkAndSetupPieces();
        } else if (message.startsWith(ProtocolConstants.TURN)) {
            String u = message.substring(ProtocolConstants.TURN.length());
            handleTurnMessage(u);
        } else if (message.startsWith(ProtocolConstants.MOVE_PIECE)) {
            String[] p = message.split("::");
            handleMovePiece(p);
        } else if (message.equals(ProtocolConstants.GAME_END)) {
            handleGameEnd();
        } else {
            String[] parts = message.split("::", 2);
            if (parts.length == 2) {
                String sender = parts[0];
                String msg = parts[1];
                addMessageToChat(sender, msg);
            }
        }
    }

    private void checkAndSetupPieces() {
        if (puzzleSetupDone && receivedPiecesArray != null) {
            boolean allReceived = true;
            for (ImageIcon icon : receivedPiecesArray) {
                if (icon == null) {
                    allReceived = false;
                    break;
                }
            }
            if (allReceived) {
                setupPiecesOnBottomPanel();
            }
        }
    }

    private void sendPuzzleInfo(int pieceCount) {
        if (selectedImageData == null) return;
        String base64Image = Base64.getEncoder().encodeToString(selectedImageData);
        sendMessageToServer(ProtocolConstants.PUZZLE_INFO + pieceCount + "::" + base64Image);
    }

    private void showOriginalImage(byte[] imageData) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
            BufferedImage original = ImageIO.read(bais);
            originalIcon = new ImageIcon(original.getScaledInstance(500, 500, Image.SCALE_SMOOTH));
            imageLabel.setIcon(originalIcon);
            imagePanel.revalidate();
            imagePanel.repaint();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void storePuzzlePiece(int index, byte[] imageData) {
        try {
            if (receivedPiecesArray == null) {
                receivedPiecesArray = new ImageIcon[pieceCount];
            }
            ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
            BufferedImage piece = ImageIO.read(bais);
            receivedPiecesArray[index] = new ImageIcon(piece);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handlePuzzleSetup() {
        rowCount = (int)Math.sqrt(pieceCount);
        colCount = rowCount;

        pieceLabels.clear();

        puzzleContainerPanel = new JPanel();
        puzzleContainerPanel.setLayout(new BorderLayout());

        topBoardPanel = new JPanel(new GridLayout(rowCount, colCount, 0, 0));
        topBoardPanel.setPreferredSize(new Dimension(actualWidth, actualHeight));

        for (int i=0; i<pieceCount; i++) {
            JLabel placeholder = new JLabel();
            placeholder.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            topBoardPanel.add(placeholder);
        }

        bottomPiecesPanel = new JPanel(null);
        bottomPiecesPanel.setPreferredSize(new Dimension(600,200));

        JScrollPane bottomScroll = new JScrollPane(bottomPiecesPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topBoardPanel, bottomScroll);
        verticalSplit.setDividerLocation(300);
        puzzleContainerPanel.add(verticalSplit, BorderLayout.CENTER);

        imagePanel.removeAll();
        imagePanel.add(puzzleContainerPanel, BorderLayout.CENTER);
        imagePanel.revalidate();
        imagePanel.repaint();
        puzzleSetupDone = true;
    }

    // 같은 행에 퍼즐 조각을 무작위 순서로 일렬 배치
    private void setupPiecesOnBottomPanel() {
        bottomPiecesPanel.removeAll();
        pieceLabels.clear();

        int pieceWidth = actualWidth / colCount;
        int pieceHeight = actualHeight / rowCount;

        // 클라이언트에서도 무작위 순서로 섞어서 원본 그림 유추 어렵게 하기
        List<ImageIcon> pieceList = new ArrayList<>(Arrays.asList(receivedPiecesArray));
        Collections.shuffle(pieceList);

        // 가로로 일렬로 배치
        int gap = 10; // 조각 사이 간격
        int totalWidth = pieceList.size()*(pieceWidth+gap) + gap;
        int panelWidth = Math.max(totalWidth, 600);
        bottomPiecesPanel.setPreferredSize(new Dimension(panelWidth, 200));

        int x = gap;
        int y = (200 - pieceHeight)/2; // 수직 중앙정렬
        for (int i=0; i<pieceList.size(); i++) {
            ImageIcon icon = pieceList.get(i);
            // pieceIndex를 알기 위해 original array에서 인덱스 찾기
            int originalIndex = Arrays.asList(receivedPiecesArray).indexOf(icon);
            PieceLabel lbl = new PieceLabel(icon, originalIndex);
            lbl.originalPos = new Point(x, y);
            lbl.setBounds(x, y, pieceWidth, pieceHeight);
            pieceLabels.add(lbl);
            bottomPiecesPanel.add(lbl);
            x += pieceWidth + gap;
        }

        bottomPiecesPanel.revalidate();
        bottomPiecesPanel.repaint();
    }

    private void handleMovePiece(String[] parts) {
        int pIndex = Integer.parseInt(parts[1]);
        int r = Integer.parseInt(parts[2]);
        int c = Integer.parseInt(parts[3]);
        String result = parts[4];

        PieceLabel lbl = null;
        for (PieceLabel pl : pieceLabels) {
            if (pl.pieceIndex == pIndex) {
                lbl = pl;
                break;
            }
        }
        if (lbl == null) return;

        if ("success".equals(result)) {
            Component comp = topBoardPanel.getComponent(r*colCount+c);
            if (comp instanceof JLabel) {
                JLabel slot = (JLabel)comp;
                slot.setIcon(lbl.getIcon());
            }
            bottomPiecesPanel.remove(lbl);
            pieceLabels.remove(lbl);
            bottomPiecesPanel.revalidate();
            bottomPiecesPanel.repaint();
        } else {
            lbl.setLocation(lbl.originalPos);
        }
    }

    private void handleGameEnd() {
        JOptionPane.showMessageDialog(this, "게임 종료! 모든 조각이 맞게 배치되었습니다.");
    }

    private void handleTurnMessage(String user) {
        currentTurn = user;
        updateParticipantsTurn();
    }

    private void updateParticipantsTurn() {
        for (Component comp : participantsPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel p = (JPanel)comp;
                String uid = p.getToolTipText();
                p.removeAll();
                ImageIcon userIcon = loadUserImage(uid);
                JLabel profileLabel = new JLabel(new ImageIcon(userIcon.getImage().getScaledInstance(40,40,Image.SCALE_SMOOTH)));
                p.add(profileLabel, BorderLayout.CENTER);
                if (uid.equals(currentTurn)) {
                    JLabel turnLabel = new JLabel("TURN");
                    turnLabel.setForeground(Color.RED);
                    p.add(turnLabel, BorderLayout.EAST);
                }
            }
        }

        participantsPanel.revalidate();
        participantsPanel.repaint();
    }

    private void updateParticipants(String[] users) {
        participantsPanel.removeAll();
        for (String user : users) {
            ImageIcon userIcon = loadUserImage(user);
            JLabel profileLabel = new JLabel(new ImageIcon(userIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH)));
            JPanel participantPanel = new JPanel(new BorderLayout());
            participantPanel.setToolTipText(user);
            participantPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            participantPanel.add(profileLabel, BorderLayout.CENTER);
            participantsPanel.add(participantPanel);
        }
        participantsPanel.revalidate();
        participantsPanel.repaint();
        updateParticipantsTurn();
    }

    private void addMessageToChat(String sender, String message) {
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBackground(Color.WHITE);

        JPanel profilePanel = new JPanel(new BorderLayout());
        profilePanel.setBackground(Color.WHITE);

        ImageIcon icon = loadUserImage(sender);
        JLabel profileLabel = new JLabel(new ImageIcon(icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH)));
        profilePanel.add(profileLabel, BorderLayout.CENTER);

        JLabel messageLabel = new JLabel("<html><b>" + message + "</html>");
        messageLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        String time = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date());
        JLabel timeLabel = new JLabel(time);
        timeLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        timeLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 2, 10));
        timeLabel.setOpaque(true);
        timeLabel.setBackground(chatAreaPanel.getBackground());

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
        leftPanel.setBackground(Color.WHITE);
        leftPanel.add(profileLabel);
        leftPanel.add(Box.createHorizontalStrut(5));
        leftPanel.add(messageLabel);

        Dimension textSize = messageLabel.getPreferredSize();
        int panelWidth = profileLabel.getPreferredSize().width + 5 + textSize.width;
        int panelHeight = Math.max(profileLabel.getPreferredSize().height, textSize.height);

        leftPanel.setMaximumSize(new Dimension(panelWidth, panelHeight));
        leftPanel.setPreferredSize(new Dimension(panelWidth, panelHeight));
        leftPanel.setMinimumSize(new Dimension(panelWidth, panelHeight));

        messagePanel.add(leftPanel, BorderLayout.CENTER);
        messagePanel.add(timeLabel, BorderLayout.SOUTH);

        messagePanel.setMaximumSize(new Dimension(chatAreaPanel.getWidth(), 60));
        messagePanel.setPreferredSize(new Dimension(chatAreaPanel.getWidth(), 60));
        messagePanel.setMinimumSize(new Dimension(chatAreaPanel.getWidth(), 60));

        messagePanel.setMaximumSize(new Dimension(panelWidth + 20, panelHeight + 10));
        messagePanel.setPreferredSize(new Dimension(panelWidth + 20, panelHeight + 10));
        messagePanel.setMinimumSize(new Dimension(panelWidth + 20, panelHeight + 10));

        messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        chatAreaPanel.add(messagePanel);
        chatAreaPanel.add(Box.createVerticalStrut(5));
        chatAreaPanel.revalidate();
        chatAreaPanel.repaint();

        JScrollPane scrollPane = (JScrollPane) chatAreaPanel.getParent().getParent();
        SwingUtilities.invokeLater(() -> {
            JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
            verticalScrollBar.setValue(verticalScrollBar.getMaximum());
        });
    }

    private ImageIcon loadUserImage(String userName) {
        String imagePath = userName + ".jpeg";
        ImageIcon icon;
        try {
            icon = new ImageIcon(imagePath);
            if (icon.getIconWidth() <= 0) {
                throw new IOException("이미지 로드 실패: " + imagePath);
            }
        } catch (Exception e) {
            imagePath = "default.jpeg";
            icon = new ImageIcon(imagePath);
        }
        return icon;
    }

    private void selectFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home"), "Desktop"));

        FileNameExtensionFilter filter = new FileNameExtensionFilter("이미지 파일", "jpg", "png", "gif", "jpeg");
        fileChooser.setFileFilter(filter);

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                selectedImageData = loadFileAsBytes(file);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "이미지 로드 실패: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }
            ImageIcon imageIcon = new ImageIcon(selectedImageData);
            Image image = imageIcon.getImage().getScaledInstance(500, 500, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(image));
        }
    }

    private byte[] loadFileAsBytes(File file) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try(FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int read;
            while((read = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
        }
        return baos.toByteArray();
    }

    public static void main(String[] args) {
        new ClientGUI();
    }
}
