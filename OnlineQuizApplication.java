package Quiz;

import javax.swing.*;
import javax.swing.Timer; // Explicitly use Swing Timer
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class OnlineQuizApplication extends JFrame {
    private ArrayList<Question> questions;
    private ArrayList<Score> scores;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private String playerName;

    private static final String QUESTIONS_FILE = "questions.dat";
    private static final String SCORES_FILE = "scores.dat";

    private JLabel lblQuestion, lblTimer, lblQuestionNum;
    private JRadioButton[] options;
    private ButtonGroup optionGroup;
    private JButton btnNext, btnSubmit;
    private JProgressBar progressBar;

    private javax.swing.Timer quizTimer; // Explicitly declare as Swing Timer
    private int timeRemaining = 300; // 5 minutes in seconds

    public OnlineQuizApplication() {
        loadQuestions();
        loadScores();

        if (questions.isEmpty()) {
            createSampleQuestions();
            saveQuestions();
        }

        // Shuffle questions
        Collections.shuffle(questions);

        playerName = JOptionPane.showInputDialog(this, "Enter your name:");
        if (playerName == null || playerName.trim().isEmpty()) {
            playerName = "Anonymous";
        }

        setTitle("Online Quiz Application - Player: " + playerName);
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        createUI();
        displayQuestion();
        startTimer();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void createSampleQuestions() {
        Object[][] sampleData = {
                { "What is the capital of France?", "London", "Berlin", "Paris", "Madrid", 3 },
                { "Which planet is known as the Red Planet?", "Venus", "Mars", "Jupiter", "Saturn", 2 },
                { "Who wrote 'Romeo and Juliet'?", "Charles Dickens", "William Shakespeare", "Jane Austen",
                        "Mark Twain", 2 },
                { "What is 15 + 27?", "42", "41", "43", "40", 1 },
                { "Which programming language is known for 'Write Once, Run Anywhere'?", "Python", "C++", "Java",
                        "JavaScript", 3 },
                { "What is the largest ocean on Earth?", "Atlantic", "Indian", "Arctic", "Pacific", 4 },
                { "In which year did World War II end?", "1943", "1944", "1945", "1946", 3 },
                { "What is the square root of 144?", "11", "12", "13", "14", 2 },
                { "Which element has the chemical symbol 'O'?", "Gold", "Oxygen", "Silver", "Iron", 2 },
                { "Who painted the Mona Lisa?", "Vincent van Gogh", "Pablo Picasso", "Leonardo da Vinci",
                        "Michelangelo", 3 }
        };

        for (Object[] data : sampleData) {
            Question q = new Question();
            q.question = (String) data[0];
            q.options = new String[] {
                    (String) data[1],
                    (String) data[2],
                    (String) data[3],
                    (String) data[4]
            };
            q.correctAnswer = (Integer) data[5];
            questions.add(q);
        }
    }

    private void loadQuestions() {
        questions = new ArrayList<>();
        try {
            File file = new File(QUESTIONS_FILE);
            if (!file.exists())
                return;

            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 6) {
                    Question q = new Question();
                    q.question = parts[0];
                    q.options = new String[] { parts[1], parts[2], parts[3], parts[4] };
                    q.correctAnswer = Integer.parseInt(parts[5]);
                    questions.add(q);
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveQuestions() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(QUESTIONS_FILE));
            for (Question q : questions) {
                writer.println(q.question + "|" + q.options[0] + "|" + q.options[1] +
                        "|" + q.options[2] + "|" + q.options[3] + "|" + q.correctAnswer);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadScores() {
        scores = new ArrayList<>();
        try {
            File file = new File(SCORES_FILE);
            if (!file.exists())
                return;

            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 4) {
                    Score s = new Score();
                    s.playerName = parts[0];
                    s.score = Integer.parseInt(parts[1]);
                    s.totalQuestions = Integer.parseInt(parts[2]);
                    s.date = parts[3];
                    scores.add(s);
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveScore(String name, int score, int total, String date) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(SCORES_FILE, true));
            writer.println(name + "|" + score + "|" + total + "|" + date);
            writer.close();

            Score s = new Score();
            s.playerName = name;
            s.score = score;
            s.totalQuestions = total;
            s.date = date;
            scores.add(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Top Panel - Timer and Question Number
        JPanel topPanel = new JPanel(new BorderLayout());
        lblQuestionNum = new JLabel("Question 1 of " + questions.size());
        lblQuestionNum.setFont(new Font("Arial", Font.BOLD, 14));

        lblTimer = new JLabel("Time: 5:00");
        lblTimer.setFont(new Font("Arial", Font.BOLD, 16));
        lblTimer.setForeground(new Color(0, 128, 0));

        topPanel.add(lblQuestionNum, BorderLayout.WEST);
        topPanel.add(lblTimer, BorderLayout.EAST);

        // Progress Bar
        progressBar = new JProgressBar(0, questions.size());
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        JPanel headerPanel = new JPanel(new BorderLayout(5, 5));
        headerPanel.add(topPanel, BorderLayout.NORTH);
        headerPanel.add(progressBar, BorderLayout.SOUTH);

        // Question Panel
        JPanel questionPanel = new JPanel(new BorderLayout());
        questionPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        lblQuestion = new JLabel();
        lblQuestion.setFont(new Font("Arial", Font.PLAIN, 16));
        lblQuestion.setVerticalAlignment(SwingConstants.TOP);
        questionPanel.add(lblQuestion, BorderLayout.CENTER);

        // Options Panel
        JPanel optionsPanel = new JPanel(new GridLayout(4, 1, 5, 10));
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        options = new JRadioButton[4];
        optionGroup = new ButtonGroup();

        for (int i = 0; i < 4; i++) {
            options[i] = new JRadioButton();
            options[i].setFont(new Font("Arial", Font.PLAIN, 14));
            optionGroup.add(options[i]);
            optionsPanel.add(options[i]);
        }

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnNext = new JButton("Next");
        btnSubmit = new JButton("Submit Quiz");
        btnSubmit.setVisible(false);

        btnNext.addActionListener(e -> nextQuestion());
        btnSubmit.addActionListener(e -> submitQuiz());

        buttonPanel.add(btnNext);
        buttonPanel.add(btnSubmit);

        // Add to main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(questionPanel, BorderLayout.CENTER);
        mainPanel.add(optionsPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void displayQuestion() {
        if (currentQuestionIndex < questions.size()) {
            Question q = questions.get(currentQuestionIndex);

            lblQuestion.setText("<html><body style='width: 600px'>" + q.question + "</body></html>");
            lblQuestionNum.setText("Question " + (currentQuestionIndex + 1) + " of " + questions.size());

            for (int i = 0; i < 4; i++) {
                options[i].setText(q.options[i]);
                options[i].setSelected(false);
            }

            progressBar.setValue(currentQuestionIndex);

            if (currentQuestionIndex == questions.size() - 1) {
                btnNext.setVisible(false);
                btnSubmit.setVisible(true);
            }
        }
    }

    private void nextQuestion() {
        checkAnswer();
        currentQuestionIndex++;
        displayQuestion();
    }

    private void checkAnswer() {
        Question q = questions.get(currentQuestionIndex);

        for (int i = 0; i < 4; i++) {
            if (options[i].isSelected() && (i + 1) == q.correctAnswer) {
                score++;
                break;
            }
        }
    }

    private void submitQuiz() {
        checkAnswer();
        quizTimer.stop();

        String date = new java.util.Date().toString();
        saveScore(playerName, score, questions.size(), date);
        showResults();

        int choice = JOptionPane.showConfirmDialog(this,
                "Do you want to take the quiz again?",
                "Quiz Completed",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            new OnlineQuizApplication();
        } else {
            showLeaderboard();
            System.exit(0);
        }
    }

    private void showResults() {
        double percentage = (score * 100.0) / questions.size();
        String grade;

        if (percentage >= 90)
            grade = "A+";
        else if (percentage >= 80)
            grade = "A";
        else if (percentage >= 70)
            grade = "B";
        else if (percentage >= 60)
            grade = "C";
        else if (percentage >= 50)
            grade = "D";
        else
            grade = "F";

        String message = String.format(
                "Quiz Completed!\n\n" +
                        "Player: %s\n" +
                        "Score: %d / %d\n" +
                        "Percentage: %.2f%%\n" +
                        "Grade: %s\n" +
                        "Time Taken: %d seconds",
                playerName, score, questions.size(), percentage, grade, (300 - timeRemaining));

        JOptionPane.showMessageDialog(this, message, "Results", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showLeaderboard() {
        StringBuilder sb = new StringBuilder("=== LEADERBOARD ===\n\n");

        // Sort by score descending
        scores.sort((s1, s2) -> Integer.compare(s2.score, s1.score));

        int rank = 1;
        int limit = Math.min(10, scores.size());

        for (int i = 0; i < limit; i++) {
            Score s = scores.get(i);
            double percentage = (s.score * 100.0) / s.totalQuestions;
            sb.append(String.format("%d. %s - %d/%d (%.1f%%) - %s\n",
                    rank++, s.playerName, s.score, s.totalQuestions, percentage, s.date));
        }

        if (scores.isEmpty()) {
            sb.append("No scores yet!");
        }

        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 300));

        JOptionPane.showMessageDialog(this, scrollPane, "Leaderboard", JOptionPane.INFORMATION_MESSAGE);
    }

    private void startTimer() {
        quizTimer = new javax.swing.Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                timeRemaining--;
                int minutes = timeRemaining / 60;
                int seconds = timeRemaining % 60;
                lblTimer.setText(String.format("Time: %d:%02d", minutes, seconds));

                if (timeRemaining <= 30) {
                    lblTimer.setForeground(Color.RED);
                } else if (timeRemaining <= 60) {
                    lblTimer.setForeground(Color.ORANGE);
                }

                if (timeRemaining <= 0) {
                    quizTimer.stop();
                    JOptionPane.showMessageDialog(OnlineQuizApplication.this,
                            "Time's up!", "Quiz Ended", JOptionPane.WARNING_MESSAGE);
                    submitQuiz();
                }
            }
        });
        quizTimer.start();
    }

    class Question {
        String question;
        String[] options;
        int correctAnswer;
    }

    class Score {
        String playerName;
        int score;
        int totalQuestions;
        String date;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OnlineQuizApplication());
    }
}