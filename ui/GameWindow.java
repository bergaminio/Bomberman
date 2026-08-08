package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import common.Action;
import common.Direction;
import game.Game;
import player.Player;

// Das Fenster: zeichnen lassen, Tasten entgegennehmen, Text anzeigen.
// Es kennt weder GameService noch Sockets - was mit einer Taste passiert,
// entscheidet der PlayerInput, den es hereingereicht bekommt.
public class GameWindow extends JFrame {
    private final GamePanel panel = new GamePanel();
    private final JLabel status = new JLabel(" ");
    private final JLabel hint = new JLabel(" ");
    private final PlayerInput input;

    public GameWindow(String title, PlayerInput input) {
        super(title);
        this.input = input;

        status.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        status.setForeground(new Color(226, 230, 238));
        status.setBorder(BorderFactory.createEmptyBorder(8, 12, 2, 12));

        hint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        hint.setForeground(new Color(150, 158, 172));
        hint.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));

        JLabel[] labels = { status, hint };
        javax.swing.JPanel bottom = new javax.swing.JPanel(new java.awt.GridLayout(2, 1));
        bottom.setBackground(new Color(22, 24, 30));
        for (JLabel label : labels) {
            bottom.add(label);
        }

        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
    }

    public void open() {
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        panel.requestFocusInWindow();
    }

    // Key Bindings statt KeyListener: die funktionieren unabhaengig davon,
    // welche Komponente gerade den Fokus hat. Ein KeyListener auf dem Panel
    // wuerde stumm aufhoeren zu reagieren, sobald der Fokus wandert.
    public void bindPlayer(int playerIndex, PlayerKeys keys) {
        bind(keys.getUp(), playerIndex, Action.move(Direction.UP));
        bind(keys.getDown(), playerIndex, Action.move(Direction.DOWN));
        bind(keys.getLeft(), playerIndex, Action.move(Direction.LEFT));
        bind(keys.getRight(), playerIndex, Action.move(Direction.RIGHT));
        bind(keys.getBomb(), playerIndex, Action.BOMB);
    }

    public void bindCommand(int keyCode, String name, Runnable command) {
        inputMap().put(KeyStroke.getKeyStroke(keyCode, 0), name);
        actionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                command.run();
            }
        });
    }

    // Darf aus jedem Thread aufgerufen werden. invokeLater schiebt die
    // Arbeit auf den Event-Dispatch-Thread, dem in Swing alle Komponenten
    // gehoeren. Direktes Zeichnen aus einem Netzwerkthread waere ein Fehler,
    // der sich nur gelegentlich und schwer reproduzierbar zeigt.
    public void showState(Game game, String statusText, String hintText) {
        SwingUtilities.invokeLater(() -> {
            panel.setGame(game);
            status.setText(statusText);
            hint.setText(hintText);
        });
    }

    public void showHint(String hintText) {
        SwingUtilities.invokeLater(() -> hint.setText(hintText));
    }

    public void showStatus(String statusText) {
        SwingUtilities.invokeLater(() -> status.setText(statusText));
    }

    public static String describePlayers(Game game) {
        StringBuilder line = new StringBuilder();

        for (int i = 0; i < game.getPlayers().size(); i++) {
            Player player = game.getPlayers().get(i);

            if (line.length() > 0) {
                line.append("    ");
            }

            line.append((char) ('A' + i)).append(' ').append(player.getName());
            line.append(player.isAlive()
                ? " [" + player.getActiveBombs() + "/" + player.getBombCapacity() + "]"
                : " tot");
        }

        return line.toString();
    }

    public static String describeKeys(List<PlayerKeys> keys, int count) {
        StringBuilder line = new StringBuilder();

        for (int i = 0; i < count && i < keys.size(); i++) {
            if (line.length() > 0) {
                line.append("    ");
            }
            line.append((char) ('A' + i)).append(": ").append(keys.get(i).getDescription());
        }

        return line.toString();
    }

    private void bind(int keyCode, int playerIndex, Action action) {
        String name = "player" + playerIndex + "-key" + keyCode;

        inputMap().put(KeyStroke.getKeyStroke(keyCode, 0), name);
        actionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                input.onAction(playerIndex, action);
            }
        });
    }

    private InputMap inputMap() {
        return panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private ActionMap actionMap() {
        return panel.getActionMap();
    }
}
