package aibot.structure;

import aibot.*;
import aibot.structure.ChunkedStructureMap.*;
import aibot.structure.ChunkedStructureMap.StructureBlock.*;
import aibot.util.*;
import arc.struct.*;
import mindustry.*;
import mindustry.game.Schematic.*;
import mindustry.gen.*;
import mindustry.world.*;

//stores a group of connected blocks with a similar purpose for ease of computation, similar to a schematic.
public class Structure{
    public Seq<StructureBlock> blocks = new Seq<>();
    public AIStrategiser teamai;
    public int minx=99999,miny=99999,maxx,maxy;

    public boolean generating = false;
    public Task generatingTask = null;
    public Runnable onGenerated = ()->{};


    public Structure(AIStrategiser teamai){
        this.teamai = teamai;
    }

    public void addBlock(Block b, int x,int y){
        addBlock(b,x,y,null,0);
    }
    public void addBlock(Block b, int x,int y,int rotation){
        addBlock(b,x,y,null,rotation);
    }
    public void addBlock(Block b, int x,int y, Object config){
        addBlock(b,x,y,config,0);
    }
    public void addBlock(Block b, int x,int y, Object config,int rotation){
        blocks.add(new StructureBlock(new Stile(b,x,y,config,(byte)rotation)));
        blocks.peek().structure = this;
        int o = -(b.size-1)/2;
        minx = Math.min(x+o, minx);
        maxx = Math.max(x+o+b.size-1, maxx);
        miny = Math.min(y+o, miny);
        maxy = Math.max(y+o+b.size-1, maxy);
    }

    public void addBlockDynamic(Block b, int x,int y, Object config,int rotation){
        addBlock(b,x,y,config,rotation);
    }
    public void removeBlock(StructureBlock b){
        blocks.remove(b);
        recalcBounds();
    }
    //called by the strategiser.
    public void onGenerated(){};



    public void updateProximity(){}

    public void recalcBounds(){
        minx = 99999;
        maxx = 0;
        miny = 99999;
        maxy = 0;
        Stile tile;
        int size;
        for(var block:blocks){
            tile = block.stile;
            size = tile.block.size-1;
            int o = -(size)/2;
            minx = Math.min(tile.x+o, minx);
            maxx = Math.max(tile.x+o+size, maxx);
            miny = Math.min(tile.y+o, miny);
            maxy = Math.max(tile.y+o+size, maxy);
        }
    }

    public void mergeStructure(Structure structure){
        for(var block:structure.blocks){
            blocks.add(block);
            block.structure = this;
        }
        structure.blocks.clear();
    }


    public boolean isComplete(){
        if(blocks.isEmpty()){
            return true;
        }
        for(StructureBlock s:blocks){
            s.update();
            if(s.status != BuildStatus.FINISHED){
                return false;
            }
        }
        return true;
    }

    //spawns the structure immediately, used in console.
    public void cheatBuild(){
        for(StructureBlock s:blocks){
            Tile t = Vars.world.tile(s.stile.x,s.stile.y);
            Call.setTile(t,s.stile.block, teamai.team,s.stile.rotation);
            t.build.configure(s.stile.config);
            s.update();
        }
    }
}
