package aibot;

import aibot.AIStrategiser.*;
import aibot.structure.*;
import aibot.util.*;
import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.world.*;

import java.util.*;

import static mindustry.Vars.*;


public class AIGlobalControl extends Plugin{
    public static WorldMapper mapper;
    public static Seq<AIPlayer> players= new Seq<>();
    public static ObjectMap<Team, AIStrategiser> ais = new ObjectMap<>();
    public static int lastPopup = 0;
    public static TaskManager manager = new TaskManager();

    public static Object test = null;
    //called when game initializes
    @Override
    public void init(){
        Events.on(WorldLoadEvent.class,(e)->{
            mapper  = new WorldMapper(Vars.world);
            manager.clearAll();
            ais.clear();
            Vars.state.teams.getActive().each(t->{
                ais.put(t.team,new AIStrategiser(t.team));
            });
            for(AIPlayer player:players){
                player.shooting = false;
                Team team = netServer.assignTeam(player.controlling);
                player.add(team);
                ais.get(team).addPlayer(player);
            }
            ObjectSet<Building> searched = new ObjectSet<>();
            for(Tile t:world.tiles){
                if(t.build!=null && !searched.contains(t.build)){
                    searched.add(t.build);
                    var v= ais.get(t.build.team);
                    if(v!=null){
                        v.onBlockCreated(t.build);
                    }
                }
            }
        });

        Events.on(PlayerJoin.class,(e)->{
            for(Entry<Team, AIStrategiser> ai:ais){
                ai.value.onPlayerJoin(e);
            }
        });
        Events.on(PlayerChatEvent.class,(e)->{
            for(Entry<Team, AIStrategiser> ai:ais){
                ai.value.onChatEvent(e);
            }
        });

        Events.on(BlockBuildEndEvent.class, e->{
            if(e.breaking){
                return;
            }
            var v= ais.get(e.team);
            if(v!=null){
                v.onBlockCreated(e.tile.build);
            }
            mapper.onBlockCreated(e.tile.build);
        });
        Events.on(ConfigEvent.class, e->{
            var v= ais.get(e.tile.team);
            if(v!=null){
                v.onBlockConfigured(e.tile);
            }
        });

        Events.on(UnitDestroyEvent.class, e->{
            for(var p:players){
                if(p.unit()== e.unit){
                    p.spawnWait = 50;
                }
            }
        });

        Events.on(BlockBuildBeginEvent.class, e->{
            if(!e.breaking){
                return;
            }
            if(e.tile.build != null){
                var v= ais.get(e.team);
                if(v!=null){
                    v.onBlockDestroyed(e.tile.build,e.unit.getPlayer());
                }
                mapper.onBlockDestroyed(e.tile.build);
            }else if(state.rules.infiniteResources){
                var v= ais.get(e.team);
                if(v!=null){
                    v.map.removeBlock(e.tile.x, e.tile.y);
                }
            }
        });

        Events.on(BlockDestroyEvent.class, e->{
            if(e.tile.build != null){
                var v= ais.get(e.tile.build.team);
                if(v!=null){
                    v.onBlockDestroyed(e.tile.build,null);
                }
                mapper.onBlockDestroyed(e.tile.build);
            }
        });

        Events.run(Trigger.update,()->{
            if(mapper!=null){
                mapper.update();
            }
            manager.onTick();
            for(AIPlayer player:players){
                player.update();
                if(player.team() != player.controller.strategiser.team){
                    player.controller.strategiser.controllers.remove(player.controller);
                    if(ais.get(player.team())==null){
                        ais.put(player.team(),new AIStrategiser(player.team()));
                    }
                    ais.get(player.team()).addPlayer(player);
                }
            }
            for(Entry<Team, AIStrategiser> ai:ais){
                ai.value.update();
            }
            lastPopup--;
            if(lastPopup<0){
                lastPopup = 120;
                int ypos = 100;
                Call.infoPopup("Tasks: "+manager.tasks.size,lastPopup/60, Align.topLeft,ypos,0,0,0); ypos += 27;
                for(Entry<Team, AIStrategiser> ai:ais){
                    ypos += ai.value.drawPopup(0,ypos,lastPopup/60);
                }
            }
        });
        Vars.mods.getScripts().runConsole("this.aimain = function(){return Vars.mods.getMod(\"ai-bot\").main;}");
        Vars.mods.getScripts().runConsole("this.ai = function(t){return this.aimain().ais.get(t);}");
        Vars.mods.getScripts().runConsole("this.measure = function(t){Vars.mods.getMod(\"ai-bot\").main.test = t; return Vars.mods.getMod(\"ai-bot\").main.measure();}");
        BlockCategories.init();
    }


    //register commands that run on the server
    @Override
    public void registerServerCommands(CommandHandler handler){

    }

    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.<Player>register("spawnai", "<team>", "Spawn an ai.", (args, player) -> {
            Team team = getTeam(args[0]);
            if(team!=null){
                String name = randomName();
                while(nameBeingUsed(name)){
                   name = randomName();
                }
                spawnAI(name, Color.rgb(Mathf.random(255),Mathf.random(255),Mathf.random(255)),team);
                return;
            }
        });

