package aibot.tasks;

import aibot.*;
import arc.math.*;
import arc.math.geom.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.world.*;

public class InventoryTransferTask extends AITask{
    boolean discardIfFull = false;
    Building build;
    float delay = 0;
    float delayBetweenRetries = 60;
    boolean todrop = false;
    {{
        arrivalDistance = Vars.itemTransferRange-10f;
    }}
    public InventoryTransferTask(AIPlayer player, Building destination, boolean discardIfFull){
        super(player, new Vec2(destination.x,destination.y));
        this.discardIfFull=discardIfFull;
        this.build = destination;

    }

    @Override
    public void onArrive(){

    }

    @Override
    public void doTask(){

        delay--;
        if(delay<=0){
            if(player.unit().stack.amount==0){
                taskActive=false;
                return;
            }
            if(todrop){
                InputHandler.dropItem(player.get(), Mathf.random(360));
                taskActive=false;
                return;
            }
            player.transferInventory(build);
            if(player.unit().stack.amount==0){
                taskActive=false;
                return;
            }else{
                if(discardIfFull){
                    todrop = true;
                }
                delay = delayBetweenRetries;
            }
        }

    }

    @Override
    public void interrupt(){
        taskActive=false;
    }
}
