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
        Search search = beginSearch(grid, start, goal);
        while (!search.isFinished()) {
            search.step();
        }
        return search.getPath();
    }

    /** Creates a search that advances by one expanded node per call to step(). */
    public Search beginSearch(Grid grid, Node start, Node goal) {
        return new Search(grid, start, goal);
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
        return Collections.unmodifiableList(path);
    }

    public static final class Search {
        private final Grid grid;
        private final Node goal;
        private final Map<Node, Node> previous = new HashMap<>();
        private final Map<Node, Integer> gScore = new HashMap<>();
        private final Set<Node> closed = new HashSet<>();
        private final Set<Node> openNodes = new HashSet<>();
        private final PriorityQueue<SearchEntry> open = new PriorityQueue<>();
        private List<Node> path = Collections.emptyList();
        private boolean finished;

        private Search(Grid grid, Node start, Node goal) {
            if (grid == null || start == null || goal == null) {
                throw new IllegalArgumentException("Grid, start, and goal are required");
            }
            this.grid = grid;
            this.goal = goal;
            if (!grid.isWalkable(start.getX(), start.getY())
                    || !grid.isWalkable(goal.getX(), goal.getY())) {
                finished = true;
            } else {
                gScore.put(start, 0);
                openNodes.add(start);
                open.add(new SearchEntry(start, 0, heuristic(start, goal)));
            }
        }

        /** Expands one valid candidate; returns false once there is no work left. */
        public boolean step() {
            if (finished) {
                return false;
            }
            while (!open.isEmpty()) {
                SearchEntry entry = open.poll();
                Node current = entry.node;
                // A better route may have been added while this entry was queued.
                if (closed.contains(current) || entry.g != gScore.get(current)) {
                    continue;
                }
                openNodes.remove(current);
                closed.add(current);
                if (current.equals(goal)) {
                    path = buildPath(previous, current);
                    finished = true;
                    return true;
                }
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
                        openNodes.add(neighbor);
                        open.add(new SearchEntry(neighbor, tentativeG,
                                tentativeG + heuristic(neighbor, goal)));
                    }
                }
                return true;
            }
            finished = true;
            return false;
        }

        public boolean isFinished() {
            return finished;
        }

        public List<Node> getPath() {
            return path;
        }

        public Set<Node> getOpenNodes() {
            return Collections.unmodifiableSet(openNodes);
        }

        public Set<Node> getClosedNodes() {
            return Collections.unmodifiableSet(closed);
        }
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