        handler.<Player>register("testconvext", "<team> <x> <y>", "make an ai draw a conveyor to the core.", (args, player) -> {
            Team team = getTeam(args[0]);
            if(team!=null){
                try{
                    ais.get(team).testMakePath(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
                }catch(NumberFormatException nfe){
                    Call.sendMessage("[scarlet] Error: Coordinates ("+args[1]+","+args[2]+") were unable to be parsed");
                }
            }
        });
        handler.<Player>register("testconv", "make an ai draw a conveyor to the core.", (args, player) -> {
            Team team = player.team();
            if(team!=null){
                try{
                    ais.get(team).testMakePath((int)(player.unit().x/8), (int)(player.unit().y/8));
                }catch(NumberFormatException nfe){
                    Call.sendMessage("[scarlet] Error: Coordinates ("+(int)(player.unit().x/8)+","+(int)(player.unit().y/8)+") were unable to be parsed");
                }
            }
        });

        handler.<Player>register("team", "<team>", "change team.", (args, player) -> {
            Team team = getTeam(args[0]);
            if(team!=null){
                player.team(team);
            }
        });

        handler.<Player>register("block", "<team> <block> <x> <y>", "make a bot place block", (args, player) -> {
            Team team = getTeam(args[0]);
            args[1] = args[1].trim().toLowerCase(Locale.ROOT);
            Block b = content.block(args[1]);
            if(b==null){
                Call.sendMessage("[scarlet] Error: Block ("+args[1]+") was not found");
                return;
            }
            if(team==null){
                Call.sendMessage("[scarlet] Error: Team ("+args[0]+") was not found");
                return;
            }
            var ai = ais.get(team);
            Structure its = new Structure(ai);
            int x = Integer.parseInt(args[2]);
            int y = Integer.parseInt(args[3]);
            its.addBlock(b,x,y);
            if(its.isComplete()){
               return;
            }
            ai.requests.add(new BuildRequest(ai,(bb)->0f,1,its));
            ai.structures.add(its);
        });


        handler.register("fillstuff", "<team>", "fills core with a bit of stuff.", (args, player) -> {
            Team team = getTeam(args[0]);
            if(team!=null){
               for(Item i:content.items()) {
                   team.core().items.add(i,team.core().storageCapacity);
               }
            }
        });

        handler.register("debugmap", "<team>", "ChunkedStructureMap debug.", (args, player) -> {
            Team team = getTeam(args[0]);
            if(team!=null){
                ais.get(team).map.debug();
            }
        });

        handler.register("debugtasks", "", "TaskManager debug.", (args, player) -> {
            for(var r:manager.tasks.values()){
                Call.sendMessage(r.name()+": " + Strings.fixed(100*r.amountDone(),1)+"% done");
            }
        });
    }

    public static void addPlayer(AIPlayer p, Team team){
        players.add(p);
        p.init(team);
    }

    public static AIPlayer spawnAI(String name, Color c, Team team){
        AIPlayer aiPlayer = new AIPlayer(name, c,Mathf.random(0,1000000)+"");
        ais.get(team).addPlayer(aiPlayer);
        players.add(aiPlayer);
        aiPlayer.init(team);
        return aiPlayer;
    }

    public boolean nameBeingUsed(String name){
        for(AIPlayer player:players){
            if(player.controlling.name.equals(name)){
                return true;
            }
        }
        return false;
    }

    String randomName(){
        return "[AI]"+names[Mathf.random(names.length)];
    }

    public static Team getTeam(String name){
        for(Team team: Team.all){
            if(team.name.equals(name.toLowerCase(Locale.ROOT))){
                return team;
            }
        }
        return null;
    }

    //'random' names
    public static String[] names = {
        "jumpydawning","blank_clothing","FriendlyCoyote","AmuckHelper","nobody_3","extrovertedbasis50","pot_of_chests","zpq3","didactic_illness","TheGreyBreaker",
        "pasta","Rozen","Xan","KloxEdge","frogixre","versana","evan","3444","incornge","Woodmoll","Bixel","Iris","Tammy","Wroomy","Pischer","Butters","blank","Caede",
        "Claire","itzgo","jassiKrystal","Lincle","Aide","kitkatsna","cophee","Lomis","Wintea","απάτη","ghlyfee","hrithik","A Phủ","дный","kane","artfex","anook","akimov","anyone",
        "dead","dima","discorde","sfdlk","453ggf","dedeg3f","?????","[router]","eggggg","egg","beri3","jasf","infernium","dat on","空条","Лааадно","Скатовод","сделать","освящённый",
        "號香蕉","copika","不錯","瑞恩","冠冠","我也要去了","一起来不用","Гаппи","человек","горощик","router","Ilya247","pineapple on pizza","Xelo","[red]D[green]o[blue]t","eldoof","glennFolkent",
        "sk1139","bluefox","eod","shar","farmerthanos","extravection","theanacondaguy","goobi","ash","thirst","moop","largekey","sonka","elag","BOULDER"
    };

}
