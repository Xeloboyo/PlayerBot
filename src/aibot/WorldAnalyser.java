package aibot;

import mindustry.gen.*;
import mindustry.world.*;

public abstract class WorldAnalyser{
    public WorldMapper mapper;
    boolean update = false;
    long startuptime = 0;
    public abstract void init(WorldMapper w);
    public abstract void consumeTile(Tile t);
    public void update(){}

    public abstract void onBuildPlaced(Building b);
    public abstract void onBuildRemoved(Building b);
}
