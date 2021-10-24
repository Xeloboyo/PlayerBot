package aibot.tasks;

import aibot.*;
import arc.math.geom.*;
import mindustry.*;
import mindustry.gen.*;

public class ShootTask extends AITask{
    Teamc shootat;
    float maxdist=400;
    {{
        arrivalDistance = maxdist;
    }}
    public ShootTask(AIPlayer player, Teamc target){
        super(player, new Vec2(target.x(),target.y()));
        shootat = target;
    }

    @Override
    public void onArrive(){

    }

    @Override
    public void doTask(){
        if(!shootat.isAdded() || shootat.dst(destination)>maxdist){
            taskActive=false;
            player.shooting = false;
            player.manuallyRotating=false;
            return;
        }
        float range = player.unit().range();
        if(shootat.dst(player.unit())>range && !player.hasPath()){
            player.pathTo(Utils.approach(player.pos,new Vec2(shootat.x(),shootat.y()),range*0.25f));
            player.shooting = false;
            player.manuallyRotating=false;
        }else{
            player.shooting = true;
            player.manuallyRotating=true;
            player.pointAt(shootat);
        }
    }

    @Override
    public void interrupt(){
        player.shooting = false;
    }
}
