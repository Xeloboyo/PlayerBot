package aibot.tasks;

import aibot.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;

//just moves about a spot randomly, does not exit.
public class IdleTask extends AITask{
    public IdleTask(AIPlayer player, Vec2 destination){
        super(player, destination);
    }
    float wait = 0;
    public float range = 30;
    public float minwait = 0,maxwait = 500;

    @Override
    public void onArrive(){

    }

    @Override
    public void doTask(){
        if(!player.hasPath()){
            if(wait>0){
                wait-= Time.delta;
            }else{
                player.pathTo(destination.x + Mathf.random(-range, range), destination.y + Mathf.random(-range, range));
                wait = Mathf.random(minwait,maxwait);
            }
        }
    }

    @Override
    public void interrupt(){

    }
}
