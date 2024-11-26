// ClientGUI.java

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

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
    private JTextArea chatArea;
    private JTextField chatInput;
    private JButton b_send;

    // 소켓 관련 변수
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
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
        b_selectFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectFile();
            }
        });

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

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);

        JPanel chatInputPanel = new JPanel(new BorderLayout());
        chatInput = new JTextField();
        b_send = new JButton("전송");

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
        b_startGame.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
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
                } else {
                    JOptionPane.showMessageDialog(ClientGUI.this, "서버에 연결할 수 없습니다.");
                }
            }
        });

        // 전송 버튼 이벤트 (현재는 기능 없음)
        b_send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // 채팅 기능은 나중에 구현 예정
                chatInput.setText("");
            }
        });
    }

    private boolean connectToServer() {
        try {
            socket = new Socket("localhost", serverPort);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());

            // 사용자 이름을 서버로 전송
            out.writeUTF(userName);
            out.flush();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void selectFile() {
        JFileChooser fileChooser = new JFileChooser();
        // 데스크톱 디렉토리로 설정
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home"), "Desktop"));

        // 이미지 파일 필터 추가
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "이미지 파일", "jpg", "png", "gif", "jpeg");
        fileChooser.setFileFilter(filter);

        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            // 선택한 이미지를 이미지 라벨에 표시
            ImageIcon imageIcon = new ImageIcon(file.getAbsolutePath());
            Image image = imageIcon.getImage();
            Image scaledImage = image.getScaledInstance(500, 500, Image.SCALE_SMOOTH);
            imageIcon = new ImageIcon(scaledImage);
            imageLabel.setIcon(imageIcon);
        }
    }

    public static void main(String[] args) {
        new ClientGUI();
    }
}
