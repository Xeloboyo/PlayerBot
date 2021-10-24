package aibot;

import aibot.structure.*;
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
import mindustry.world.blocks.*;

import java.util.*;

import static mindustry.Vars.*;

public class AIGlobalControl extends Plugin{
    public static WorldMapper mapper;
    public static Seq<AIPlayer> players= new Seq<>();
    public static ObjectMap<Team, AIStrategiser> ais = new ObjectMap<>();
    //called when game initializes
    @Override
    public void init(){
        Events.on(WorldLoadEvent.class,(e)->{
            mapper  = new WorldMapper(Vars.world);
            ais.clear();
            Vars.state.teams.getActive().each(t->{
                ais.put(t.team,new AIStrategiser(t.team));
            });
            for(AIPlayer player:players){
                Team team = netServer.assignTeam(player.controlling);
                player.add(team);
                ais.get(team).addPlayer(player);
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
            mapper.onBlockCreated(e.tile.build);
        });

        Events.on(BlockBuildBeginEvent.class, e->{
            if(!e.breaking){
                return;
            }
            if(e.tile.build != null){
                ConstructBlock.ConstructBuild cb = (ConstructBlock.ConstructBuild)e.tile.build;
                mapper.onBlockDestroyed(cb);
            }
        });
        Events.on(BlockDestroyEvent.class, e->{
            ConstructBlock.ConstructBuild cb = (ConstructBlock.ConstructBuild)e.tile.build;
            mapper.onBlockDestroyed(cb);
        });

        Events.run(Trigger.update,()->{
            if(mapper!=null){
                mapper.update();
            }
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
        });
        Vars.mods.getScripts().runConsole("this.ai = function(){return Vars.mods.getMod(\"ai-bot\").main;}");
        ConveyorPathfinder.init();
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

        handler.<Player>register("testconveyor", "<team> <x> <y>", "make an ai draw a conveyor to the core.", (args, player) -> {
            Team team = getTeam(args[0]);
            if(team!=null){
                try{
                    ais.get(team).testMakePath(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
                }catch(NumberFormatException nfe){
                    Call.sendMessage("[scarlet] Error: Coordinates ("+args[1]+","+args[2]+") were unable to be parsed");
                }
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

    public static String[] names = {
        "jumpydawning","blank_clothing","FriendlyCoyote","AmuckHelper","nobody_3","extrovertedbasis50","pot_of_chests","zpq3","didactic_illness","TheGreyBreaker",
        "pasta","Rozen","Xan","KloxEdge","frogixre","versana","evan","3444","incornge","Woodmoll","Bixel","Iris","Tammy","Wroomy","Pischer","Butters","blank","Caede",
        "Claire","itzgo","jassiKrystal","Lincle","Aide","kitkatsna","cophee","Lomis","Wintea","απάτη","ghlyfee","hrithik","A Phủ","дный","kane","artfex","anook","akimov","anyone",
        "dead","dima","discorde","sfdlk","453ggf","dedeg3f","?????","[router]","eggggg","egg","beri3","jasf","infernium","dat on","空条","Лааадно","Скатовод","сделать","освящённый",
        "號香蕉","copika","不錯","瑞恩","冠冠","我也要去了","一起来不用","一起来不用","Гаппи","человек","горощик","router","Ilya247","pineapple on pizza","Xelo","[red]D[green]o[blue]t"
    };

}
