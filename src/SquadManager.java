import java.util.ArrayList;
import java.util.List;

import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.WeaponType;

public class SquadManager {

	public static final String AGGRESSIVE_STATE = "AGGRESSIVE";
	public static final String UNSELECTED_STATE = "UNSELECTED";
	public static final String PASSIVE_STATE = "PASSIVE";

	public String attackState = UNSELECTED_STATE;

	private GameInfo gameInfo;
	private List<Unit> units;

	public SquadManager(GameInfo gameInfo) {
		this(new ArrayList<Unit>(), gameInfo);
	}
	public SquadManager(List<Unit> units, GameInfo gameInfo) {
		this.units = units;
		this.gameInfo = gameInfo;
	}

	public void add(Unit newUnit) {
		units.add(newUnit);
	}

	public void run() {
		String newAttackState = UNSELECTED_STATE;
		if (units.size() > 50) {
			newAttackState = AGGRESSIVE_STATE;
		} else {
			newAttackState = PASSIVE_STATE;
		}
    	gameInfo.game.drawTextScreen(10, 250, newAttackState + " : " + units.size());

    	if (newAttackState.equals(PASSIVE_STATE)) {
    	    Position startingChokeCenter = gameInfo.startingChoke.getCenter();
    		for (Unit unit : units) {
    			if ((!newAttackState.equals(attackState) || unit.isIdle()) && unit.getDistance(startingChokeCenter) > 200) {
    				unit.attack(startingChokeCenter);
    			}
    		}
    	} else if (newAttackState.equals(AGGRESSIVE_STATE)) {
    		for (Unit unit : units) {
    			WeaponType weaponType = unit.getType().groundWeapon();
    			List<Unit> targetableUnits = unit.getUnitsInRadius(weaponType.maxRange());
    			Unit highestPriorityTarget = targetableUnits.get(0);
    			double maxPriority = Integer.MIN_VALUE;
    			for (Unit target : targetableUnits) {
    				UnitType targetType = target.getType();
    				double targetPriority = (targetType.mineralPrice() + targetType.gasPrice()) /
    						(targetType.maxHitPoints() + targetType.maxShields());
    				if (!unit.getPlayer().equals(gameInfo.self) && targetPriority > maxPriority) { // TODO: pay attention to if this overprioritizes ursadons
    					maxPriority = targetPriority;
    					highestPriorityTarget = target;
    				}
    			}
    			if (maxPriority > Integer.MIN_VALUE) {
    				unit.attack(highestPriorityTarget);
    			} else {
    				unit.attack(gameInfo.enemyStartingBase);
    			}
    		}
    	}
        attackState = newAttackState;
	}
}
