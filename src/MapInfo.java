import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bwapi.TilePosition;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.BaseLocation;

public class MapInfo {

    public static final String FIGHTING_SPRIIT = "Destination";
    public static final String HEARTBREAK_RIDGE = "Destination";
    public static final String EMPIRE_OF_THE_SUN = "Destination";
    public static final String JADE = "Destination";
    public static final String CIRCUIT_BREAKER = "Destination";
    public static final String ANDROMEDA = "Destination";
    public static final String ROADRUNNER = "Destination";
    public static final String NEO_MOON_GLAIVE = "Destination";
    public static final String LA_MANCHA = "Destination";
    public static final String ELECTRIC_CIRCUIT = "Destination";
    public static final String BENZENE = "Destination";
    public static final String PYTHON = "Destination";
    public static final String DESTINATION = "| iCCup | Destination 1.1";
    public static final String ICARUS = "Destination";
    public static final String TAU_CROSS = "Destination";

    public GameInfo gameInfo;
    public String mapName;

    public List<BaseLocation> possibleEnemySpawns;
    public BaseLocation mainBase;

    public Map<UnitType, List<TilePosition>> wall;

    public MapInfo(GameInfo gameInfo, String mapName) {
        this.gameInfo = gameInfo;
        this.mapName = mapName;

        possibleEnemySpawns = new ArrayList<>();
        System.out.println("Before : " + mainBase);
        mainBase = BWTA.getStartLocation(gameInfo.self);
        System.out.println("After : " + mainBase);
        for (BaseLocation spawn : BWTA.getStartLocations()) {
            if (!spawn.equals(mainBase)) {
                possibleEnemySpawns.add(spawn);
            }
        }
        wall = generateWall();
    }

    public Map<UnitType, List<TilePosition>> generateWall() {
        Map<UnitType, List<TilePosition>> wall = new HashMap<>();
        List<TilePosition> depots = new ArrayList<>();
        List<TilePosition> barracks = new ArrayList<>();

        if (mapName.equals(DESTINATION)) {
            if (mainBase.getTilePosition().equals(new TilePosition(31, 7))) {
                depots.add(new TilePosition(47, 6));
                depots.add(new TilePosition(47, 8));
                barracks.add(new TilePosition(48, 10));
            } else if (mainBase.getTilePosition().equals(new TilePosition(64, 118))) {
                depots.add(new TilePosition(46, 114));
                depots.add(new TilePosition(46, 116));
                barracks.add(new TilePosition(45, 118));
            }
        }
        wall.put(UnitType.Terran_Supply_Depot, depots);
        wall.put(UnitType.Terran_Barracks, barracks);

        return wall;
    }

    public void addToWall(UnitType unitType, TilePosition tilePosition) {
        if (!wall.containsKey(unitType)) {
            return;
        }
        List<TilePosition> positions = wall.get(unitType);
        positions.remove(tilePosition);
        if (positions.isEmpty()) {
            wall.remove(unitType);
        }
    }

    public boolean isWallCompleted() {
        return wall.isEmpty();
    }
}
