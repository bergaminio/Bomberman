package ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import javax.swing.JPanel;

import bomb.Bomb;
import bomb.Explosion;
import common.Position;
import game.Game;
import map.GameMap;
import map.Tile;
import player.Player;

// Zeichnet den Spielzustand. Genau wie die ConsoleView liest diese Klasse
// nur - sie ruft nichts auf, was am Spiel etwas aendert.
//
// Die Spiellogik bleibt rasterbasiert: ein Spieler steht immer auf genau
// einem Feld. Fluessig wird es allein hier, indem zwischen dem Feld von
// vorhin und dem von jetzt interpoliert wird.
public class GamePanel extends JPanel {
    public static final int CELL = 44;

    private static final Color BACKGROUND = new Color(22, 24, 30);
    private static final Color GROUND_LIGHT = new Color(46, 50, 60);
    private static final Color GROUND_DARK = new Color(40, 44, 53);
    private static final Color WALL = new Color(88, 96, 112);
    private static final Color WALL_TOP = new Color(116, 126, 145);
    private static final Color CRATE = new Color(150, 98, 58);
    private static final Color CRATE_EDGE = new Color(106, 66, 36);
    private static final Color OUTLINE = new Color(16, 18, 24);

    private static final Color[] PLAYER_COLORS = {
        new Color(74, 158, 255),
        new Color(255, 92, 92),
        new Color(92, 216, 124),
        new Color(255, 206, 74)
    };

    private Game game;

    // Wo die Spieler beim letzten Tick standen und wo jetzt. Dazwischen
    // wird gezeichnet, je nach progress.
    private Position[] from;
    private Position[] to;
    private float progress = 1f;

    public GamePanel() {
        setBackground(BACKGROUND);
        setFocusable(true);
    }

    // Zustand sofort zeigen, ohne Animation. Fuer den Netzwerk-Client, der
    // rundenweise einen fertigen Stand bekommt.
    public void setGame(Game game) {
        this.game = game;
        this.to = snapshot(game);
        this.from = to;
        this.progress = 1f;

        revalidate();
        repaint();
    }

    // Nach einem Logik-Tick: die bisherigen Positionen werden zum Startpunkt
    // der Bewegung, die neuen zum Ziel.
    public void advanceTo(Game game) {
        Position[] previous = this.to;

        this.game = game;
        this.to = snapshot(game);
        this.from = previous != null && previous.length == to.length ? previous : to;
        this.progress = 0f;

        revalidate();
    }

    // 0 = gerade getickt, 1 = Ziel erreicht.
    public void setProgress(float progress) {
        this.progress = Math.max(0f, Math.min(1f, progress));
    }

    public static Color colorForPlayer(int index) {
        return PLAYER_COLORS[index % PLAYER_COLORS.length];
    }

    @Override
    public Dimension getPreferredSize() {
        if (game == null) {
            return new Dimension(13 * CELL, 11 * CELL);
        }

        return new Dimension(game.getMap().getWidth() * CELL, game.getMap().getHeight() * CELL);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        if (game == null) {
            return;
        }

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Reihenfolge wie in der ConsoleView: erst der Boden, dann was
        // darauf liegt, zuletzt die Spieler. Wer zuletzt zeichnet, gewinnt.
        drawMap(g);
        drawExplosions(g);
        drawBombs(g);
        drawPlayers(g);
        drawFuseBadges(g);

        g.dispose();
    }

    private Position[] snapshot(Game game) {
        Position[] positions = new Position[game.getPlayers().size()];

        for (int i = 0; i < positions.length; i++) {
            positions[i] = game.getPlayers().get(i).getPosition();
        }

        return positions;
    }

