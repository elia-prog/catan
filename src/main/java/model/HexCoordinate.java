package model; // הגדרת החבילה שבה נמצא הקובץ - מודל המשחק

import java.util.Objects; // ייבוא מחלקת עזר לטיפול באובייקטים (כמו hashCode ו-equals)

/**
 * מחלקה המייצגת קואורדינטה של משושה בלוח המשחק בשיטת קואורדינטות ציריות (Axial Coordinates).
 * מממשת את Comparable כדי לאפשר מיון ושימוש במבני נתונים ממוינים.
 */
public class HexCoordinate implements Comparable<HexCoordinate> {
    private final int x; // קואורדינטת X (נקראת גם q במערכות מסוימות)
    private final int y; // קואורדינטת Y (נקראת גם r במערכות מסוימות)

    // כיווני השכנים בשעון (החל משעה 1 בערך)
    // אלו השינויים ב-X ו-Y כדי להגיע לשכן של משושה
    private static final int[][] DIRECTIONS = {
            {1, 0}, {0, 1}, {-1, 1}, {-1, 0}, {0, -1}, {1, -1}
    };

    /**
     * בנאי (Constructor) ליצירת קואורדינטה חדשה.
     * [יעילות: O(1)] - בנאי פשוט המבצע השמות בלבד.
     * @param x המיקום על ציר X
     * @param y המיקום על ציר Y
     */
    public HexCoordinate(int x, int y) {
        this.x = x; // הגדרת ערך X
        this.y = y; // הגדרת ערך Y
    }

    /**
     * פונקציה המחזירה את הקואורדינטה של השכן בכיוון מסוים.
     * [יעילות: O(1)] - חישוב אריתמטי פשוט.
     * @param directionIndex אינדקס הכיוון (0-5)
     * @return אובייקט HexCoordinate חדש המייצג את השכן
     */
    public HexCoordinate getNeighbor(int directionIndex) {
        // וידוא שהאינדקס נמצא בטווח 0-5 (מבצע פעולת מודולו 6)
        int d = directionIndex % 6;
        if (d < 0) d += 6; // טיפול במספרים שליליים

        int dx = DIRECTIONS[d][0]; // השינוי ב-X לפי הכיוון
        int dy = DIRECTIONS[d][1]; // השינוי ב-Y לפי הכיוון
        return new HexCoordinate(this.x + dx, this.y + dy); // החזרת השכן החדש
    }

    /**
     * פונקציית גישה לקבלת ערך X.
     * [יעילות: O(1)] - החזרת ערך של משתנה.
     * @return ערך ה-X
     */
    public int getX() { return x; }
    
    /**
     * פונקציית גישה לקבלת ערך Y.
     * [יעילות: O(1)] - החזרת ערך של משתנה.
     * @return ערך ה-Y
     */
    public int getY() { return y; }

    /**
     * פונקציה לבדיקת שוויון בין שתי קואורדינטות.
     * [יעילות: O(1)] - השוואה פשוטה של שני ערכי int.
     * @param o האובייקט להשוואה
     * @return true אם הקואורדינטות זהות, אחרת false
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // בדיקת שוויון זיכרון
        if (o == null || getClass() != o.getClass()) return false; // בדיקת טיפוס האובייקט
        HexCoordinate that = (HexCoordinate) o; // המרה (Casting) לטיפוס המתאים
        return x == that.x && y == that.y; // השוואת הערכים עצמם
    }

    /**
     * פונקציה המחשבת ערך Hash עבור הקואורדינטה.
     * [יעילות: O(1)] - חישוב על בסיס שני ערכי int.
     * @return ערך ה-Hash המחושב
     */
    @Override
    public int hashCode() {
        return Objects.hash(x, y); // שימוש בפונקציית עזר של Java לחישוב ה-Hash
    }

    /**
     * ייצוג הקואורדינטה כמחרוזת טקסט.
     * [יעילות: O(1)] - שרשור מחרוזות קצר.
     * @return מחרוזת בפורמט "x,y"
     */
    @Override
    public String toString() {
        return x + "," + y; // החזרת המחרוזת
    }

    /**
     * פונקציה להשוואה בין קואורדינטות לצורך מיון.
     * [יעילות: O(1)] - השוואה פשוטה של ערכי int.
     * @param other הקואורדינטה האחרת להשוואה
     * @return ערך שלילי אם זה קטן מ-other, חיובי אם גדול, ו-0 אם שווים
     */
    @Override
    public int compareTo(HexCoordinate other) {
        if (this.x != other.x) return Integer.compare(this.x, other.x); // קודם כל משווים לפי X
        return Integer.compare(this.y, other.y); // אם ה-X זהה, משווים לפי Y
    }
}
