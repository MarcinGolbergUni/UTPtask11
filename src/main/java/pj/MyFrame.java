package pj;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import grpc.circles.CircleDTO;
import grpc.circles.CircleResponse;
import grpc.circles.CircleServiceGrpc;
import grpc.circles.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public
    class MyFrame
    extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(
            () -> new MyFrame()
        );
    }

    private int[] circles;
    private int circlesCount;
    private JPanel drawPanel;
    private JLabel statusLabel;

    private static final String REST_URL = "http://localhost:8080/api/circles";

    public MyFrame() {
        super("Circle Viewer - REST Client");
        
        this.circles = new int[1000];
        this.circlesCount = 0;

        drawPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );
                
                for (int i = 0; i < circlesCount; i++) {
                    int[] decoded = PositionAndColor.decode(circles[i]);
                    Color color = PositionAndColor.byteToColor(decoded[2]);
                    
                    g2d.setColor(color);
                    g2d.fillOval(decoded[0] - 5, decoded[1] - 5, 10, 10);
                    
                    g2d.setColor(color.darker());
                    g2d.drawOval(decoded[0] - 5, decoded[1] - 5, 10, 10);
                }
                
                g2d.setColor(Color.DARK_GRAY);
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                g2d.drawString("Circles count: " + circlesCount, 10, 20);
            }
            
            private void drawGrid(Graphics2D g2d) {
                g2d.setColor(new Color(240, 240, 240));
                for (int x = 0; x < getWidth(); x += 50) {
                    g2d.drawLine(x, 0, x, getHeight());
                }
                for (int y = 0; y < getHeight(); y += 50) {
                    g2d.drawLine(0, y, getWidth(), y);
                }
            }
        };
        
        drawPanel.setBackground(Color.WHITE);

        drawPanel.addMouseListener(
            new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (circlesCount < circles.length) {
                        circles[circlesCount++] = PositionAndColor.encode(
                            e.getX(),
                            e.getY(),
                            new Color(
                                (int) (Math.random() * 255),
                                (int) (Math.random() * 255),
                                (int) (Math.random() * 255)
                            )
                        );
                        drawPanel.repaint();
                        updateStatus("Circle added. Total: " + circlesCount);
                    }
                }
            }
        );

        JPanel actionPanel = new JPanel(
            new FlowLayout(FlowLayout.CENTER, 10, 5)
        );
        actionPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JButton loadFileButton = new JButton("Load (File)");
        JButton saveFileButton = new JButton("Save (File)");
        JButton loadRestButton = new JButton("Load (REST)");
        JButton loadDbButton = new JButton("Load (DB)");
        JButton clearButton = new JButton("Clear");
        JButton loadGrpcButton = new JButton("Load (gRPC)");
        
        loadRestButton.setBackground(
            new Color(70, 130, 180)
        );
        loadRestButton.setForeground(Color.WHITE);
        loadRestButton.setFocusPainted(false);

        loadDbButton.setBackground(
                new Color(70, 180, 130)
        );
        loadDbButton.setForeground(Color.WHITE);
        loadDbButton.setFocusPainted(false);

        loadGrpcButton.setBackground(
            new Color(100, 50, 150)
        );
        loadGrpcButton.setForeground(Color.WHITE);
        loadGrpcButton.setFocusPainted(false);

        actionPanel.add(loadFileButton);
        actionPanel.add(saveFileButton);
        actionPanel.add(
            new JSeparator(SwingConstants.VERTICAL)
        );
        actionPanel.add(loadRestButton);
        actionPanel.add(
            new JSeparator(SwingConstants.VERTICAL)
        );
        actionPanel.add(loadDbButton);
        actionPanel.add(loadGrpcButton); // NOWY
        actionPanel.add(clearButton);

        statusLabel = new JLabel("READY - click to add circles");
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        setLayout(new BorderLayout());
        add(actionPanel, BorderLayout.NORTH);
        add(drawPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        setSize(600, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        loadFileButton.addActionListener(
            e -> loadFromFile()
        );

        saveFileButton.addActionListener(
            e -> saveToFile()
        );

        loadRestButton.addActionListener(
            e -> loadFromRest()
        );

        loadDbButton.addActionListener(
            e -> loadFromDB()
        );

        loadGrpcButton.addActionListener(
            e -> loadFromGrpc()
        );

        clearButton.addActionListener(
            e -> {
                circles = new int[1000];
                circlesCount = 0;
                drawPanel.repaint();
                updateStatus("Cleared");
            }
        );
    }

    private void loadFromFile() {
        try (FileChannel channel = FileChannel.open(
//TODO: setup path to local project file
                Paths.get("circles.bin"),
                StandardOpenOption.READ
        )) {
            long fileSize = channel.size();
            ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);

            channel.read(buffer);
            buffer.flip();
            
            circlesCount = buffer.getInt();
            circles = new int[Math.max(circlesCount, 1000)];
            
            for (int i = 0; i < circlesCount; i++) {
                circles[i] = buffer.getInt();
            }
            
            drawPanel.repaint();
            updateStatus("File loaded: " + circlesCount + " cicles");
            
        } catch (IOException ex) {
            showError("IOException: " + ex.getMessage());
        }
    }

    private void saveToFile() {
        try (FileChannel channel = FileChannel.open(
//TODO: define path to local project file
                Paths.get("circles.bin"),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
        )) {
            int bufferSize = Integer.BYTES * (1 + circlesCount);
            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

            buffer.putInt(circlesCount);

            for (int i = 0; i < circlesCount; i++) {
                buffer.putInt(circles[i]);
            }
            
            buffer.flip();
            channel.write(buffer);
            
            updateStatus("Loaded: " + circlesCount + " circles");
            
        } catch (IOException ex) {
            showError("IOException: " + ex.getMessage());
        }
    }

    private void updateStatus(String message) {
        statusLabel.setText(message + " | " +
                java.time.LocalTime.now().toString().substring(0, 8));
    }

    private void showError(String message) {
        updateStatus("Error: " + message);
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

//================================

    private static int[] parseJsonIntArray(String json) {
        String content = json.trim();

        if (content.startsWith("[")) {
            content = content.substring(1);
        }
        if (content.endsWith("]")) {
            content = content.substring(0, content.length() - 1);
        }

        content = content.trim();
        if (content.isEmpty()) {
            return new int[0];
        }

        String[] parts = content.split(",");
        int[] result = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i].trim());
        }

        return result;
    }

    private int[] fetchCirclesFromRest() throws Exception {
        URL url = new URL(REST_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();
        
        if (responseCode != 200) {
            throw new RuntimeException("HTTP Error: " + responseCode);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        return parseJsonIntArray(response.toString());
    }

    private void loadFromRest() {
        updateStatus("Downloading...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        Thread.startVirtualThread(
            () -> {
                try {
                    int[] loadedCircles = fetchCirclesFromRest();

                    SwingUtilities.invokeLater(
                        () -> {
                            circlesCount = loadedCircles.length;
                            circles = new int[Math.max(circlesCount, 1000)];
                            System.arraycopy(loadedCircles, 0, circles, 0, circlesCount);
                            drawPanel.repaint();
                            setCursor(Cursor.getDefaultCursor());
                            updateStatus("REST: " + circlesCount + " circles");
                        }
                    );
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(
                        () -> {
                            setCursor(Cursor.getDefaultCursor());
                            showError("Exception REST: " + ex.getMessage());
                            ex.printStackTrace();
                        }
                    );
                }
            }
        );
    }

    private void loadFromDB() {
        updateStatus("DB data downloading...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        Thread.startVirtualThread(
                () -> {
//TODO: implement reading data from db as it is specified in tutorial materials
                    try {
                        String sql = "SELECT x, y, r, g, b FROM circles";
                        java.util.List<Integer> loadedCircles = new java.util.ArrayList<>();

                        try (
                                java.sql.Connection conn = java.sql.DriverManager.getConnection(
                                    "jdbc:h2:D:/Documents/LinkedInProjects/UTPTask11/dbres"
                                );
                                java.sql.PreparedStatement ps = conn.prepareStatement(sql);
                                java.sql.ResultSet rs = ps.executeQuery()
                        ) {
                            while (rs.next()) {
                                int x = rs.getInt("x");
                                int y = rs.getInt("y");
                                int r = rs.getInt("r");
                                int g = rs.getInt("g");
                                int b = rs.getInt("b");

                                // Odtworzenie zakodowanej wartości
                                Color color = new Color(r, g, b);
                                int encoded = PositionAndColor.encode(x, y, color);
                                loadedCircles.add(encoded);
                            }
                        }

                        // Konwersja List<Integer> na int[]
                        int[] circlesArray = loadedCircles.stream()
                                .mapToInt(Integer::intValue)
                                .toArray();

                        SwingUtilities.invokeLater(
                                () -> {
                                    circlesCount = circlesArray.length;
                                    circles = new int[Math.max(circlesCount, 1000)];
                                    System.arraycopy(circlesArray, 0, circles, 0, circlesCount);
                                    drawPanel.repaint();
                                    setCursor(Cursor.getDefaultCursor());
                                    updateStatus("DB: " + circlesCount + " circles loaded");
                                }
                        );

                    } catch (java.sql.SQLException ex) {
                        SwingUtilities.invokeLater(
                            () -> {
                                setCursor(Cursor.getDefaultCursor());
                                showError("SQLException: " + ex.getMessage());
                                ex.printStackTrace();
                            }
                        );
                    }
                }
        );
    }

    private void loadFromGrpc() {
        updateStatus("gRPC connecting...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        Thread.startVirtualThread(
            () -> {
                ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
                try {
                    CircleResponse response = CircleServiceGrpc.newBlockingStub(channel).getCircles(Empty.newBuilder().build());

                    ArrayList<Integer> tempList = new ArrayList<>();

                    for (CircleDTO circle : response.getCirclesList()) {
                        int x = circle.getX();
                        int y = circle.getY();
                        int r = circle.getR();
                        int g = circle.getG();
                        int b = circle.getB();

                        Color color = new Color(r, g, b);
                        int encoded = PositionAndColor.encode(x, y, color);

                        tempList.add(encoded);
                    }
                    int[] finalData = tempList.stream().mapToInt(i -> i).toArray();

                    SwingUtilities.invokeLater(
                        () -> {
                            circlesCount = finalData.length;
                            circles = new int[Math.max(circlesCount, 1000)];
                            System.arraycopy(finalData, 0, circles, 0, circlesCount);
                            drawPanel.repaint();
                            setCursor(Cursor.getDefaultCursor());
                            updateStatus("gRPC: Loaded " + circlesCount + " items");
                        }
                    );

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(
                        () -> {
                            setCursor(Cursor.getDefaultCursor());
                            showError("gRPC Error: " + ex.getMessage());
                            ex.printStackTrace();
                        }
                    );
                } finally {
                    if (channel != null) {
                        channel.shutdown();
                    }
                }
            }
        );
    }
}