import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Order;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.BaseLocation;

public class GerbilBot extends DefaultBWListener {

    private Mirror mirror = new Mirror();

    private GameInfo gameInfo = new GameInfo();
    private MapInfo mapInfo;

    private Game game;
    private Player self;

    private SquadManager marineSquadManager;
    private BuildingPlacementManager buildingPlacementManager;

    private Map<UnitType, Set<Unit>> alliedUnits = new HashMap<>();

    private int lastFrame = -1;

    public void run() {
        mirror.getModule().setEventListener(this);
        mirror.startGame();
    }

    @Override
    public void onUnitComplete(Unit unit) {
    	UnitType unitType = unit.getType();
        System.out.println(String.valueOf(game.getFrameCount()) + ": New unit discovered " + unitType);
    	if (!unit.getPlayer().equals(self)) {
    		return;
    	}
    	alliedUnits.get(unitType).add(unit);
    }

    @Override
    public void onUnitDestroy(Unit unit) {
    	UnitType unitType = unit.getType();
    	game.drawTextScreen(300, 10, String.valueOf(unitType));
    	game.drawTextScreen(300, 20, String.valueOf(alliedUnits.get(unitType).contains(unit)));
    	alliedUnits.get(unitType).remove(unit);
    }

    @Override
    public void onStart() {
    	System.out.println("starting");
        game = mirror.getGame();
        game.setLocalSpeed(5);

        self = game.self();

        gameInfo.game = game;
        gameInfo.self = self;

        //Use BWTA to analyze map
        //This may take a few minutes if the map is processed first time!
        System.out.println("Analyzing map...");
        BWTA.readMap();
        BWTA.analyze();
        System.out.println("Map data ready");

        gameInfo.startingBase = BWTA.getStartLocation(self).getPosition();
        System.out.println(gameInfo.startingBase.toTilePosition().getX() + " : " + gameInfo.startingBase.toTilePosition().getY() + " eh");
        gameInfo.startingChoke = BWTA.getNearestChokepoint(gameInfo.startingBase);
        for (BaseLocation base : BWTA.getStartLocations()) {
        	if (!base.getPosition().equals(gameInfo.startingBase)) { // TODO: modify this for >2p maps
        		gameInfo.enemyStartingBase = base.getPosition();
        		break;
        	}
        }

        mapInfo = new MapInfo(gameInfo, game.mapName());
        buildingPlacementManager = new BuildingPlacementManager(gameInfo, mapInfo);

        int i = 0;
        for (BaseLocation baseLocation : BWTA.getBaseLocations()) {
        	System.out.println("Base location #" + (++i) + ". Printing location's region polygon:");
        	for(Position position : baseLocation.getRegion().getPolygon().getPoints()){
        		System.out.print(position + ", ");
        	}
        	System.out.println();
        }
        populateUnitMap(alliedUnits);
    }

