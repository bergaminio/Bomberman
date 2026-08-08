# Bomberman
To practice the OOP paradigma with JAVA
- No Copying Code from Ai
- Allowed to use Ai as guidance
- Have Fun
- Branches  
<img width="480" height="480" alt="KonataLuckyStarGIF" src="https://github.com/user-attachments/assets/8f8ac9a1-8c11-4443-a2c6-1ae8dd788857" />
   ## Ordnerstruktur

  ```text
  bomberman/
  ├─ Main.java
  ├─ common/
  │  ├─ Position.java
  │  └─ Direction.java
  ├─ game/
  │  ├─ Game.java
  │  └─ GameStatus.java
  ├─ player/
  │  ├─ Player.java
  │  └─ PlayerStatus.java
  ├─ map/
  │  ├─ GameMap.java
  │  ├─ Tile.java
  │  ├─ TileType.java
  │  ├─ Block.java
  │  └─ BlockType.java
  ├─ bomb/
  │  ├─ Bomb.java
  │  └─ Explosion.java
  ├─ service/
  │  ├─ GameService.java
  │  ├─ MovementService.java
  │  ├─ BombService.java
  │  └─ MapValidator.java
  ├─ persistence/
  │  ├─ HighscoreRepository.java
  │  └─ MapRepository.java
  ├─ ui/
  │  └─ ConsoleView.java
  └─ network/
     ├─ GameServer.java
     ├─ GameClient.java
     ├─ ClientHandler.java
     ├─ GameStateCodec.java
     └─ Message.java
  ```

  Dazu kamen `common/Action.java` und `persistence/HighscoreEntry.java`.

## Starten

Gebraucht wird ein JDK ab Version 17. `run.ps1` kompiliert alles nach `out/`
und startet danach. Swing ist im JDK enthalten, es muss nichts nachinstalliert
werden.

| Aufruf | Was passiert |
|---|---|
| `.\run.ps1` | Fenster, **Echtzeit**, 2 Spieler an einer Tastatur |
| `.\run.ps1 -ProgramArgs gui,4` | dasselbe mit 4 Spielern |
| `.\run.ps1 -ProgramArgs gui,2,maps\arena.txt` | mit eigener Map |
| `.\run.ps1 -ProgramArgs konsole` | Konsole, **rundenbasiert** |
| `.\run.ps1 -ProgramArgs server,5555,2` | Server fuers Netzwerkspiel |
| `.\run.ps1 -ProgramArgs client,127.0.0.1,5555` | Netzwerkspiel im Fenster |
| `.\run.ps1 -ProgramArgs client-konsole,127.0.0.1,5555` | Netzwerkspiel auf der Konsole |

Ein falscher Modus zeigt diese Liste an.

**Lokal ist Echtzeit, im Netzwerk rundenbasiert.** Im Fenster tickt ein Timer
alle 450 ms weiter, alle druecken gleichzeitig. Der Server dagegen wartet pro
Runde auf einen Zug von jedem -- das haelt den Konsolen-Client lauffaehig
(`Scanner.nextLine()` blockiert nun mal) und braucht ueber einen Hotspot keinen
Lag-Ausgleich.

Maps sind Textdateien: `#` unzerstoerbare Mauer, `o` zerstoerbarer Block,
`.` Boden. Der Rand muss geschlossen und alle vier Ecken erreichbar sein,
sonst lehnt der `MapValidator` sie ab.

**Eigene Maps** sind Textdateien: `#` unzerstoerbare Mauer, `o` zerstoerbarer
Block, `.` Boden. Der Rand muss geschlossen und alle vier Ecken erreichbar
sein, sonst lehnt der `MapValidator` sie ab.

**Uebers Netzwerk.** Ein PC startet den Server, alle spielen ueber Clients --
auch der Server-PC braucht einen eigenen Client. Der Server zeigt beim Start
alle Adressen an, unter denen er erreichbar ist. Fuer Mitspieler auf einem
anderen PC statt `127.0.0.1` diese Adresse einsetzen.

### Ueber einen Handy-Hotspot

Funktioniert ohne Portfreigabe, weil beide PCs im selben lokalen Netz haengen.
Internet wird nicht gebraucht -- der Hotspot muss nicht mal Daten haben.

1. Beide PCs mit demselben Hotspot verbinden.
2. Server starten. Er bindet automatisch auf alle Netzwerkkarten.
3. Aus der Adressliste des Servers die Zeile mit dem WLAN-Adapter nehmen.
4. Beim ersten Start fragt die Windows-Firewall, ob Java Verbindungen
   annehmen darf. **Fuer private Netzwerke erlauben** -- ohne das kommt
   niemand durch.
5. Die Clients auf den anderen PCs mit dieser Adresse starten.

## Steuerung

**Im Fenster**, alle an derselben Tastatur:

| Spieler | Bewegen | Bombe |
|---|---|---|
| A | `W` `A` `S` `D` | Leertaste |
| B | Pfeiltasten | Enter |
| C | `I` `J` `K` `L` | `U` |
| D | Numblock `8` `4` `5` `6` | Numblock `0` |

`N` startet ein neues Spiel, `Esc` beendet. Im Netzwerk-Client spielt jeder
mit `W A S D` und Leertaste, egal welche Farbe er hat.

**Auf der Konsole:** `w` `a` `s` `d` gehen, `b` Bombe, `x` warten, `q`
aufgeben.

Zeichen auf dem Konsolenfeld: `#` Mauer, `o` zerstoerbarer Block, `*` Feuer,
`.` Boden, `A`-`D` Spieler, `x` Toter. `A2` heisst: Spieler A steht auf einer
Bombe mit Zuender 2. Im Fenster erscheint dafuer ein kleines Abzeichen in der
Ecke des Feldes.

## Spielablauf

Zuender laufen 3 Ticks, die Explosion ist einen Tick sichtbar. Der Feuerstrahl
stoppt am Rand, vor unzerstoerbaren Mauern und an einem zerstoerbaren Block,
den er dabei wegsprengt. Bomben im Feuer gehen sofort mit hoch. Auf die eigene
Bombe kommt man nicht zurueck. Wer als Letzter lebt, gewinnt.