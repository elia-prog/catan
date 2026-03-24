package model;

import java.util.Objects;

public class HexCoordinate implements Comparable<HexCoordinate> {
    private final int x; // נקרא גם q
    private final int y; // נקרא גם r

    // כיווני השכנים בשעון (החל משעה 1 בערך)
    // אלו השינויים ב-X ו-Y כדי להגיע לשכן
    private static final int[][] DIRECTIONS = {
            {1, 0}, {0, 1}, {-1, 1}, {-1, 0}, {0, -1}, {1, -1}
    };

    public HexCoordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public HexCoordinate getNeighbor(int directionIndex) {
        // directionIndex צריך להיות בין 0 ל-5
        int d = directionIndex % 6;
        if (d < 0) d += 6;

        int dx = DIRECTIONS[d][0];
        int dy = DIRECTIONS[d][1];
        return new HexCoordinate(this.x + dx, this.y + dy);
    }

    public int getX() { return x; }
    public int getY() { return y; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HexCoordinate that = (HexCoordinate) o;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return x + "," + y;
    }

    // נצטרך את זה כדי למיין מפתחות
    @Override
    public int compareTo(HexCoordinate other) {
        if (this.x != other.x) return Integer.compare(this.x, other.x);
        return Integer.compare(this.y, other.y);
    }
}