/**
 * Grafiskt användargränssnitt för klienten i Digi-Whack-projektet.
 * Klassen hanterar navigation mellan startsida, Digi-Whack och Simon Says,
 * samt kommunikation med servern via socket.
 *
 * Klienten kan starta spel, visa resultat, hämta leaderboard och skicka highscores.
 *
 * @author Tom Olofsson
 * @author Saif Atia
 */

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class GameClientGUI extends JFrame {

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    private CardLayout cardLayout;
    private JPanel mainContainer;

    private JLabel digiScoreLabel;
    private JLabel digiAvgTimeLabel;
    private JLabel digiBestTimeLabel;
    private JTextArea digiLeaderboardArea;

    private boolean hardcoreMode = false;
    private JButton hardcoreButton;

    private JLabel simonScoreLabel;
    private JTextArea simonLeaderboardArea;

    private final ArrayList<String[]> leaderboard = new ArrayList<>();
    private final ArrayList<String[]> simonLeaderboard = new ArrayList<>();

    private final Color DARK_PURPLE = new Color(28, 5, 45);
    private final Color DEEP_BLUE = new Color(8, 8, 45);
    private final Color HOT_PINK = new Color(255, 40, 170);
    private final Color GOLD = new Color(255, 205, 55);
    private final Color ORANGE = new Color(255, 120, 20);
    private final Color LIGHT_TEXT = new Color(255, 245, 255);
    private final Color CARD_BG = new Color(22, 10, 55);

    /**
     * Skapar klientfönstret, ansluter till servern och initierar alla GUI-paneler.
     * CardLayout används för att kunna växla mellan meny, Digi-Whack och Simon Says.
     *
     * @author Tom Olofsson
     */
    public GameClientGUI() {
        setTitle("Digi-Whack");
        setSize(950, 560);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        connectToServer();

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        mainContainer.add(createMenuPanel(), "MENU");
        mainContainer.add(createDigiWhackPanel(), "DIGI");
        mainContainer.add(createSimonPanel(), "SIMON");

        setContentPane(mainContainer);

        cardLayout.show(mainContainer, "MENU");
    }

    /**
     * Skapar startsidan där användaren kan välja mellan Digi-Whack och Simon Says.
     *
     * @return panelen som representerar startsidan
     *
     * @author Tom Olofsson
     */
    private JPanel createMenuPanel() {
        BackgroundPanel root = new BackgroundPanel();
        root.setLayout(new BorderLayout(20, 20));
        root.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JLabel titleLabel = new JLabel("DIGI-WHACK", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial Black", Font.BOLD, 42));
        titleLabel.setForeground(GOLD);

        JLabel subtitleLabel = new JLabel("Välj vilket spel du vill spela", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        subtitleLabel.setForeground(new Color(255, 190, 240));

        ImageIcon originalIcon = new ImageIcon(getClass().getResource("/digiwhackloga.png"));

        Image scaledImage = originalIcon.getImage().getScaledInstance(
                600,
                -1,
                Image.SCALE_SMOOTH
        );

        ImageIcon scaledIcon = new ImageIcon(scaledImage);

        JLabel logoLabel = new JLabel(scaledIcon);
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        titlePanel.add(logoLabel);
        titlePanel.add(Box.createVerticalStrut(100));

        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(5));

        titlePanel.add(subtitleLabel);

        GameButton digiButton = new GameButton("DIGI-WHACK");
        digiButton.setPreferredSize(new Dimension(280, 75));
        digiButton.addActionListener(e -> {
            cardLayout.show(mainContainer, "DIGI");
            if (out != null && in != null) {
                loadDigiLeaderboard();
            }
        });

        GameButton simonButton = new GameButton("SIMON SAYS");
        simonButton.setPreferredSize(new Dimension(280, 75));
        simonButton.addActionListener(e -> {
            cardLayout.show(mainContainer, "SIMON");
            if (out != null && in != null) {
                loadSimonLeaderboard();
            }
        });

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 20, 20));
        buttonPanel.setOpaque(false);
        buttonPanel.add(digiButton);
        buttonPanel.add(simonButton);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.add(buttonPanel);

        root.add(titlePanel, BorderLayout.NORTH);
        root.add(centerWrapper, BorderLayout.CENTER);

        return root;
    }

    /**
     * Skapar GUI-panelen för Digi-Whack.
     * Panelen innehåller play-knapp, back-knapp, resultatfält och leaderboard.
     *
     * @return panelen för Digi-Whack
     *
     * @author Tom Olofsson
     * @author Saif Atia - lagt till hardcore mode toggle
     */
    private JPanel createDigiWhackPanel() {
        BackgroundPanel root = new BackgroundPanel();
        root.setLayout(new BorderLayout(20, 20));
        root.setBorder(BorderFactory.createEmptyBorder(20, 25, 25, 25));

        JLabel titleLabel = new JLabel("DIGI-WHACK", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial Black", Font.BOLD, 36));
        titleLabel.setForeground(GOLD);

        JLabel subtitleLabel = new JLabel("Tryck PLAY för att testa din reaktionsförmåga", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        subtitleLabel.setForeground(new Color(255, 190, 240));

        GameButton playButton = new GameButton("PLAY");
        playButton.setPreferredSize(new Dimension(230, 70));
        playButton.addActionListener(e -> playDigiWhack());

        hardcoreButton = new JButton("HARDCORE: OFF");
        hardcoreButton.setFont(new Font("Arial Black", Font.BOLD, 14));
        hardcoreButton.setForeground(Color.WHITE);
        hardcoreButton.setFocusPainted(false);
        hardcoreButton.setBorder(BorderFactory.createLineBorder(GOLD, 2));
        hardcoreButton.setPreferredSize(new Dimension(170, 45));
        hardcoreButton.setOpaque(true);
        hardcoreButton.setContentAreaFilled(true);

        updateHardcoreButton();

        hardcoreButton.addActionListener(e -> toggleHardcoreMode());

        JButton backButton = createBackButton();
        backButton.addActionListener(e -> cardLayout.show(mainContainer, "MENU"));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.add(backButton);
        buttonPanel.add(playButton);
        buttonPanel.add(hardcoreButton);

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(subtitleLabel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        root.add(topPanel, BorderLayout.NORTH);

        NeonPanel resultPanel = new NeonPanel("LATEST RESULT");
        resultPanel.setLayout(new GridLayout(4, 1, 10, 10));

        JLabel resultTitle = createBigLabel("YOUR ROUND");
        digiScoreLabel = createStatLabel("SCORE: -");
        digiAvgTimeLabel = createStatLabel("AVG TIME: -");
        digiBestTimeLabel = createStatLabel("BEST TIME: -");

        resultPanel.add(resultTitle);
        resultPanel.add(digiScoreLabel);
        resultPanel.add(digiAvgTimeLabel);
        resultPanel.add(digiBestTimeLabel);

        NeonPanel leaderboardPanel = new NeonPanel("LEADERBOARD");
        leaderboardPanel.setLayout(new BorderLayout());

        digiLeaderboardArea = createLeaderboardArea();
        digiLeaderboardArea.setText("Leaderboard loading...");
        leaderboardPanel.add(digiLeaderboardArea, BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 25, 25));
        centerPanel.setOpaque(false);
        centerPanel.add(resultPanel);
        centerPanel.add(leaderboardPanel);

        root.add(centerPanel, BorderLayout.CENTER);

        return root;
    }

    /**
     * Skapar GUI-panelen för Simon Says.
     * Panelen innehåller play-knapp, back-knapp, resultatfält och leaderboard.
     *
     * @return panelen för Simon Says
     *
     * @author Tom Olofsson
     */
    private JPanel createSimonPanel() {
        BackgroundPanel root = new BackgroundPanel();
        root.setLayout(new BorderLayout(20, 20));
        root.setBorder(BorderFactory.createEmptyBorder(20, 25, 25, 25));

        JLabel titleLabel = new JLabel("SIMON SAYS", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial Black", Font.BOLD, 36));
        titleLabel.setForeground(GOLD);

        JLabel subtitleLabel = new JLabel("Kom ihåg sekvensen och försök slå ditt rekord", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        subtitleLabel.setForeground(new Color(255, 190, 240));

        GameButton playButton = new GameButton("PLAY");
        playButton.setPreferredSize(new Dimension(230, 70));
        playButton.addActionListener(e -> playSimonSays());

        JButton backButton = createBackButton();
        backButton.addActionListener(e -> cardLayout.show(mainContainer, "MENU"));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.add(backButton);
        buttonPanel.add(playButton);

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(subtitleLabel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        root.add(topPanel, BorderLayout.NORTH);

        NeonPanel resultPanel = new NeonPanel("LATEST RESULT");
        resultPanel.setLayout(new GridLayout(3, 1, 10, 10));

        JLabel resultTitle = createBigLabel("YOUR ROUND");
        simonScoreLabel = createStatLabel("SCORE: -");

        resultPanel.add(resultTitle);
        resultPanel.add(simonScoreLabel);

        NeonPanel leaderboardPanel = new NeonPanel("LEADERBOARD");
        leaderboardPanel.setLayout(new BorderLayout());

        simonLeaderboardArea = createLeaderboardArea();

        leaderboardPanel.add(simonLeaderboardArea, BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 25, 25));
        centerPanel.setOpaque(false);
        centerPanel.add(resultPanel);
        centerPanel.add(leaderboardPanel);

        root.add(centerPanel, BorderLayout.CENTER);

        return root;
    }

    /**
     * Skapar en tillbaka-knapp som används för att gå tillbaka till startsidan.
     *
     * @return en stylad JButton för navigation tillbaka till menyn
     *
     * @author Tom Olofsson
     */
    private JButton createBackButton() {
        JButton button = new JButton("← BACK");
        button.setFont(new Font("Arial Black", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(80, 40, 120));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(GOLD, 2));
        button.setPreferredSize(new Dimension(120, 45));
        return button;
    }

    /**
     * Skapar ett textfält som används för att visa leaderboard-information.
     *
     * @return en JTextArea anpassad för leaderboard-visning
     *
     * @author Tom Olofsson
     */
    private JTextArea createLeaderboardArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setOpaque(false);
        area.setForeground(LIGHT_TEXT);
        area.setFont(new Font("Monospaced", Font.BOLD, 15));
        area.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        return area;
    }

    /**
     * Skapar en större rubriklabel som används i resultatpanelerna.
     *
     * @param text texten som ska visas
     * @return en formaterad JLabel
     *
     * @author Tom Olofsson
     */
    private JLabel createBigLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(GOLD);
        label.setFont(new Font("Arial Black", Font.BOLD, 24));
        return label;
    }

    /**
     * Skapar en label för resultatdata, exempelvis score eller reaktionstid.
     *
     * @param text texten som ska visas
     * @return en formaterad JLabel
     *
     * @author Tom Olofsson
     */
    private JLabel createStatLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(LIGHT_TEXT);
        label.setFont(new Font("Arial", Font.BOLD, 22));
        return label;
    }

    /**
     * Försöker ansluta klienten till servern via socket på localhost och port 5000.
     * Om anslutningen lyckas skapas input- och outputströmmar för kommunikationen.
     *
     * @author Tom Olofsson
     */
    private void connectToServer() {
        try {
            socket = new Socket("localhost", 5000);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
        } catch (Exception e) {
            System.out.println("Kunde inte ansluta till servern.");
        }
    }

    /**
     * Startar en Digi-Whack-runda genom att skicka kommando 1 till servern.
     * Läser sedan statuskod och resultat från servern.
     * Om spelaren kvalar in på leaderboarden visas en popup där namn kan anges.
     *
     * @author Tom Olofsson
     */
    private void playDigiWhack() {

        if (out == null || in == null) {
            JOptionPane.showMessageDialog(this, "Ingen anslutning till servern.");
            return;
        }

        try {
            out.writeInt(1);
            out.writeBoolean(hardcoreMode);
            out.flush();

            int status = in.readInt();

            if (status == 0) {
                String errorMessage = in.readUTF();
                JOptionPane.showMessageDialog(this, errorMessage);
                return;
            }

            int averageTime = in.readInt();
            int score = in.readInt();
            int bestTime = in.readInt();

            updateDigiResult(score, averageTime, bestTime);

            loadDigiLeaderboard();

            if (qualifiesForDigiLeaderboard(score, averageTime)) {
                String name = JOptionPane.showInputDialog(
                        this,
                        "Jackpot! Du kvalade in på leaderboarden.\nSkriv ditt namn:"
                );

                if (name != null && !name.trim().isEmpty()) {
                    sendDigiHighscore(name.trim(), score, averageTime);
                    loadDigiLeaderboard();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Metod som togglar hardcore mode i DigiWhack
     * Växlar läget mellan på eller av i klienten.
     * Uppdaterar även knappens text och färg beroende på valt läge.
     *
     * @author Saif Atia
     */

    private void toggleHardcoreMode() {
        hardcoreMode = !hardcoreMode;

        if (hardcoreMode) {
            hardcoreButton.setText("HARDCORE: ON");
            hardcoreButton.setBackground(new Color(150, 0, 0));
        } else {
            hardcoreButton.setText("HARDCORE: OFF");
            hardcoreButton.setBackground(new Color(80, 40, 120));
        }
    }

    /**
     * Uppdaterar hardcore-knappens text och färg beroende på valt läge.
     *
     * @author Saif Atia
     */
    private void updateHardcoreButton() {
        if (hardcoreMode) {
            hardcoreButton.setText("HARDCORE: ON");
            hardcoreButton.setBackground(new Color(150, 0, 0)); // läskig röd
        } else {
            hardcoreButton.setText("HARDCORE: OFF");
            hardcoreButton.setBackground(new Color(80, 40, 120)); // som back-knappen
        }

        hardcoreButton.repaint();
    }

    /**
     * Startar en Simon Says-runda genom att skicka kommando 4 till servern.
     * Läser sedan statuskod och score från servern.
     * Om spelaren kvalar in på Simon Says leaderboard visas en popup där namn kan anges.
     *
     * @author Tom Olofsson
     */
    private void playSimonSays() {
        if (out == null || in == null) {
            JOptionPane.showMessageDialog(this, "Ingen anslutning till servern.");
            return;
        }

        try {
            out.writeInt(4);
            out.flush();

            int status = in.readInt();

            if (status == 0) {
                String errorMessage = in.readUTF();
                JOptionPane.showMessageDialog(this, errorMessage);
                return;
            }

            int score = in.readInt();

            updateSimonResult(score);

            loadSimonLeaderboard();

            if (qualifiesForSimonLeaderboard(score)) {
                String name = JOptionPane.showInputDialog(
                        this,
                        "Du kvalade in på Simon Says leaderboarden!\nSkriv ditt namn:"
                );

                if (name != null && !name.trim().isEmpty()) {
                    sendSimonHighscore(name.trim(), score);
                    loadSimonLeaderboard();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Uppdaterar resultatfältet för Digi-Whack i GUI:t.
     *
     * @param score spelarens poäng
     * @param averageTime spelarens genomsnittliga reaktionstid
     * @param bestTime spelarens bästa reaktionstid
     *
     * @author Tom Olofsson
     */
    private void updateDigiResult(int score, int averageTime, int bestTime) {
        digiScoreLabel.setText("SCORE: " + score);
        digiAvgTimeLabel.setText("AVG TIME: " + averageTime + " ms");
        digiBestTimeLabel.setText("BEST TIME: " + bestTime + " ms");
    }

    /**
     * Uppdaterar resultatfältet för Simon Says i GUI:t.
     *
     * @param score spelarens poäng
     *
     * @author Tom Olofsson
     */
    private void updateSimonResult(int score) {
        simonScoreLabel.setText("SCORE: " + score);
    }

    /**
     * Hämtar Simon Says leaderboard från servern.
     * Klienten skickar kommando 5 och läser sedan antal poster,
     * namn och score för varje leaderboard-entry.
     *
     * @author Tom Olofsson
     */
    private void loadSimonLeaderboard() {
        try {
            out.writeInt(5);
            out.flush();

            int numberOfEntries = in.readInt();

            simonLeaderboard.clear();

            for (int i = 0; i < numberOfEntries; i++) {
                String name = in.readUTF();
                int score = in.readInt();

                simonLeaderboard.add(new String[]{
                        name,
                        String.valueOf(score)
                });
            }

            updateSimonLeaderboardArea();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Hämtar Digi-Whack leaderboard från servern.
     * Klienten skickar kommando 2 och läser sedan antal poster,
     * namn, score och average time för varje leaderboard-entry.
     *
     * @author Tom Olofsson
     */
    private void loadDigiLeaderboard() {
        try {
            out.writeInt(2);
            out.flush();

            int numberOfEntries = in.readInt();
            leaderboard.clear();

            for (int i = 0; i < numberOfEntries; i++) {
                String name = in.readUTF();
                int score = in.readInt();
                int averageTime = in.readInt();

                leaderboard.add(new String[]{
                        name,
                        String.valueOf(score),
                        String.valueOf(averageTime)
                });
            }

            updateDigiLeaderboardArea();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Uppdaterar textfältet som visar Digi-Whack leaderboard.
     *
     * @author Tom Olofsson
     */
    private void updateDigiLeaderboardArea() {
        StringBuilder text = new StringBuilder();

        text.append(" RANK   PLAYER       SCORE   AVG\n");
        text.append("====================================\n");

        for (int i = 0; i < leaderboard.size(); i++) {
            String[] entry = leaderboard.get(i);

            text.append(String.format(
                    " #%02d   %-10s   %5s   %s ms\n",
                    i + 1,
                    entry[0],
                    entry[1],
                    entry[2]
            ));
        }

        if (leaderboard.isEmpty()) {
            text.append("\n No high scores yet.");
        }

        digiLeaderboardArea.setText(text.toString());
    }

    /**
     * Uppdaterar textfältet som visar Simon Says leaderboard.
     *
     * @author Tom Olofsson
     */
    private void updateSimonLeaderboardArea() {
        StringBuilder text = new StringBuilder();

        text.append(" RANK   PLAYER       SCORE\n");
        text.append("============================\n");

        for (int i = 0; i < simonLeaderboard.size(); i++) {
            String[] entry = simonLeaderboard.get(i);

            text.append(String.format(
                    " #%02d   %-10s   %5s\n",
                    i + 1,
                    entry[0],
                    entry[1]
            ));
        }

        if (simonLeaderboard.isEmpty()) {
            text.append("\n No high scores yet.");
        }

        simonLeaderboardArea.setText(text.toString());
    }

    /**
     * Kontrollerar om ett Digi-Whack-resultat kvalar in på leaderboarden.
     * Resultat jämförs först efter score och vid lika score efter lägst average time.
     *
     * @param score spelarens poäng
     * @param averageTime spelarens genomsnittliga reaktionstid
     * @return true om resultatet kvalar in, annars false
     *
     * @author Tom Olofsson
     */
    private boolean qualifiesForDigiLeaderboard(int score, int averageTime) {
        if (leaderboard.size() < 10) {
            return true;
        }

        String[] lastEntry = leaderboard.get(leaderboard.size() - 1);

        int lowestTopScore = Integer.parseInt(lastEntry[1]);
        int slowestTopAverage = Integer.parseInt(lastEntry[2]);

        if (score > lowestTopScore) {
            return true;
        }

        return score == lowestTopScore && averageTime < slowestTopAverage;
    }

    /**
     * Kontrollerar om ett Simon Says-resultat kvalar in på leaderboarden.
     * Resultat jämförs endast efter score.
     *
     * @param score spelarens poäng
     * @return true om resultatet kvalar in, annars false
     *
     * @author Tom Olofsson
     */
    private boolean qualifiesForSimonLeaderboard(int score) {
        if (simonLeaderboard.size() < 10) {
            return true;
        }

        String[] lastEntry = simonLeaderboard.get(simonLeaderboard.size() - 1);
        int lowestTopScore = Integer.parseInt(lastEntry[1]);

        return score > lowestTopScore;
    }

    /**
     * Skickar ett nytt Digi-Whack highscore till servern.
     *
     * @param name spelarens namn
     * @param score spelarens poäng
     * @param averageTime spelarens genomsnittliga reaktionstid
     *
     * @author Tom Olofsson
     */
    private void sendDigiHighscore(String name, int score, int averageTime) {
        try {
            out.writeInt(3);
            out.writeUTF(name);
            out.writeInt(score);
            out.writeInt(averageTime);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Skickar ett nytt Simon Says highscore till servern.
     *
     * @param name spelarens namn
     * @param score spelarens poäng
     *
     * @author Tom Olofsson
     */
    private void sendSimonHighscore(String name, int score) {
        try {
            out.writeInt(6);
            out.writeUTF(name);
            out.writeInt(score);
            out.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Anpassad bakgrundspanel som ritar en gradientbakgrund och ljuseffekter.
     *
     * @author Tom Olofsson
     */
    private class BackgroundPanel extends JPanel {
        /**
         * Ritar bakgrunden för panelen med gradient och dekorativa cirklar.
         *
         * @param g grafikobjektet som används för att rita komponenten
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            GradientPaint gradient = new GradientPaint(
                    0, 0, DARK_PURPLE,
                    getWidth(), getHeight(), DEEP_BLUE
            );

            g2.setPaint(gradient);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(255, 40, 170, 35));
            g2.fillOval(-100, -80, 300, 300);

            g2.setColor(new Color(255, 205, 55, 25));
            g2.fillOval(getWidth() - 220, getHeight() - 180, 300, 300);
        }
    }

    /**
     * Anpassad panel med neonliknande ram och rundade hörn.
     * Används för resultat- och leaderboard-paneler.
     *
     * @author Tom Olofsson
     */
    private class NeonPanel extends JPanel {
        private final String title;

        /**
         * Skapar en neonpanel med angiven titel.
         *
         * @param title titeln som visas högst upp på panelen
         */
        public NeonPanel(String title) {
            this.title = title;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(45, 20, 20, 20));
        }

        /**
         * Ritar neonpanelens bakgrund, ram och titel.
         *
         * @param g grafikobjektet som används för att rita komponenten
         */
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int arc = 35;

            g2.setColor(new Color(255, 40, 170, 70));
            g2.setStroke(new BasicStroke(8));
            g2.drawRoundRect(5, 5, getWidth() - 10, getHeight() - 10, arc, arc);

            g2.setColor(CARD_BG);
            g2.fillRoundRect(8, 8, getWidth() - 16, getHeight() - 16, arc, arc);

            g2.setColor(GOLD);
            g2.setStroke(new BasicStroke(3));
            g2.drawRoundRect(8, 8, getWidth() - 16, getHeight() - 16, arc, arc);

            g2.setFont(new Font("Arial Black", Font.BOLD, 18));
            g2.setColor(GOLD);
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(title);
            g2.drawString(title, (getWidth() - textWidth) / 2, 32);

            g2.dispose();

            super.paintComponent(g);
        }
    }

    /**
     * Anpassad knapp.
     * Knappen ritas med gradient, rundade hörn och highlight-effekt.
     *
     * @author Tom Olofsson
     */
    private class GameButton extends JButton {
        /**
         * Skapar en knapp med angiven text.
         *
         * @param text texten som visas på knappen
         */
        public GameButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("Arial Black", Font.BOLD, 28));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        /**
         * Ritar knappens utseende med gradient, ram och glanseffekt.
         *
         * @param g grafikobjektet som används för att rita komponenten
         */
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            GradientPaint gradient = new GradientPaint(
                    0, 0, HOT_PINK,
                    0, getHeight(), ORANGE
            );

            g2.setPaint(gradient);
            g2.fill(new RoundRectangle2D.Double(4, 4, getWidth() - 8, getHeight() - 8, 40, 40));

            g2.setColor(new Color(255, 220, 80));
            g2.setStroke(new BasicStroke(4));
            g2.drawRoundRect(4, 4, getWidth() - 8, getHeight() - 8, 40, 40);

            g2.setColor(new Color(255, 255, 255, 80));
            g2.fillRoundRect(12, 10, getWidth() - 24, getHeight() / 3, 25, 25);

            super.paintComponent(g);
            g2.dispose();
        }
    }

    /**
     * Startar klientprogrammet.
     *
     * @author Tom Olofsson
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameClientGUI gui = new GameClientGUI();
            gui.setVisible(true);
        });
    }
}