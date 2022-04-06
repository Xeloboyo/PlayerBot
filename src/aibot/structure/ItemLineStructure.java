package aibot.structure;

import aibot.*;
import aibot.structure.ConveyorPathfinder.*;
import arc.struct.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.distribution.*;

public class ItemLineStructure extends Structure{
    Item[] carrying;
    float maxthroughput = 0;
    Building targetstructure;
    // supply this information
    // analyse schematics
    // then make drills. :troll:
    public ItemLineStructure(int x, int y, Building to, Item[] items,AIStrategiser team){
        super(team);
        ConveyorPathfinder conveyorPathfinder = new ConveyorPathfinder();
        conveyorPathfinder.allowedItems.addAll(items);
        Seq<ConveyorNode> path =  conveyorPathfinder.pathfind(x,y,to, Vars.world);
        maxthroughput = 60;
        for(ConveyorNode bn:path){
            addBlock(bn.block,bn.x,bn.y,bn.config,bn.rotation); //hopefully its in order
            if(bn.block instanceof Conveyor conveyor){
                maxthroughput = Math.min(conveyor.displayedSpeed,maxthroughput);
            }else if(bn.block instanceof Junction junction){
                maxthroughput = Math.min(junction.itemCapacity/(junction.speed/60f),maxthroughput);
            }else if(bn.block instanceof BufferedItemBridge bridge){
                maxthroughput = Math.min(Math.min(bridge.bufferCapacity/(bridge.speed/60),15),maxthroughput);
            }else if(bn.block instanceof ItemBridge bridge){
                maxthroughput = Math.min(60f/bridge.transportTime,maxthroughput);
            }
        }
        targetstructure = to;
    }
}
