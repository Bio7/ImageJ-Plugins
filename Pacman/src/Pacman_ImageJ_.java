/**
 * PACMAN IJ - ImageJ Plugin
 * 
 * GENERAL DESCRIPTION:
 * This plugin implements a classic Pacman game within ImageJ. 
 * Features include:
 * - Dynamic level loading via String arrays.
 * - Warp tunnels that allow entities to cross from one side of the screen to the other.
 * - Advanced ghost rendering with body extensions and movement-tracking eyes.
 * - Power-pellet system (Power Timer) allowing Pacman to eat ghosts.
 * 
 * CONTROLS:
 * [S] - Start Game
 * [P] - Pause / Resume
 * [Arrow Keys] - Move Pacman
 * [ESC] - Exit Plugin (standard ImageJ behavior)
 */

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import ij.plugin.*;

public class Pacman_ImageJ_ implements PlugIn, KeyListener {
    private final int TILE = 26, ROWS = 21, COLS = 21;
    private final int ARENA_W = COLS * TILE, SIDE_W = 160;
    private final int W = ARENA_W + SIDE_W, H = ROWS * TILE;

    private int score = 0, lives = 3, level = 1, powerTimer = 0;
    private int[][] map = new int[ROWS][COLS]; 
    private boolean running = true, paused = false, gameStarted = false;
    private boolean up, down, left, right;
    private int frame = 0;

    class Entity {
        int x, y, dx, dy, nextDx, nextDy;
        Entity(int cx, int cy) { x = cx * TILE; y = cy * TILE; }
    }

    private Entity pac;
    private ArrayList<Entity> ghosts = new ArrayList<>();
    private ColorProcessor cp;

    public void run(String arg) {
        IJ.resetEscape();
        setupLevel(true);
        cp = new ColorProcessor(W, H);
        drawWalls();
        
        ImagePlus imp = new ImagePlus("Pacman ImageJ - Level " + level, cp);
        imp.show();

        ImageCanvas canvas = imp.getCanvas();
        canvas.addKeyListener(this);
        canvas.requestFocus();

        while (running && !IJ.escapePressed()) {
            if (imp.getWindow() == null || imp.getWindow().isClosed()) break;
            if (gameStarted && !paused) {
                handleMovement();
                updateLogic();
                checkLevelComplete();
            }
            updateOverlay(imp);
            IJ.wait(40);
            frame++;
            if (gameStarted && !paused && powerTimer > 0) powerTimer--;
            if (lives <= 0) {
                IJ.showMessage("GAME OVER", "Final Score: " + score);
                gameStarted = false;
                setupLevel(true);
            }
        }
    }

    /**
     * LEVEL DATA DEFINITION
     * Includes the new Level 4 with a giant "P" pattern in the center.
     */
    /**
     * LEVEL DATA DEFINITION
     * Each array must have EXACTLY 21 strings, each 21 characters long.
     */
    /**
     * LEVEL DATA DEFINITION
     * This block is designed to be visually readable like the actual game maze.
     * '#' = Wall, '.' = Dot, '*' = Power Pellet, 'P' = Pacman, 'G' = Ghost spawn.
     */
    private String[] getLevelData(int lvl) {
        String[][] mazes = {
            { // --- LEVEL 1: CLASSIC ---
                "#####################",
                "#.........#.........#",
                "#.###.###.#.###.###.#",
                "#*# #.# #.#.# #.# #*#",
                "#.###.###.#.###.###.#",
                "#...................#",
                "#.###.#.#####.#.###.#",
                "#.....#...#...#.....#",
                "#####.### # ###.#####",
                "    #.#  GGG  #.#    ",
                "      .  GGG  .      ",
                "#####.# ##### #.#####",
                "    #.#       #.#    ",
                "#####.# ##### #.#####",
                "#.........#.........#",
                "#.###.###.#.###.###.#",
                "#*..#.....P.....#..*#",
                "###.#.#.#####.#.#.###",
                "#.....#...#...#.....#",
                "#.#################.#",
                "#####################" 
            },
            { // --- LEVEL 2: THE HUB ---
                "#####################",
                "#*........#........*#",
                "#.#######.#.#######.#",
                "#.#######.#.#######.#",
                "#.........#.........#",
                "#.###.#########.###.#",
                "#.#...#.......#...#.#",
                "#.#.#.#.#####.#.#.#.#",
                "#...#...........#...#",
                "###.#.### G ###.#.###",
                "      .  GGG  .      ",
                "###.#.#########.#.###",
                "#...#...........#...#",
                "#.###.#########.###.#",
                "#.........#.........#",
                "#.#######.#.#######.#",
                "#*......#.P.#......*#",
                "#######.#.#.#.#######",
                "#.......#...#.......#",
                "#.#################.#",
                "#####################" 
            },
            { // --- LEVEL 3: THE CROSS ---
                "#####################",
                "#.........#.........#",
                "#*#######...#######*#",
                "#.#######.#.#######.#",
                "#.........#.........#",
                "#####.#########.#####",
                "    #.#  GGG  #.#    ",
                "#####.# ##### #.#####",
                "#.........#.........#",
                "#.#######.#.#######.#",
                "      .   P   .      ",
                "#.#######.#.#######.#",
                "#.........#.........#",
                "#####.# ##### #.#####",
                "    #.#       #.#    ",
                "#####.#########.#####",
                "#.........#.........#",
                "#.#######.#.#######.#",
                "#*#######...#######*#",
                "#.........G.........#",
                "#####################" 
            },
            { // --- LEVEL 4: THE BIG P ---
                "#####################",
                "#*.......###.......*#",
                "#.######.###.######.#",
                "#.######.....######.#",
                "#.#      ###      #.#",
                "#.# #####   ##### #.#",
                "#.# #   # G #   # #.#",
                "#.# # ####### # # #.#",
                "#.# # #     # # # #.#",
                "      # ##### #      ",
                "#.# # # G G G # # #.#",
                "#.# # # ##### # # #.#",
                "#.# # #       # # #.#",
                "#.# # ######### # #.#",
                "#.# #           # #.#",
                "#.# ############# #.#",
                "#*...............P..#",
                "###.#############.###",
                "#...................#",
                "#.#################.#",
                "#####################" 
            }
        };

        return mazes[(lvl - 1) % mazes.length];
    }


