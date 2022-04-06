package aibot;

import aibot.Utils.*;
import aibot.analysers.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

//contains all the analysers and provides data for higher level decisions
public class WorldMapper{
    ObjectMap<String,WorldAnalyser> analysers = new ObjectMap<>();
    World world;
    public int tick = 0;
    public long startuptime = 0;
    public ThreatAnalyser threatAnalyser;
    public OreAnalyser oreAnalyser;
    public TeamAnalyser teamAnalyser;

    public WorldMapper(World world){
        StopWatch startup = new StopWatch();
        this.world = world;
        threatAnalyser = new ThreatAnalyser();
        oreAnalyser = new OreAnalyser(new Item[]{
                    Items.copper,Items.lead,Items.sand, Items.coal, Items.titanium, Items.thorium
                });
        teamAnalyser = new TeamAnalyser();
        analysers.put("ores", oreAnalyser);
        analysers.put("threat", threatAnalyser);
        analysers.put("team", teamAnalyser);

        StopWatch stopWatch = new StopWatch();

        for(String key: analysers.keys()){
            analysers.get(key).init(this);
            analysers.get(key).mapper = this;
            analysers.get(key).startuptime += stopWatch.click();
        }
        for(String key: analysers.keys()){
            WorldAnalyser wa = analysers.get(key);
            for(Tile t:world.tiles){
                wa.consumeTile(t);
            }
            wa.startuptime += stopWatch.click();
        }
        startuptime = startup.click();
    }

    boolean called = false;
    public void callStatsDebug(){
        called = true;
        Call.sendMessage("[blue]World Mapper[gray] took [white]"+ Utils.formatMillis(startuptime)+"[gray] to complete startup" );
        for(String key: analysers.keys()){
            Call.sendMessage("[gray][World Analyser] [pink]'"+key+"' [gray]took [white]"+ Utils.formatMillis(analysers.get(key).startuptime)+"[gray] to complete analysis" );
        }
    }

    public <T extends WorldAnalyser> T analyser(Class<T> type, String name){
        return type.cast(analysers.get(name));
    }

    public WorldAnalyser analyser(String name){
        return analysers.get(name);
    }

    public void update(){
        if(!called){
            callStatsDebug();
        }
        for(String key: analysers.keys()){
            analysers.get(key).update();
        }
        tick++;
    }

    public void onBlockCreated(Building b){
        for(String key: analysers.keys()){
            analysers.get(key).onBuildPlaced(b);
        }
    }
    public void onBlockDestroyed(Building b){
        for(String key: analysers.keys()){
            analysers.get(key).onBuildRemoved(b);
        }
    }

    public World world(){
        return world;
    }
}


