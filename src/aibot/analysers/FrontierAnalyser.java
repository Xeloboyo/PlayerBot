package aibot.analysers;

import aibot.*;
import mindustry.gen.*;
import mindustry.world.*;

/**
 * This will analyse the 'territories' encompassed by teams, this often comes from intuition.
 * As humans we know instinctively where the 'frontline' is or should be from a variety of factors. This module will likely be the most
 * expensive to run and attempt to analyse the frontier to simulate that intuition.
 * This informs the placement and layout of the base and which tiles are safe to build in.
 *
 * Plan of operation:
 * We need to define distinct levels of control of each team: (may change)
 *  - Safe building zones (SBZ), tiles that are not under risk of attack
 *      and would be where critical facilities like power generation, unit factories and ammo production should be located.
 *      If a critical facility stops being in a SBZ, defenses should be placed to ensure it is again.
 *
 *  - Territorial building zone (TBZ), tiles that are under the team's projected influence but may be under risk,
 *      these zones can expand into range of an enemy structure provided the structure doesn't target ground. Buildings placed here
 *      are mainly defense, drills, and other to expand the SBZ or to eliminate intrusions into the territory
 *
 *  - Border zones (BZ) Tiles that are right next to the enemy's territory, but have enemy influence (e.g. turrets)
 *      Offensive building should be used here.
 *
 *  - Demilitarised zones (DMZ), tiles that cannot be expanded into or are too risky to build in.
 *      This includes enemy core build areas, envrionment solid blocks and the interiors of unbuildable liquid surfaces.
 *
 *  - Unguarded Enemy zones (UEZ), tiles that are under the relative influence of the enemy but have no defense to ensure that.
 *       All tiles are initially assumed to be Unguarded enemy tiles.
 *
 *  - Guarded Enemy zones (GEZ), these tiles are under firm control and only the most extreme of offensive building should be used here.
 *
 *      TODO
 */
public class FrontierAnalyser extends WorldAnalyser{

    @Override
    public void init(WorldMapper w){

    }

    @Override
    public void consumeTile(Tile t){

    }

    @Override
    public void onBuildPlaced(Building b){

    }

    @Override
    public void onBuildRemoved(Building b){

    }
}
