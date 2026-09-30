package astar;

/** A rectangular grid where true means walkable and false means blocked. */
public final class Grid {
    private final boolean[][] walkable;
    private final int width;
    private final int height;

    /** Rows are indexed by y, and cells within each row by x. */
    public Grid(boolean[][] walkable) {
        if (walkable == null || walkable.length == 0 || walkable[0] == null
                || walkable[0].length == 0) {
            throw new IllegalArgumentException("Grid must have at least one row and column");
        }

        height = walkable.length;
        width = walkable[0].length;
        this.walkable = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            if (walkable[y] == null || walkable[y].length != width) {
                throw new IllegalArgumentException("Grid rows must have equal length");
            }
            System.arraycopy(walkable[y], 0, this.walkable[y], 0, width);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isWalkable(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height && walkable[y][x];
    }

    /** Returns a new grid with one changed tile, suitable for future map editing. */
    public Grid withWalkability(int x, int y, boolean value) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException("Tile outside grid: " + x + ", " + y);
        }
        boolean[][] copy = new boolean[height][width];
        for (int row = 0; row < height; row++) {
            System.arraycopy(walkable[row], 0, copy[row], 0, width);
        }
        copy[y][x] = value;
        return new Grid(copy);
    }
}
