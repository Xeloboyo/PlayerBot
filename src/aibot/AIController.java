package aibot;

import aibot.AIStrategiser.*;
import aibot.tasks.*;

//this class manages state and executes tasks based on what the strategist wants.
//some controllers are better then others.
public abstract class AIController{
    public AIPlayer player;
    public AIStrategiser strategiser;
    public AIRequest currentRequest;

    public AITask defaultTask(){
        return new IdleTask(player, player.pos.cpy());
    }
    public void init(AIPlayer player){
        this.player=player;
    }
    public abstract AIRequest next();
    public void joinNext(){
        currentRequest = next();
        if(currentRequest != null){
            currentRequest.controllers.add(this);
        }
    }
    public void abandonRequest(){
        currentRequest.controllers.remove(this);
        currentRequest =null;
    }

    public abstract void update();
    public abstract void reset();



}
