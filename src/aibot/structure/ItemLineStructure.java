package aibot.structure;

import aibot.*;
import aibot.structure.ChunkedStructureMap.*;
import aibot.structure.ConveyorPathfinder.*;
import arc.struct.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;

public class ItemLineStructure extends Structure{
    public Item[] carrying;
    public float maxthroughput = 0;
    public Building targetstructure;
    // supply this information
    // analyse schematics
    // then make drills. :troll:

    public static Item[] getSurrounding(int x, int y, AIStrategiser team){
        Seq<Item> emitted = new Seq<>();
        for(int i = 0;i<4;i++){
            var t= team.map.getEmitter(x,y,i);
            if(t==null || t.length ==0){
                continue;
            }
            for(Item item:t){
                if(!emitted.contains(item)){
                    emitted.add(item);
                }
            }
        }

        return emitted.toArray(Item.class);
    }

    private ItemLineStructure(AIStrategiser team){
        super(team);
        carrying = new Item[]{};
    }

    public ItemLineStructure(int x, int y, Building to, AIStrategiser team){
        this(x,y,to,getSurrounding(x,y,team),team);
    }
    //todo: some kind of class that handles structures generated over multiple ticks.
    public ItemLineStructure(int x, int y, Building to, Item[] items,AIStrategiser team){
        super(team);
        ConveyorPathfinder conveyorPathfinder = new ConveyorPathfinder();
        conveyorPathfinder.allowedItems.addAll(items);
        Seq<ConveyorNode> path =  conveyorPathfinder.pathfind(x,y,to, Vars.world);
        maxthroughput = 60;
        for(ConveyorNode bn:path){
            addBlock(bn.block,bn.x,bn.y,bn.config,bn.rotation); //hopefully its in order
        }
        targetstructure = to;
        carrying = items;
        recalcThroughput();

        Call.sendMessage("1st node:"+blocks.get(0).stile.x+","+blocks.get(0).stile.y+" -- "+blocks.get(0).stile.block);
        Call.sendMessage("Last node:"+blocks.peek().stile.x+","+blocks.peek().stile.y+" -- "+blocks.peek().stile.block);
    }

    public void recalcThroughput(){
        maxthroughput = 60;
        for(StructureBlock sbn:blocks){
            Block block = sbn.stile.block;
            if(block instanceof Conveyor conveyor){
                maxthroughput = Math.min(conveyor.displayedSpeed,maxthroughput);
            }else if(block instanceof Junction junction){
                maxthroughput = Math.min(junction.itemCapacity/(junction.speed/60f),maxthroughput);
            }else if(block instanceof BufferedItemBridge bridge){
                maxthroughput = Math.min(Math.min(bridge.bufferCapacity/(bridge.speed/60),15),maxthroughput);
            }else if(block instanceof ItemBridge bridge){
                maxthroughput = Math.min(60f/bridge.transportTime,maxthroughput);
            }
        }
    }

    @Override
    public void addBlockDynamic(Block b, int x, int y, Object config, int rotation){

        super.addBlockDynamic(b, x, y, config, rotation);
    }

    @Override
    public void updateProximity(){
        super.updateProximity();
    }

    @Override
    public void removeBlock(StructureBlock b){
        int index = blocks.indexOf(b);
        if(index==-1){
            return;
        }
        ItemLineStructure lineStructure = new ItemLineStructure(teamai);
        lineStructure.targetstructure = targetstructure;
        targetstructure = null;

        for(int i =index+1;i<blocks.size;i++){
            lineStructure.blocks.add(blocks.get(i));
            blocks.get(i).structure = lineStructure;
        }

        blocks.removeRange(index,blocks.size-1);
        recalcBounds();
        recalcThroughput();
        lineStructure.recalcThroughput();
        lineStructure.recalcBounds();
        teamai.structures.add(lineStructure);

    }


}
