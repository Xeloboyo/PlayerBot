package aibot;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import java.util.*;

import static mindustry.Vars.*;

public class Utils{
    public static ObjectSet<String> common_words = new ObjectSet<>();

    public static String getNormalName(String rawname){
        rawname = Strings.stripColors(rawname);
        //remove [tags]
        StringBuilder namewithouttags= new StringBuilder();
        int level  = 0;
        for(int i =0;i<rawname.length();i++){
            char c = rawname.charAt(i);
            if(c =='[' || c =='('){
                level += 1;
            }else if(c ==']' || c ==')'){
                level -= 1;
                if(level<0){
                    level = 0;
                }
            }else if(level == 0){
                namewithouttags.append(c);
            }
        }
        //shortens the name into a nickname if theres spaces (random) e.g. pineapple-off-pizza -> pineapple
        if(Mathf.random(namewithouttags.length())>8){
            String[] split = namewithouttags.toString().split("[-_\\s]");
            for(int i = 0;i<split.length;i++){
                if(common_words.contains(split[i])){ // dont want nickname to be like 'the'
                    continue;
                }
                namewithouttags.setLength(0);
                namewithouttags.append(split[i]);
                break;
            }
        }
        //remove all weird numbers and stuff: bob1234#$@ -> bob
        String name="";
        for(int i =0;i<namewithouttags.length();i++){
            char c = namewithouttags.charAt(i);
            if(Character.isLetter(c)||c==' '){
                name+=c;
            }
        }
        name = name.toLowerCase(Locale.ROOT).trim();
        return name;
    }

    public static Vec2 randVec(float len){
        float a = Mathf.random(360);
        return new Vec2(Mathf.sinDeg(a)*len,Mathf.cosDeg(a)*len);
    }

    public static Vec2 approach(Vec2 from,Vec2 to, float dist){
        return to.cpy().sub(from).nor().scl(-dist).add(to);
    }

    public static Teamc target(Team team, float x, float y, float range, boolean air, boolean ground){
        return Units.closestTarget(team, x, y, range, u -> u.checkTarget(air, ground), t -> ground);
    }
    public static boolean invalid(Teamc target, Team team, float x, float y){
        return Units.invalidateTarget(target, team, x, y);
    }

    public static void coveredTiles(Block type, int x, int y, Cons<Tile> cons){
        if(type.size==1){
            cons.get(Vars.world.tile(x,y));
            return;
        }

        int offsetx = -(type.size - 1) / 2;
        int offsety = -(type.size - 1) / 2;

        for(int dx = 0; dx < type.size; dx++){
            for(int dy = 0; dy < type.size; dy++){
                int wx = dx + offsetx + x, wy = dy + offsety + y;
                cons.get(Vars.world.tile(wx,wy));
            }
        }
    }

    public static boolean insideValidTerritory(Block type, Team team, int x, int y){
        if(!state.rules.editor){
            //find closest core, if it doesn't match the team, placing is not legal
            if(state.rules.polygonCoreProtection){
                float mindst = Float.MAX_VALUE;
                CoreBuild closest = null;
                for(TeamData data : state.teams.active){
                    for(CoreBuild tile : data.cores){
                        float dst = tile.dst2(x * tilesize + type.offset, y * tilesize + type.offset);
                        if(dst < mindst){
                            closest = tile;
                            mindst = dst;
                        }
                    }
                }
                if(closest != null && closest.team != team){
                    return false;
                }
            }else if(state.teams.anyEnemyCoresWithin(team, x * tilesize + type.offset, y * tilesize + type.offset, state.rules.enemyCoreBuildRadius + tilesize)){
                return false;
            }
        }
        return true;
    }

    static {
        common_words.add("on");
        common_words.add("the");
        common_words.add("a");
        common_words.add("at");
        common_words.add("it");
        common_words.add("i");
        common_words.add("am");
        common_words.add("this");
        common_words.add("that");
        common_words.add("my");
        common_words.add("your");
        common_words.add("are");
        common_words.add("of");
        common_words.add("ontop");
        common_words.add("upon");
        common_words.add("what");
        common_words.add("is");
    }

    public static class MovingAverage{
        float[] values;
        int index=0;
        public float average=0;
        float total=0;

        int delay = 0, tick=0;
        float accum = 0;
        public MovingAverage(int length,int delay){
            values = new float[length];
            this.delay=delay;
        }
        public void push(float value){
            tick--;
            if(tick>=0){
                accum +=value;
                return;
            }
            tick=delay;
            accum/=(float)(delay+1);


            index++;
            if(index>=values.length){
                index=0;
            }
            total-=values[index];
            values[index]=accum;
            total+=accum;

            average = total/values.length;
            accum=0;
        }

        public float getTotal(){
            return total;
        }
    }

    public static class StopWatch{
        long millis;
        public StopWatch(){
            millis = Time.millis();
        }

        //idk what to call it
        public long click(){
            long diff = Time.millis()-millis;
            millis+=diff;
            return diff;
        }
    }



}
