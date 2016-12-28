import java.util.Map;
import java.util.Set;

import bwapi.Player;
import bwapi.Unit;
import bwapi.UnitType;

public class Util {

    public static int currentMineralsIncludingIntents(Map<UnitType, Set<Unit>> alliedUnits, Player player) {
    	int intendedMinerals = player.minerals();
    	for (Unit scv : alliedUnits.get(UnitType.Terran_SCV)) {
    		if (scv.isConstructing() && scv.getBuildUnit() == null) {
    			intendedMinerals -= scv.getBuildType().mineralPrice();
    		}
    	}
    	return intendedMinerals;
    }

    public static int intendedSupplyTotal(Map<UnitType, Set<Unit>> alliedUnits, Player player) {
    	int intendedSupply = player.supplyTotal();
    	for (Unit scv : alliedUnits.get(UnitType.Terran_SCV)) {
    		if (scv.isConstructing() && scv.getBuildType() == UnitType.Terran_Supply_Depot) {
    			intendedSupply += UnitType.Terran_Supply_Depot.supplyProvided();
    		}
    	}
    	return intendedSupply;
    }

}
