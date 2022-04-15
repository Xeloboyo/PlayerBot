package aibot.structure;


import aibot.*;
import aibot.structure.BuildPathfinder.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.Schematic.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.ItemBridge.*;

import static aibot.BlockCategories.*;
import static mindustry.Vars.world;

public abstract class BuildPathfinder<T extends BuildNode> extends Pathfinder<T>{

    AIStrategiser ai;

    public static Stile solid = new Stile(Blocks.cliff,0,0,null,(byte)0);
    public static Stile air = new Stile(Blocks.air,0,0,null,(byte)0);

    public static final int bad_emitter = 2;
    public static final int allowed_emitter = 1;
    public static final int no_emitter = 0;

    public Stile block(int x, int y, World w){

        if(ai!=null && ai.map!=null){
            var n= ai.map.getBlock(x,y);
            if(n!=null){
                return n.stile;
            }
        }
        Tile t = w.tile(x,y);
        return (t!=null && t.block()==Blocks.air)?air:solid; // its all cliff? always has been
    }

    public int emitter(int fx, int fy, int dir, ObjectSet<Item> allowedItems){
        if(ai ==null){
            return no_emitter;
        }
        var items = ai.map.getEmitter(fx,fy,dir);
        if(items==null){
            return no_emitter;
        }
        if(items.length==0 || allowedItems==null || allowedItems.isEmpty()){
            return bad_emitter;
        }
        for(int i = 0;i<items.length;i++){
            if(!allowedItems.contains(items[i])){
                return allowed_emitter;
            }
        }
        return bad_emitter;
    }

    public int leftOf(int dir){
        return (dir+1)%4;
    }
    public int rightOf(int dir){
        return (dir+3)%4;
    }


    /*
        three types of node:
            type conveyor:
                cost 1
                next node must be in the oriented front of previous
                there can be no tiles with item emitters to the side
                target tile cannot have anything on it.
            type junction:
                cost 2:
                next node must be in the same direction
                there can be no tiles with item emitters to one side and a item acceptor on the other.
                target tile can have conveyor that is perpendicular or empty
                target cannot have anything that isnt the previous in front.
            type bridge:
                cost:6
                if previous type is conveyor, use conveyor rules.
                otherwise blah blah.

    */
    public static class BuildNode extends Pathfinder.Node{
        int state;
        int rotation;


        Block block;
        Object config;

        public BuildNode(int x, int y, int state){
            super(x, y);
            this.state = state;
        }

        public BuildNode(Node prev, int x, int y, int state, float addcost){
            super(prev, x, y, addcost);
            this.state=state;
        }
    }
}
