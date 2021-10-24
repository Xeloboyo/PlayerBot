package aibot.analysers;

import aibot.*;
import arc.func.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

import java.util.*;

public class OreAnalyser extends WorldAnalyser{
    Item[] oreDrops;
    public OrePatch[][] oremap;
    ObjectMap<Item,TileIndexer> ores = new ObjectMap<>();
    public Seq<OrePatch> orePatches = new Seq<>();

    public OreAnalyser(Item[] oreDrops){
        this.oreDrops = oreDrops;
        for(Item ore:oreDrops){
            ores.put(ore,new TileIndexer());
        }
    }

    public Tile getClosestOre(float x, float y, Item t){
        if(ores.get(t)!=null){
            return ores.get(t).getClosestTile(x,y, tile-> !tile.solid()&& tile.build==null);
        }
        return null;
    }

    @Override
    public void init(WorldMapper w){
        oremap = new OrePatch[w.world().width()][w.world().height()];
    }

    @Override
    public void consumeTile(Tile t){
        Item drop = t.drop();
        if(drop!= null){
            OrePatch patch = new OrePatch(drop);
            if(t.x>0 && oremap[t.x-1][t.y]!=null && oremap[t.x-1][t.y].drop == drop){
                final OrePatch np =  oremap[t.x-1][t.y];
                oremap[t.x-1][t.y].add(patch, tile->{oremap[tile.x][tile.y] = np;});
                patch = np;
            }
            if(t.y>0 && oremap[t.x][t.y-1]!=null && oremap[t.x][t.y-1]!=patch && oremap[t.x][t.y-1].drop == drop){
                orePatches.remove(patch);
                final OrePatch np =  oremap[t.x][t.y-1];
                oremap[t.x][t.y-1].add(patch, tile->{oremap[tile.x][tile.y] = np;});
                patch = np;
            }
            patch.add(t);
            oremap[t.x][t.y]=patch;
            if(!orePatches.contains(patch)){
                orePatches.add(patch);
            }

            if(ores.get(drop)!=null){
                ores.get(drop).addTile(t);
            }
        }
    }

    @Override
    public void onBuildPlaced(Building b){

    }

    @Override
    public void onBuildRemoved(Building b){

    }


    public static class OrePatch{
        public Item drop;
        public Seq<Tile> contains = new Seq<>();
        public int minx = Integer.MAX_VALUE, miny = Integer.MAX_VALUE, maxx = 0 ,maxy = 0;

        public OrePatch(Item drop){
            this.drop = drop;
        }

        void add(Tile t){
            contains.add(t);
            minx = Math.min(minx,t.x);
            miny = Math.min(miny,t.y);
            maxx = Math.max(maxx,t.x);
            maxy = Math.max(maxy,t.y);
        }
        void add(OrePatch t, Cons<Tile> onAdd){
            for(Tile tile:t.contains){
                add(tile);
                onAdd.get(tile);
            }
        }
    }


}
