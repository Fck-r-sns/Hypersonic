import java.util.*;

class Position {
    int x;
    int y;

    Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "{" + x + "," + y + "}";
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Position)) {
            return false;
        }
        final Position other = (Position) obj;
        return x == other.x
                && y == other.y;
    }
}

class Boomer {
    static final int ENTITY_TYPE = 0;
    int id;
    Position position = new Position(0, 0);
    int maxBombsAvailable = 1;
    int bombsAvailable;
    int explosionRange;

    @Override
    public String toString() {
        return "Boomer {" + position + "," + bombsAvailable + "," + explosionRange + "}";
    }
}

class Bomb {
    static final int ENTITY_TYPE = 1;
    static final int COUNTDOWN = 8;
    Position position = new Position(0, 0);
    int timer;
    int explosionRange;

    @Override
    public String toString() {
        return "Bomb {" + position + "," + timer + "," + explosionRange + "}";
    }
}

class Item {
    static final int ENTITY_CODE = 2;

    enum Type {
        ExtraRange(1),
        ExtraBomb(2);

        int code;

        Type(int code) {
            this.code = code;
        }
    }

    Position position = new Position(0, 0);
    Type type;
}

class Cell {
    enum Type {
        Floor('.'),
        Box('0'),
        BoxWithExtraRange('1'),
        BoxWithExtraBomb('2'),
        ExtraRange('R'),
        ExtraBomb('B');

        char symbol;

        Type(char symbol) {
            this.symbol = symbol;
        }
    }

    static EnumSet<Type> BOX_SUBTYPES = EnumSet.of(Type.Box, Type.BoxWithExtraBomb, Type.BoxWithExtraRange);
    static EnumSet<Type> BONUS_SUBTYPES = EnumSet.of(Type.ExtraRange, Type.ExtraBomb);

    Position position;
    Type type = Type.Floor;
    int utility;

    @Override
    public String toString() {
        return "" + type.symbol;
    }

    @Override
    public int hashCode() {
        // ignore utility
        int res = 0;
        res = 31 * res + position.hashCode();
        res = 31 * res + type.hashCode();
        return res;
    }

    @Override
    public boolean equals(Object obj) {
        // ignore utility
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Cell)) {
            return false;
        }
        final Cell other = (Cell) obj;
        return position.equals(other.position)
                && type == other.type;
    }
}

class Grid {
    int height;
    int width;
    Cell cells[][];

    void clear() {
        cells = new Cell[width][height];
    }

