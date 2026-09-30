package astar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/** Finds a shortest path using four cardinal moves of cost 1. */
public final class AStarPathfinder {
    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /** Returns nodes from start through goal, or an empty list if no path exists. */
    public List<Node> findPath(Grid grid, Node start, Node goal) {
        if (grid == null || start == null || goal == null) {
            throw new IllegalArgumentException("Grid, start, and goal are required");
        }
        if (!grid.isWalkable(start.getX(), start.getY())
                || !grid.isWalkable(goal.getX(), goal.getY())) {
            return Collections.emptyList();
        }

        Map<Node, Node> previous = new HashMap<>();
        Map<Node, Integer> gScore = new HashMap<>();
        Set<Node> closed = new HashSet<>();
        PriorityQueue<SearchEntry> open = new PriorityQueue<>();

        gScore.put(start, 0);
        open.add(new SearchEntry(start, 0, heuristic(start, goal)));

        while (!open.isEmpty()) {
            SearchEntry entry = open.poll();
            Node current = entry.node;
            // A better route may have been added while this entry was in the queue.
            if (closed.contains(current) || entry.g != gScore.get(current)) {
                continue;
            }
            if (current.equals(goal)) {
                return buildPath(previous, current);
            }
            closed.add(current);

            for (int[] direction : DIRECTIONS) {
                Node neighbor = new Node(current.getX() + direction[0],
                        current.getY() + direction[1]);
                if (!grid.isWalkable(neighbor.getX(), neighbor.getY())
                        || closed.contains(neighbor)) {
                    continue;
                }

                int tentativeG = entry.g + 1;
                if (tentativeG < gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    previous.put(neighbor, current);
                    gScore.put(neighbor, tentativeG);
                    open.add(new SearchEntry(neighbor, tentativeG,
                            tentativeG + heuristic(neighbor, goal)));
                }
            }
        }
        return Collections.emptyList();
    }

    private static double heuristic(Node from, Node to) {
        return Math.hypot((double) to.getX() - from.getX(),
                (double) to.getY() - from.getY());
    }

    private static List<Node> buildPath(Map<Node, Node> previous, Node goal) {
        List<Node> path = new ArrayList<>();
        for (Node node = goal; node != null; node = previous.get(node)) {
            path.add(node);
        }
        Collections.reverse(path);
        return path;
    }

    private static final class SearchEntry implements Comparable<SearchEntry> {
        private final Node node;
        private final int g;
        private final double f;

        private SearchEntry(Node node, int g, double f) {
            this.node = node;
            this.g = g;
            this.f = f;
        }

        @Override
        public int compareTo(SearchEntry other) {
            return Double.compare(f, other.f);
        }
    }
}
