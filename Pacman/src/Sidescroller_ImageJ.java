import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import ij.plugin.*;
import java.util.ArrayList;

/**
 * ImageJ Jump & Run Sidescroller - Very Simple - Basic concepts
 * Controls: Arrow Keys (Left/Right), Spacebar (Jump)
 */
public class Sidescroller_ImageJ implements PlugIn, KeyListener {
    private final int W = 800, H = 400;
    private final int TILE = 40;
    private boolean running = true;
    
    // Physics Parameters
    private double gravity = 0.8;
    private double jumpStrength = -14.0;
    private double scrollX = 0;

    class Entity {
        double x, y, dx, dy;
        int w = 30, h = 40;
        boolean onGround = false;
        Entity(int x, int y) { this.x = x; this.y = y; }
    }

    private Entity player;
    private ArrayList<Rectangle> platforms = new ArrayList<>();
    private boolean[] keys = new boolean[256];

    public void run(String arg) {
        setupLevel();
        ColorProcessor cp = new ColorProcessor(W, H);
        ImagePlus imp = new ImagePlus("IJ Sidescroller", cp);
        imp.show();
        
        imp.getCanvas().addKeyListener(this);
        imp.getCanvas().requestFocus();

        // Main game loop
        while (running) {
            if (imp.getWindow() == null || imp.getWindow().isClosed()) break;
            
            updatePhysics();
            draw(imp, cp);
            IJ.wait(25);
        }
    }

    private void setupLevel() {
        player = new Entity(100, 200);
        // Ground and Platforms
        platforms.add(new Rectangle(0, H - 40, 2000, 40)); // Long floor
        platforms.add(new Rectangle(300, 280, 150, 20));
        platforms.add(new Rectangle(500, 200, 150, 20));
        platforms.add(new Rectangle(750, 280, 200, 20));
        platforms.add(new Rectangle(1100, 220, 100, 20));
    }

    private void updatePhysics() {
        // Horizontal movement
        if (keys[KeyEvent.VK_LEFT]) player.dx = -6;
        else if (keys[KeyEvent.VK_RIGHT]) player.dx = 6;
        else player.dx = 0;

        // Jump Logic
        if (keys[KeyEvent.VK_SPACE] && player.onGround) {
            player.dy = jumpStrength;
            player.onGround = false;
        }

        // Apply Gravity
        player.dy += gravity;
        
        // Calculate predicted positions for collision detection
        double nextX = player.x + player.dx;
        double nextY = player.y + player.dy;
        
        player.onGround = false;

        // Simple AABB Collision Detection
        for (Rectangle p : platforms) {
            // Horizontal Collision (Walls)
            if (nextX < p.x + p.width && nextX + player.w > p.x &&
                player.y < p.y + p.height && player.y + player.h > p.y) {
                player.dx = 0;
            }
            
            // Vertical Collision (Floor/Ceiling)
            if (player.x < p.x + p.width && player.x + player.w > p.x &&
                nextY < p.y + p.height && nextY + player.h > p.y) {
                
                if (player.dy > 0) { // Landing
                    player.y = p.y - player.h;
                    player.dy = 0;
                    player.onGround = true;
                } else if (player.dy < 0) { // Hitting head
                    player.y = p.y + p.height;
                    player.dy = 0;
                }
                nextY = player.y;
            }
        }

        player.x += player.dx;
        player.y += player.dy;

        // Camera Scrolling (Keep player centered)
        scrollX = player.x - W / 2;
        if (scrollX < 0) scrollX = 0;
        
        // Death by falling
        if (player.y > H) {
            player.x = 100;
            player.y = 200;
            player.dy = 0;
        }
    }

    private void draw(ImagePlus imp, ColorProcessor cp) {
        cp.setColor(new Color(135, 206, 235)); // Sky Blue
        cp.fill();
        
        Overlay ov = new Overlay();

        // Draw platforms (relative to scroll position)
        for (Rectangle p : platforms) {
            Roi r = new Roi(p.x - scrollX, p.y, p.width, p.height);
            r.setFillColor(new Color(34, 139, 34)); // Forest Green
            ov.add(r);
        }

        // Draw player
        Roi pRoi = new Roi(player.x - scrollX, player.y, player.w, player.h);
        pRoi.setFillColor(Color.RED);
        ov.add(pRoi);

        imp.setOverlay(ov);
    }

    public void keyPressed(KeyEvent e) { if(e.getKeyCode() < 256) keys[e.getKeyCode()] = true; }
    public void keyReleased(KeyEvent e) { if(e.getKeyCode() < 256) keys[e.getKeyCode()] = false; }
    public void keyTyped(KeyEvent e) {}
}
