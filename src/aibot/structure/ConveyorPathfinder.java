package aibot.structure;

import aibot.*;
import aibot.structure.ConveyorPathfinder.*;
import arc.func.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.pooling.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.game.Schematic.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.distribution.ItemBridge.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.production.Drill.*;
import mindustry.world.blocks.sandbox.*;
import mindustry.world.blocks.sandbox.ItemSource.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.blocks.storage.StorageBlock.*;
import mindustry.world.blocks.storage.Unloader.*;

import static aibot.AIGlobalControl.mapper;
import static mindustry.Vars.world;

public class ConveyorPathfinder extends BuildPathfinder<ConveyorNode>{
    //static ObjectSet<Block> emitter = new ObjectSet<>(); //blocks that can emit items (conveyors are not allowed to path next to them normally)
    //static ObjectSet<Block> accepter = new ObjectSet<>(); //blocks that can accept any item (conveyors are not allowed to unintentionally insert items in)
    static ObjectSet<Block> conveyor = new ObjectSet<>(); // conveyors.
    static ObjectSet<Block> bridge = new ObjectSet<>(); // bridges
    static ObjectSet<Block> empty = new ObjectSet<>(); // blocks that are effectively air (incl. air). e.g boulders.
    static ObjectMap<Block,Func<Building,Item[]>> emitterType = new ObjectMap<>();

    public Boolf<Block> emitter =  b->b.outputsItems();
    public Boolf<Block> accepter =  b->b.acceptsItems;

    static IntMap<Float> itemcost = new IntMap<>();

    public ObjectSet<Item> allowedItems = new ObjectSet<>();

    public interface TargetFunc{
        public Stile get(int x,int y);
    }

    Team team;
    Building target;
    ///dir
    Block type;
    int index;
    short touchingemitter = 0;
    boolean touchingAcceptor = false;

    public static void init(){
        //not individual blocks to have better compatibility with mods
        for(Block b:Vars.content.blocks()){
            if (b instanceof Conveyor){
                conveyor.add(b);
            }
            if (b instanceof ItemBridge){
                bridge.add(b);
            }
            if (b instanceof AirBlock || (b instanceof Prop && !b.solid)){
                empty.add(b);
            }
            if (b instanceof Drill){
                emitterType.put(b,(build)->{return new Item[]{((DrillBuild)build).dominantItem};});
            }
            if (b instanceof GenericCrafter crafter){
                emitterType.put(b,(build)-> new Item[]{ crafter.outputItem.item});
            }
            if (b instanceof Unloader){
                emitterType.put(b,(build)->{return new Item[]{((UnloaderBuild)build).config()};});
            }
            if (b instanceof ItemSource){
                emitterType.put(b,(build)->{return new Item[]{((ItemSourceBuild)build).config()};});
            }
        }
        //relative cost, real cost is influenced by the availability and convenience of items
        itemcost.put(Items.sand.id,0.8f);
        itemcost.put(Items.copper.id,0.4f);
        itemcost.put(Items.lead.id,0.6f);
        itemcost.put(Items.coal.id,1.0f);
        itemcost.put(Items.scrap.id,0.8f);
        itemcost.put(Items.titanium.id,1.0f);
        itemcost.put(Items.thorium.id,4.0f); // only ore that cannot be mined by units so yeh


    }
    ConveyorPathfinder(){
        dirs = new int[][]{{1,0},{0,1},{-1,0},{0,-1}};
        recalcLens();
    }

    public Seq<ConveyorNode> pathfind(int x, int y, Building building, World w){
        target = building;
        team = building.team;
        ai = AIGlobalControl.ais.get(team);
        type = Blocks.conveyor;
        touchingemitter = 0;
        return pathfind(x,y,building.tile.x,building.tile.y,w);
    }

    @Override
    public void onPathingStart(){

    }

