package aibot.structure;


import aibot.*;
import aibot.structure.BuildPathfinder.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.Schematic.*;
import mindustry.world.*;

import java.util.*;

public abstract class BuildPathfinder<T extends BuildNode> extends Pathfinder<T>{

    AIStrategiser ai;

    public static Stile solid = new Stile(Blocks.cliff,0,0,null,(byte)0);
    public static Stile air = new Stile(Blocks.air,0,0,null,(byte)0);

    public Stile block(int x, int y, World w){

        if(ai!=null && ai.map!=null){
            var n= ai.map.getBlock(x,y);
            if(n!=null){
                return n.block;
            }
        }
        Tile t = w.tile(x,y);
        return t.block()==Blocks.air?air:solid; // its all cliff? always has been
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
