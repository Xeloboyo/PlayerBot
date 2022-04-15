package aibot;

import arc.func.*;
import arc.struct.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.production.Drill.*;
import mindustry.world.blocks.sandbox.*;
import mindustry.world.blocks.sandbox.ItemSource.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.storage.Unloader.*;

public class BlockCategories{
    public static ObjectSet<Block> conveyor = new ObjectSet<>(); // conveyors.
    public static ObjectSet<Block> bridge = new ObjectSet<>(); // bridges
    public static ObjectSet<Block> empty = new ObjectSet<>(); // blocks that are effectively air (incl. air). e.g boulders.
    public static ObjectMap<Block, Func<Building, Item[]>> emitterType = new ObjectMap<>();
    public static IntMap<Float> itemcost = new IntMap<>();

    public static void init(){
        for(Block b : Vars.content.blocks()){
            if(b instanceof Conveyor){
                conveyor.add(b);
            }
            if(b instanceof ItemBridge){
                bridge.add(b);
            }
            if(b instanceof AirBlock || (b instanceof Prop && !b.solid)){
                empty.add(b);
            }
            if(b instanceof Drill){
                emitterType.put(b, (build) -> {
                    return new Item[]{((DrillBuild)build).dominantItem};
                });
            }
            if(b instanceof GenericCrafter crafter){
                emitterType.put(b, (build) -> new Item[]{crafter.outputItem.item});
            }
            if(b instanceof Unloader){
                emitterType.put(b, (build) -> {
                    return new Item[]{((UnloaderBuild)build).config()};
                });
            }
            if(b instanceof ItemSource){
                emitterType.put(b, (build) -> {
                    return new Item[]{((ItemSourceBuild)build).config()};
                });
            }
        }

        //relative cost, real cost is influenced by the availability and convenience of items
        itemcost.put(Items.sand.id, 0.8f);
        itemcost.put(Items.copper.id, 0.4f);
        itemcost.put(Items.lead.id, 0.6f);
        itemcost.put(Items.coal.id, 1.0f);
        itemcost.put(Items.scrap.id, 0.8f);
        itemcost.put(Items.titanium.id, 1.0f);
        itemcost.put(Items.thorium.id, 4.0f); // only ore that cannot be mined by units so yeh
    }

    public static class ItemQuantity{
        public Item item;
        public float amount;

    }
}
