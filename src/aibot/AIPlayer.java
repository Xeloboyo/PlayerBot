package aibot;

import aibot.PlayerPathfinder.*;
import aibot.controllers.*;
import aibot.tasks.*;
import arc.*;
import arc.graphics.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.game.Schematic.*;
import mindustry.gen.*;
import mindustry.net.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

//executes whatever tasks are given to it by the controller. also handles the updates itself.
public class AIPlayer{
    Player controlling;
    public MovementSimulator movement;
    public PlayerPathfinder pather;
    public AIController controller;
    public Seq<AITask> tasks = new Seq<>();
    int updateDelay =3, lastUpdate =0;

    public boolean manuallyRotating = false;
    public float manualRotation = 0;
    public Vec2 pos;
    Tile mining=null;
    boolean chatting = false;
    public boolean building = false;
    public boolean shooting = false;
    public Seq<BuildPlan> buildPlans = new Seq<>();
    public Vec2 point = new Vec2();
    public boolean demandsClosed = false;
    boolean defaultTask = false;
    public static int maxPlanSize = 300;
    public BuildPlan[] bp = new BuildPlan[maxPlanSize];

    AIPlayer(String name, Color color, String ip){
        this(name,color,ip,new Idler());
    }
    AIPlayer(String name, Color color, String ip,AIController controller){
        controlling =Player.create();
        controlling.name=name;
        controlling.color=color;
        controlling.con=(new NetConnection(ip){
            @Override
            public void send(Object object, boolean reliable){}
            @Override
            public void close(){demandsClosed = true;}
        });
        controlling.con.uuid = name;
        this.controller=controller;
    }

    public void init(Team t){
        add(t);
        controller.init(this);
        Events.fire(new PlayerConnect(controlling));
        NetServer.connectConfirm(controlling);

    }

    public void add(Team t){
        controlling.team(t);
        controlling.add();
        pather = new DesktopPather(this);
        movement = new MovementSimulator(this);
        tasks.clear();
        controller.reset();
    }
    //js ai().ais.get(Team.sharded).controllers.get(0).player.build(Blocks.copperWall, 40,82)
    public void pathTo(float x,float y){
        movement.stop();
        pather.pathFind(
        new Point2((int)(pos.x / tilesize), (int)(pos.y / tilesize)),
        new Point2((int)(x / tilesize), (int)(y / tilesize)),
        movement.path);
        movement.reset();
    }
    public void pathTo(Vec2 w){
        pathTo(w.x,w.y);
    }


    public void update(){
        sendUpdate();
        if(pos!=null){
            pos.set(unit());
            controller.update();
            if(tasks.isEmpty()){
                tasks.add(controller.defaultTask());
                defaultTask = true;
            }
            AITask task = currentTask();
            if(!task.taskActive){
                if(task.destination==null){
                    task.endTask();
                    tasks.remove(task);
                    movement.stop();
                }else{
                    if(pos.dst(task.destination) < task.arrivalDistance){
                        movement.stop();
                        task.arrive();
                    }else{
                        if(movement.path.isEmpty()){
                            pathTo(task.destination.x, task.destination.y);
                        }
                    }
                }
            }else if(task.taskActive){
                task.doTask();
                if(!task.taskActive){
                    task.endTask();
                    tasks.remove(task);
                    movement.stop();
                }
            }
            buildPlans.filter(b->b!=null && b.progress<1);
            buildPlans.filter(b->{
                if(!b.breaking){
                    return true;
                }
                if(world==null || world.tile(b.x,b.y)==null){
                    return false;
                }
                return world.tile(b.x,b.y).build!=null;
            });
        }
        movement.update();

    }

    public AITask currentTask(){
        return tasks.isEmpty()?null:tasks.first();
    }

    //add something to do to the back of the queue
    public void addTask(AITask task){
        addTask(task,()->{});
    }
    public void addTask(AITask task, Runnable onExit){
        task.onExit=onExit;
        if(defaultTask){
            tasks.clear();
            defaultTask=false;
        }

        tasks.add(task);
    }
    //add something to do to the front of the queue to be done right now
    public void interruptTask(AITask task){
        interruptTask(task,()->{});
    }
    public void interruptTask(AITask task, Runnable onExit){
        task.onExit=onExit;
        AITask oldtask = currentTask();
        if(oldtask!=null){
            oldtask.interrupt();
            oldtask.endTask();
        }
        movement.stop();
        if(defaultTask){
            tasks.clear();
            defaultTask=false;
        }
        tasks.insert(0,task);
    }


    public void chat(String msg){
        interruptTask(new ChatTask(this,msg));
    }
    public void build(Block block, int x, int y){interruptTask(new BuildTask(this,new Stile(block, x, y, null, (byte)0)));}

    public void sendUpdate(){
        if(controlling==null || controlling.unit()==null || controlling.unit().type==null){
            pos=null;
            movement.stop();
            return;
        }
        if(pos==null){
            initPos();
        }
        lastUpdate++;
        if(lastUpdate>=updateDelay){
            bp = new BuildPlan[Math.min(maxPlanSize,buildPlans.size)];
            for(int i = 0;i<bp.length;i++){
                bp[i] = buildPlans.get(i);
            }
            lastUpdate=0;
            NetServer.clientSnapshot(
            controlling,
            1,
            controlling.unit().id,
            false,
            pos.getX(),
            pos.getY(),
            point.x,
            point.y,
            manuallyRotating ? manualRotation : movement.rotation,
            0,
            movement.velocity.x,
            movement.velocity.y,
            mining,
            false,
            shooting,
            chatting,
            building,
            bp,
            0,
            0,
            1,
            1);
        }
    }



    void initPos(){
        pos= new Vec2(controlling.unit().x,controlling.unit().y);
        movement.stop();
    }

    public void setChatting(boolean chatting){
        this.chatting = chatting;
    }

    public void setMining(Tile mining){
        this.mining = mining;
    }

    public void pointAt(Position point){
        this.point.set(point);
        this.manualRotation = this.point.cpy().sub(pos).angle();
    }

    public void transferInventory(Building build){
        Call.transferInventory(controlling, build);
    }

    public Player get(){
        return controlling;
    }
    public Team team(){ return controlling.team();}
    public Unit unit(){ return controlling.unit();}
    public CoreBuild core(){
        return controlling.team().core();
    }

    public boolean hasPath(){
        return !movement.path.isEmpty();
    }
}