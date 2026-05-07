package view; // הגדרת החבילה שבה נמצא הקובץ - תצוגת המשחק

/**
 * מחלקה המשמשת כנקודת הכניסה להרצת האפליקציה.
 * לעיתים נדרשת מחלקה כזו בנפרד מ-Application של JavaFX כדי למנוע בעיות תאימות.
 */
public class Launcher {
    /**
     * פונקציית ה-main: נקודת הכניסה הראשית של התוכנית.
     * [יעילות: O(1)] - פעולות הדפסה והרצה פשוטות.
     * @param args ארגומנטים משורת הפקודה (אם קיימים)
     */
    public static void main(String[] args) {
        // הדפסת הודעת פתיחה למסוף (Console)
        System.out.println("========================================");
        System.out.println("מפעיל את קטאן - גרסת בחירת מצב משחק...");
        System.out.println("========================================");
        
        // הפעלת אפליקציית JavaFX על ידי קריאה ל-launch של CatanApp
        CatanApp.launch(CatanApp.class, args);
    }
}
