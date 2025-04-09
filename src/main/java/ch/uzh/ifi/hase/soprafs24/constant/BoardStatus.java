package ch.uzh.ifi.hase.soprafs24.constant;

public class BoardStatus {
    private static final int BOARD_SIZE = 15;

    // Types of multipliers
    public static final String DOUBLE_LETTER = "DL";
    public static final String TRIPLE_LETTER = "TL";
    public static final String DOUBLE_WORD = "DW";
    public static final String TRIPLE_WORD = "TW";
    public static final String NORMAL = "";

    // Static board layout (standard Scrabble board)
    private static final String[][] BOARD_MULTIPLIERS = setupBoardMultipliers();

    /**
     * Initialize the standard Scrabble board multipliers
     */
    private static String[][] setupBoardMultipliers() {
        String[][] board = new String[BOARD_SIZE][BOARD_SIZE];

        // Initialize with normal tiles
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = NORMAL;
            }
        }

        board[0][0] = TRIPLE_WORD;   board[0][7] = TRIPLE_WORD;   board[0][14] = TRIPLE_WORD;
        board[7][0] = TRIPLE_WORD;   board[7][14] = TRIPLE_WORD;
        board[14][0] = TRIPLE_WORD;  board[14][7] = TRIPLE_WORD;  board[14][14] = TRIPLE_WORD;

        board[1][1] = DOUBLE_WORD;   board[2][2] = DOUBLE_WORD;   board[3][3] = DOUBLE_WORD;
        board[4][4] = DOUBLE_WORD;   board[10][10] = DOUBLE_WORD; board[11][11] = DOUBLE_WORD;
        board[12][12] = DOUBLE_WORD; board[13][13] = DOUBLE_WORD;
        board[1][13] = DOUBLE_WORD;  board[2][12] = DOUBLE_WORD;  board[3][11] = DOUBLE_WORD;
        board[4][10] = DOUBLE_WORD;  board[10][4] = DOUBLE_WORD;  board[11][3] = DOUBLE_WORD;
        board[12][2] = DOUBLE_WORD;  board[13][1] = DOUBLE_WORD;
        board[7][7] = DOUBLE_WORD;

        board[1][5] = TRIPLE_LETTER; board[1][9] = TRIPLE_LETTER;
        board[5][1] = TRIPLE_LETTER; board[5][5] = TRIPLE_LETTER; board[5][9] = TRIPLE_LETTER; board[5][13] = TRIPLE_LETTER;
        board[9][1] = TRIPLE_LETTER; board[9][5] = TRIPLE_LETTER; board[9][9] = TRIPLE_LETTER; board[9][13] = TRIPLE_LETTER;
        board[13][5] = TRIPLE_LETTER; board[13][9] = TRIPLE_LETTER;

        board[0][3] = DOUBLE_LETTER; board[0][11] = DOUBLE_LETTER;
        board[2][6] = DOUBLE_LETTER; board[2][8] = DOUBLE_LETTER;
        board[3][0] = DOUBLE_LETTER; board[3][7] = DOUBLE_LETTER; board[3][14] = DOUBLE_LETTER;
        board[6][2] = DOUBLE_LETTER; board[6][6] = DOUBLE_LETTER; board[6][8] = DOUBLE_LETTER; board[6][12] = DOUBLE_LETTER;
        board[7][3] = DOUBLE_LETTER; board[7][11] = DOUBLE_LETTER;
        board[8][2] = DOUBLE_LETTER; board[8][6] = DOUBLE_LETTER; board[8][8] = DOUBLE_LETTER; board[8][12] = DOUBLE_LETTER;
        board[11][0] = DOUBLE_LETTER; board[11][7] = DOUBLE_LETTER; board[11][14] = DOUBLE_LETTER;
        board[12][6] = DOUBLE_LETTER; board[12][8] = DOUBLE_LETTER;
        board[14][3] = DOUBLE_LETTER; board[14][11] = DOUBLE_LETTER;

        return board;
    }

    /**
     * Get the multiplier at a specific board position
     * @param row Row index
     * @param col Column index
     * @return Multiplier type (DL, TL, DW, TW, or empty for normal)
     */
    public static String getMultiplier(int row, int col) {
        if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) {
            return NORMAL;
        }
        return BOARD_MULTIPLIERS[row][col];
    }
}
