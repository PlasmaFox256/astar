package astar;

/** A map source kept separate from rendering so an editor can supply a Grid later. */
public final class PrebuiltLevel {
    private static final String[] ROWS = {
        "........................",
        "....#...................",
        "....#......######.......",
        "....#...................",
        "....#...........#.......",
        "....#####.......#.......",
        "................#.......",
        "................#.......",
        "..######........#.......",
        ".......#........#.......",
        ".......#................",
        ".......#....######......",
        "............#...........",
        "............#...........",
        "........................",
        "........................"
    };

    private PrebuiltLevel() { }

    public static Grid create() {
        boolean[][] walkable = new boolean[ROWS.length][ROWS[0].length()];
        for (int y = 0; y < ROWS.length; y++) {
            if (ROWS[y].length() != walkable[y].length) {
                throw new IllegalStateException("Level rows must have equal length");
            }
            for (int x = 0; x < ROWS[y].length(); x++) {
                char tile = ROWS[y].charAt(x);
                if (tile != '.' && tile != '#') {
                    throw new IllegalStateException("Unknown level tile: " + tile);
                }
                walkable[y][x] = tile == '.';
            }
        }
        return new Grid(walkable);
    }
}
