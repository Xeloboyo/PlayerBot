package aibot.analysers;

import arc.func.*;
import arc.math.*;
import arc.struct.*;
import mindustry.*;
import mindustry.type.*;
import mindustry.world.*;

public class TileIndexer{
    Seq<Tile> index = new Seq<>(); // will change soon to better data structure


    public Tile getClosestTile(float x, float y, Boolf<Tile> filter){
        x/= Vars.tilesize;
        y/=Vars.tilesize;
        float cdist = Float.MAX_VALUE;
        Tile output=null;
        for(Tile t:index){
            if(!filter.get(t)){
                continue;
            }
            float fdist = Mathf.dst2(t.x,t.y,x,y);
            if(fdist<cdist){
                cdist = fdist;
                output = t;
            }
        }
        return output;
    }

    public void addTile(Tile t){
        index.add(t);
    }
}
