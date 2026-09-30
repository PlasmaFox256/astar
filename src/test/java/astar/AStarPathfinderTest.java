package astar;

import java.util.List;

public final class AStarPathfinderTest {
    public static void main(String[] args) {
        AStarPathfinder pathfinder = new AStarPathfinder();

        Grid openGrid = new Grid(new boolean[][] {
            {true, true, true},
            {true, true, true},
            {true, true, true}
        });
        checkPath(pathfinder.findPath(openGrid, new Node(0, 0), new Node(2, 2)),
                openGrid, new Node(0, 0), new Node(2, 2), 5);

        Grid detour = new Grid(new boolean[][] {
            {true, false, true, true},
            {true, false, true, true},
            {true, true, true, true}
        });
        checkPath(pathfinder.findPath(detour, new Node(0, 0), new Node(3, 0)),
                detour, new Node(0, 0), new Node(3, 0), 8);

        Grid divided = new Grid(new boolean[][] {
            {true, false, true},
            {true, false, true}
        });
        check(pathfinder.findPath(divided, new Node(0, 0), new Node(2, 0)).isEmpty(),
                "Expected no path across a wall");
        check(pathfinder.findPath(divided, new Node(1, 0), new Node(2, 0)).isEmpty(),
                "Expected no path from a blocked cell");
        check(pathfinder.findPath(divided, new Node(0, 0), new Node(3, 0)).isEmpty(),
                "Expected no path to an out-of-bounds cell");

        checkPath(pathfinder.findPath(openGrid, new Node(1, 1), new Node(1, 1)),
                openGrid, new Node(1, 1), new Node(1, 1), 1);

        boolean[][] cells = {{true, true}};
        Grid copied = new Grid(cells);
        cells[0][1] = false;
        check(copied.isWalkable(1, 0), "Grid should copy its input");

        System.out.println("AStarPathfinder tests passed");
    }

    private static void checkPath(List<Node> path, Grid grid, Node start, Node goal,
            int expectedLength) {
        check(path.size() == expectedLength,
                "Expected " + expectedLength + " nodes, got " + path);
        check(path.get(0).equals(start), "Path does not start at start");
        check(path.get(path.size() - 1).equals(goal), "Path does not end at goal");
        for (int i = 0; i < path.size(); i++) {
            Node node = path.get(i);
            check(grid.isWalkable(node.getX(), node.getY()), "Path enters a blocked cell");
            if (i > 0) {
                Node prior = path.get(i - 1);
                int distance = Math.abs(node.getX() - prior.getX())
                        + Math.abs(node.getY() - prior.getY());
                check(distance == 1, "Path contains a non-cardinal move");
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
