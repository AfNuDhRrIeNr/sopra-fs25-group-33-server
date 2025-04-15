package ch.uzh.ifi.hase.soprafs24.constant;
import java.util.Map;

public class LetterPoints {
    public static final Map<Character, Integer> LETTER_POINTS = Map.ofEntries(
        
        Map.entry('A', 1),
        Map.entry('E', 1),
        Map.entry('I', 1),
        Map.entry('O', 1),
        Map.entry('U', 1),
        Map.entry('L', 1),
        Map.entry('N', 1),
        Map.entry('S', 1),
        Map.entry('T', 1),
        Map.entry('R', 1),
        
        Map.entry('D', 2),
        Map.entry('G', 2),
        
        Map.entry('B', 3),
        Map.entry('C', 3),
        Map.entry('M', 3),
        Map.entry('P', 3),
        
        Map.entry('F', 4),
        Map.entry('H', 4),
        Map.entry('V', 4),
        Map.entry('W', 4),
        Map.entry('Y', 4),
        
        Map.entry('K', 5),
        
        Map.entry('J', 8),
        Map.entry('X', 8),
        
        Map.entry('Q', 10),
        Map.entry('Z', 10)
        
    );
    
    /**
     * Gets the point value for a letter
     * @param letter The letter to get points for
     * @return The point value (0 if letter not found)
     */
    public static int getPoints(char letter) {
        return LETTER_POINTS.get(letter);
    }
}
