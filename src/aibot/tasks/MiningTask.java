package aibot.tasks;

import aibot.*;
import aibot.analysers.*;
import arc.math.*;
import arc.math.geom.*;
import mindustry.*;
import mindustry.world.*;

import static aibot.AIGlobalControl.mapper;

public class MiningTask extends AITask{
    Tile mine;
    float duration, timemined=0 ;

    //random movements to simulate player
    float impatience = 0;
    float delay  = 0;
    {{
        arrivalDistance = Vars.mineTransferRange*0.5f;
    }}
    public MiningTask(AIPlayer player, Tile target,float duration){
        super(player, new Vec2(target.worldx(),target.worldy()));
        this.duration=duration;
        this.mine =target;
    }

    @Override
    public void onArrive(){
        player.setMining(mine);
    }

    @Override
    public void doTask(){
        if(timemined==0){
            impatience = Mathf.random(0,timemined*0.001f+0.2f);
        }

        delay--;
        if(delay<0){
            float mineradius = player.unit().type.miningRange - 10;
            float rad = mineradius/(2f*impatience+1);
            Vec2 park = Utils.randVec(rad).add(destination);

            while(mapper.threatAnalyser.get(park.x,park.y) != null && mapper.threatAnalyser.get(park.x,park.y).getDps(player.unit())>0){
                park = Utils.randVec(rad).add(destination);
            }
            player.pathTo(park);
            float time = 200f/((3*impatience)+0.1f);
            delay = Math.max(time+Mathf.random(-10,10),20);
            if(mine.build!=null || player.unit().stack.amount>=player.unit().itemCapacity()){
                timemined=duration;
            }
        }


        timemined++;

        if(timemined>duration){
            taskActive = false;
            player.setMining(null);
        }
    }

    @Override
    public void interrupt(){
        player.setMining(null);
    }
}
