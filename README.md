# A* pathfinding visualizer

The desktop app uses LibGDX to show each expansion of the A* search on a
prebuilt 24 × 16 grid. The map is defined in `PrebuiltLevel` independently of
the renderer; a later level editor can create a `Grid` from its own cells or
use `Grid.withWalkability()`, then pass it to `AStarDesktop.replaceGrid()` on
the render thread. Replacing the map resets endpoint selection and search.

To launch it, use JDK 8 or newer and run:

```sh
./gradlew run
```

On macOS the Gradle application task supplies `-XstartOnFirstThread` for the
desktop window. The first run downloads Gradle and LibGDX dependencies.

Click a white tile for **START**, then another white tile for **FINISH**. Walls
cannot be selected. Use the buttons or keyboard:

| Action | Button | Key |
| --- | --- | --- |
| Start or pause playback | Run/Pause | Space |
| Expand one node and pause | Step | N |
| Adjust expansions per second | Slower / Faster | - / + |
| Clear search and keep endpoints | Restart | R |
| Choose new endpoints | Choose tiles | C |

White is open floor, black is wall, green is the search frontier, red is
expanded nodes, light blue is the final path, and dark blue marks the two
endpoints. Every tile has a dark gray outline. The status line reports search
progress and path length. The search accepts only cardinal moves.

## Pathfinder API

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

For incremental playback, call `beginSearch(grid, start, goal)` and repeatedly
call `step()`. Each call expands at most one node. `getOpenNodes()`,
`getClosedNodes()`, `getPath()`, and `isFinished()` expose the current state.
The original `findPath()` method runs this same search to completion.

To run the dependency-free pathfinder tests through Gradle:

```sh
./gradlew pathfinderTest
```

Or compile and run those tests directly with a Java 8 or newer JDK:

```sh
mkdir -p build/classes
javac -d build/classes src/main/java/astar/*.java src/test/java/astar/*.java
java -cp build/classes astar.AStarPathfinderTest
```
