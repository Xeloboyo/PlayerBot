package aibot;

import aibot.analysers.*;
import aibot.analysers.OreAnalyser.*;
import aibot.analysers.TeamAnalyser.*;
import aibot.analysers.TeamAnalyser.TeamStats.*;
import aibot.structure.*;
import aibot.util.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.game.Schematic.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import java.lang.reflect.*;
import java.util.*;

import static aibot.AIGlobalControl.mapper;

//controls multiple AI controllers on a team, allowing them to more easily work together by having a common state.
//this high level class responds most strongly to world events and analysers
//things done here:
//  -  base planning and generation
//  -  alerts from bot players
//  -  plan of attack
//  -  frontline defense and turret pushing (using turrets to expand the frontline forward)
// AI difficulty is determined by how slow this strategiser is set, and what jobs its controllers would take.
// The controllers also don't technically need to listen to the strategiser, and the strategiser's decisions
// are often open-ended, and that's how bad(der) decisions are generated
public class AIStrategiser{
    public Team team;
    public Seq<AIController> controllers = new Seq<>();
    public Seq<AIRequest> requests = new Seq<>();
    public int updatedelay = 30, tick =0, totaltick=0;// every half second
    //mining
    public ObjectMap<Item,MiningRequest> mreq = new ObjectMap<>();
    //emergency core defense
    public ObjectMap<CoreBuild, Float> corehps = new ObjectMap<>();
    //structures
    public Seq<Structure> structures= new Seq<>();
    //analysers
    public TeamStats teamStats;
    //base planning
    public ChunkedStructureMap map;

    //constants
    public static Seq<Item> mineItems = Seq.with(Items.copper, Items.lead, Items.titanium, Items.thorium);

    public AIStrategiser(Team team){
        this.team = team;
        reset();
    }
    public <T extends AIController> void addPlayer(AIPlayer player){
        controllers.add(player.controller);
        player.controller.strategiser=this;
    }
    public <T extends AIController> AIPlayer createPlayer(String name, Class<T> controller){
        AIPlayer player = new AIPlayer(name, Color.white,name+"_BOT_IP");
        try{
            player.controller=controller.getConstructor().newInstance();
            addPlayer(player);
            AIGlobalControl.addPlayer(player,team);
            return player;
        }catch(InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e){
            e.printStackTrace();
        }
        return null;
    }

    public void onChatEvent(PlayerChatEvent event){
        if(controllers.size==0){return;}
        if(event.message.toLowerCase(Locale.ROOT).trim().equals("hi")){
            requests.add(new AlertRequest(this,(a)-> 60f/(60f+totaltick-a.tickcreated),1,event.player));
        }
    }
    public void onPlayerJoin(PlayerJoin event){
        if(controllers.size==0){return;}
        int workers = Mathf.random(1,Math.min(3,controllers.size));
        requests.add(new AlertRequest(this,(a)-> 60f/(60f+totaltick-a.tickcreated),workers,event.player));
    }

    public void reset(){
        requests.clear();
        mreq.clear();
        teamStats=mapper.teamAnalyser.get(team);
        teamStats.get(Items.copper).unlocked= true;
        teamStats.get(Items.lead).unlocked= true;
        map = new ChunkedStructureMap();
    }

