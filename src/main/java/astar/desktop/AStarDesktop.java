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
import java.util.Arrays;

/** Interactive desktop view of the existing four-direction A* pathfinder. */
public final class AStarDesktop extends ApplicationAdapter {
    private static final int WINDOW_WIDTH = 1100;
    private static final int WINDOW_HEIGHT = 790;
    private static final Color BACKGROUND = new Color(.12f, .14f, .18f, 1f);
    private static final Color PANEL = new Color(.18f, .21f, .26f, 1f);
    private static final Color OUTLINE = new Color(.27f, .27f, .27f, 1f);
    private static final Color OPEN = new Color(.25f, .77f, .35f, 1f);
    private static final Color CLOSED = new Color(.86f, .27f, .27f, 1f);
    private static final Color PATH = new Color(.54f, .83f, 1f, 1f);
    private static final Color ENDPOINT = new Color(.10f, .23f, .58f, 1f);
    private static final String[] BUTTONS = {"Run/Pause", "Step", "Slower", "Faster", "Restart", "Edit map"};
    private static final String[] EDITOR_BUTTONS = {"Wall", "Eraser", "Set Start", "Set Finish", "Save", "Cancel", "Clear all"};
    private static final float BUTTON_Y = 68f;
    private static final float BUTTON_H = 34f;
    private static final float BUTTON_W = 130f;
    private static final float BUTTON_GAP = 10f;

    private Grid grid;
    private final AStarPathfinder pathfinder = new AStarPathfinder();
    private AStarPathfinder.Search search;
    private Node start;
    private Node finish;
    private Grid editorGrid;
    private Node editorStart;
    private Node editorFinish;
    private EditorTool editorTool = EditorTool.WALL;
    private boolean resumeAfterCancel;
    private boolean painting;
    private boolean running;
    private int stepsPerSecond = 8;
    private float accumulated;
    private float tileSize;
    private float gridLeft;
    private float gridBottom;
    private OrthographicCamera camera;
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont smallFont;
    private BitmapFont normalFont;
    private BitmapFont largeFont;
    private GlyphLayout layout;