    private void drawMap(Graphics2D g) {
        GameMap map = game.getMap();

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                Tile tile = map.getTile(new Position(x, y));
                int px = x * CELL;
                int py = y * CELL;

                if (tile.getBlock() == null) {
                    g.setColor((x + y) % 2 == 0 ? GROUND_LIGHT : GROUND_DARK);
                    g.fillRect(px, py, CELL, CELL);
                    continue;
                }

                if (tile.getBlock().isDestroyable()) {
                    drawCrate(g, px, py);
                } else {
                    drawWall(g, px, py);
                }
            }
        }
    }

    private void drawWall(Graphics2D g, int px, int py) {
        g.setColor(WALL);
        g.fillRect(px, py, CELL, CELL);

        // Heller Rand oben links: billiger 3D-Effekt.
        g.setColor(WALL_TOP);
        g.fillRect(px, py, CELL, 4);
        g.fillRect(px, py, 4, CELL);

        g.setColor(BACKGROUND);
        g.drawRect(px, py, CELL - 1, CELL - 1);
    }

    private void drawCrate(Graphics2D g, int px, int py) {
        g.setColor(GROUND_DARK);
        g.fillRect(px, py, CELL, CELL);

        g.setColor(CRATE);
        g.fillRoundRect(px + 3, py + 3, CELL - 6, CELL - 6, 8, 8);

        g.setColor(CRATE_EDGE);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(px + 3, py + 3, CELL - 6, CELL - 6, 8, 8);
        g.drawLine(px + 3, py + CELL / 2, px + CELL - 3, py + CELL / 2);
    }

    private void drawExplosions(Graphics2D g) {
        for (Explosion explosion : game.getExplosions()) {

            // Faellt von 1 auf 0, waehrend die Explosion ablaeuft. Das Feuer
            // verglueht dadurch, statt schlagartig zu verschwinden.
            float life = (explosion.getRemainingTicks() - progress)
                / (float) explosion.getDurationTicks();
            life = Math.max(0f, Math.min(1f, life));

            if (life <= 0f) {
                continue;
            }

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, life));

            for (Position position : explosion.getAffectedPositions()) {
                int px = position.getX() * CELL;
                int py = position.getY() * CELL;

                // Der Radius reicht ueber die Zelle hinaus, damit benachbarte
                // Feuerfelder ineinander uebergehen statt als einzelne Lampen
                // zu wirken.
                Point2D centre = new Point2D.Float(px + CELL / 2f, py + CELL / 2f);
                RadialGradientPaint fire = new RadialGradientPaint(
                    centre, CELL * (0.6f + 0.35f * life),
                    new float[] { 0f, 0.35f, 0.7f, 1f },
                    new Color[] {
                        new Color(255, 248, 214),
                        new Color(255, 186, 64),
                        new Color(238, 108, 28),
                        new Color(180, 50, 16)
                    });

                g.setPaint(fire);
                g.fillRect(px, py, CELL, CELL);
            }

            g.setComposite(AlphaComposite.SrcOver);
        }
    }

    private void drawBombs(Graphics2D g) {
        for (Bomb bomb : game.getBombs()) {
            double px = bomb.getPosition().getX() * CELL;
            double py = bomb.getPosition().getY() * CELL;

            double size = CELL * bombScale(bomb);
            double offset = (CELL - size) / 2;

            g.setColor(new Color(18, 18, 22));
            g.fill(new Ellipse2D.Double(px + offset, py + offset, size, size));

            g.setColor(new Color(255, 120, 60));
            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Double(px + offset, py + offset, size, size));

            drawCentred(g, String.valueOf(bomb.getFuseTicks()),
                (float) px + CELL / 2f, (float) py + CELL / 2f, 14, Color.WHITE);
        }
    }

    // Ein Herzschlag pro Tick, der staerker wird je naeher die Explosion ist.
    private double bombScale(Bomb bomb) {
        double beat = Math.sin(progress * Math.PI);
        double urgency = bomb.getFuseTicks() >= 4 ? 0 : (4 - bomb.getFuseTicks()) / 4.0;

        return 0.62 + beat * (0.05 + 0.12 * urgency);
    }

    private void drawPlayers(Graphics2D g) {
        for (int i = 0; i < game.getPlayers().size(); i++) {
            Player player = game.getPlayers().get(i);

            double px = interpolate(from[i].getX(), to[i].getX()) * CELL;
            double py = interpolate(from[i].getY(), to[i].getY()) * CELL;

            if (!player.isAlive()) {
                drawDead(g, to[i].getX() * CELL, to[i].getY() * CELL);
                continue;
            }

            double size = CELL - 14;
            double offset = (CELL - size) / 2;

            // Schatten, damit die Figur auf dem Boden steht statt zu schweben.
            g.setColor(new Color(0, 0, 0, 70));
            g.fill(new Ellipse2D.Double(px + offset + 2, py + offset + 5, size, size));

            g.setColor(colorForPlayer(i));
            g.fill(new Ellipse2D.Double(px + offset, py + offset, size, size));

            g.setColor(OUTLINE);
            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Double(px + offset, py + offset, size, size));

            drawCentred(g, String.valueOf((char) ('A' + i)),
                (float) px + CELL / 2f, (float) py + CELL / 2f, 15, OUTLINE);
        }
    }

    private void drawDead(Graphics2D g, int px, int py) {
        g.setColor(new Color(110, 110, 118));
        g.setStroke(new BasicStroke(4f));
        g.drawLine(px + 12, py + 12, px + CELL - 12, py + CELL - 12);
        g.drawLine(px + CELL - 12, py + 12, px + 12, py + CELL - 12);
    }

    // Sanftes Ein- und Ausschwingen statt gleichmaessigem Rutschen.
    private double interpolate(int fromValue, int toValue) {
        if (fromValue == toValue) {
            return fromValue;
        }

        double eased = progress * progress * (3 - 2 * progress);
        return fromValue + (toValue - fromValue) * eased;
    }

    // Steht ein Spieler auf seiner Bombe, verdeckt sein Kreis den Zuender.
    // Dasselbe Problem hat die ConsoleView mit der zweiten Zeichenspalte
    // geloest ("A2"); hier kommt ein kleines Abzeichen in die Ecke.
    private void drawFuseBadges(Graphics2D g) {
        for (Bomb bomb : game.getBombs()) {
            if (!isPlayerOn(bomb.getPosition())) {
                continue;
            }

            int px = bomb.getPosition().getX() * CELL;
            int py = bomb.getPosition().getY() * CELL;
            int size = 17;

            g.setColor(new Color(18, 18, 22));
            g.fillOval(px + 2, py + 2, size, size);

            g.setColor(new Color(255, 140, 60));
            g.setStroke(new BasicStroke(2f));
            g.drawOval(px + 2, py + 2, size, size);

            drawCentred(g, String.valueOf(bomb.getFuseTicks()),
                px + 2 + size / 2f, py + 2 + size / 2f, 11, Color.WHITE);
        }
    }

    private boolean isPlayerOn(Position position) {
        for (Player player : game.getPlayers()) {
            if (player.isAlive() && player.getPosition().equals(position)) {
                return true;
            }
        }
        return false;
    }

    private void drawCentred(Graphics2D g, String text, float centreX, float centreY,
                             int size, Color color) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size));
        FontMetrics metrics = g.getFontMetrics();

        float x = centreX - metrics.stringWidth(text) / 2f;
        float y = centreY + (metrics.getAscent() - metrics.getDescent()) / 2f;

        g.setColor(color);
        g.drawString(text, x, y);
    }
}