    public void update(){
        //adding req
        tick++;
        totaltick++;
        if(tick>updatedelay){
            tick=0;
            //mining req
            for(Item i:mreq.keys()){
                if(mreq.get(i).complete){
                    mreq.remove(i);
                }
            }
            int cutoff=750;

            Seq<ItemStats> low = teamStats.filter(items->items.amount<cutoff && mineItems.contains(items.item));
            for(ItemStats i:low){
                if(mreq.get(i.item)==null && i.unlocked){
                    MiningRequest mr = new MiningRequest(this,MiningRequest.defaultUrgency,2,i.item,cutoff);
                    requests.add(mr);
                    mreq.put(i.item,mr);
                }
            }

            for(CoreBuild i:corehps.keys()){
                if(!i.added){
                    corehps.remove(i);
                }
            }
            for(CoreBuild cb:team.cores()){
                if(!corehps.containsKey(cb)){
                    corehps.put(cb,cb.health);
                }
                if(corehps.get(cb)>cb.health){
                    if(Utils.target(team,cb.x,cb.y,200,true,true)!=null){
                        DefendRequest dr = new DefendRequest(this,(e)->2.0f-(e.defend.health/e.defend.maxHealth),(int)Math.max(1,controllers.size*0.7f),cb);
                        if(!requests.contains(dr)){
                            requests.add(dr);
                        }
                    }
                }
                corehps.put(cb,cb.health);
            }

            if(structures.isEmpty()){
                OreAnalyser oa = mapper.analyser(OreAnalyser.class,"ores");
                OrePatch closest = null;
                float dis = 99999999;
                for(OrePatch patch:oa.orePatches){
                    if(patch.contains.size>500){
                        continue;
                    }
                    int mdis = 9999999;
                    for(CoreBuild cb:team.cores()){
                        Tile coretile = cb.tile;
                        int ccx = Mathf.clamp(coretile.x,patch.minx,patch.maxx);
                        int ccy = Mathf.clamp(coretile.y,patch.miny,patch.maxy);
                        mdis = Math.min(mdis,Math.abs(ccx-coretile.x)+Math.abs(ccy-coretile.y));
                    }
                    if(mdis<dis){
                        dis = mdis;
                        closest = patch;
                    }
                }
                if(closest!=null){
                    Structure testStructure = new Structure(this);
                    for(int i = closest.minx;i<=closest.maxx;i++){
                        for(int j = closest.miny;j<=closest.maxy;j++){
                           if((i%5==0 || i%5==2)){
                               if(j%2==0){
                                   if(Build.validPlace(Blocks.mechanicalDrill, team, i, j, 0)){
                                       testStructure.addBlock(Blocks.mechanicalDrill, i, j);
                                   }
                               }else if (j==closest.miny){
                                   if(Build.validPlace(Blocks.mechanicalDrill, team, i, j-1, 0)){
                                      testStructure.addBlock(Blocks.mechanicalDrill, i, j-1);
                                   }
                               }
                           }
                        }
                    }
                    addStructure(testStructure);
                    BuildRequest breq= new BuildRequest(this,(b)->0f,1,testStructure);
                    requests.add(breq);
                }

            }

        }


        //req management
        requests.each(req->{
            req.urgency = req.urgencyprov.get(req);
            req.complete = req.fufilled();
        });
        // && req.tickcreated+req.expirytime>totaltick
        requests.each(req->{
            if(req.tickcreated+req.expirytime<totaltick){
                req.complete = true;
            }
        });
        requests.filter(req->!req.complete);
        structures.filter(structure -> !structure.blocks.isEmpty());
    }

    public int drawPopup(int x,int y, int duration){
        int lh = 27;
        int currentheight = lh;
        Call.infoPopup("[#"+team.color+"]"+team.name+" AI",duration, Align.topLeft,y,x,0,0);
        Call.infoPopup("has "+(controllers.size)+" agents and "+requests.size+" requests",duration, Align.topLeft,y+currentheight,x,0,0); currentheight +=lh;
        for(AIRequest req:requests){
            String text = req.type.getSimpleName()+" [white]"+req.controllers.size+"/"+req.recommendedWorkers+" P [gray], exp: T-[red]"+(req.tickcreated+req.expirytime-totaltick);
            Call.infoPopup(text,duration, Align.topLeft,y+currentheight,x+50,0,0); currentheight +=lh;
        };
        return currentheight;
    }

    public void testMakePath(int fromx, int fromy){
        ItemLineStructure its = new ItemLineStructure(fromx,fromy,team.core(),this);
        //BuildRequest breq= new BuildRequest(this,(b)->0f,1,its);
        //if(its.complete()){
        //    return;
        //}
       // requests.add(breq);
        addStructure(its);
        its.cheatBuild();
    }

    public void addStructure(Structure structure){
        if(structure.blocks.isEmpty()){
            return;
        }
        for(var block: structure.blocks){
            map.addBlock(block);
        }
        structures.add(structure);
    }

    public TeamStats getTeamStats(){
        return teamStats;
    }

    public void onBlockDestroyed(Building build,Player p){
        if(p!=null){
            map.removeBlock(build.tile.x,build.tile.y);
        }
    }

    public void onBlockCreated(Building build){
        //add it to the list....
        map.addBlock(new Stile(build.block,build.tile.x,build.tile.y, build.config(), (byte)build.rotation));
    }

    public void onBlockConfigured(Building tile){
        map.updateDynamic();
    }

    //used by controllers, produced by strategisers, high level tasks for ai.
    public  static abstract class AIRequest <T extends AIRequest>{
        public AIStrategiser ai;
        //0 to 1, 1 being extremely urgent and 0 being basically an idle task
        public float urgency;
        public Floatf<T> urgencyprov;
        // the ai currently working on this request
        public Seq<AIController> controllers = new Seq<>();
        public int recommendedWorkers;
        public boolean complete = false;
        public int tickcreated = 0;
        public int expirytime = 5000;

