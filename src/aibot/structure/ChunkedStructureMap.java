package aibot.structure;


import aibot.*;
import aibot.tasks.*;
import arc.graphics.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.Schematic.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.blocks.distribution.ItemBridge.*;


import static aibot.BlockCategories.*;
import static aibot.structure.ChunkedStructureMap.StructureBlock.BuildStatus.*;
import static mindustry.Vars.world;

public class ChunkedStructureMap{
    IntMap<Chunk> chunks = new IntMap<>();
    OrderedSet<Point2> emitterUpdatePos = new OrderedSet<>();

    public void addBlock(StructureBlock structureBlock){
        if(structureBlock.stile.block.size==1){
            set(structureBlock,structureBlock.stile.x,structureBlock.stile.y);
            updateSurround(structureBlock.stile.x,structureBlock.stile.y);
            if(structureBlock.stile.block.configurable && structureBlock.stile.block.outputsItems()){
                emitterUpdatePos.add(new Point2(structureBlock.stile.x,structureBlock.stile.y));
            }
            return;
        }
        int offsetx = -(structureBlock.stile.block.size - 1) / 2;
        int offsety = -(structureBlock.stile.block.size - 1) / 2;
        for(int i = 0; i<structureBlock.stile.block.size; i++){
            for(int j = 0; j<structureBlock.stile.block.size; j++){
                set(structureBlock,structureBlock.stile.x+i+offsetx,structureBlock.stile.y+j+offsety);
            }
        }
        updateSurround(structureBlock.stile.x+offsetx,structureBlock.stile.y+offsety,structureBlock.stile.block.size);
        if(structureBlock.stile.block.configurable && structureBlock.stile.block.outputsItems()){
            emitterUpdatePos.add(new Point2(structureBlock.stile.x+offsetx,structureBlock.stile.y+offsety));
        }
    }
    public void addBlock(Stile stile){
        addBlock(new StructureBlock(stile));
    }

    public void updateDynamic(){
        for(var pt:emitterUpdatePos){
            updateSurround(pt.x, pt.y,getBlock(pt.x, pt.y).stile.block.size);
        }
    }

    void updateSurround(int x,int y){
        updateTile(x, y);
        for(int i = 0;i<4;i++){
            int ax = Geometry.d4x(i)+x;
            int ay = Geometry.d4y(i)+y;
            if(!(ax<0 || ax>=world.width() ||ay<0 || ay>=world.height())){
                updateTile(ax,ay);
            }
        }
    }

    void updateTile(int x,int y){
        int gt = Point2.pack(x >> 4, y >> 4);
        if(!chunks.containsKey(gt)){
            chunks.put(gt,new Chunk(this,new Point2(x >> 4, y >> 4)));
        }
        chunks.get(gt).update(x,y);
    }

    void updateSurround(int x,int y,int size){
        if(size==1){
            updateSurround(x,y);
            return;
        }
        for(int i = 0; i<size; i++){
            for(int j = 0; j<size; j++){
                updateTile(x+i,y+j);
            }
        }
        int acx = 0;
        int acy = 0;
        for(int i = 0;i<4;i++){
            acx += Geometry.d4x(i)*(size-1);
            acy += Geometry.d4y(i)*(size-1);
            for(int z = 0;z<size; z++){
                int ax = acx + Geometry.d4x(i) + x + Geometry.d4x(i+1) * z;
                int ay = acy + Geometry.d4y(i) + y + Geometry.d4y(i+1) * z;
                if(!(ax < 0 || ax >= world.width() || ay < 0 || ay >= world.height())){
                    updateTile(ax, ay);
                }
            }
        }
    }
    //ai().ais.get(Team.sharded).map.getBlock(31,176)
    public void removeBlock(int x, int y){
        var block = getBlock(x,y);
        if(block==null){
            return;
        }
        int size = block.stile.block.size;
        int offsetx = -(size - 1) / 2;
        int offsety = -(size - 1) / 2;
        if(block.stile.block.size==1){
            set(null,x,y);
        }else{
            for(int i = 0; i < size; i++){
                for(int j = 0; j < size; j++){
                    set(null, block.stile.x + i + offsetx, block.stile.y + j + offsety);
                }
            }
        }
        if(block.getStructure()!=null){
            block.getStructure().removeBlock(block);
        }
        updateSurround(block.stile.x+offsetx,block.stile.y+offsety,size);
        if(emitterUpdatePos.contains(new Point2(block.stile.x+offsetx,block.stile.y+offsety))){
            emitterUpdatePos.remove(new Point2(block.stile.x+offsetx,block.stile.y+offsety));
        }
    }
    public StructureBlock getBlock(int x,int y){
        int gt = Point2.pack(x >> 4, y >> 4);
        if(!chunks.containsKey(gt)){
            return null;
        }
        return chunks.get(gt).get(x,y);
    }