    String showTypes() {
        final StringBuilder sb = new StringBuilder(height * width);
        for (int rowIndex = 0; rowIndex < height; ++rowIndex) {
            for (int columnIndex = 0; columnIndex < width; ++columnIndex) {
                sb.append(cells[columnIndex][rowIndex].type.symbol);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    String showUtility() {
        final StringBuilder sb = new StringBuilder(height * width);
        for (int rowIndex = 0; rowIndex < height; ++rowIndex) {
            for (int columnIndex = 0; columnIndex < width; ++columnIndex) {
                sb.append(cells[columnIndex][rowIndex].utility);
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}

class Target {
    enum Type {
        BombPlace,
        Bonus
    }
    Position position;
    int distance;
    int utility;
    Type type;

    @Override
    public String toString() {
        return "Target {" + position + "," + distance + "," + utility + "}";
    }
}

class World {
    final Grid grid = new Grid();
    final Boomer player = new Boomer();
    final Boomer enemy = new Boomer();
    List<Bomb> playerBombs = new ArrayList<>();
    List<Bomb> enemyBombs = new ArrayList<>();
    final Map<Position, Item> items = new HashMap<>();
    Target target;
}

// main class must be Player
class Player {

    private final Scanner in = new Scanner(System.in);
    private final World world = new World();

    public static void main(String args[]) {
        final Player game = new Player();
        game.initWorld();
        game.run();
    }

    void run() {
        // game loop
        while (true) {
            updateWorldState();
            in.nextLine();

            final Set<Position> exceptions = new HashSet<>(2);
            for (Bomb bomb : world.playerBombs) {
                exceptions.add(bomb.position);
                getBoxesAffectedByExplosion(bomb.position, bomb.explosionRange, null).forEach(c -> exceptions.add(c.position));
            }
            for (Bomb bomb : world.enemyBombs) {
                exceptions.add(bomb.position);
                getBoxesAffectedByExplosion(bomb.position, bomb.explosionRange, null).forEach(c -> exceptions.add(c.position));
            }

            calculateCellsUtility(exceptions);

//            System.err.println(world.grid.showTypes());
            System.err.println(world.grid.showUtility());
//            System.err.println(world.player);
//            System.err.println(world.enemy);
//            System.err.println(world.playerBomb);
//            System.err.println(world.enemyBomb);
            System.err.println(world.target);

            int scanRange = Bomb.COUNTDOWN;
            if (world.player.bombsAvailable > 0) {
                scanRange = 4;
            }
            world.target = findNearestCellWithHighestUtility(scanRange, exceptions);
            if (world.target == null) {
                scanRange *= 2;
                world.target = findNearestCellWithHighestUtility(scanRange, exceptions);
            }
            if (world.target == null) {
                scanRange *= 2;
                world.target = findNearestCellWithHighestUtility(scanRange, exceptions);
            }
            if (world.target == null) {
                world.target = new Target();
                world.target.position = new Position(0, 0);
            }

            if (world.target.position.equals((world.player.position))) {
                if (world.target.type == Target.Type.BombPlace) {
                    System.out.println("BOMB " + world.target.position.x + " " + world.target.position.y);
                } else {
                    System.out.println("MOVE " + world.target.position.x + " " + world.target.position.y);
                }
                world.target = null;
            } else {
                System.out.println("MOVE " + world.target.position.x + " " + world.target.position.y);
            }
        }
    }

    void initWorld() {
        world.grid.width = in.nextInt();
        world.grid.height = in.nextInt();
        world.grid.clear();
        world.player.id = in.nextInt();
        in.nextLine();
    }

    void updateWorldState() {
        world.playerBombs.clear();
        world.enemyBombs.clear();
        world.grid.clear();
        for (int rowIndex = 0; rowIndex < world.grid.height; rowIndex++) {
            final String row = in.nextLine();
            for (int columnIndex = 0; columnIndex < world.grid.width; ++columnIndex) {
                final Cell cell = new Cell();
                cell.position = new Position(columnIndex, rowIndex);
                final char typeSymbol = row.charAt(columnIndex);
                if (typeSymbol == Cell.Type.Box.symbol) {
                    cell.type = Cell.Type.Box;
                } else if (typeSymbol == Cell.Type.BoxWithExtraRange.symbol) {
                    cell.type = Cell.Type.BoxWithExtraRange;
                } else if (typeSymbol == Cell.Type.BoxWithExtraBomb.symbol) {
                    cell.type = Cell.Type.BoxWithExtraBomb;
                }
                world.grid.cells[columnIndex][rowIndex] = cell;
            }
        }
        int entities = in.nextInt();
        for (int i = 0; i < entities; i++) {
            int entityType = in.nextInt();
            int owner = in.nextInt();
            int x = in.nextInt();
            int y = in.nextInt();
            int param1 = in.nextInt();
            int param2 = in.nextInt();
            switch (entityType) {
                case Boomer.ENTITY_TYPE:
                    final Boomer boomer = (owner == world.player.id) ? world.player : world.enemy;
                    boomer.position.x = x;
                    boomer.position.y = y;
                    boomer.bombsAvailable = param1;
                    boomer.explosionRange = param2;
                    break;
                case Bomb.ENTITY_TYPE:
                    final Bomb bomb = new Bomb();
                    if (owner == world.player.id) {
                        world.playerBombs.add(bomb);
                    } else {
                        world.enemyBombs.add(bomb);
                    }
                    bomb.position.x = x;
                    bomb.position.y = y;
                    bomb.timer = param1;
                    bomb.explosionRange = param2;
                    break;
                case Item.ENTITY_CODE:
                    final Item item = new Item();
                    item.position.x = x;
                    item.position.y = y;
                    if (param1 == Item.Type.ExtraRange.code) {
                        item.type = Item.Type.ExtraRange;
                        world.grid.cells[x][y].type = Cell.Type.ExtraRange;
                    } else if (param1 == Item.Type.ExtraBomb.code) {
                        item.type = Item.Type.ExtraBomb;
                        world.grid.cells[x][y].type = Cell.Type.ExtraBomb;
                    }
                    world.items.put(item.position, item);
                    break;
                default:
                    break;
            }
        }
    }

    void calculateCellsUtility(final Set<Position> exceptions) {
        final int width = world.grid.width;
        final int height = world.grid.height;
        final int bombRange = world.player.explosionRange - 1;
        for (int columnIndex = 0; columnIndex < width; ++columnIndex) {
            for (int rowIndex = 0; rowIndex < height; ++rowIndex) {
                final Cell cell = world.grid.cells[columnIndex][rowIndex];
                if (cell.type == Cell.Type.Floor) {
                    final Set<Cell> boxes = getBoxesAffectedByExplosion(new Position(columnIndex, rowIndex), bombRange, exceptions);
                    cell.utility = boxes.size();
                } else if (Cell.BONUS_SUBTYPES.contains(cell.type)) {
                    final int distanceToBonus = calculateDistance(world.player.position, cell.position);
                    cell.utility = (distanceToBonus <= 2) ? 5 : 0;
                    if (cell.type == Cell.Type.ExtraRange && world.player.explosionRange > 4) {
                        cell.utility = 0;
                    }
                    if (cell.type == Cell.Type.ExtraBomb && world.player.maxBombsAvailable > 2) {
                        cell.utility = 0;
                    }
                }
            }
        }
    }

    Set<Cell> getBoxesAffectedByExplosion(Position bombPosition, int bombRange, final Set<Position> exceptions) {
        final int width = world.grid.width;
        final int height = world.grid.height;
        final int explosionColumnLeft = Math.max(bombPosition.x - bombRange, 0);
        final int explosionColumnRight = Math.min(bombPosition.x + bombRange, width - 1);
        final int explosionRowTop = Math.max(bombPosition.y - bombRange, 0);
        final int explosionRowBottom = Math.min(bombPosition.y + bombRange, height - 1);
        final Set<Cell> boxes = new HashSet<>(4);
        for (int explosionColumnIndex = bombPosition.x - 1; explosionColumnIndex >= explosionColumnLeft; --explosionColumnIndex) {
            final Cell cell = world.grid.cells[explosionColumnIndex][bombPosition.y];
            if (Cell.BONUS_SUBTYPES.contains(cell.type)) {
                break;
            }
            if (Cell.BOX_SUBTYPES.contains(cell.type)) {
                if (exceptions == null || !exceptions.contains(cell.position)) {
                    boxes.add(cell);
                }
                break;
            }
        }
        for (int explosionColumnIndex = bombPosition.x + 1; explosionColumnIndex <= explosionColumnRight; ++explosionColumnIndex) {
            final Cell cell = world.grid.cells[explosionColumnIndex][bombPosition.y];
            if (Cell.BONUS_SUBTYPES.contains(cell.type)) {
                break;
            }
            if (Cell.BOX_SUBTYPES.contains(cell.type)) {
                if (exceptions == null || !exceptions.contains(cell.position)) {
                    boxes.add(cell);
                }
                break;
            }
        }
        for (int explosionRowIndex = bombPosition.y - 1; explosionRowIndex >= explosionRowTop; --explosionRowIndex) {
            final Cell cell = world.grid.cells[bombPosition.x][explosionRowIndex];
            if (Cell.BONUS_SUBTYPES.contains(cell.type)) {
                break;
            }
            if (Cell.BOX_SUBTYPES.contains(cell.type)) {
                if (exceptions == null || !exceptions.contains(cell.position)) {
                    boxes.add(cell);
                }
                break;
            }
        }
        for (int explosionRowIndex = bombPosition.y + 1; explosionRowIndex <= explosionRowBottom; ++explosionRowIndex) {
            final Cell cell = world.grid.cells[bombPosition.x][explosionRowIndex];
            if (Cell.BONUS_SUBTYPES.contains(cell.type)) {
                break;
            }
            if (Cell.BOX_SUBTYPES.contains(cell.type)) {
                if (exceptions == null || !exceptions.contains(cell.position)) {
                    boxes.add(cell);
                }
                break;
            }
        }
        return boxes;
    }

    Target findNearestCellWithHighestUtility(int scanRange, final Set<Position> exceptions) {
        final int width = world.grid.width;
        final int height = world.grid.height;
        final int playerX = world.player.position.x;
        final int playerY = world.player.position.y;
        final int columnDeltaMin = Math.max(-scanRange, 0 - playerX);
        final int columnDeltaMax = Math.min(scanRange, width - 1 - playerX);
        int distanceToMax = 0;
        Position positionOfMax = new Position(0, 0);
        int max = 0;
        for (int columnDelta = columnDeltaMin; columnDelta <= columnDeltaMax; ++columnDelta) {
            final int columnIndex = playerX + columnDelta;
            final int rowDeltaMin = Math.max(-scanRange + columnDelta, 0 - playerY);
            final int rowDeltaMax = Math.min(scanRange - columnDelta, height - 1 - playerY);
            for (int rowDelta = rowDeltaMin; rowDelta <= rowDeltaMax; ++rowDelta) {
                final int rowIndex = playerY + rowDelta;
                final Position currentPosition = new Position(columnIndex, rowIndex);
                if (exceptions.contains(currentPosition)) {
                    continue;
                }
                final int currentUtility = world.grid.cells[columnIndex][rowIndex].utility;
                if (currentUtility < max) {
                    continue;
                }
                final int currentDistance = calculateDistance(playerX, playerY, columnIndex, rowIndex);
                if (currentUtility > max || (currentUtility == max && currentDistance < distanceToMax)) {
                    positionOfMax = currentPosition;
                    max = currentUtility;
                    distanceToMax = currentDistance;
                }
            }
        }
        final Target target = new Target();
        target.position = positionOfMax;
        target.distance = distanceToMax;
        target.utility = max;
        if (Cell.BONUS_SUBTYPES.contains(world.grid.cells[target.position.x][target.position.y].type)) {
            target.type = Target.Type.Bonus;
        } else {
            target.type = Target.Type.BombPlace;
        }
        return target;
    }

    int calculateDistance(int x1, int y1, int x2, int y2) {
        // manhattan distance without obstacles
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    int calculateDistance(Position pos1, Position pos2) {
        // manhattan distance without obstacles
        return Math.abs(pos1.x - pos2.x) + Math.abs(pos1.y - pos2.y);
    }

//    double findCosBetweenVectors(Position origin, Position vec1, Position vec2) {
//
//    }
}