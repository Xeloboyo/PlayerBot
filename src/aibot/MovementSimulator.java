package aibot;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.*;

//usually the physics would be simulated by the client, so we need to do our own to make it look somewhat believable.
public class MovementSimulator{
    AIPlayer player;
    Vec2 velocity =new Vec2();
    public Seq<Point2> path = new Seq<>();

    public Vec2 prevPos =  new Vec2();
    public Vec2 nextPos =  new Vec2();
    Vec2 targetPos =  new Vec2();
    Vec2 tmp1 = new Vec2();
    float speed = 0;
    float rotation = 0;
    public float lengthAlongSegment = 0;
    public float segmentLength = 0;
    public int pindex = 0;

    public MovementSimulator(AIPlayer player){
        this.player = player;
    }

    public void reset(){
        lengthAlongSegment = 0;
        pindex = 0;
        if(path.isEmpty()){
            return;
        }
        prevPos.set(player.pos);
        nextPos.set(path.first().x * Vars.tilesize,path.first().y * Vars.tilesize);
        targetPos.set(path.peek().x * Vars.tilesize,path.peek().y * Vars.tilesize);
        segmentLength = nextPos.dst(prevPos);

        if(segmentLength==0){
            segmentLength=1;
        }
    }


    public void update(){

        if(player.pos==null || path.isEmpty()){
            velocity.set(0,0);
            speed=0;
            return;
        }
        if(Float.isNaN(player.pos.x) || Float.isNaN(player.pos.y)){
            stop();
            player.pos.set(0,0);
            return;
        }
        Unit unit = player.get().unit();
        UnitType type = unit.type;
        if(type.equals(UnitTypes.block)){
            return;
        }
        lengthAlongSegment += Time.delta * speed;
        lengthAlongSegment = Math.min(lengthAlongSegment,segmentLength);

        float tdist = tmp1.set(targetPos).sub(player.pos).len();
        speed = Math.min(speed + type.accel* Time.delta*1.3f, Math.min(tdist, type.speed));

        float t = lengthAlongSegment/segmentLength;
        player.pos.set(Mathf.lerp(prevPos.x,nextPos.x,t),Mathf.lerp(prevPos.y,nextPos.y,t));

        if(t>=1){
            pindex++;
            if(pindex>=path.size){
                stop();
                return;
            }
            prevPos.set(nextPos);
            nextPos.set(path.get(pindex).x * Vars.tilesize,path.get(pindex).y * Vars.tilesize);
            segmentLength = nextPos.dst(prevPos);
            lengthAlongSegment = 0;
        }

        velocity.set(speed*(nextPos.x-prevPos.x)/segmentLength , speed*(nextPos.y-prevPos.y)/segmentLength );
        rotation = velocity.angle();
    }

    public void stop(){
        path.clear();
    }
}