    @Override
    public void onFrame() {
        game.drawTextScreen(10, 10, "Playing as " + self.getName() + " - " + self.getRace());
        game.drawTextScreen(10, 20, "Frame: " + String.valueOf(game.getFrameCount()));

        // for discovering wall information
//        for (int i = 0; i < game.mapWidth(); i++) {
//            for (int j = 0; j < game.mapHeight(); j++) {
//                game.drawTextMap(new TilePosition(i, j).toPosition(), i + "/" + j);
//            }
//        }

        // TODO: fix this hacky way of persisting attack state between squads that exist at different times
        String previousSquadAttackState = null;
        if (marineSquadManager != null) {
        	previousSquadAttackState = marineSquadManager.attackState;
        }
        marineSquadManager = new SquadManager(new ArrayList<>(alliedUnits.get(UnitType.Terran_Marine)), gameInfo);
        if (previousSquadAttackState != null) {
        	marineSquadManager.attackState = previousSquadAttackState;
        }

        if (self.supplyUsed() * 100 >= Util.intendedSupplyTotal(alliedUnits, self) * 75) {
        	if (Util.currentMineralsIncludingIntents(alliedUnits, self) >= 100) {
        		Unit scv = scvMiningMinerals();
        		if (scv != null) {
					TilePosition buildTile = buildingPlacementManager.getBuildTile(game, UnitType.Terran_Supply_Depot, self.getStartLocation());
					if (buildTile != null) {
						scv.build(UnitType.Terran_Supply_Depot, buildTile);
					}
        		}
        	}
        }
        if (alliedUnits.get(UnitType.Terran_SCV).size() < alliedUnits.get(UnitType.Terran_Command_Center).size() * 20) {
		    for (Unit commandCenter : alliedUnits.get(UnitType.Terran_Command_Center)) {
		    	if (Util.currentMineralsIncludingIntents(alliedUnits, self) >= 50 && !commandCenter.isTraining()) {
		    		commandCenter.train(UnitType.Terran_SCV);
		    	}
		    }
        }
        for (Unit barracks : alliedUnits.get(UnitType.Terran_Barracks)) {
        	if (Util.currentMineralsIncludingIntents(alliedUnits, self) >= 50 && !barracks.isTraining()) {
        		barracks.train(UnitType.Terran_Marine);
        	}
        }
        // TODO: make a CC at an expansion (find the next expo first), redistribute scvs etc.
        if (Util.currentMineralsIncludingIntents(alliedUnits, self) >= 150) {
        	Unit scv = scvMiningMinerals();
        	if (scv != null) {
				TilePosition buildTile = buildingPlacementManager.getBuildTile(game, UnitType.Terran_Barracks, self.getStartLocation());
				if (buildTile != null) {
					scv.build(UnitType.Terran_Barracks, buildTile);
				}
        	}
        }
        for (Unit scv : alliedUnits.get(UnitType.Terran_SCV)) {
        	if (scv.isIdle()) {
                Unit closestMineral = null;

                // finds the closest mineral
                for (Unit neutralUnit : game.neutral().getUnits()) {
                    if (neutralUnit.getType().isMineralField()) {
                        if (closestMineral == null || scv.getDistance(neutralUnit) < scv.getDistance(closestMineral)) {
                            closestMineral = neutralUnit;
                        }
                    }
                }
                if (closestMineral != null) {
                    scv.gather(closestMineral, false);
                }
        	}
        }

        marineSquadManager.run();

        int taskedScvs = 0;
        for (Unit scv : alliedUnits.get(UnitType.Terran_SCV)) {
        	if (scv.getOrder() != null && scv.getOrder() != Order.WaitForMinerals) {
        		taskedScvs++;
        	}
        }
        game.drawTextScreen(10, 280, "tasked: " + taskedScvs);

        game.drawTextScreen(10, 200, self.supplyTotal() + " : " + self.supplyUsed());
        game.drawTextScreen(10, 220, Util.intendedSupplyTotal(alliedUnits, self) + " : " + Util.currentMineralsIncludingIntents(alliedUnits, self));

        int i = 100;
        for (UnitType unitType : alliedUnits.keySet()) {
        	if (alliedUnits.get(unitType).size() > 0) {
        		game.drawTextScreen(10, i, unitType.toString() + " : " + alliedUnits.get(unitType).size());
        		i += 10;
        	}
        }
        if (lastFrame < game.getFrameCount() - 1) {
        	System.out.println("skipped " + lastFrame + " to " + game.getFrameCount());
        }
    	lastFrame = game.getFrameCount();
    }

    private Unit scvMiningMinerals() {
		for (Unit scv : alliedUnits.get(UnitType.Terran_SCV)) {
			if (scv.isGatheringMinerals()) {
				return scv;
			}
		}
		return null;
    }

    private void populateUnitMap(Map<UnitType, Set<Unit>> map) {
        Field[] fields = UnitType.class.getDeclaredFields();
        for (Field field : fields) {
        	field.setAccessible(true);
        	if (Modifier.isStatic(field.getModifiers())) {
        		try {
        			map.put((UnitType)field.get(null), new HashSet<Unit>());
        		} catch (Exception e) {
        			if (!(e instanceof ClassCastException)) {
        				System.out.println(e);
        			}
        		}
        	}
        }
    }

    public static void main(String[] args) {
        new GerbilBot().run();
    }
}