    private void setupLevel(boolean resetAll) {
        if (resetAll) { score = 0; lives = 3; level = 1; }
        powerTimer = 0;
        String[] layout = getLevelData(level);
        for (int r = 0; r < ROWS; r++) {
            String line = layout[r];
            for (int c = 0; c < COLS; c++) {
                char tile = (c < line.length()) ? line.charAt(c) : ' ';
                if (tile == '#') map[r][c] = 0;
                else if (tile == '.') map[r][c] = 2;
                else if (tile == '*') map[r][c] = 3;
                else map[r][c] = 1;
                if (tile == 'P') pac = new Entity(c, r);
            }
        }
        ghosts.clear();
        for (int i = 0; i < 4; i++) ghosts.add(new Entity(10, 10));
        up = down = left = right = false;
        if (cp != null) drawWalls();
    }

    private void handleMovement() {
        if (up) { pac.nextDx = 0; pac.nextDy = -1; }
        else if (down) { pac.nextDx = 0; pac.nextDy = 1; }
        else if (left) { pac.nextDx = -1; pac.nextDy = 0; }
        else if (right) { pac.nextDx = 1; pac.nextDy = 0; }
    }

    private void updateLogic() {
        if (pac.x <= -TILE) pac.x = ARENA_W - 2;
        else if (pac.x >= ARENA_W) pac.x = -TILE + 2;

        if (pac.x % TILE == 0 && pac.y % TILE == 0) {
            if (canMove(pac.x, pac.y, pac.nextDx, pac.nextDy)) {
                pac.dx = pac.nextDx; pac.dy = pac.nextDy;
            } else if (!canMove(pac.x, pac.y, pac.dx, pac.dy)) {
                pac.dx = 0; pac.dy = 0;
            }
            int r = pac.y / TILE, c = pac.x / TILE;
            if (c >= 0 && c < COLS && r >= 0 && r < ROWS) {
                if (map[r][c] == 2) { map[r][c] = 1; score += 10; }
                if (map[r][c] == 3) { map[r][c] = 1; score += 50; powerTimer = 150; }
            }
        }
        pac.x += pac.dx * 2; pac.y += pac.dy * 2;

        for (Entity g : ghosts) {
            if (g.x <= -TILE) g.x = ARENA_W - 2;
            else if (g.x >= ARENA_W) g.x = -TILE + 2;
            if (g.x % TILE == 0 && g.y % TILE == 0) {
                int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}};
                ArrayList<int[]> pos = new ArrayList<>();
                for (int[] d : dirs) if (canMove(g.x, g.y, d[0], d[1])) pos.add(d);
                if (!pos.isEmpty()) {
                    int[] p = pos.get((int)(Math.random() * pos.size()));
                    g.dx = p[0]; g.dy = p[1];
                }
            }
            int s = (powerTimer > 0) ? 1 : 2;
            g.x += g.dx * s; g.y += g.dy * s;
            if (Math.abs(pac.x - g.x) < TILE*0.7 && Math.abs(pac.y - g.y) < TILE*0.7) {
                if (powerTimer > 0) { g.x = 10 * TILE; g.y = 10 * TILE; score += 200; }
                else { lives--; setupLevel(false); IJ.wait(600); return; }
            }
        }
    }

    private boolean canMove(int x, int y, int dx, int dy) {
        int nx = (x / TILE) + dx;
        int ny = (y / TILE) + dy;
        // Erlaube Tunnel (außerhalb der COLS), wenn die aktuelle Zeile passierbar ist
        if (nx < 0 || nx >= COLS) return (ny >= 0 && ny < ROWS);
        if (ny < 0 || ny >= ROWS) return false;
        return map[ny][nx] != 0;
    }

    private void checkLevelComplete() {
        boolean dots = false;
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++) if (map[r][c] > 1) dots = true;
        if (!dots) { level++; setupLevel(false); IJ.wait(1000); }
    }

    private void drawWalls() {
        cp.setColor(Color.BLACK); cp.fill();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (map[r][c] == 0) {
                    cp.setColor(new Color(0, 0, 150)); cp.fillRect(c * TILE, r * TILE, TILE, TILE);
                    cp.setColor(new Color(0, 180, 255)); cp.drawRect(c * TILE + 3, r * TILE + 3, TILE - 6, TILE - 6);
                }
            }
        }
    }

    private void updateOverlay(ImagePlus imp) {
        Overlay ov = new Overlay();
        if (!gameStarted) {
            TextRoi tr = new TextRoi(ARENA_W/2-100, H/2-40, "PACMAN ImageJ\n'S' TO START");
            tr.setStrokeColor(Color.YELLOW); tr.setFont(new Font("Monospaced", Font.BOLD, 22)); ov.add(tr);
        } else {
            for (int r = 0; r < ROWS; r++)
                for (int c = 0; c < COLS; c++) {
                    if (map[r][c] == 2) ov.add(new OvalRoi(c*TILE+11, r*TILE+11, 4, 4){{setFillColor(Color.WHITE);}});
                    else if (map[r][c] == 3) ov.add(new OvalRoi(c*TILE+7, r*TILE+7, 12, 12){{setFillColor(Color.WHITE);}});
                }
            int open = (frame % 6 < 3) ? 35 : 0;
            int ang = (pac.dx < 0) ? 180 : (pac.dy < 0) ? 90 : (pac.dy > 0) ? 270 : 0;
            ShapeRoi pr = new ShapeRoi(new Arc2D.Double(pac.x+2, pac.y+2, TILE-4, TILE-4, ang+open, 360-2*open, Arc2D.PIE));
            pr.setFillColor(Color.YELLOW); ov.add(pr);

            Color[] gc = {Color.RED, Color.PINK, Color.CYAN, Color.ORANGE};
            for (int i=0; i<ghosts.size(); i++) {
                Entity g = ghosts.get(i);
                Color col = (powerTimer > 0) ? (powerTimer < 40 && frame % 4 < 2 ? Color.WHITE : Color.BLUE) : gc[i%4];
                ov.add(new OvalRoi(g.x+3, g.y+2, TILE-6, TILE-8){{setFillColor(col);}});
                ov.add(new Roi(g.x+3, g.y+10, TILE-6, 10){{setFillColor(col);}});
                ov.add(new OvalRoi(g.x+6+g.dx*2, g.y+6+g.dy*2, 6, 6){{setFillColor(Color.WHITE);}});
                ov.add(new OvalRoi(g.x+14+g.dx*2, g.y+6+g.dy*2, 6, 6){{setFillColor(Color.WHITE);}});
                ov.add(new OvalRoi(g.x+8+g.dx*4, g.y+8+g.dy*4, 3, 3){{setFillColor(Color.BLACK);}});
                ov.add(new OvalRoi(g.x+16+g.dx*4, g.y+8+g.dy*4, 3, 3){{setFillColor(Color.BLACK);}});
            }
            TextRoi stat = new TextRoi(ARENA_W+20, 40, "SCORE: " + score + "\nLIVES: " + lives + "\nLEVEL: " + level);
            stat.setStrokeColor(Color.WHITE); stat.setFont(new Font("Monospaced", Font.BOLD, 18)); ov.add(stat);
        }
        imp.setOverlay(ov);
    }

    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_S) gameStarted = true;
        if (k == KeyEvent.VK_P) paused = !paused;
        if (k == KeyEvent.VK_UP) { up=true; down=false; left=false; right=false; }
        if (k == KeyEvent.VK_DOWN) { up=false; down=true; left=false; right=false; }
        if (k == KeyEvent.VK_LEFT) { up=false; down=false; left=true; right=false; }
        if (k == KeyEvent.VK_RIGHT) { up=false; down=false; left=false; right=true; }
    }
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_UP) up = false; if (k == KeyEvent.VK_DOWN) down = false;
        if (k == KeyEvent.VK_LEFT) left = false; if (k == KeyEvent.VK_RIGHT) right = false;
    }
    public void keyTyped(KeyEvent e) {}
}
