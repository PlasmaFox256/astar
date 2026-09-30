# A* pathfinding

This Java 8 component finds a shortest path through a rectangular grid. Coordinates
are `(x, y)`, with `x` increasing across columns and `y` increasing down rows.
In a `boolean[][]` grid, `cells[y][x]` is `true` for walkable cells and `false`
for blocked cells. Each cardinal move costs 1. The heuristic is Euclidean
distance, and A* ranks candidates by `f = g + h`.

```java
import astar.AStarPathfinder;
import astar.Grid;
import astar.Node;
import java.util.List;

boolean[][] cells = {
    {true, true, true},
    {true, false, true},
    {true, true, true}
};

Grid grid = new Grid(cells);
List<Node> path = new AStarPathfinder().findPath(
    grid, new Node(0, 0), new Node(2, 2));
```

The returned list includes both endpoints in order. It is empty when the goal
cannot be reached or either endpoint is blocked or outside the grid. `Grid`
copies its input, so later changes to the array do not affect a search.

To compile and run the dependency-free tests:

```sh
mkdir -p build/classes
javac -d build/classes src/main/java/astar/*.java src/test/java/astar/*.java
java -cp build/classes astar.AStarPathfinderTest
```
