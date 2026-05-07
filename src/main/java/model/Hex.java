package model; // מגדיר שהקובץ שייך לחבילה model - חלק המודל בארכיטקטורת MVC

import java.util.ArrayList; // ייבוא מחלקה לניהול רשימה דינמית
import java.util.List; // ייבוא ממשק המייצג רשימה

/**
 * מחלקה זו מייצגת משושה בודד על הלוח.
 * לכל משושה יש סוג קרקע (יער, הרים וכו') ומספר אסימון.
 * המשושה מחזיק מידע על הקואורדינטות שלו, השודד, והאלמנטים הגאומטריים המקיפים אותו (קודקודים וצלעות).
 */
public class Hex {
    private final HexCoordinate coordinate; // המיקום של המשושה בלוח (קואורדינטות ציריות)
    private final TerrainType type; // סוג הקרקע (למשל FOREST מפיק עץ, HILLS מפיק לבנה)
    private final int numberToken; // המספר שצריך לצאת בקוביות (2-12) כדי שהמשושה יפיק משאבים

    // שדה השודד - משתנה בוליאני המציין האם השודד נמצא כרגע על המשושה הזה
    private boolean hasRobber;

    // הקשרים לגרף - רשימת הקודקודים (נקודות המפגש) והצלעות (דרכים) שמרכיבים את המשושה הזה
    private final List<Vertex> vertices; // רשימת קודקודי המשושה (מקסימום 6)
    private final List<Edge> edges; // רשימת צלעות המשושה (מקסימום 6)

    /**
     * בנאי (Constructor) ליצירת משושה חדש.
     * [יעילות: O(1)] - אתחול משתנים ויצירת רשימות ריקות.
     * @param coordinate קואורדינטת המשושה בלוח
     * @param type סוג הקרקע של המשושה
     * @param numberToken המספר המשויך למשושה
     */
    public Hex(HexCoordinate coordinate, TerrainType type, int numberToken) {
        this.coordinate = coordinate; // השמת הקואורדינטה
        this.type = type; // השמת סוג הקרקע
        this.numberToken = numberToken; // השמת מספר האסימון

        // בתחילת המשחק, השודד תמיד מוצב במדבר (DESERT), על פי חוקי קטאן
        this.hasRobber = (type == TerrainType.DESERT);

        this.vertices = new ArrayList<>(6); // אתחול רשימת קודקודים עם קיבולת התחלתית של 6
        this.edges = new ArrayList<>(6);    // אתחול רשימת צלעות עם קיבולת התחלתית של 6
    }

    /**
     * פונקציה הבודקת האם השודד נמצא כרגע על המשושה.
     * [יעילות: O(1)] - החזרת ערך בוליאני פשוט.
     * @return true אם השודד כאן, אחרת false
     */
    public boolean hasRobber() {
        return hasRobber; // החזרת מצב השודד
    }

    /**
     * פונקציה להצבה או הסרה של השודד מהמשושה.
     * [יעילות: O(1)] - השמת ערך בוליאני.
     * @param hasRobber המצב החדש של השודד על המשושה
     */
    public void setRobber(boolean hasRobber) {
        this.hasRobber = hasRobber; // עדכון מצב השודד
    }

    /**
     * פונקציית גישה לקבלת סוג הקרקע של המשושה.
     * [יעילות: O(1)] - החזרת אובייקט TerrainType.
     * @return סוג הקרקע
     */
    public TerrainType getType() {
        return type; // החזרת הסוג
    }

    /**
     * פונקציית גישה לקבלת מספר האסימון של המשושה.
     * [יעילות: O(1)] - החזרת ערך int.
     * @return מספר האסימון
     */
    public int getNumberToken() {
        return numberToken; // החזרת המספר
    }

    /**
     * פונקציית גישה לקבלת הקואורדינטות של המשושה.
     * [יעילות: O(1)] - החזרת אובייקט HexCoordinate.
     * @return קואורדינטות המשושה
     */
    public HexCoordinate getCoordinate() {
        return this.coordinate; // החזרת הקואורדינטה
    }

    /**
     * פונקציית גישה לקבלת רשימת הקודקודים (הפינות) של המשושה.
     * [יעילות: O(1)] - החזרת רשימת אובייקטי Vertex.
     * @return רשימת הקודקודים
     */
    public List<Vertex> getVertices() {
        return vertices; // החזרת הרשימה
    }

    /**
     * פונקציית גישה לקבלת רשימת הצלעות של המשושה.
     * [יעילות: O(1)] - החזרת רשימת אובייקטי Edge.
     * @return רשימת הצלעות
     */
    public List<Edge> getEdges() {
        return edges; // החזרת הרשימה
    }

    /**
     * פונקציה המוסיפה קודקוד למשושה במהלך תהליך בניית הלוח.
     * [יעילות: O(N) כש-N הוא מספר הקודקודים, אך כאן N תמיד <= 6 לכן O(1)]
     * @param v הקודקוד להוספה
     */
    public void addVertex(Vertex v) {
        if (!vertices.contains(v)) { // בדיקה אם הקודקוד כבר קיים ברשימה כדי למנוע כפילויות
            vertices.add(v); // הוספת הקודקוד לרשימה
        }
    }

    /**
     * פונקציה המוסיפה צלע למשושה במהלך תהליך בניית הלוח.
     * [יעילות: O(N) כש-N הוא מספר הצלעות, אך כאן N תמיד <= 6 לכן O(1)]
     * @param e הצלע להוספה
     */
    public void addEdge(Edge e) {
        if (!edges.contains(e)) { // בדיקה אם הצלע כבר קיימת ברשימה כדי למנוע כפילויות
            edges.add(e); // הוספת הצלע לרשימה
        }
    }

    /**
     * ייצוג המשושה כמחרוזת טקסט לצורך הדפסה וניפוי שגיאות.
     * [יעילות: O(1)] - שרשור מחרוזות.
     * @return מחרוזת המכילה את מיקום המשושה, סוגו והמספר שלו
     */
    @Override
    public String toString() {
        return "Hex at " + coordinate + ": " + type + " (" + numberToken + ")"; // יצירת והחזרת המחרוזת
    }
}
