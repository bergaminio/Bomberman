package ui;

import java.awt.event.KeyEvent;
import java.util.List;

// Tastenbelegung eines Spielers. Vier Richtungen plus Bombe.
public class PlayerKeys {
    private final int up;
    private final int down;
    private final int left;
    private final int right;
    private final int bomb;
    private final String description;

    private PlayerKeys(int up, int down, int left, int right, int bomb, String description) {
        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
        this.bomb = bomb;
        this.description = description;
    }

    public static PlayerKeys wasd() {
        return new PlayerKeys(KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D,
            KeyEvent.VK_SPACE, "W A S D + Leertaste");
    }

    public static PlayerKeys arrows() {
        return new PlayerKeys(KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
            KeyEvent.VK_ENTER, "Pfeiltasten + Enter");
    }

    public static PlayerKeys ijkl() {
        return new PlayerKeys(KeyEvent.VK_I, KeyEvent.VK_K, KeyEvent.VK_J, KeyEvent.VK_L,
            KeyEvent.VK_U, "I J K L + U");
    }

    public static PlayerKeys numpad() {
        return new PlayerKeys(KeyEvent.VK_NUMPAD8, KeyEvent.VK_NUMPAD5, KeyEvent.VK_NUMPAD4,
            KeyEvent.VK_NUMPAD6, KeyEvent.VK_NUMPAD0, "Numblock 8 4 5 6 + 0");
    }

    // Reihenfolge passt zu den Startecken A, B, C, D.
    public static List<PlayerKeys> defaults() {
        return List.of(wasd(), arrows(), ijkl(), numpad());
    }

    public int getUp() {
        return up;
    }

    public int getDown() {
        return down;
    }

    public int getLeft() {
        return left;
    }

    public int getRight() {
        return right;
    }

    public int getBomb() {
        return bomb;
    }

    public String getDescription() {
        return description;
    }
}
