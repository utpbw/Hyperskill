package com.example.connectfour;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BoardGame extends JPanel {
    private static final int ROWS = 6;
    private static final int COLS = 7;
    private final Button[][] buttons = new Button[ROWS][COLS];
    private final BoardLogic boardLogic;
    private final Player playerX = new Player("X");
    private final Player playerO = new Player("O");
    private Player currentPlayer = playerX;
    private boolean gameOver = false;
    private final JButton resetButton;

    public BoardGame() {
        setLayout(new BorderLayout());

        // Create game board
        JPanel boardPanel = new JPanel(new GridLayout(ROWS, COLS, 2, 2));
        boardPanel.setBackground(Color.BLACK);
        boardLogic = new BoardLogic(buttons);

        // Initialize buttons
        for (int row = ROWS - 1; row >= 0; row--) {
            for (int col = 0; col < COLS; col++) {
                buttons[row][col] = new Button(row, col);
                buttons[row][col].addActionListener(new ButtonClickListener());
                boardPanel.add(buttons[row][col]);
            }
        }

        // Reset button
        resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> resetGame());

        // Add components
        add(boardPanel, BorderLayout.CENTER);
        add(resetButton, BorderLayout.SOUTH);
    }

    private void resetGame() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                buttons[row][col].reset();
            }
        }
        currentPlayer = playerX;
        gameOver = false;
    }

    private class ButtonClickListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (gameOver) {
                System.out.println("Game over! No more moves allowed.");
                return; // Prevent moves after game ends
            }

            Button clickedButton = (Button) e.getSource();
            int col = clickedButton.getCol();
            int row = boardLogic.getLowestEmptyRow(col);

            System.out.println("Player " + currentPlayer.getSymbol() + " clicked column " + col);

            if (row != -1) { // Ensure column is not full
                buttons[row][col].setValue(currentPlayer.getSymbol());
                System.out.println("Placed " + currentPlayer.getSymbol() + " at (" + row + ", " + col + ")");

                if (boardLogic.checkWin(row, col, currentPlayer.getSymbol())) {
                    gameOver = true;
                    revalidate();
                    System.out.println("Player " + currentPlayer.getSymbol() + " wins!");
                    repaint();
                    return; // Stop further moves
                }

                // ✅ Ensure the turn switches after a valid move
                currentPlayer = (currentPlayer == playerX) ? playerO : playerX;
                System.out.println("Turn switched! Now it's " + currentPlayer.getSymbol() + "'s turn.");
            } else {
                System.out.println("Column " + col + " is full! Choose another column.");
            }
        }
    }
}
