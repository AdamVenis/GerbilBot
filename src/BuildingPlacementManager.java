import bwapi.Game;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;

public class BuildingPlacementManager {

    public GameInfo gameInfo;
    public MapInfo mapInfo;

    public BuildingPlacementManager(GameInfo gameInfo, MapInfo mapInfo) {
        this.gameInfo = gameInfo;
        this.mapInfo = mapInfo;
    }


    // Returns a suitable TilePosition to build a given building type near
    // specified TilePosition aroundTile, or null if not found. (builder parameter is our worker)
    public TilePosition getBuildTile(Game game, UnitType buildingType, TilePosition aroundTile) {
        TilePosition ret = null;
        int maxDist = 8;
        int stopDist = 40;

        // Refinery, Assimilator, Extractor
        if (buildingType.isRefinery()) {
            for (Unit n : game.neutral().getUnits()) {
                if ((n.getType() == UnitType.Resource_Vespene_Geyser) &&
                    (Math.abs(n.getTilePosition().getX() - aroundTile.getX()) < stopDist) &&
                    (Math.abs(n.getTilePosition().getY() - aroundTile.getY()) < stopDist)) {
                    return n.getTilePosition();
                }
            }
        }

        System.out.println(mapInfo.isWallCompleted());
        System.out.println(mapInfo.wall);
        if (!mapInfo.isWallCompleted() && mapInfo.wall.containsKey(buildingType)) {
            TilePosition tilePosition = mapInfo.wall.get(buildingType).get(0);
            mapInfo.addToWall(buildingType, tilePosition);
            System.out.println("returning " + tilePosition.toString());
            return tilePosition;
        }

        while (maxDist < stopDist && ret == null) {
            for (int i = -maxDist; i < maxDist; i++) {
                TilePosition tilePosition = new TilePosition(aroundTile.getX() + i, aroundTile.getY() - maxDist);
                if (game.canBuildHere(tilePosition, buildingType)) {
                    return tilePosition;
                }
                tilePosition = new TilePosition(aroundTile.getX() + i, aroundTile.getY() + maxDist);
                if (game.canBuildHere(tilePosition, buildingType)) {
                    return tilePosition;
                }
                tilePosition = new TilePosition(aroundTile.getX() - maxDist, aroundTile.getY() + i);
                if (game.canBuildHere(tilePosition, buildingType)) {
                    return tilePosition;
                }
                tilePosition = new TilePosition(aroundTile.getX() + maxDist, aroundTile.getY() + i);
                if (game.canBuildHere(tilePosition, buildingType)) {
                    return tilePosition;
                }
            }
            maxDist += 4;
        }
        return null;
    }

}
