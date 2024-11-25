// ClientGUI.java

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ClientGUI extends JFrame {
    private JTextField t_pieceCount;
    private JButton b_selectFile;
    private JButton b_start;
    private JLabel imageLabel;
    private JTextArea chatArea;
    private JTextField chatInput;

    public ClientGUI() {
        super("TogetherPuzzle");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        buildGUI();
        setSize(900, 600);
        setVisible(true);
    }

    private void buildGUI() {
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

        add(topPanel, BorderLayout.NORTH);

        // 중앙 패널: 이미지와 채팅창
        JPanel centerPanel = new JPanel(new BorderLayout());

        // 이미지 표시를 위한 라벨
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);
        JScrollPane imageScrollPane = new JScrollPane(imageLabel);

        centerPanel.add(imageScrollPane, BorderLayout.CENTER);

        // 채팅창 패널
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatArea = new JTextArea(20, 20);
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);

        chatInput = new JTextField();
        chatInput.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // 현재는 입력한 메시지를 채팅 영역에 표시만 함
                String message = chatInput.getText();
                if (!message.isEmpty()) {
                    chatArea.append("나: " + message + "\n");
                    chatInput.setText("");
                }
            }
        });

        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        chatPanel.add(chatInput, BorderLayout.SOUTH);

        centerPanel.add(chatPanel, BorderLayout.EAST);

        add(centerPanel, BorderLayout.CENTER);

        // 하단 패널: 시작 버튼
        JPanel bottomPanel = new JPanel();
        b_start = new JButton("시작!");
        bottomPanel.add(b_start);
        add(bottomPanel, BorderLayout.SOUTH);
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
            // 이미지 크기 조정 (필요에 따라 조절 가능)
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