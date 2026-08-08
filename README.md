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
und startet danach.

**Lokal am selben PC (2 bis 4 Spieler, abwechselnd tippen):**

```powershell
.\run.ps1
```

**Mit eigener Map:**

```powershell
.\run.ps1 -ProgramArgs maps\arena.txt
```

Maps sind Textdateien: `#` unzerstoerbare Mauer, `o` zerstoerbarer Block,
`.` Boden. Der Rand muss geschlossen und alle vier Ecken erreichbar sein,
sonst lehnt der `MapValidator` sie ab.

**Uebers Netzwerk.** Ein PC startet den Server, alle spielen ueber Clients --
auch der Server-PC braucht einen eigenen Client:

```powershell
.\run.ps1 -MainClass network.GameServer -ProgramArgs 5555,2
```

Der Server zeigt beim Start alle Adressen an, unter denen er erreichbar ist.
Dann pro Spieler ein eigenes Konsolenfenster:

```powershell
.\run.ps1 -MainClass network.GameClient -ProgramArgs 127.0.0.1,5555
```

Fuer Mitspieler auf einem anderen PC statt `127.0.0.1` die Adresse
einsetzen, die der Server angezeigt hat.

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

| Taste | Wirkung |
|---|---|
| `w` `a` `s` `d` | ein Feld gehen |
| `b` | Bombe legen |
| `x` | eine Runde warten |
| `q` | aufgeben |

Das Spiel ist rundenbasiert: alle Spieler geben ihren Zug ein, danach laeuft
ein Tick -- Zuender zaehlen runter, faellige Bomben explodieren, Treffer
werden ausgewertet. Wer als Letzter lebt, gewinnt.

Zeichen auf dem Feld: `#` Mauer, `o` zerstoerbarer Block, `*` Feuer,
`.` Boden, `A`-`D` Spieler, `x` Toter. `A2` heisst: Spieler A steht auf
einer Bombe mit Zuender 2.