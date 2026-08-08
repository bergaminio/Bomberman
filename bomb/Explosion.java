package bomb;

import java.util.List;

import common.Position;

public class Explosion {
    public static final int DEFAULT_DURATION_TICKS = 1;

    private final List<Position> affectedPositions;
    private final int durationTicks;
    private int remainingTicks;

    public Explosion(List<Position> affectedPositions) {
        this(affectedPositions, DEFAULT_DURATION_TICKS);
    }

    public Explosion(List<Position> affectedPositions, int durationTicks) {
        // Kopie statt der uebergebenen Liste: sonst koennte der Aufrufer die
        // Felder einer bereits erzeugten Explosion nachtraeglich aendern.
        this.affectedPositions = List.copyOf(affectedPositions);
        this.durationTicks = durationTicks;
        this.remainingTicks = durationTicks;
    }

    public List<Position> getAffectedPositions() {
        return affectedPositions;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    // Ausgangswert, damit eine Anzeige ausrechnen kann, wie weit die
    // Explosion schon abgelaufen ist.
    public int getDurationTicks() {
        return durationTicks;
    }

    // Funktioniert nur, weil Position equals() hat. Sonst wuerde contains()
    // Objekte vergleichen und immer false liefern.
    public boolean covers(Position position) {
        return affectedPositions.contains(position);
    }

    public void tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
    }

    public boolean isFinished() {
        return remainingTicks <= 0;
    }
}
