package ui;

import common.Action;

// Das Fenster meldet gedrueckte Tasten hierhin und weiss nicht, was damit
// passiert. Beim lokalen Spiel wird die Aktion sofort angewendet, beim
// Netzwerkspiel ueber den Socket geschickt. Dasselbe Fenster, zwei Welten.
public interface PlayerInput {

    void onPressed(int playerIndex, Action action);

    // Nur wer gehaltene Tasten auswertet, muss das ueberschreiben. Der
    // Netzwerk-Client schickt einen Zug pro Runde und interessiert sich
    // nicht dafuer, wann die Taste wieder losgelassen wird.
    default void onReleased(int playerIndex, Action action) {
    }
}
