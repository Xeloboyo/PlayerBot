package aibot;
import arc.math.geom.Vec2;
import arc.struct.*;

public abstract class AITask{
    protected AIPlayer player;
    public Vec2 destination;
    public float arrivalDistance= 8;
    public boolean taskActive=false;
    public Runnable onExit = ()->{};

    Seq<AITask> nextTasks =  new Seq<>();

    public AITask(AIPlayer player, Vec2 destination){
        this.player = player;
        this.destination = destination;
    }
    void arrive(){
        taskActive = true;
        onArrive();
    }

    public void addNextTask(AITask nextTask){
        this.nextTasks.add(nextTask);
    }

    public void endTask(){
        taskActive = false;
        onExit.run();
    }

    //presetup for the task
    public abstract void onArrive();

    //actually doing the thing
    public abstract void doTask();

    //when a task is terminated prematurely
    public abstract void interrupt();
}
