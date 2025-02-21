package com.example.connectfour;

import javax.swing.*;

public class Connect4 extends JFrame {
    public Connect4() {
        setTitle("Connect Four");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        add(new BoardGame()); // Ensure BoardGame is added
        setSize(600, 500);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Connect4::new);
    }
}
