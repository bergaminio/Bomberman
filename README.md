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
     └─ Message.java
  ```