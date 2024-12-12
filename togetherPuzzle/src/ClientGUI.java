import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class ClientGUI extends JFrame {
    private JTextField t_pieceCount;
    private JButton b_selectFile;
    private JButton b_start;
    private JLabel imageLabel;

    private CardLayout cardLayout;
    private JPanel mainPanel;

    private JTextField t_name;
    private JTextField t_port;

    // 채팅 관련 컴포넌트
    private JPanel chatAreaPanel;
    private JPanel participantsPanel;
    private JTextField chatInput;
    private JButton b_send;

    // 소켓 관련 변수
    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;
    private String userName;
    private int serverPort;

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

        // 접속 화면 패널
        JPanel connectPanel = new JPanel(new BorderLayout());

        // 중앙에 이름 및 포트 번호 입력 필드 배치
        JPanel fieldsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JLabel nameLabel = new JLabel("이름: ");
        t_name = new JTextField(15);

        JLabel portLabel = new JLabel("포트 번호: ");
        t_port = new JTextField(5);
        t_port.setText("12345"); // 기본 포트 번호

        fieldsPanel.add(nameLabel);
        fieldsPanel.add(t_name);
        fieldsPanel.add(portLabel);
        fieldsPanel.add(t_port);

        // 하단에 '시작하기' 버튼 배치
        JButton b_startGame = new JButton("시작하기");

        connectPanel.add(fieldsPanel, BorderLayout.CENTER);
        connectPanel.add(b_startGame, BorderLayout.SOUTH);

        // 메인 화면 패널
        JPanel gamePanel = new JPanel(new BorderLayout());

        // 상단 패널: 조각 개수와 파일 선택 버튼
        JPanel topPanel = new JPanel(new BorderLayout());

        // 왼쪽: 조각 개수 입력
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel l_pieceCount = new JLabel("조각 개수: ");
        t_pieceCount = new JTextField(10);
        leftPanel.add(l_pieceCount);
        leftPanel.add(t_pieceCount);

        // 오른쪽: 파일 선택 버튼
        b_selectFile = new JButton("파일 선택");
        b_selectFile.addActionListener(e -> selectFile());

        topPanel.add(leftPanel, BorderLayout.WEST);
        topPanel.add(b_selectFile, BorderLayout.EAST);

        gamePanel.add(topPanel, BorderLayout.NORTH);

        // 중앙 패널: 이미지와 채팅창
        JPanel centerPanel = new JPanel(new BorderLayout());

        // 이미지 표시를 위한 패널
        JPanel imagePanel = new JPanel(new BorderLayout());
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);
        JScrollPane imageScrollPane = new JScrollPane(imageLabel);

        imagePanel.add(imageScrollPane, BorderLayout.CENTER);

        // 채팅창 패널
        JPanel chatPanel = new JPanel(new BorderLayout());

        chatAreaPanel = new JPanel();
        chatAreaPanel.setLayout(new BoxLayout(chatAreaPanel, BoxLayout.Y_AXIS));
        JScrollPane chatScrollPane = new JScrollPane(chatAreaPanel);

        JPanel chatInputPanel = new JPanel(new BorderLayout());
        chatInput = new JTextField();// 채팅 입력 필드

        //메시지전송버튼 디자인
        b_send = new JButton("↑");
        b_send.setFont(new Font("Arial", Font.BOLD, 20)); 
        b_send.setPreferredSize(new Dimension(60, 30)); 
        b_send.setFocusPainted(false); 
        b_send.setBorder(BorderFactory.createLineBorder(new Color(220, 130, 150), 2)); 
        b_send.setBackground(new Color(255, 182, 193)); // 연한 핑크색 배경
        b_send.setForeground(Color.WHITE); // 텍스트 색상
        b_send.setOpaque(true); // 배경색 적용 가능
        b_send.setContentAreaFilled(true); // 버튼 내부 영역 유지
        
        // 상단 패널: 참가자 목록
        participantsPanel = new JPanel();
        participantsPanel.setLayout(new FlowLayout(FlowLayout.LEFT)); // 참가자 이미지를 왼쪽 정렬
        participantsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        participantsPanel.setPreferredSize(new Dimension(800, 70)); // 폭 800px, 높이 70px
        participantsPanel.setBackground(new Color(255, 220, 230)); // 파스텔 핑크 배경색 설정

        chatPanel.add(participantsPanel, BorderLayout.NORTH);

        // 채팅 입력 필드와 전송 버튼을 한 줄에 배치
        chatInputPanel.add(chatInput, BorderLayout.CENTER);
        chatInputPanel.add(b_send, BorderLayout.EAST);

        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);

        // 이미지 패널과 채팅 패널을 분할하여 배치
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, imagePanel, chatPanel);
        splitPane.setDividerLocation(600);

        centerPanel.add(splitPane, BorderLayout.CENTER);

        gamePanel.add(centerPanel, BorderLayout.CENTER);

        // 하단 패널: '시작!' 버튼
        JPanel bottomPanel = new JPanel();
        b_start = new JButton("시작!");
        bottomPanel.add(b_start);
        gamePanel.add(bottomPanel, BorderLayout.SOUTH);

        // 메인 패널에 두 개의 카드 추가
        mainPanel.add(connectPanel, "connect");
        mainPanel.add(gamePanel, "game");

        add(mainPanel);

        // '시작하기' 버튼 이벤트 처리
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

        // 전송 버튼 이벤트
        b_send.addActionListener(e -> sendMessage());

        // Enter 키로 메시지 전송
        chatInput.addActionListener(e -> sendMessage());
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
    // 메시지 전송
    private void sendMessage() {
        String message = chatInput.getText().trim();
        if (message.isEmpty()) return;

        try {
            writer.write(userName + ":" + message + "\n");
            writer.flush();
            chatInput.setText(""); // 입력 필드 초기화
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "메시지 전송 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 메시지 수신 스레드
    private void startChatReceiver() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String message;
                while ((message = reader.readLine()) != null) {
                    if (message.startsWith("USER_LIST::")) {
                        // 사용자 목록 수신
                        String[] users = message.substring("USER_LIST::".length()).split(",");
                        SwingUtilities.invokeLater(() -> updateParticipants(users));
                    } else {
                        // 일반 메시지 처리
                        String[] parts = message.split("::", 2);
                        if (parts.length == 2) {
                            String sender = parts[0];
                            String msg = parts[1];
                            SwingUtilities.invokeLater(() -> addMessageToChat(sender, msg));
                        }
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "서버와의 연결이 끊어졌습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

     // 참가자 목록 갱신
    private void updateParticipants(String[] users) {
        participantsPanel.removeAll(); // 기존 참가자 목록 초기화

        for (String user : users) {
            ImageIcon userIcon = loadUserImage(user);
            JLabel profileLabel = new JLabel(new ImageIcon(userIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH)));

            JPanel participantPanel = new JPanel(new BorderLayout());
            participantPanel.setToolTipText(user); // 사용자 이름 툴팁
            participantPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            participantPanel.add(profileLabel, BorderLayout.CENTER);

            //JLabel nameLabel = new JLabel(user);
            //participantPanel.add(nameLabel, BorderLayout.SOUTH);

            participantsPanel.add(participantPanel);
        }

        participantsPanel.revalidate();
        participantsPanel.repaint();
    }

    //채팅 메시지를 화면에 표시
    private void addMessageToChat(String sender, String message) {
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout());
        messagePanel.setBackground(Color.WHITE);

// 프로필 이미지를 위한 패널
    JPanel profilePanel = new JPanel();
    profilePanel.setLayout(new BorderLayout());
    profilePanel.setBackground(Color.WHITE);

    // 프로필 이미지
    ImageIcon icon = loadUserImage(sender);
    JLabel profileLabel = new JLabel(new ImageIcon(icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH)));
    profilePanel.add(profileLabel, BorderLayout.CENTER); // 중앙 정렬

    // 메시지 텍스트
    JLabel messageLabel = new JLabel("<html><b>" + message + "</html>");
    messageLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

    // 메시지 전송 시간 텍스트
    String time = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date());
    JLabel timeLabel = new JLabel(time);
    timeLabel.setFont(new Font("Arial", Font.ITALIC, 10)); // 작은 글씨체로 표시
    timeLabel.setHorizontalAlignment(SwingConstants.RIGHT); // 오른쪽 정렬
    timeLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 2, 10));
    timeLabel.setOpaque(true); // 배경색 적용 가능하도록 설정
    timeLabel.setBackground(chatAreaPanel.getBackground()); // 채팅 패널의 배경색과 동일하게 설정



    // 왼쪽 패널 (프로필 이미지와 메시지)
    JPanel leftPanel = new JPanel();
    leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
    leftPanel.setBackground(Color.WHITE);
    leftPanel.add(profileLabel);
    leftPanel.add(Box.createHorizontalStrut(5)); // 프로필과 메시지 간 간격
    leftPanel.add(messageLabel);

    // 메시지 텍스트 크기 계산
    Dimension textSize = messageLabel.getPreferredSize();
    int panelWidth = profileLabel.getPreferredSize().width + 5 + textSize.width; // 프로필 + 간격 + 텍스트 너비
    int panelHeight = Math.max(profileLabel.getPreferredSize().height, textSize.height); // 프로필 높이와 텍스트 높이 중 큰 값

    // LEFTPANEL 크기 동적으로 설정
    leftPanel.setMaximumSize(new Dimension(panelWidth, panelHeight));
    leftPanel.setPreferredSize(new Dimension(panelWidth, panelHeight));
    leftPanel.setMinimumSize(new Dimension(panelWidth, panelHeight));

    // 메시지 패널에 요소 배치
    messagePanel.add(leftPanel, BorderLayout.CENTER); // 왼쪽에 메시지
    messagePanel.add(timeLabel, BorderLayout.SOUTH);  // 오른쪽 하단에 시간

    // 일정한 높이 유지 (간격 일정하게 유지)
    messagePanel.setMaximumSize(new Dimension(chatAreaPanel.getWidth(), 60));
    messagePanel.setPreferredSize(new Dimension(chatAreaPanel.getWidth(), 60));
    messagePanel.setMinimumSize(new Dimension(chatAreaPanel.getWidth(), 60));

    // 메시지 패널 크기 동적으로 조정
    messagePanel.setMaximumSize(new Dimension(panelWidth + 20, panelHeight + 10)); // 패딩 추가
    messagePanel.setPreferredSize(new Dimension(panelWidth + 20, panelHeight + 10));
    messagePanel.setMinimumSize(new Dimension(panelWidth + 20, panelHeight + 10));

    // 메시지 패널 왼쪽 정렬 설정
    messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    // 메시지 패널 추가 및 간격 일정 유지
    chatAreaPanel.add(messagePanel);
    chatAreaPanel.add(Box.createVerticalStrut(5)); // 일정한 간격 유지
    chatAreaPanel.revalidate();
    chatAreaPanel.repaint();

    // 자동 스크롤
    JScrollPane scrollPane = (JScrollPane) chatAreaPanel.getParent().getParent();
    SwingUtilities.invokeLater(() -> {
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setValue(verticalScrollBar.getMaximum());
    });
}

    
    // 이미지 로드
    private ImageIcon loadUserImage(String userName) {
        String imagePath = userName + ".jpeg"; // 사용자 이름에 기반한 이미지 파일명
        ImageIcon icon;

        try {
            icon = new ImageIcon(imagePath);
            if (icon.getIconWidth() <= 0) { // 이미지 로드 실패 확인
                throw new IOException("이미지 로드 실패: " + imagePath);
            }
        } catch (Exception e) {
            System.err.println("이미지를 로드할 수 없습니다: " + imagePath);
            // 기본 이미지 사용
            imagePath = "default.jpeg";
            icon = new ImageIcon(imagePath);
        }
        return icon;
    }
    
    //파일 선택
    private void selectFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home"), "Desktop"));

        FileNameExtensionFilter filter = new FileNameExtensionFilter("이미지 파일", "jpg", "png", "gif", "jpeg");
        fileChooser.setFileFilter(filter);

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            ImageIcon imageIcon = new ImageIcon(file.getAbsolutePath());
            Image image = imageIcon.getImage().getScaledInstance(500, 500, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(image));
        }
    }

    public static void main(String[] args) {
        new ClientGUI();
    }
}

