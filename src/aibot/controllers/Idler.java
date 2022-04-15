package aibot.controllers;

import aibot.*;
import aibot.AIStrategiser.*;
import aibot.analysers.*;
import aibot.structure.*;
import aibot.structure.ChunkedStructureMap.*;
import aibot.structure.ChunkedStructureMap.StructureBlock.*;
import aibot.tasks.*;
import aibot.util.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.Schematic.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static aibot.AIGlobalControl.mapper;

//wanders around the base and says hi. Also mines copper/lead at the beginning of the game.
//defends buildings lazily by shooting at enemies
//may also complain when a resource is low.
//builds drills (maybe)
public class Idler extends AIController{
    public float idledelay = 0;
    public boolean mining = false;
    public boolean shooting = false;
    public boolean delivering = false;
    public StructureBlock building = null;

    public float maxIdleTime = 500;

    public AITask defaultTask(){
        CoreBuild cb =  player.unit().closestCore();
        if(cb!=null){
            IdleTask tk = new IdleTask(player, new Vec2(cb.x,cb.y));;
            tk.maxwait=1500;
            tk.range=100;
            return tk;
        }else{
            return new IdleTask(player, player.pos.cpy());
        }
    }

    // doesnt care about priority.
    Seq<AIRequest> available = new Seq<>();
    @Override
    public AIRequest next(){
        available.clear();
        for(AIRequest ar:strategiser.requests){
            if(ar.complete || ar.controllers.size>=ar.recommendedWorkers){continue;}
            if(ar instanceof MiningRequest||
               ar instanceof AlertRequest||
               ar instanceof BuildRequest||
               ar instanceof DefendRequest){
                available.add(ar);
            }
        }
        if(available.isEmpty())
            return null;
        return available.get(Mathf.random(available.size-1));
    }

    @Override
    public void update(){
        idledelay-=Time.delta;
        if(idledelay>0){
            return;
        }
        idledelay = Mathf.random(maxIdleTime);
        if(currentRequest==null){
            joinNext();
        }
        //why is this logic in the controller itself?
        //mine stuff

        if(player.core()!=null && currentRequest!=null){

            if(currentRequest instanceof MiningRequest){
                MiningRequest request = (MiningRequest)currentRequest;
                if((request.complete && !delivering &&!mining) || !player.unit().canMine(request.item)){
                    abandonRequest();
                    return;
                }
                if(player.unit().stack.amount>=player.unit().itemCapacity() && !delivering){
                    delivering = true;
                    player.interruptTask(new InventoryTransferTask(player,player.core(),true),
                    ()->{delivering = false;});
                }else if(!mining && !delivering){
                    if(player.unit().stack.item != request.item && player.unit().stack.amount > 0){
                        delivering = true;
                        player.interruptTask(new InventoryTransferTask(player, player.core(), true),
                        () -> {
                            delivering = false;
                            idledelay = 0;
                        });
                    }else{
                        OreAnalyser oa = mapper.analyser(OreAnalyser.class, "ores");
                        Tile tile = oa.getClosestOre(player.pos.x, player.pos.y, request.item);
                        mining = true;
                        player.addTask(new MiningTask(player, tile, Mathf.random(500, 2000)),
                        () -> {
                            mining = false;
                        });
                    }
                }
            }else if(currentRequest instanceof AlertRequest){
                AlertRequest request = (AlertRequest)currentRequest;
                if(request.alert instanceof Player && request.alert != player.get()){
                    greet((Player)request.alert);
                    request.alerted++;
                }
                abandonRequest();
            }else if(currentRequest instanceof DefendRequest){
                DefendRequest request = (DefendRequest)currentRequest;
                Teamc target = Utils.target(player.team(), request.defend.x, request.defend.y, 200, true, true);

                if(target != null && !shooting){
                    shooting = true;
                    player.interruptTask(new ShootTask(player, target), () -> {
                        shooting = false;
                    });
                }else if(currentRequest.complete || !shooting){
                    abandonRequest();
                }
            }else if(currentRequest instanceof BuildRequest){
                if(currentRequest.complete){
                    building = null;
                    abandonRequest();
                    return;
                }
                if(building!=null){
                    return;
                }
                BuildRequest request = (BuildRequest)currentRequest;
                Structure sc = request.structure;

                if(request.fastbuild){
                    Stile[] plan = new Stile[sc.blocks.size];
                    for(int i = 0; i < sc.blocks.size; i++){
                        plan[i] = sc.blocks.get(i).stile;
                    }
                    building = sc.blocks.get(0);
                    player.interruptTask(new BuildTask(player, plan), () -> {
                        building = null;
                    });
                }else{
                    for(StructureBlock sb : sc.blocks){
                        if(!sb.pending && sb.status == BuildStatus.NOT_STARTED){
                            sb.pending = true;
                            building = sb;
                            player.interruptTask(new BuildTask(player, sb.stile), () -> {
                                building = null;
                            });
                        }
                    }
                }

                if(building==null){
                    abandonRequest();
                }
            }

            /*
            //todo: replace with analyser.



            */
        }
    }

    @Override
    public void reset(){
        idledelay = 0;
        mining = false;
        delivering = false;
        currentRequest=null;
    }

    String greetings[] = {"hi @","hello @","its @","the @ arrives","oh no","@!"};
    public void greet(Player pl){
        String greet = greetings[Mathf.random(greetings.length-1)];
        String name = Utils.getNormalName(pl.name);

        greet = greet.replace("@", name);
        player.chat(greet);
    }
}
