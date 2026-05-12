import java.io.*;
import javax.sound.sampled.*;
import javax.swing.SwingUtilities;

public class NetworkThread extends Thread {
    private DataInputStream in;
    private GameFrame gameFrame;
    private Grid sharedArena;

    public NetworkThread(DataInputStream in, GameFrame gameFrame, Grid sharedArena) {
        this.in = in;
        this.gameFrame = gameFrame;
        this.sharedArena = sharedArena;
    }


    @Override
    public void run() {
        try {
            while (true) {
                String message = in.readUTF(); 
                System.out.println("[NetworkThread] Received: " + message);
                parseMessage(message);
            }
        } catch (IOException e) {
            System.out.println("Disconnected from server.");
            SwingUtilities.invokeLater(() -> gameFrame.showMessage("Disconnected from server."));
        }
    }

    private void playSound(String soundFile, float volume) {
    new Thread(() -> {
        try {
            File file = new File(soundFile);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);

            // --- VOLUME ADJUSTMENT ---
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                
                // Clamp volume between 0.0001 and 1.0 to avoid math errors
                float volumeLevel = Math.max(0.0001f, Math.min(1.0f, volume));
                
                // Convert linear scale (0.0 to 1.0) to Decibels
                float dB = (float) (Math.log(volumeLevel) / Math.log(10.0) * 20.0);
                gainControl.setValue(dB);
            }

            clip.start();
        } catch (Exception e) {
            System.out.println("Sound error: " + e.getMessage());
        }
    }).start();
    }

    private void parseMessage(String message) {
        String[] parts = message.split(":");

        switch (parts[0]) {
            case "START":
                SwingUtilities.invokeLater(() -> {
                    gameFrame.showMessage("Game Started! Move with Arrows/Space, Click to Fire!");
                    gameFrame.setGameActive(true); 
                });
                break;

            case "MOVE_SYNC":
                if (parts.length == 5) {
                    String shipName = parts[1];
                    int r = Integer.parseInt(parts[2]);
                    int c = Integer.parseInt(parts[3]);
                    boolean h = parts[4].equals("H");
                    
                    SwingUtilities.invokeLater(() -> {
                        Ship s = sharedArena.getShipByName(shipName);
                        if (s != null) {
                            sharedArena.placeShip(s, r, c, h);
                            gameFrame.repaintCanvas();
                        }
                    });
                }
                break;

            case "ATTACK_SYNC":
                if (parts.length == 4) {
                    int r = Integer.parseInt(parts[1]);
                    int c = Integer.parseInt(parts[2]);
                    String result = parts[3];
                    
                    SwingUtilities.invokeLater(() -> {
                        sharedArena.receiveAttack(r, c);

                        if (result.equals("HIT") || result.equals("MINE_HIT")) {
                            playSound("explosion.wav", 0.02f); // Make sure this file exists in your project folder
                            
                            // --- SCREEN SHAKE CHECK ---
                            // If the ship at these coordinates belongs to the player, shake the screen
                            for (Ship ship : sharedArena.getShips()) {
                                if (ship.occupies(r, c) && ship.getName().equals(gameFrame.getControlledShipName())) {
                                    gameFrame.triggerScreenShake();
                                    break;
                                }
                            }
                            
                            if (result.equals("HIT")) gameFrame.addExplosion(r, c);
                            else gameFrame.addMineExplosion(r, c);
                        } else {
                            playSound("splash.wav", 0.02f); // Make sure this file exists
                            gameFrame.addExplosion(r, c); 
                        }
                        gameFrame.repaintCanvas();
                    });
                }
                break;

            case "TILE_SYNC":
                if (parts.length == 4) {
                    int tr = Integer.parseInt(parts[1]);
                    int tc = Integer.parseInt(parts[2]);
                    int type = Integer.parseInt(parts[3]);
                    SwingUtilities.invokeLater(() -> {
                        sharedArena.setTileStatus(tr, tc, type);
                        gameFrame.repaintCanvas();
                    });
                }
                break;

            case "SMOKE_SYNC":
                if (parts.length == 3) {
                    String shipName = parts[1];
                    boolean isSmoked = parts[2].equals("ON");
                    SwingUtilities.invokeLater(() -> {
                        Ship s = sharedArena.getShipByName(shipName);
                        if (s != null) {
                            s.setSmoked(isSmoked);
                            gameFrame.repaintCanvas();
                        }
                    });
                }
                break;

            case "GAMEOVER":
                if (parts.length == 2) {
                    int winnerNumber = Integer.parseInt(parts[1]);
                    SwingUtilities.invokeLater(() -> {
                        gameFrame.setGameActive(false); 
                        int myNumber = gameFrame.getPlayerNumber();
                        if (winnerNumber == myNumber) {
                            gameFrame.showMessage("YOU WIN! Enemy ship sunk!");
                        } else {
                            gameFrame.showMessage("You lose. Opponent sunk your ship.");
                        }
                        gameFrame.repaintCanvas();
                    });
                }
                break;

            default:
                System.out.println("[NetworkThread] Unknown message: " + message);
                break;
        }
    }
}