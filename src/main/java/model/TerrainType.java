package model; // הגדרת החבילה שבה נמצא הקובץ - מודל המשחק

/**
 * אינאם המייצג את סוגי השטחים השונים על לוח המשחק
 */
public enum TerrainType {
    FOREST(ResourceType.WOOD),   // יער - מפיק עץ
    HILLS(ResourceType.BRICK),    // גבעות - מפיקות לבנים
    PASTURE(ResourceType.SHEEP),  // מרעה - מפיק צמר/כבשים
    FIELDS(ResourceType.WHEAT),   // שדות - מפיקים חיטה
    MOUNTAINS(ResourceType.ORE),  // הרים - מפיקים עפרה/ברזל
    DESERT(ResourceType.NONE),    // מדבר - לא מפיק דבר
    WATER_TILE(ResourceType.NONE); // משבצת מים - לא מפיקה דבר

    private final ResourceType resource; // המשאב המשויך לסוג השטח

    /**
     * בנאי (Constructor) עבור האינאם שמקשר בין סוג השטח למשאב שהוא מפיק
     * @param resource סוג המשאב שהשטח מפיק
     */
    TerrainType(ResourceType resource) {
        this.resource = resource; // השמת המשאב במשתנה הפרטי
    }

    /**
     * פונקציית גישה המחזירה את סוג המשאב המופק מהשטח.
     * [יעילות: O(1)] - זמן ריצה קבוע.
     * @return סוג המשאב המשויך לשטח זה
     */
    public ResourceType getResource() {
        return resource; // החזרת המשאב
    }
}
