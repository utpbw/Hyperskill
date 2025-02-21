package com.example.connectfour;

import javax.swing.*;
import java.awt.*;

public class Button extends JButton {
    private final int row;
    private final int col;
    private String value = " ";
    private static final Color BASELINE_COLOR = Color.BLACK;
    private static final Color WINNING_COLOR = Color.GREEN;

    public Button(int row, int col) {
        super(" "); // Label like "A1"
        this.row = row;
        this.col = col;
        setName("Button" + (char) ('A' + col) + String.valueOf(row + 1));
        setBackground(BASELINE_COLOR);
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public boolean isEmpty() {
        return value.equals(" ");
    }

    public void setValue(String value) {
        if (isEmpty()) { // Only allow changing empty cells
            this.value = value;
            setText(value);
        }
    }

    public String getValue() {
        return value;
    }

    public void highlightWin() {
        setOpaque(true); // ✅ Ensure color change is visible
        setBackground(WINNING_COLOR); // ✅ Change background color for winning pieces
        repaint(); // ✅ Force UI update
    }


    public void reset() {
        value = " ";
        setText(value);
        setBackground(BASELINE_COLOR);
    }
}