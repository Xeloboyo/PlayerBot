package aibot;

import aibot.Pathfinder.*;
import aibot.PlayerPathfinder.*;
import aibot.analysers.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.gen.*;

import java.util.*;

//for air unit
public abstract class PlayerPathfinder extends Pathfinder<MoveNode>{
    AIPlayer player;
    ThreatAnalyser threat;

    public PlayerPathfinder(AIPlayer player){
        this.player = player;
        threat = AIGlobalControl.mapper.analyser(ThreatAnalyser.class,"threat");
    }

    public abstract void pathFind(Point2 from, Point2 to, Seq<Point2> out);


    public static class DesktopPather extends PlayerPathfinder{
        public DesktopPather(AIPlayer player){
            super(player);
        }

        @Override
        public void pathFind(Point2 from, Point2 to, Seq<Point2> out){
            Unit unit = player.unit();
            if((Math.abs(to.x-from.x)==1 && Math.abs(to.y-from.y)==1) || unit==null){
                out.add(from);
                out.add(to);
                return;
            }
            out.addAll(pathfind(from.x,from.y,to.x,to.y,Vars.world,(x,y,v,t)->{
                return threat.get(x,y).getDps(unit,v,t);
            },unit));
        }
    }


    public static class MoveNode extends Node{
        float directionChangePenalty = 0;
        float time=0;

        MoveNode(MoveNode prev, int x, int y, float addcost){
            super(prev,x,y,addcost);
        }

