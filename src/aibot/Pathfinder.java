package aibot;

import aibot.PlayerPathfinder.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.core.*;

import java.util.*;

public abstract class Pathfinder<T extends Pathfinder.Node>{
    public static class Node{
        float costsofar = 0;
        public int x;
        public int y;
        public Node prev;
        float hueristic = 0;
        public int dirx;
        public int diry;
        float invdirlen = 1;

        public Node(Node prev, int x, int y, float addcost){
            this.prev = prev;
            this.x = x;
            this.y = y;
            this.costsofar = prev.costsofar + addcost;
        }

        public Node(int x, int y){
            this.x = x;
            this.y = y;
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

    public T getNode(Node prev, int x, int y, float addcost){
        if(prev!=null){
            return (T)new Node(prev,x,y,addcost);
        }
        return (T)new Node(x,y);
    }

    //octile distance;
    public float hueristic(int x, int y, int x2, int y2){
        int dx = Math.abs(x - x2);
        int dy = Math.abs(y - y2);
        return 1.414f * Math.min(dx, dy) + Math.abs(dx - dy);
    }

    int getIntFromPoint(int x, int y){
        return x + y * 10000;
    }

    public int[][] dirs = {{-1, 0}, {-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}};
    public float[] dirlens = {1,1.414f,1,1.414f,1,1.414f,1,1.414f};
    public float[] invdirlens = {1,0.7071f,1,0.7071f,1,0.7071f,1,0.7071f};

    public void recalcLens(){
        dirlens = new float[dirs.length];
        invdirlens = new float[dirs.length];
        for(int i = 0;i<dirs.length;i++){
            dirlens[i] = Mathf.dst(dirs[i][0],dirs[i][1]);
            invdirlens[i] = 1f/dirlens[i];
        }
    }

    public final float maxcost = 999999f;

    public abstract void onPathingStart();

    public abstract float addedcost(int nx,int ny, T current, int dirindex, World w, Point2 changepos);

    public boolean isDestination(int x, int y, int x2, int y2,World w){
        return (x == x2 && y == y2);
    }
    //debug
    protected int steps;

    public boolean inBounds(int x,int y,World world){
        return !(x < 0 || y < 0 || x >= world.width() || y >= world.height());
    }



    protected Seq<T> pathfind(int x, int y, int x2, int y2, World world){
        steps = 0;
        T start = getNode(null,x, y,0);
        affectNode(start);
        T destination = null;

        IntMap<Float> times = new IntMap<>();
        IntMap<T> explored = new IntMap<>();
        PriorityQueue<T> queue = new PriorityQueue<>((a, b) -> Float.compare(a.hueristic + a.costsofar, b.hueristic + b.costsofar));
        onPathingStart();

        queue.add(start);
        explored.put(getIntFromPoint(x, y), start);
        Point2 newpos = new Point2(-1,-1);
        while(!queue.isEmpty()){
            steps++;
            T current = queue.poll();
            if(isDestination(current.x,current.y,x2 ,y2,world)){
                destination = current;
                break;
            }
            for(int i = 0; i < dirs.length; i++){
                int nx = dirs[i][0] + current.x;
                int ny = dirs[i][1] + current.y;
                if(!inBounds(nx,ny,world)){
                    continue;
                }
                newpos.set(-1,-1);
                float addedcost = addedcost(nx,ny,current,i,world,newpos);
                if(addedcost>maxcost){
                    continue;
                }
                if(newpos.x!=-1){
                    nx = newpos.x;
                    ny = newpos.y;
                }

                // die if theres a better path already here
                int poskey = getIntFromPoint(nx, ny);
                if(explored.containsKey(poskey)){
                    if(explored.get(poskey).costsofar <= addedcost + current.costsofar){
                        continue;
                    }
                }

                T branch = getNode(current, nx, ny, addedcost);
                branch.hueristic = hueristic(nx, ny, x2, y2) ;
                branch.dirx = dirs[i][0];
                branch.diry = dirs[i][1];
                branch.invdirlen = invdirlens[i];
                affectNode(branch);
                queue.add(branch);

                explored.put(getIntFromPoint(nx, ny), branch);
            }
        }
        Seq<T> path = new Seq<>();
        T p = null;
        T c = destination;
        while(c != null){
            path.add(c);
            p = c;
            c = (T)c.prev;
        }
        postProcess(path);
        return path;
    }

    public void affectNode(T node){

    }

    public void postProcess(Seq<T> path){
        path.reverse();
    }
}
