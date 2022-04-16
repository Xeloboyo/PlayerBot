package aibot.util;

import arc.struct.*;
import mindustry.gen.*;

public class TaskManager{
    public OrderedMap<String,Task> tasks = new OrderedMap<>();
    public int cyclesPerTick = 1000;
    public int cyclesComputedLastTick = 0;

    public TaskManager(){
        tasks.orderedKeys().ordered = false;
    }

    public void onTick(){
        float totalweight = 0;
        for(var task:tasks.values()){
            totalweight+=1.0/task.computeWeight();
        }
        for(var task:tasks.values()){
            int amount = (int)Math.max(1,cyclesPerTick*(1.0/(task.computeWeight()*totalweight)));
            for(int i =0;i<amount;i++){
                task.doTask();
            }
            cyclesComputedLastTick+=amount;
        }
        var i = tasks.iterator();
        while(i.hasNext()){
            var r = i.next();
            if(r.value.isFinished()){
                Call.sendMessage(r.value.name()+" was completed");
                i.remove();
            }
        }
    }

    public void addTask(Task t){
        tasks.put(t.name(),t);
    }
    public void addQueuedTask(String name,Task t){
        if(tasks.containsKey(name)){
            if(tasks.get(name) instanceof QueuedTask queue){
                queue.tasks.add(t);
            }
        }
        QueuedTask qt = new QueuedTask(name,t);
        tasks.put(name,qt);
    }

    public void clearAll(){
        for(var task:tasks.values()){
            task.abort();
        }
        tasks.clear();
    }

    public static class QueuedTask implements Task{
        Seq<Task> tasks = new Seq<>();
        Task current = null;
        String name;
        public QueuedTask(String name,Task current){
            this.current = current;
            this.name=name;
        }

        @Override
        public void doTask(){
            if(current==null){
                return;
            }
            current.doTask();
            if(current.isFinished()){
                if(tasks.isEmpty()){
                    current = null;
                    return;
                }
                current = tasks.pop();
            }
        }

        @Override
        public boolean isFinished(){
            return current==null;
        }

        @Override
        public float amountDone(){
            if(current==null){return 1;}
            return current.amountDone();
        }

        @Override
        public float computeWeight(){
            if(current==null){return 9999;}
            return current.computeWeight();
        }

        @Override
        public void abort(){
            for(var t:tasks){
                t.abort();
            }
            tasks.clear();
            current = null;
        }

        @Override
        public String name(){
            return name;
        }

        public Task getCurrentTask(){
            return current;
        }
    }

}
