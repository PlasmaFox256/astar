package astar.desktop;

import astar.AStarPathfinder;
import astar.Grid;
import astar.Node;
import astar.PrebuiltLevel;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Interactive desktop view of the existing four-direction A* pathfinder. */
public final class AStarDesktop extends ApplicationAdapter {
    private static final Color BACKGROUND = new Color(.12f, .14f, .18f, 1f);
    private static final Color PANEL = new Color(.18f, .21f, .26f, 1f);
    private static final Color OUTLINE = new Color(.27f, .27f, .27f, 1f);
    private static final Color OPEN = new Color(.25f, .77f, .35f, 1f);
    private static final Color CLOSED = new Color(.86f, .27f, .27f, 1f);
    private static final Color PATH = new Color(.54f, .83f, 1f, 1f);
    private static final Color ENDPOINT = new Color(.10f, .23f, .58f, 1f);
    private static final String[] BUTTONS = {"Run/Pause", "Step", "Slower", "Faster", "Restart", "Choose tiles"};
    private static final float BUTTON_Y = 68f;
    private static final float BUTTON_H = 34f;
    private static final float BUTTON_W = 130f;
    private static final float BUTTON_GAP = 10f;

    private Grid grid;
    private final AStarPathfinder pathfinder = new AStarPathfinder();
    private AStarPathfinder.Search search;
    private Node start;
    private Node finish;
    private boolean running;
    private int stepsPerSecond = 8;
    private float accumulated;
    private float tileSize;
    private float gridLeft;
    private float gridBottom;
    private OrthographicCamera camera;
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout layout;

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("A* Pathfinding Visualizer");
        config.setWindowedMode(1100, 790);
        config.setResizable(true);
        config.setWindowSizeLimits(1000, 700, -1, -1);
        config.useVsync(true);
        new Lwjgl3Application(new AStarDesktop(PrebuiltLevel.create()), config);
    }

    public AStarDesktop(Grid grid) {
        if (grid == null) {
            throw new IllegalArgumentException("Grid is required");
        }
        this.grid = grid;
    }

    /** Call on the LibGDX render thread when an editor supplies a new map. */
    public void replaceGrid(Grid grid) {
        if (grid == null) {
            throw new IllegalArgumentException("Grid is required");
        }
        this.grid = grid;
        chooseTiles();
        if (camera != null) {
            resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
    }

    @Override
    public void create() {
        camera = new OrthographicCamera();
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        layout = new GlyphLayout();
        font.getData().setScale(1.1f);
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button == Input.Buttons.LEFT) {
                    onClick(screenX, Gdx.graphics.getHeight() - screenY);
                    return true;
                }
                return false;
            }

            @Override
            public boolean keyDown(int keycode) {
                switch (keycode) {
                    case Input.Keys.SPACE: toggleRunning(); return true;
                    case Input.Keys.N: stepOnce(); return true;
                    case Input.Keys.MINUS: changeSpeed(-1); return true;
                    case Input.Keys.EQUALS: changeSpeed(1); return true;
                    case Input.Keys.R: restart(); return true;
                    case Input.Keys.C: chooseTiles(); return true;
                    default: return false;
                }
            }
        });
    }

    @Override
    public void resize(int width, int height) {
        if (camera == null || width <= 0 || height <= 0) {
            return;
        }
        camera.setToOrtho(false, width, height);
        camera.update();
        tileSize = Math.min((width - 40f) / grid.getWidth(),
                (height - 175f) / grid.getHeight());
        gridLeft = (width - tileSize * grid.getWidth()) / 2f;
        gridBottom = 140f + (height - 155f - tileSize * grid.getHeight()) / 2f;
    }

    @Override
    public void render() {
        if (running && search != null && !search.isFinished()) {
            accumulated += Math.min(Gdx.graphics.getDeltaTime(), .25f);
            float interval = 1f / stepsPerSecond;
            while (accumulated >= interval && !search.isFinished()) {
                search.step();
                accumulated -= interval;
            }
            if (search.isFinished()) {
                running = false;
            }
        }
        Gdx.gl.glClearColor(BACKGROUND.r, BACKGROUND.g, BACKGROUND.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
        drawShapes();
        drawText();
    }

    private void drawShapes() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                shapes.setColor(tileColor(x, y));
                shapes.rect(gridLeft + x * tileSize,
                        gridBottom + (grid.getHeight() - y - 1) * tileSize,
                        tileSize, tileSize);
            }
        }
        float buttonsLeft = (Gdx.graphics.getWidth() - buttonRowWidth()) / 2f;
        for (int i = 0; i < BUTTONS.length; i++) {
            shapes.setColor(PANEL);
            shapes.rect(buttonsLeft + i * (BUTTON_W + BUTTON_GAP),
                    BUTTON_Y, BUTTON_W, BUTTON_H);
        }
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(OUTLINE);
        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                shapes.rect(gridLeft + x * tileSize,
                        gridBottom + (grid.getHeight() - y - 1) * tileSize,
                        tileSize, tileSize);
            }
        }
        shapes.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < BUTTONS.length; i++) {
            shapes.rect(buttonsLeft + i * (BUTTON_W + BUTTON_GAP),
                    BUTTON_Y, BUTTON_W, BUTTON_H);
        }
        shapes.end();
    }

    private Color tileColor(int x, int y) {
        if (!grid.isWalkable(x, y)) return Color.BLACK;
        Node node = new Node(x, y);
        if (node.equals(start) || node.equals(finish)) return ENDPOINT;
        if (search != null) {
            if (search.getPath().contains(node)) return PATH;
            if (search.getClosedNodes().contains(node)) return CLOSED;
            if (search.getOpenNodes().contains(node)) return OPEN;
        }
        return Color.WHITE;
    }

    private void drawText() {
        batch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(1f);
        layout.setText(font, "FINISH");
        font.getData().setScale(Math.min(.78f, (tileSize - 4f) / layout.width));
        for (Node node : new Node[] {start, finish}) {
            if (node == null) continue;
            String label = node.equals(start) ? "START" : "FINISH";
            float centerX = gridLeft + (node.getX() + .5f) * tileSize;
            float baseline = gridBottom + (grid.getHeight() - node.getY() - .5f) * tileSize + 5f;
            drawCentered(label, centerX, baseline);
        }
        font.getData().setScale(1f);
        float buttonsLeft = (Gdx.graphics.getWidth() - buttonRowWidth()) / 2f;
        for (int i = 0; i < BUTTONS.length; i++) {
            drawCentered(BUTTONS[i], buttonsLeft + i * (BUTTON_W + BUTTON_GAP) + BUTTON_W / 2f,
                    BUTTON_Y + 24f);
        }
        font.getData().setScale(1.4f);
        drawCentered(status(), Gdx.graphics.getWidth() / 2f, 132f);
        font.getData().setScale(1f);
        drawCentered("Green: open   Red: closed   Light blue: path   Dark blue: endpoints",
                Gdx.graphics.getWidth() / 2f, 48f);
        drawCentered("Space: run/pause   N: step   +/-: speed   R: restart   C: choose tiles",
                Gdx.graphics.getWidth() / 2f, 25f);
        batch.end();
    }

    private void drawCentered(String text, float centerX, float baseline) {
        layout.setText(font, text);
        font.draw(batch, text, centerX - layout.width / 2f, baseline);
    }

    private String status() {
        if (start == null) return "Click a white tile to choose START";
        if (finish == null) return "Click another white tile to choose FINISH";
        if (search == null) return "Ready - press Run/Pause or Step   |   " + stepsPerSecond + " steps/s";
        if (search.isFinished()) {
            return search.getPath().isEmpty() ? "No path found - choose new tiles" :
                    "Path found: " + (search.getPath().size() - 1) + " moves";
        }
        return (running ? "Searching" : "Paused") + "   |   " + stepsPerSecond
                + " steps/s   |   Open " + search.getOpenNodes().size()
                + "   Closed " + search.getClosedNodes().size();
    }

    private void onClick(float screenX, float screenY) {
        float buttonsLeft = (Gdx.graphics.getWidth() - buttonRowWidth()) / 2f;
        for (int i = 0; i < BUTTONS.length; i++) {
            float left = buttonsLeft + i * (BUTTON_W + BUTTON_GAP);
            if (screenX >= left && screenX < left + BUTTON_W
                    && screenY >= BUTTON_Y && screenY < BUTTON_Y + BUTTON_H) {
                switch (i) {
                    case 0: toggleRunning(); break;
                    case 1: stepOnce(); break;
                    case 2: changeSpeed(-1); break;
                    case 3: changeSpeed(1); break;
                    case 4: restart(); break;
                    case 5: chooseTiles(); break;
                    default: break;
                }
                return;
            }
        }
        int x = (int) ((screenX - gridLeft) / tileSize);
        int rowFromBottom = (int) ((screenY - gridBottom) / tileSize);
        if (screenX < gridLeft || screenY < gridBottom || x < 0 || x >= grid.getWidth()
                || rowFromBottom < 0 || rowFromBottom >= grid.getHeight()) return;
        int y = grid.getHeight() - rowFromBottom - 1;
        if (!grid.isWalkable(x, y) || finish != null) return;
        Node selected = new Node(x, y);
        if (start == null) start = selected;
        else if (!selected.equals(start)) finish = selected;
    }

    private static float buttonRowWidth() {
        return BUTTONS.length * BUTTON_W + (BUTTONS.length - 1) * BUTTON_GAP;
    }

    private void ensureSearch() {
        if (start != null && finish != null && search == null) {
            search = pathfinder.beginSearch(grid, start, finish);
        }
    }

    private void toggleRunning() {
        ensureSearch();
        if (search != null && !search.isFinished()) {
            running = !running;
            accumulated = 0f;
        }
    }

    private void stepOnce() {
        ensureSearch();
        if (search != null && !search.isFinished()) {
            running = false;
            accumulated = 0f;
            search.step();
        }
    }

    private void changeSpeed(int direction) {
        stepsPerSecond = Math.max(1, Math.min(60, stepsPerSecond + direction * 2));
        accumulated = 0f;
    }

    private void restart() {
        running = false;
        accumulated = 0f;
        search = null;
    }

    private void chooseTiles() {
        restart();
        start = null;
        finish = null;
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
    }
}