    void set(StructureBlock sb, int x, int y){
        int gt = Point2.pack(x >> 4, y >> 4);
        if(!chunks.containsKey(gt)){
           chunks.put(gt,new Chunk(this,new Point2(x >> 4, y >> 4)));
        }
        chunks.get(gt).set(sb,x,y);
    }
    public Item[] getEmitter(int x, int y, int dir){
        int gt = Point2.pack(x >> 4, y >> 4);
        if(!chunks.containsKey(gt)){
            return null;
        }
        return chunks.get(gt).getEmittance(x,y,dir);
    }
    //null array is just literally nothing, empty array is every item.
    public static Item[] emitter(StructureBlock sb, int fx, int fy, World w){
        if(sb ==null){
            return null;
        }
        var b = sb.stile;
        if(!b.block.update || w.tile(b.x,b.y).build == null){
            return null;
        }
        //todo: use funcs so mods can add their own shit?...
        if(bridge.contains(b.block)){
            if(w.tile(b.x,b.y).build instanceof ItemBridgeBuild ibb){
                if(ibb.link != -1){ // only bridge ends emit
                    return null;
                }else{
                    //but not behind in that direction.
                    for(int i =0;i<ibb.incoming.size;i++){
                        int inc = ibb.incoming.get(i);
                        var tile = world.tile(inc);
                        if ((b.x-tile.x)*(b.x-fx) + (b.y-tile.y)*(b.y-fy)>0){
                            return  null;
                        }
                    }
                    return new Item[0];

                }
            }
            return new Item[0];
        }else if(conveyor.contains(b.block)){ //conveyor ends only emit in the direction
            int r = w.tile(b.x,b.y).build.rotation;
            if(b.x + Geometry.d4x(r) == fx && b.y + Geometry.d4y(r) == fy){
                return new Item[0];
            }
            return null;
        }else if(b.block.outputsItems()){
            var func = emitterType.get(b.block);
            if(func!=null){
                return func.get(w.tile(b.x,b.y).build);
            }
            return new Item[0];
        }
        return null;
    }
    // /js ai().ais.get(Team.sharded).map.debug()
    public void debugStructure(int x, int y){
        var blk = getBlock(x,y);
        if(blk!=null){
            for(var other:blk.structure.blocks){
                Call.effect(Fx.dooropen,other.stile.x*8,other.stile.y*8,1, Color.white);
            }
        }
    }

    public void debug(){
        for(var chunk:chunks){
            var t=  chunk.value.emitterItems;
            for(int i = 0;i < Chunk.chunksize;i++){
                for(int j = 0;j < Chunk.chunksize;j++){
                    int ax = (i+chunk.value.tilepos.x);
                    int ay = (j+chunk.value.tilepos.y);
                    if(chunk.value.tile[i][j]!=null){
                        Call.effect(Fx.dooropen,ax*8,ay*8,1, Color.white);
                    }
                    for(int z = 0;z<4;z++){
                       if(t[i][j][z]!=null){
                           Call.effect(Fx.overdriveWave,ax*8+Geometry.d4x(z)*2,ay*8+Geometry.d4y(z)*2,3, Color.cyan);
                       }
                    }
                }
            }
        }
        for(var pt:emitterUpdatePos){

            Call.effect(Fx.hitBulletBig,pt.x*8,pt.y*8,1, Color.white);
        }
    }

    public static class Chunk{
        public static final int chunksize = 16;
        public StructureBlock[][] tile= new StructureBlock[chunksize][chunksize];
        public Item[][][][] emitterItems = new Item[chunksize][chunksize][4][]; //oh golly gosh a 4d array
        public Point2 pos;
        public Point2 tilepos;
        public ChunkedStructureMap cmap;

        public Chunk(ChunkedStructureMap map,Point2 pos){
            this.pos = pos;
            tilepos = new Point2(pos.x*chunksize,pos.y*chunksize);
            this.cmap=map;
        }
        void set(StructureBlock s, int x,int y){
            if(tile[x-tilepos.x][y-tilepos.y]!=null && s!=null){
                tile[x-tilepos.x][y-tilepos.y].set(s);
                return;
            }
            tile[x-pos.x*chunksize][y-pos.y*chunksize] = s;
        }
        StructureBlock get(int x,int y){
           return tile[x-tilepos.x][y-tilepos.y];
        }

        Item[] getEmittance(int x, int y, int dir){
            return emitterItems[x-tilepos.x][y-tilepos.y][dir];
        }

        void update(int x,int y){
            var em = emitterItems[x-tilepos.x][y-tilepos.y];
            boolean b = (tile[x-tilepos.x][y-tilepos.y]!=null);
            for(int i = 0;i<4;i++){
                if(b){
                    em[i] = null;
                    continue;
                }
                int ax = Geometry.d4x(i)+x-tilepos.x;
                int ay = Geometry.d4y(i)+y-tilepos.y;
                if(ax<0 || ax>=chunksize ||ay<0 || ay>=chunksize){
                    em[i] = emitter(cmap.getBlock(ax+tilepos.x,ay+tilepos.y),x,y,world);
                }else{
                    em[i] = emitter(tile[ax][ay],x,y,world);
                }
            }
            if(b){
                var build = world.tile(x,y).build;
                if(build!=null){
                    tile[x - tilepos.x][y - tilepos.y].stile.config = build.config();
                }
            }
        }

    }
    public static class StructureBlock{
        public BuildStatus status = BuildStatus.NOT_STARTED;
        public Stile stile;
        public boolean pending = false;
        Structure structure = null;
        public StructureBlock(Stile stile){
            this.stile = stile;
        }

        public enum BuildStatus{
            NOT_STARTED,UNDER_CONSTRUCTION,FINISHED,OBSTRUCTED
        }

        void update(){
            Tile tile = Vars.world.tile(stile.x, stile.y);
            if(tile!=null && tile.build!=null){
                if(tile.build.block == stile.block && tile.build.rotation == stile.rotation){
                    status = FINISHED;
                    pending = false;
                }
                else if(tile.build instanceof ConstructBuild){
                    ConstructBuild ctb = (ConstructBuild)tile.build;
                    if(ctb.block == stile.block){
                        status = UNDER_CONSTRUCTION;
                    }
                }else{
                    status = NOT_STARTED;
                }
            }
        }

        public void set(StructureBlock other){
            stile = other.stile;
            status = other.status;
            pending = other.pending;
            update();
        }

        public BuildTask asBuildTask(AIPlayer player){
            return new BuildTask(player, stile);
        }

        public Structure getStructure(){
            return structure;
        }
    }
}
