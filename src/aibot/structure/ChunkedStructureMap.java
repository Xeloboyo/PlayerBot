package aibot.structure;


import aibot.*;
import aibot.structure.Structure.*;
import aibot.tasks.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.*;
import mindustry.game.Schematic.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;

import static aibot.structure.ChunkedStructureMap.StructureBlock.BuildStatus.*;

public class ChunkedStructureMap{
    IntMap<Chunk> chunks = new IntMap<>();
    public void addBlock(StructureBlock stile){
        if(stile.block.block.size==1){
            set(stile,stile.block.x,stile.block.y);
            return;
        }
        int offsetx = -(stile.block.block.size - 1) / 2;
        int offsety = -(stile.block.block.size - 1) / 2;
        for(int i =0;i<stile.block.block.size;i++){
            for(int j =0;j<stile.block.block.size;j++){
                set(stile,stile.block.x+i+offsetx,stile.block.y+j+offsety);
            }
        }
    }
    public void addBlock(Stile stile){
        addBlock(new StructureBlock(stile));
    }
    public StructureBlock getBlock(int x,int y){
        int gt = Point2.pack(x >> 4, y >> 4);
        if(!chunks.containsKey(gt)){
            return null;
        }
        return chunks.get(gt).get(x,y);
    }

    void set(StructureBlock sb, int x, int y){
        int gt = Point2.pack(x << 4, y << 4);
        if(!chunks.containsKey(gt)){
           chunks.put(gt,new Chunk(new Point2(x >> 4, y >> 4)));
        }
        chunks.get(gt).set(sb,x,y);
    }

    public static class Chunk{
        public static final int chunksize = 16;
        public StructureBlock[][] tile= new StructureBlock[chunksize][chunksize];
        public Point2 pos;

        public Chunk(Point2 pos){
            this.pos = pos;
        }
        void set(StructureBlock s, int x,int y){
            if(tile[x-pos.x*chunksize][y-pos.y*chunksize]!=null && s!=null){
                tile[x-pos.x*chunksize][y-pos.y*chunksize].set(s);
                return;
            }
            tile[x-pos.x*chunksize][y-pos.y*chunksize] = s;
        }
        StructureBlock get(int x,int y){
                   return tile[x-pos.x*chunksize][y-pos.y*chunksize];
               }
    }
    public static class StructureBlock{
        public BuildStatus status = BuildStatus.NOT_STARTED;
        public Stile block;
        public boolean pending = false;
        Structure structure = null;
        public StructureBlock(Stile block){
            this.block=block;
        }

        public enum BuildStatus{
            NOT_STARTED,UNDER_CONSTRUCTION,FINISHED,OBSTRUCTED
        }

        void update(){
            Tile tile = Vars.world.tile(block.x,block.y);
            if(tile!=null && tile.build!=null){
                if(tile.build.block == block.block && tile.build.rotation == block.rotation){
                    status = FINISHED;
                    pending = false;
                }
                else if(tile.build instanceof ConstructBuild){
                    ConstructBuild ctb = (ConstructBuild)tile.build;
                    if(ctb.block == block.block){
                        status = UNDER_CONSTRUCTION;
                    }
                }else{
                    status = NOT_STARTED;
                }
            }
        }

        public void set(StructureBlock other){
            block = other.block;
            status = other.status;
            pending = other.pending;
            update();
        }

        public BuildTask asBuildTask(AIPlayer player){
            return new BuildTask(player,block);
        }
    }
}
