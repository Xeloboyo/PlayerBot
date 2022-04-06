package aibot.structure;

import aibot.*;
import aibot.structure.ChunkedStructureMap.*;
import aibot.structure.ChunkedStructureMap.StructureBlock.*;
import aibot.tasks.*;
import arc.struct.*;
import mindustry.*;
import mindustry.game.Schematic.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;

//stores the set of blocks to be placed.....

// 24/Oct/21 ----
// Instead of storing the structures themselves, perhaps an analyser could store all requested tiles..
public class Structure{
    public Seq<StructureBlock> blocks = new Seq<>();
    public AIStrategiser teamai;


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
    }



    public boolean complete(){
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
            Tile t = Vars.world.tile(s.block.x,s.block.y);
            Call.setTile(t,s.block.block, teamai.team,s.block.rotation);
            t.build.configure(s.block.config);
            s.update();
        }
    }
}
