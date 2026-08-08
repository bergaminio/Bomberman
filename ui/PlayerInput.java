package ui;

import common.Action;

// Das Fenster meldet gedrueckte Tasten hierhin und weiss nicht, was damit
// passiert. Beim lokalen Spiel wird die Aktion sofort angewendet, beim
// Netzwerkspiel ueber den Socket geschickt. Dasselbe Fenster, zwei Welten.
public interface PlayerInput {
    void onAction(int playerIndex, Action action);
}
