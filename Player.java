package com.mijuego.entities;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import com.mijuego.utils.AudioManager;
import com.mijuego.map.TileMap;
import com.mijuego.map.Tile;
import com.mijuego.utils.CollisionManager;
import com.mijuego.utils.InputManager;
import com.mijuego.core.GS;
import com.mijuego.core.Camera;
import com.mijuego.utils.ResourceManager;

public class Player extends Entities {

    private TileMap map;
    private Color color = Color.BLUE;
    private int coins = 0; // contador de monedas

    private int damageCooldown = 0; // frames restantes hasta poder recibir daño otra vez
    private final int DAMAGE_COOLDOWN_FRAMES = 20;

    private boolean onGround = false;

    private final double GRAVITY = GS.DSC(0.2);
    private final double JUMP_SPEED = GS.DSC(-5);
    private final double MOVE_SPEED = GS.DSC(3);
    
    private BufferedImage spriteQuieto;
    private BufferedImage spriteQuieto2;
    private BufferedImage spriteMov;
    private BufferedImage spriteMov2;
    private BufferedImage spriteSalto;
    private BufferedImage spriteSalto2;
    private String lastDirection = "right"; // "left" or "right"
    private int animFrame = 0;
    private int animCounter = 0;
    private final int ANIM_SPEED = 7; // frames por cambio de sprite

    public Player(double x, double y, int width, int height, int health, TileMap map) {
        super(x, y, width, height, health); // health inicial en 3
        this.map = map;
        spriteQuieto = ResourceManager.loadImage("/assets/sprites/pinguinoQuieto.png");
        spriteMov = ResourceManager.loadImage("/assets/sprites/PinguinoMov.png");
        spriteQuieto2 = ResourceManager.loadImage("/assets/sprites/pinguinoQuieto2.png");
        spriteMov2 = ResourceManager.loadImage("/assets/sprites/PinguinoMov2.png");
        spriteSalto = ResourceManager.loadImage("/assets/sprites/pinguinoSalto.png");
        spriteSalto2 = ResourceManager.loadImage("/assets/sprites/pinguinoSalto2.png");
    }
    
    

    @Override
    public void update() {
        if (damageCooldown > 0) damageCooldown--;

        // Si el jugador ya no tiene vida, no actualizar más
        if (!isAlive()) {
        	this.width = 0;
        	this.height = 0;
            // Opcional: podrías agregar efectos de muerte aquí
            return;
        }

        // 🔹 Movimiento horizontal
        dx = 0;

        boolean moving = false;
        if (InputManager.isLeft())  { dx = -MOVE_SPEED; moving = true; lastDirection = "left"; }
        if (InputManager.isRight()) { dx = MOVE_SPEED; moving = true; lastDirection = "right"; }
        // Animación de pasos
        if (moving && onGround) {
            animCounter++;
            if (animCounter >= ANIM_SPEED) {
                animCounter = 0;
                animFrame = (animFrame + 1) % 2; // solo dos frames por dirección
            }
        } else {
            animFrame = 0;
            animCounter = 0;
        }

        // 🔹 Salto
        if (InputManager.isUp() && onGround) {
            dy = JUMP_SPEED;
            onGround = false;
            AudioManager.playJump();
        }

        // 🔹 Gravedad
        dy += GRAVITY;

        // 🔹 Colisiones por eje
        CollisionManager.checkTileCollisionX(this, map);
        x += dx;

        CollisionManager.checkTileCollisionY(this, map);
        y += dy;

        // 🔹 Actualizar onGround
        int tileSize = Tile.SIZE;
        int leftCol = (int)x / tileSize;
        int rightCol = (int)(x + width - 1) / tileSize;
        int bottomRow = (int)(y + height) / tileSize;

        onGround = false;
        for (int col = leftCol; col <= rightCol; col++) {
            if (map.isTileSolid(bottomRow, col)) {
                onGround = true;
                break;
            }
        }

        // 🔹 Chequear si toca tile mortal
        int topRow = (int)y / tileSize;
        for (int row = topRow; row <= bottomRow; row++) {
            for (int col = leftCol; col <= rightCol; col++) {
                if (map.getTile(row, col).isKill()) {
                    this.damage(this.getHealth());
                    AudioManager.playLose();// vida a 0
                    return;
                }
            }
        }
    }
    


    // Getter
    public int getCoins() {
        return coins;
    }

    public void addCoins(int amount) {
        coins += amount;
        AudioManager.playCoin();
    }

    public void takeDamage(int amount) {
        if (damageCooldown == 0) {
            damage(1);
            AudioManager.playHurt();// Solo baja 1 de health por daño de enemigo
            damageCooldown = DAMAGE_COOLDOWN_FRAMES;
            
            if (!isAlive()) {
                AudioManager.playLose();      // sonido perder
                AudioManager.stopMusic();     // cortar música de fondo
            }
        }
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    @Override
    public void draw(Graphics2D g, Camera camera) {
        if (!isAlive()) {
            // No dibujar nada si está muerto
            return;
        }
        BufferedImage spriteToDraw;
        if (!onGround) {
            // En el aire, mostrar sprite de salto según última dirección
            spriteToDraw = lastDirection.equals("right") ? spriteSalto : spriteSalto2;
        } else if (dx == 0) {
            // Quieto, mostrar sprite según última dirección
            spriteToDraw = lastDirection.equals("right") ? spriteQuieto : spriteQuieto2;
        } else if (dx > 0) {
            // Animación derecha
            spriteToDraw = (animFrame == 0) ? spriteQuieto : spriteMov;
        } else {
            // Animación izquierda
            spriteToDraw = (animFrame == 0) ? spriteQuieto2 : spriteMov2;
        }
        // Usar GS.SC para escalar el tamaño del sprite
        int drawWidth = GS.SC(20);
        int drawHeight = GS.SC(20);
        int drawX = (int)(x - camera.getX());
        int drawY = (int)(y - camera.getY());
        if (spriteToDraw != null) {
            g.drawImage(
                spriteToDraw,
                drawX,
                drawY,
                drawWidth, drawHeight,
                null
            );
        } else {
            g.setColor(color);
            g.fillRect(
                drawX,
                drawY,
                drawWidth, drawHeight
                );
        }
    }
}

