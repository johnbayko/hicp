package hicp;

public final class TextDirection {
    private final String id;
    public TextDirection(String newID) {
        id = newID;
    }
    public String toString() {
        return id;
    }
    public static TextDirection getTextDirection(String newID) {
        if (RIGHT.toString().equalsIgnoreCase(newID)) {
            return RIGHT;
        }
        if (LEFT.toString().equalsIgnoreCase(newID)) {
            return LEFT;
        }
        if (UP.toString().equalsIgnoreCase(newID)) {
            return UP;
        }
        if (DOWN.toString().equalsIgnoreCase(newID)) {
            return DOWN;
        }
        return null;
    }
    public static final TextDirection RIGHT = new TextDirection("RIGHT");
    public static final TextDirection LEFT = new TextDirection("LEFT");
    public static final TextDirection UP = new TextDirection("UP");
    public static final TextDirection DOWN = new TextDirection("DOWN");
}
