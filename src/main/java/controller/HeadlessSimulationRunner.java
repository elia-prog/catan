package controller; // מגדיר שהקובץ שייך לחבילה controller (בקר)

import model.Player; // מייבא את מחלקת השחקן מהמודל
import java.util.HashMap; // מייבא מבנה נתונים של מפה (HashMap)
import java.util.Map; // מייבא את הממשק של מפה

/**
 * מחלקה זו מריצה סימולציה של המשחק ללא ממשק גרפי (Headless).
 * היא משמשת לבדיקת איזון בין בוטים והרצת כמות גדולה של משחקים במהירות.
 */
public class HeadlessSimulationRunner {
    /**
     * [יעילות: O(G * A)] - G הוא מספר המשחקים, A הוא מספר הפעולות המקסימלי למשחק.
     * פונקציית הכניסה הראשית להרצת הסימולציה.
     */
    public static void main(String[] args) {
        GameEngine.setHeadless(true); // מגדיר למנוע המשחק לעבוד במצב ללא גרפיקה
        int totalGames = 100; // מספר המשחקים הכולל להרצה בסימולציה
        Map<String, Integer> winCounter = new HashMap<>(); // מפה לספירת ניצחונות לכל שחקן/בוט
        System.out.println("Starting Headless Simulation (" + totalGames + " games)..."); // הדפסת הודעת פתיחה

        // לולאה המריצה את המשחקים בזה אחר זה
        for (int i = 1; i <= totalGames; i++) {
            GameEngine engine = new GameEngine(true); // יצירת מנוע משחק חדש במצב אוטונומי (בוטים בלבד)
            int actionCount = 0; // מונה פעולות למניעת לולאה אינסופית
            int maxActions = 10000; // הגבלת מספר פעולות למשחק (Watchdog)

            // לולאת המשחק - כל עוד המשחק לא נגמר ולא עברנו את מקסימום הפעולות
            while (!engine.isGameOver() && actionCount < maxActions) {
                engine.executeSingleAiAction(); // ביצוע פעולה אחת של הבוט התורן
                actionCount++; // קידום מונה הפעולות
            }

            // בדיקה אם המשחק הסתיים בגלל הגבלת פעולות
            if (actionCount >= maxActions) {
                System.out.println("Game " + i + " timed out (Watchdog triggered)."); // הודעה על חריגת זמן
            } else {
                Player winner = null; // משתנה למציאת המנצח
                // חיפוש השחקן שהגיע ל-10 נקודות או יותר
                for (int pIdx = 0; pIdx < engine.getPlayers().size() && winner == null; pIdx++) {
                    Player p = engine.getPlayers().get(pIdx); // קבלת השחקן הנוכחי מהרשימה
                    if (p.getVictoryPoints() >= 10) { // בדיקה אם יש לו מספיק נקודות ניצחון
                        winner = p; // הגדרה כמנצח
                    }
                }
                // אם נמצא מנצח, מעדכנים את מונה הניצחונות
                if (winner != null) {
                    winCounter.put(winner.getName(), winCounter.getOrDefault(winner.getName(), 0) + 1);
                }
            }
            // הדפסת התקדמות כל 10 משחקים
            if (i % 10 == 0) System.out.println("Completed " + i + " games...");
        }

        // הדפסת סיכום תוצאות הסימולציה
        System.out.println("\n========================================");
        System.out.println("      CATAN AI SIMULATION SUMMARY");
        System.out.println("========================================");
        winCounter.forEach((name, wins) -> {
            double winRate = (wins * 100.0) / totalGames; // חישוב אחוז הניצחונות
            System.out.println(String.format("%-20s : %d wins (%.1f%%)", name, wins, winRate)); // הדפסת השורה בפורמט מעומד
        });
    }
}