        MoveNode(int x, int y){
            super(x,y);
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof MoveNode){
                MoveNode n = (MoveNode)o;
                if(n.x == x && n.y == y){
                    return true;
                }
            }
            return false;
        }
    }


    @Override
    public MoveNode getNode(Node prev, int x, int y, float addcost){
        if(prev!=null){
            return new MoveNode((MoveNode)prev,x,y,addcost);
        }
        return new MoveNode(x,y);
    }

    @Override
    public void onPathingStart(){ }

    @Override
    public float addedcost(int nx, int ny, MoveNode current, int i, World world, Point2 changepos){
        float addedmovecost = current.directionChangePenalty;
        if(current.prev != null){
            float bonus = current.dirx == dirs[i][0] && current.diry == dirs[i][1] ? -maxbonus : 0;
            //                          prevcost + dot(prev dir, current dir) + bonus
            addedmovecost = Mathf.clamp(addedmovecost - 0.4f * (current.dirx * current.invdirlen * dirs[i][0] * invdirlens[i] + current.diry * current.invdirlen * dirs[i][1] * invdirlens[i] - 1) + bonus, 0, 1);
        }
        this.addedmovecost=addedmovecost;
        float movecost = minmovecost + addedmovecost;
        float addedcost = movecost * dirlens[i];
        this.ntime = (current.time + addedcost)*8;
        if(size == 0){
            addedcost += cost.get(nx,ny,times,ntime) * movecost; // larger units probably need large sample areas.
        }else{
            float maxcost = cost.get(nx,ny,times,ntime);
            maxcost = Math.max(maxcost, nx - size >= 0 ?             cost.get(nx - size,ny,times,ntime) : 0);
            maxcost = Math.max(maxcost, nx + size < world.width() ?  cost.get(nx + size,ny,times,ntime) : 0);
            maxcost = Math.max(maxcost, ny - size > 0 ?              cost.get(nx,ny - size,times,ntime) : 0);
            maxcost = Math.max(maxcost, ny + size < world.height() ? cost.get(nx,ny + size,times,ntime) : 0);
            addedcost += maxcost * movecost;
        }
        return addedcost;
    }

    @Override
    public void affectNode(MoveNode node){
        node.directionChangePenalty = addedmovecost;
        node.time = ntime;
    }

    @Override
    public float hueristic(int x, int y, int x2, int y2){
        return super.hueristic(x, y, x2, y2) * minmovecost + (addedmovecost * addedmovecost / maxbonus) * 0.5f;
    }

    //temp for each dir
    float addedmovecost = 0, ntime = 0;
    ///temp for each path
    int size = 0;
    float maxbonus = 0.1f,minmovecost =0;
    Costp cost;
    IntMap<Float> times;

    public Seq<Point2> pathfind(int x, int y, int x2, int y2, World world, Costp cost, Unit u){
        size = (int)(u.hitSize/ Vars.tilesize);
        minmovecost = 1f/u.type.speed;
        this.cost=cost;
        times = new IntMap<>();
        Seq<MoveNode> raw =  pathfind(x,y,x2,y2,world);
        Seq<Point2> out = new Seq<>();
        for(MoveNode m:raw){
            out.add(new Point2(m.x,m.y));
        }
        return out;
    }
    /*
    public Seq<Point2> pathfind(int x, int y, int x2, int y2, World world, Costp cost, Unit u){
        int size = (int)(u.hitSize/ Vars.tilesize);
        float minmovecost = 1f/u.type.speed;
        float maxbonus = 0.1f;

        MoveNode start = new MoveNode(x, y);
        MoveNode destination = null;

        IntMap<Float> times = new IntMap<>();
        IntMap<MoveNode> explored = new IntMap<>();
        PriorityQueue<MoveNode> queue = new PriorityQueue<>((a, b) -> Float.compare(a.hueristic + a.costsofar, b.hueristic + b.costsofar));

        queue.add(start);
        explored.put(getIntFromPoint(x, y), start);
        while(!queue.isEmpty()){
            MoveNode current = queue.poll();
            if(current.x == x2 && current.y == y2){
                destination = current;
                break;
            }
            for(int i = 0; i < dirs.length; i++){
                int nx = dirs[i][0] + current.x;
                int ny = dirs[i][1] + current.y;
                if(nx < 0 || ny < 0 || nx >= world.width() || ny >= world.height()){
                    continue;
                }
                float dirlen = dirlens[i];

                float addedmovecost = current.directionChangePenalty;

                if(current.prev != null){
                    float bonus = current.dirx == dirs[i][0] && current.diry == dirs[i][1] ? -maxbonus : 0;
                    //                          prevcost + dot(prev dir, current dir) + bonus
                    addedmovecost = Mathf.clamp(addedmovecost - 0.4f * (current.dirx * current.invdirlen * dirs[i][0] * invdirlens[i] + current.diry * current.invdirlen * dirs[i][1] * invdirlens[i] - 1) + bonus, 0, 1);
                }
                float movecost = minmovecost + addedmovecost;
                float addedcost = movecost * dirlen;
                float ntime = (current.time + addedcost)*8;
                if(size == 0){
                    addedcost += cost.get(nx,ny,times,ntime) * movecost; // larger units probably need large sample areas.
                }else{
                    float maxcost = cost.get(nx,ny,times,ntime);
                    maxcost = Math.max(maxcost, nx - size >= 0 ?             cost.get(nx - size,ny,times,ntime) : 0);
                    maxcost = Math.max(maxcost, nx + size < world.width() ?  cost.get(nx + size,ny,times,ntime) : 0);
                    maxcost = Math.max(maxcost, ny - size > 0 ?              cost.get(nx,ny - size,times,ntime) : 0);
                    maxcost = Math.max(maxcost, ny + size < world.height() ? cost.get(nx,ny + size,times,ntime) : 0);
                    addedcost += maxcost * movecost;
                }

                // die if theres a better path already here
                int poskey = getIntFromPoint(nx, ny);
                if(explored.containsKey(poskey)){
                    if(explored.get(poskey).costsofar <= addedcost + current.costsofar){
                        continue;
                    }
                }

                MoveNode branch = new MoveNode(current, nx, ny, addedcost);
                branch.hueristic = hueristic(nx, ny, x2, y2) * minmovecost + (addedmovecost * addedmovecost / maxbonus) * 0.5f;
                branch.dirx = dirs[i][0];
                branch.diry = dirs[i][1];
                branch.directionChangePenalty = addedmovecost;
                branch.time = ntime;
                branch.invdirlen = invdirlens[i];


                queue.add(branch);

                explored.put(getIntFromPoint(nx, ny), branch);
            }
        }
        Seq<Point2> path = new Seq<>();
        MoveNode p = null;
        MoveNode c = destination;
        while(c != null){
            if(p==null || (p.dirx!=c.dirx || p.diry != c.diry)){
                path.add(new Point2(c.x, c.y));
            }
            p = c;
            c = c.prev;

        }
        path.reverse();
        return path;
    }*/

    @Override
    public void postProcess(Seq<MoveNode> path){
        if(path.isEmpty()){return;}
        for(int i=1;i<path.size;i++){
            MoveNode p = path.get(i-1);
            MoveNode c = path.get(i);
            if(!(p.dirx!=c.dirx || p.diry != c.diry)){
                path.remove(i);
                i--;
            }
        }
        super.postProcess(path);
    }

    public interface Costp{
        float get(int x, int y,IntMap<Float> visited, float time);
    }


}