    private enum EditorTool { WALL, ERASER, START, FINISH }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("A* Pathfinding Visualizer");
        config.setWindowedMode(WINDOW_WIDTH, WINDOW_HEIGHT);
        config.setResizable(false);
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
        editorGrid = null;
        editorStart = null;
        editorFinish = null;
        resumeAfterCancel = false;
        painting = false;
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
        smallFont = new BitmapFont(Gdx.files.classpath("fonts/ui-small.fnt"));
        normalFont = new BitmapFont(Gdx.files.classpath("fonts/ui-normal.fnt"));
        largeFont = new BitmapFont(Gdx.files.classpath("fonts/ui-large.fnt"));
        layout = new GlyphLayout();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button == Input.Buttons.LEFT) {
                    painting = onClick(screenX, Gdx.graphics.getHeight() - screenY);
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (painting && editorGrid != null
                        && (editorTool == EditorTool.WALL || editorTool == EditorTool.ERASER)) {
                    paintTile(screenX, Gdx.graphics.getHeight() - screenY);
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (button == Input.Buttons.LEFT) painting = false;
                return false;
            }

            @Override
            public boolean keyDown(int keycode) {
                if (editorGrid != null) {
                    switch (keycode) {
                        case Input.Keys.NUM_1: editorTool = EditorTool.WALL; return true;
                        case Input.Keys.NUM_2: editorTool = EditorTool.ERASER; return true;
                        case Input.Keys.NUM_3: editorTool = EditorTool.START; return true;
                        case Input.Keys.NUM_4: editorTool = EditorTool.FINISH; return true;
                        case Input.Keys.S: saveEditor(); return true;
                        case Input.Keys.ESCAPE: cancelEditor(); return true;
                        case Input.Keys.DEL: clearEditor(); return true;
                        default: return false;
                    }
                }
                switch (keycode) {
                    case Input.Keys.SPACE: toggleRunning(); return true;
                    case Input.Keys.N: stepOnce(); return true;
                    case Input.Keys.MINUS: changeSpeed(-1); return true;
                    case Input.Keys.EQUALS: changeSpeed(1); return true;
                    case Input.Keys.R: restart(); return true;
                    case Input.Keys.E: openEditor(); return true;
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
        Grid visible = visibleGrid();
        tileSize = (float) Math.floor(Math.min((width - 40f) / visible.getWidth(),
                (height - 175f) / visible.getHeight()));
        gridLeft = Math.round((width - tileSize * visible.getWidth()) / 2f);
        gridBottom = Math.round(140f + (height - 155f - tileSize * visible.getHeight()) / 2f);
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
        Grid visible = visibleGrid();
        String[] buttons = visibleButtons();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int y = 0; y < visible.getHeight(); y++) {
            for (int x = 0; x < visible.getWidth(); x++) {
                shapes.setColor(tileColor(x, y));
                shapes.rect(gridLeft + x * tileSize,
                        gridBottom + (visible.getHeight() - y - 1) * tileSize,
                        tileSize, tileSize);
            }
        }
        float buttonsLeft = (Gdx.graphics.getWidth() - buttonRowWidth(buttons)) / 2f;
        for (int i = 0; i < buttons.length; i++) {
            shapes.setColor(editorGrid != null && i == editorTool.ordinal() ? ENDPOINT : PANEL);
            shapes.rect(buttonsLeft + i * (BUTTON_W + BUTTON_GAP),
                    BUTTON_Y, BUTTON_W, BUTTON_H);
        }
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(OUTLINE);
        for (int y = 0; y < visible.getHeight(); y++) {
            for (int x = 0; x < visible.getWidth(); x++) {
                shapes.rect(gridLeft + x * tileSize,
                        gridBottom + (visible.getHeight() - y - 1) * tileSize,
                        tileSize, tileSize);
            }
        }
        shapes.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < buttons.length; i++) {
            shapes.rect(buttonsLeft + i * (BUTTON_W + BUTTON_GAP),
                    BUTTON_Y, BUTTON_W, BUTTON_H);
        }
        shapes.end();
    }

    private Color tileColor(int x, int y) {
        if (!visibleGrid().isWalkable(x, y)) return Color.BLACK;
        Node node = new Node(x, y);
        if (node.equals(visibleStart()) || node.equals(visibleFinish())) return ENDPOINT;
        if (editorGrid == null && search != null) {
            if (search.getPath().contains(node)) return PATH;
            if (search.getClosedNodes().contains(node)) return CLOSED;
            if (search.getOpenNodes().contains(node)) return OPEN;
        }
        return Color.WHITE;
    }

    private void drawText() {
        Grid visible = visibleGrid();
        String[] buttons = visibleButtons();
        batch.begin();
        smallFont.setColor(Color.WHITE);
        normalFont.setColor(Color.WHITE);
        largeFont.setColor(Color.WHITE);
        for (Node node : new Node[] {visibleStart(), visibleFinish()}) {
            if (node == null) continue;
            String label = node.equals(visibleStart()) ? "START" : "FINISH";
            float centerX = gridLeft + (node.getX() + .5f) * tileSize;
            float baseline = gridBottom + (visible.getHeight() - node.getY() - .5f) * tileSize
                    + smallFont.getCapHeight() / 2f;
            drawCentered(smallFont, label, centerX, baseline);
        }
        float buttonsLeft = (Gdx.graphics.getWidth() - buttonRowWidth(buttons)) / 2f;
        for (int i = 0; i < buttons.length; i++) {
            drawCentered(normalFont, buttons[i], buttonsLeft + i * (BUTTON_W + BUTTON_GAP) + BUTTON_W / 2f,
                    BUTTON_Y + 24f);
        }
        drawCentered(largeFont, status(), Gdx.graphics.getWidth() / 2f, 132f);
        if (editorGrid != null) {
            drawCentered(normalFont, "Paint or drag on the map. Eraser clears walls and endpoints.",
                    Gdx.graphics.getWidth() / 2f, 48f);
            drawCentered(normalFont, "1-4: tools   S: save   Esc: cancel   Delete: clear all",
                    Gdx.graphics.getWidth() / 2f, 25f);
        } else {
            drawCentered(normalFont, "Green: open   Red: closed   Light blue: path   Dark blue: endpoints",
                    Gdx.graphics.getWidth() / 2f, 48f);
            drawCentered(normalFont, "Space: run/pause   N: step   +/-: speed   R: restart   E: edit map",
                    Gdx.graphics.getWidth() / 2f, 25f);
        }
        batch.end();
    }

    private void drawCentered(BitmapFont font, String text, float centerX, float baseline) {
        layout.setText(font, text);
        font.draw(batch, text, Math.round(centerX - layout.width / 2f), Math.round(baseline));
    }

    private String status() {
        if (editorGrid != null) return "Editing map - " + editorTool.name().toLowerCase() + " selected";
        if (start == null) return "Click a white tile to choose START";
        if (finish == null) return "Click another white tile to choose FINISH";
        if (search == null) return "Ready - press Run/Pause or Step   |   " + stepsPerSecond + " steps/s";
        if (search.isFinished()) {
            return search.getPath().isEmpty() ? "No path found - edit map or endpoints" :
                    "Path found: " + (search.getPath().size() - 1) + " moves";
        }
        return (running ? "Searching" : "Paused") + "   |   " + stepsPerSecond
                + " steps/s   |   Open " + search.getOpenNodes().size()
                + "   Closed " + search.getClosedNodes().size();
    }

    private boolean onClick(float screenX, float screenY) {
        String[] buttons = visibleButtons();
        float buttonsLeft = (Gdx.graphics.getWidth() - buttonRowWidth(buttons)) / 2f;
        for (int i = 0; i < buttons.length; i++) {
            float left = buttonsLeft + i * (BUTTON_W + BUTTON_GAP);
            if (screenX >= left && screenX < left + BUTTON_W
                    && screenY >= BUTTON_Y && screenY < BUTTON_Y + BUTTON_H) {
                if (editorGrid != null) {
                    switch (i) {
                        case 0: editorTool = EditorTool.WALL; break;
                        case 1: editorTool = EditorTool.ERASER; break;
                        case 2: editorTool = EditorTool.START; break;
                        case 3: editorTool = EditorTool.FINISH; break;
                        case 4: saveEditor(); break;
                        case 5: cancelEditor(); break;
                        case 6: clearEditor(); break;
                        default: break;
                    }
                } else {
                    switch (i) {
                        case 0: toggleRunning(); break;
                        case 1: stepOnce(); break;
                        case 2: changeSpeed(-1); break;
                        case 3: changeSpeed(1); break;
                        case 4: restart(); break;
                        case 5: openEditor(); break;
                        default: break;
                    }
                }
                return false;
            }
        }
        if (editorGrid != null) return paintTile(screenX, screenY);
        int x = (int) ((screenX - gridLeft) / tileSize);
        int rowFromBottom = (int) ((screenY - gridBottom) / tileSize);
        if (screenX < gridLeft || screenY < gridBottom || x < 0 || x >= grid.getWidth()
                || rowFromBottom < 0 || rowFromBottom >= grid.getHeight()) return false;
        int y = grid.getHeight() - rowFromBottom - 1;
        if (!grid.isWalkable(x, y) || finish != null) return false;
        Node selected = new Node(x, y);
        if (start == null) start = selected;
        else if (!selected.equals(start)) finish = selected;
        return false;
    }

    private boolean paintTile(float screenX, float screenY) {
        int x = (int) ((screenX - gridLeft) / tileSize);
        int rowFromBottom = (int) ((screenY - gridBottom) / tileSize);
        if (screenX < gridLeft || screenY < gridBottom || x < 0 || x >= editorGrid.getWidth()
                || rowFromBottom < 0 || rowFromBottom >= editorGrid.getHeight()) return false;
        int y = editorGrid.getHeight() - rowFromBottom - 1;
        Node tile = new Node(x, y);
        switch (editorTool) {
            case WALL:
                editorGrid = editorGrid.withWalkability(x, y, false);
                clearEndpointAt(tile);
                break;
            case ERASER:
                editorGrid = editorGrid.withWalkability(x, y, true);
                clearEndpointAt(tile);
                break;
            case START:
                editorGrid = editorGrid.withWalkability(x, y, true);
                editorStart = tile;
                if (tile.equals(editorFinish)) editorFinish = null;
                break;
            case FINISH:
                editorGrid = editorGrid.withWalkability(x, y, true);
                editorFinish = tile;
                if (tile.equals(editorStart)) editorStart = null;
                break;
            default: break;
        }
        return true;
    }

    private void clearEndpointAt(Node tile) {
        if (tile.equals(editorStart)) editorStart = null;
        if (tile.equals(editorFinish)) editorFinish = null;
    }

    private void openEditor() {
        resumeAfterCancel = running;
        running = false;
        editorGrid = grid;
        editorStart = start;
        editorFinish = finish;
        editorTool = EditorTool.WALL;
    }

    private void saveEditor() {
        Grid savedGrid = editorGrid;
        Node savedStart = editorStart;
        Node savedFinish = editorFinish;
        replaceGrid(savedGrid);
        start = savedStart;
        finish = savedFinish;
    }

    private void cancelEditor() {
        editorGrid = null;
        editorStart = null;
        editorFinish = null;
        running = resumeAfterCancel;
        painting = false;
    }

    private void clearEditor() {
        boolean[][] cells = new boolean[editorGrid.getHeight()][editorGrid.getWidth()];
        for (boolean[] row : cells) Arrays.fill(row, true);
        editorGrid = new Grid(cells);
        editorStart = null;
        editorFinish = null;
    }

    private Grid visibleGrid() { return editorGrid == null ? grid : editorGrid; }
    private Node visibleStart() { return editorGrid == null ? start : editorStart; }
    private Node visibleFinish() { return editorGrid == null ? finish : editorFinish; }
    private String[] visibleButtons() { return editorGrid == null ? BUTTONS : EDITOR_BUTTONS; }

    private static float buttonRowWidth(String[] buttons) {
        return buttons.length * BUTTON_W + (buttons.length - 1) * BUTTON_GAP;
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
        smallFont.dispose();
        normalFont.dispose();
        largeFont.dispose();
    }
}