    @Override
    public ConveyorNode getNode(Node prev, int x, int y, float addcost){
        if(prev!=null){
            return new ConveyorNode((ConveyorNode)prev,x,y,addcost);
        }
        return new ConveyorNode(x,y);
    }
    public boolean accepter(int x,int y,World w){
        return accepter(block(x,y,w),w);
    }
    public boolean accepter(Stile b, World w){
        if(b ==null || b==air || b==solid){
            return false;
        }
        if(bridge.contains(b.block)){
            return ((ItemBridgeBuild)w.tile(b.x,b.y).build).link != -1;
        }else if(accepter.get(b.block)){
            return true;
        }
        return false;
    }
    public boolean emitter(int fx,int fy,int x,int y,boolean allowAcceptedItems, World w){
        return emitter(block(x,y,w),fx,fy,allowAcceptedItems,w);
    }
    public boolean emitter(Stile b, int fx,int fy,boolean allowAcceptedItems,World w){
        if(b ==null || b==air || b==solid){
            return false;
        }
        if(!b.block.update || w.tile(b.x,b.y).build == null){
            return false;
        }
        if(bridge.contains(b.block)){
            if(w.tile(b.x,b.y).build instanceof ItemBridgeBuild ibb){
                if(ibb.link != -1){ // only bridge ends emit
                    return false;
                }else{
                    //but not behind in that direction.

                    for(int i =0;i<ibb.incoming.size;i++){
                        int inc = ibb.incoming.get(i);
                        var tile = world.tile(inc);
                        if ((b.x-tile.x)*(b.x-fx) + (b.y-tile.y)*(b.y-fy)>0){
                            return  false;
                        }
                    }
                    return true;

                }
            }
            return true;
        }else if(conveyor.contains(b.block)){ //conveyor ends only emit in the direction
            int r = w.tile(b.x,b.y).build.rotation;
            return b.x + dirs[r][0] == fx && b.y + dirs[r][1] == fy;
        }else if(emitter.get(b.block)){
            if(allowAcceptedItems && !allowedItems.isEmpty()){
                var func = emitterType.get(b.block);
                if(func!=null){
                    var items = func.get(w.tile(b.x,b.y).build);
                    for(Item item:items){
                        if(!allowedItems.contains(item)){
                            return true;
                        }
                    }
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public int rotation(int x,int y,World w){
        Tile t = w.tile(x,y);
        if(t.build!=null){
            return t.build.rotation;
        }
        return 0;
    }
    public boolean impassable(int x, int y, World w){
        return !empty.contains(block(x,y, w).block) || !w.floor(x,y).placeableOn;
    }

    public float floorcost(int x, int y, World w){
        float totalcost = 0;
        if(!w.tile(x,y).solid()){ // cant place drills there anyway so no penalty for bordering a ore patch under a wall or something
            Item drop = w.tile(x,y).drop();
            if(drop!=null){
                totalcost += itemcost.get(drop.id);
            }
        }
        totalcost += mapper.threatAnalyser.get(x,y).getGroundDps(team,true);

        return totalcost;
    }

    public boolean isSolid(Block b){
        return !empty.contains(b);
    }


    @Override
    public boolean isDestination(int x, int y, int x2, int y2,World world){
        if(target instanceof CoreBuild cb && world.tile(x,y).build instanceof StorageBuild sb){
            return sb.linkedCore == cb;
        }
        return world.tile(x,y).build == target || super.isDestination(x,y,x2,y2,world);
    }

    @Override
    public boolean inBounds(int x, int y, World world){
        return (x > 0 && y > 0 && x < world.width()-1 && y < world.height()-1);
    }

    @Override
    public float hueristic(int x, int y, int x2, int y2){
        float cd = Math.abs(x - x2)+Math.abs(y - y2);
        if(target instanceof CoreBuild cb){
            for( var b: cb.proximity){
                if(b instanceof StorageBuild sb && sb.linkedCore==cb){
                    cd = Math.min(cd, Math.abs(x - b.tile.x) + Math.abs(y - b.tile.y));
                }
            }
        }
        return cd;
    }

    @Override
    public float addedcost(int nx, int ny, ConveyorNode prevNode, int i, World w, Point2 changepos){
        //todo:
        // -option for armoured conveyors
        // -pathing for plastanium belts
        // -pathing for phase conveyors
        // -liquids

        index = i;
        if(isDestination(nx,ny,0,0,w)){
            return -999;
        }
        //cannot revert back on itself
        if(prevNode.prev != null){
            if(nx == prevNode.prev.x && nx == prevNode.prev.y){
                return maxcost + 1;
            }
        }
        //junctions must carry on the direction
        if(prevNode.block == Blocks.junction){
            if(dirs[i][0] != prevNode.dirx || dirs[i][1] != prevNode.diry){
                return maxcost + 1;
            }
        }
        float minmovecost = 1;
        float movecost = minmovecost;
        type = Blocks.conveyor;
        touchingemitter = 0;
        touchingAcceptor = false;
        boolean isBlocked = false;
        boolean junction_failed = true;
        Stile here = block(nx,ny,w);

        nobridge: {
            //if theres a conveyor here try to junction.
            if(conveyor.contains(here.block) && Vars.world.tile(nx, ny).build.team == team){
                if(here.rotation == i){
                    isBlocked = true; break nobridge;
                }
                int ax = nx + dirs[i][0];
                int ay = ny + dirs[i][1];
                if(!inBounds(ax, ay, w)){
                    return maxcost + 1;
                }
                if(impassable(ax, ay, w)){
                    isBlocked = true; break nobridge;
                }
                type = Blocks.junction;
                movecost = minmovecost * 2;
            }
            if(type == Blocks.conveyor){
                int ox = nx;
                int oy = ny;

                //if theres a junction there, tunnel through the junction to the other side
                while(block(nx, ny, w).block == Blocks.junction){
                    int ax = nx + dirs[i][0];
                    int ay = ny + dirs[i][1];
                    if(!inBounds(ax, ay, w)){
                        return maxcost + 1;
                    }
                    if(block(ax, ay, w).block != Blocks.junction && impassable(ax, ay, w)){
                        nx = ox;
                        ny = oy;
                        changepos.set(ox, oy);
                        junction_failed = true;
                        break;
                        //if otherside is blocked then revert
                    }
                    nx = ax;
                    ny = ay;
                    changepos.set(ax, ay);
                    junction_failed = false;
                }
                //check if the front is just blocked lmao
                isBlocked = impassable(nx,ny,w);
                if(isBlocked){
                    break nobridge;
                }
                //die if emitter in front
                if(emitter(nx,ny,nx + dirs[i][0], ny + dirs[i][1], true,w)){
                    isBlocked = true; break nobridge;
                }else{
                    int side = 0;
                    float mcostadd = 0;
                    //disallow any placed junctions allowing adjacent buildings to pollute each other.
                    Stile left = block(nx - dirs[i][1], ny + dirs[i][0],w);
                    Stile right = block(nx + dirs[i][1], ny - dirs[i][0],w);

                    if(emitter(left,nx,ny,false, w)){
                        if(accepter(right, w)){
                            isBlocked = true; break nobridge;
                        }else{
                            mcostadd=0.5f;
                        }
                        side++;
                    }
                    if(emitter(right,nx,ny,false, w)){
                        if(accepter(left, w)){
                            isBlocked = true; break nobridge;
                        }else{
                            mcostadd=0.5f;
                        }
                        side++;
                    }

                    if(side >0){
                        //just place junction if it wont interfere and at least one side is an emitter
                        type = Blocks.junction;
                        movecost = minmovecost * 2 + mcostadd;
                    }
                }

            }
        }
        //if going through the junctions is illegal then this path is likely fucked.
        if(!junction_failed && isBlocked){
            return maxcost + 1;
        }
        //cannot have the end of the bridge at the previous spot bc the previous spot was feeding into something.
        //so a bridge check is forced here.
        if(prevNode.touchingAccepter){
            junction_failed = true;
            isBlocked = true;
            type = Blocks.conveyor;
        }

        //if this tile is unbuildable attempt to bridge.
        if(type == Blocks.conveyor && isBlocked){
            int ax = nx + dirs[i][0];
            int ay = ny + dirs[i][1];
            for(int z = 0; z < 3; z++){
                int lx = ax + dirs[i][0] * z;
                int ly = ay + dirs[i][1] * z;
                if(!impassable(lx, ly, w)){
                    //makes sure theres nothing that would absorb bridge items unintentionally
                    //todo: what if the accepter is the target.
                    if(accepter(lx + dirs[i][0], ly + dirs[i][1], w) ||
                    accepter(lx - dirs[i][1], ly + dirs[i][0], w) ||
                    accepter(lx + dirs[i][1], ly - dirs[i][0], w)){
                        if(z<2){
                            continue;
                        }
                        touchingAcceptor = true;
                    }
                        type = Blocks.itemBridge;
                        movecost = minmovecost * 12;
                        nx = lx;
                        ny = ly;
                        changepos.set(lx, ly);
                        break;

                }
            }
            if(type == Blocks.conveyor){
                return maxcost + 1;
            }
        }

        //bridge will record where it touched a emitter. If it needs to bridge again, it can only bridge in the same direction as the emitter.
        if(type==Blocks.itemBridge){
            if(prevNode.block==Blocks.itemBridge){
                if(prevNode.touchedMultipleEmitter){
                   return maxcost + 1;
                }
                else if(prevNode.touchingemitter > 0 && prevNode.touchingemitter != 1 << index){
                    return maxcost + 1;
                }
            }
            for(int z = 0; z < dirs.length; z++){
                if(emitter(nx,ny,nx + dirs[z][0], ny + dirs[z][1],true,w)){
                    touchingemitter |= 1<<z;
                }
            }
        }

        float addedcost = movecost;
        if(prevNode.prev != null){
            float e = prevNode.dirx != dirs[i][0] || prevNode.diry != dirs[i][1] ? 0.5f : 0;
            addedcost += e;
        }
        //todo: effecient way to get whether its inside a build range
        //conveyor will try to avoid being next to ores and turret ranges.
        float maxfloorcost = floorcost(nx,ny, w);
        if(nx < w.width()-1)  maxfloorcost = Math.max(floorcost(nx+1,ny, w),maxfloorcost);
        if(nx > 0)            maxfloorcost = Math.max(floorcost(nx-1,ny, w),maxfloorcost);
        if(ny < w.height()-1) maxfloorcost = Math.max(floorcost(nx,ny+1, w),maxfloorcost);
        if(ny > 0)            maxfloorcost = Math.max(floorcost(nx,ny-1, w),maxfloorcost);
        return addedcost + maxfloorcost;
    }

    @Override
    public void postProcess(Seq<ConveyorNode> nodes){
        boolean madeBridge = false;
        if(nodes.isEmpty()){
            Call.sendMessage("oh no conveyor pathfinder died after "+steps+" steps");
            return;
        }
        int pdir = nodes.get(0).rotation;
        for(ConveyorNode n : nodes){
            if(n.block == Blocks.conveyor){
                int cdir = n.rotation;
                n.rotation = pdir;
                pdir = cdir;
            }
        }
        ConveyorNode forward=null;
        for(ConveyorNode n : nodes){
            if(!madeBridge){
                if(forward != null && forward.block == Blocks.itemBridge && n.block != Blocks.itemBridge){
                    n.block = Blocks.itemBridge;
                    madeBridge = true;
                }
            }else{
                madeBridge = false;
            }
            if(forward != null && n.block == Blocks.itemBridge){
                if(forward.block == Blocks.itemBridge){
                    n.config = Vars.world.tile(forward.x,forward.y).pos();
                }
            }
            forward = n;
        }
        nodes.remove(nodes.get(0));
    }

    @Override
    public void affectNode(ConveyorNode node){
        node.rotation = index%4;
        node.block = type;
        node.touchingemitter = touchingemitter;
        node.touchedMultipleEmitter =  touchingemitter!=0 && touchingemitter!=-1 && touchingemitter!=2 && touchingemitter!=4 && touchingemitter!=8;
        node.touchingAccepter = touchingAcceptor;
    }

    public static class ConveyorNode extends BuildNode{

        //yeah bridges suck
        short touchingemitter = 0;
        boolean touchedMultipleEmitter;
        boolean touchingAccepter;

        ConveyorNode(ConveyorNode prev, int x, int y, float addcost){
            super(prev, x, y,0, addcost);
        }

        ConveyorNode(int x, int y){
            super(x, y,0);
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof ConveyorNode){
                ConveyorNode n = (ConveyorNode)o;
                if(n.x == x && n.y == y){
                    return true;
                }
            }
            return false;
        }
    }
}
