package com.example.connectfour;

public class BoardLogic {
    private static final int ROWS = 6;
    private static final int COLS = 7;
    private final Button[][] board;

    public BoardLogic(Button[][] board) {
        this.board = board;
    }

    /**
     * Finds the lowest available row in the given column.
     *
     * @param col The column index (0 - 6)
     * @return The lowest available row index (0 - 5), or -1 if the column is full.
     */
    public int getLowestEmptyRow(int col) {
        for (int row = ROWS - 1; row >= 0; row--) { // Start from the bottom
            if (board[row][col].isEmpty()) {
                return row;
            }
        }
        return -1; // Column is full
    }

    public boolean checkWin(int row, int col, String playerSymbol) {
        System.out.println("Checking win for " + playerSymbol + " at (" + row + ", " + col + ")");

        boolean horizontal = checkDirection(row, col, 1, 0, playerSymbol);
        boolean vertical = checkDirection(row, col, 0, 1, playerSymbol);
        boolean diagonal1 = checkDirection(row, col, 1, 1, playerSymbol);
        boolean diagonal2 = checkDirection(row, col, 1, -1, playerSymbol);

        System.out.println("Horizontal: " + horizontal);
        System.out.println("Vertical: " + vertical);
        System.out.println("Diagonal ↘: " + diagonal1);
        System.out.println("Diagonal ↙: " + diagonal2);

        return horizontal || vertical || diagonal1 || diagonal2;
    }



    private boolean checkDirection(int row, int col, int rowStep, int colStep, String playerSymbol) {
        int count = 1; // Start with the placed piece
        int startRow = row, startCol = col;
        int endRow = row, endCol = col;

        // ✅ Check forward (positive direction)
        for (int i = 1; i < 4; i++) {
            int newRow = row + (i * rowStep);
            int newCol = col + (i * colStep);

            if (isValidCell(newRow, newCol) && board[newRow][newCol].getValue().equals(playerSymbol)) {
                count++;
                endRow = newRow;
                endCol = newCol;
            } else {
                break; // Stop if different symbol or out of bounds
            }
        }

        // ✅ Check backward (negative direction)
        for (int i = 1; i < 4; i++) {
            int newRow = row - (i * rowStep);
            int newCol = col - (i * colStep);

            if (isValidCell(newRow, newCol) && board[newRow][newCol].getValue().equals(playerSymbol)) {
                count++;
                startRow = newRow;
                startCol = newCol;
            } else {
                break;
            }
        }

        System.out.println("Checking direction (" + rowStep + ", " + colStep + ") → Found " + count + " in a row");

        if (count >= 4) {
            highlightWinningCells(startRow, startCol, endRow, endCol, rowStep, colStep);
            return true;
        }
        return false;
    }

    private void highlightWinningCells(int startRow, int startCol, int endRow, int endCol, int rowStep, int colStep) {
        System.out.println("Highlighting winning cells from (" + startRow + ", " + startCol + ") to (" + endRow + ", " + endCol + ")");

        int newRow = startRow, newCol = startCol;

        while (isValidCell(newRow, newCol)) {
            board[newRow][newCol].highlightWin();
            if (newRow == endRow && newCol == endCol) break; // Stop at last cell

            newRow += rowStep;
            newCol += colStep;
        }
    }



    private boolean isValidCell(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }

}