        public AIRequest(AIStrategiser ai , Floatf<T> urgency, int recommendedWorkers){
            this.urgencyprov = urgency;
            this.ai=ai;
            this.recommendedWorkers = recommendedWorkers;
            tickcreated=ai.totaltick;
        }

        public abstract boolean fufilled();
        //why
        public Class<T> type;

        @Override
        public boolean equals(Object o){
            if(this == o) return true;
            if(!(o instanceof AIRequest)) return false;
            AIRequest<?> aiRequest = (AIRequest<?>)o;
            return toString().equals(o.toString());
        }

        @Override
        public int hashCode(){
            return Objects.hash(toString());
        }

        @Override
        public String toString(){
            return "AIRequest{" +
            "recommendedWorkers=" + recommendedWorkers +
            ", complete=" + complete +
            '}';
        }
    }

    public static class MiningRequest extends AIRequest<MiningRequest>{
        public Item item;
        public int mineuntil;
        public ItemStats stats;
        static Floatf<MiningRequest> defaultUrgency = (e)->{
            return ((e.mineuntil-e.stats.amount)/(float)e.mineuntil)-(1-10f/(10f+e.stats.getIncome()));
        };
        {{type = MiningRequest.class;}}

        public MiningRequest(AIStrategiser ai, Floatf<MiningRequest> urgency, int recommendedWorkers, Item item, int mineuntil){
            super(ai, urgency, recommendedWorkers);
            this.item = item;
            this.mineuntil = mineuntil;
            stats = mapper.analyser(TeamAnalyser.class, "team").get(ai.team).get(item);
        }

        @Override
        public boolean fufilled(){
            return stats.amount>mineuntil && stats.getIncome()>=0;
        }

        @Override
        public String toString(){
            return "MiningRequest{" +
            "recommendedWorkers=" + recommendedWorkers +
            ", complete=" + complete +
            ", item=" + item +
            ", mineuntil=" + mineuntil +
            '}';
        }
    }

    public static class AlertRequest extends AIRequest<AlertRequest>{
        public int alerted=0;
        public Object alert;
        {{type = AlertRequest.class;}}
        public AlertRequest(AIStrategiser ai, Floatf<AlertRequest> urgency, int recommendedWorkers, Object alert){
            super(ai, urgency, recommendedWorkers);
            this.alert=alert;
            if(alert instanceof Player){
                expirytime = 1000;
            }
        }

        @Override
        public boolean fufilled(){
            return alerted>=recommendedWorkers;
        }

        @Override
        public String toString(){
            return "AlertRequest{" +
            "recommendedWorkers=" + recommendedWorkers +
            ", complete=" + complete +
            ", alerted=" + alerted +
            ", alert=" + alert +
            '}';
        }
    }

    //placeholdery
    public static class DefendRequest extends AIRequest<DefendRequest>{
        public Building defend;
        {{type = DefendRequest.class;}}
        public DefendRequest(AIStrategiser ai, Floatf<DefendRequest> urgency, int recommendedWorkers, Building defend){
            super(ai, urgency, recommendedWorkers);
            this.defend=defend;
        }

        @Override
        public boolean fufilled(){
            return Utils.target(ai.team,defend.x(),defend.y(),200,true,true)==null;
        }

        @Override
        public String toString(){
            return "DefendRequest{" +
            "recommendedWorkers=" + recommendedWorkers +
            ", complete=" + complete +
            ", defend=" + defend +
            '}';
        }

        @Override
        public boolean equals(Object o){
            if(this == o) return true;
            if(!(o instanceof DefendRequest)) return false;
            if(!super.equals(o)) return false;
            DefendRequest that = (DefendRequest)o;
            return Objects.equals(defend, that.defend);
        }

        @Override
        public int hashCode(){
            return Objects.hash(super.hashCode(), defend);
        }
    }

    public static class BuildRequest extends AIRequest<BuildRequest>{
        public Structure structure;
        //is true will build all at once, like if a player is placing a schematic or conveyors, otherwise one at a time like if a player is improvising.
        public boolean fastbuild = true;
        {{type = BuildRequest.class;}}
        public BuildRequest(AIStrategiser ai, Floatf<BuildRequest> urgency, int recommendedWorkers, Structure structure){
            super(ai, urgency, recommendedWorkers);
            this.structure=structure;
            expirytime = 500000;
        }

        @Override
        public boolean fufilled(){
            return structure.isComplete();
        }
    }

}
