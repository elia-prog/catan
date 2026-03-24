package model;

public enum ResourceType {
    WOOD,   // עץ
    BRICK,  // לבנים
    SHEEP,  // צמר/כבשים
    WHEAT,  // חיטה
    ORE,    // ברזל
    NONE;    // למקרה של מדבר

    public String toHebrew() {
        switch (this) {
            case WOOD: return "עץ";
            case BRICK: return "לבנה";
            case SHEEP: return "כבשה";
            case WHEAT: return "חיטה";
            case ORE: return "עפרה";
            default: return "ללא";
        }
    }
}