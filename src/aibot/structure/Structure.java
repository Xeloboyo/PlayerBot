package aibot.structure;

import aibot.*;
import aibot.structure.Structure.StructureBlock.*;
import aibot.tasks.*;
import arc.struct.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.game.Schematic.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;

//stores the set of blocks to be placed.....

// 24/Oct/21 ----
// Instead of storing the structures themselves, perhaps an analyser could store all
public class Structure{
    public Seq<StructureBlock> blocks = new Seq<>();
    public Team team;

    public Structure(Team team){
        this.team=team;
    }

    public void addBlock(Block b, int x,int y){
        addBlock(b,x,y,null,0);
    }
    public void addBlock(Block b, int x,int y,int rotation){
        addBlock(b,x,y,null,rotation);
    }
    public void addBlock(Block b, int x,int y, Object config,int rotation){
        blocks.add(new StructureBlock(this,new Stile(b,x,y,config,(byte)rotation)));
    }
    public void addBlock(Block b, int x,int y, Object config){
        addBlock(b,x,y,config,0);
    }

    public boolean complete(){
        for(StructureBlock s:blocks){
            s.update();
            if(s.status != BuildStatus.FINISHED){
                return false;
            }
        }
        return true;
    }

    //spawns the structure immediately
    public void cheatBuild(){
        for(StructureBlock s:blocks){
            Tile t = Vars.world.tile(s.block.x,s.block.y);
            Call.setTile(t,s.block.block,team,s.block.rotation);
            t.build.configure(s.block.config);
            s.update();
        }
    }


    public static class StructureBlock{
        public BuildStatus status = BuildStatus.NOT_STARTED;
        public Stile block;
        public Structure structure;
        public boolean pending = false;
        public StructureBlock(Structure s,Stile block){
            this.block=block;
            this.structure=s;
        }

        public enum BuildStatus{
            NOT_STARTED,UNDER_CONSTRUCTION,FINISHED
        }

        void update(){
            Tile tile = Vars.world.tile(block.x,block.y);
            if(tile.build!=null){
                if(tile.build.block == block.block && tile.build.rotation == block.rotation){
                    status = BuildStatus.FINISHED;
                    pending = false;
                }
                else if(tile.build instanceof ConstructBuild){
                    ConstructBuild ctb = (ConstructBuild)tile.build;
                    if(ctb.block == block.block){
                        status = BuildStatus.UNDER_CONSTRUCTION;
                    }
                }else{
                    status = BuildStatus.NOT_STARTED;
                }
            }
        }

        public BuildTask asBuildTask(AIPlayer player){
            return new BuildTask(player,block);
        }
    }


}
