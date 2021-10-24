package aibot.analysers;

import aibot.*;
import aibot.Utils.*;
import arc.func.*;
import arc.struct.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.modules.*;
//js
// Vars.mods.list().get(0).main.mapper.analyser(java.lang.Object,"team").get(Team.sharded).get(Items.copper)


//provides income and friendly unit analysis.
public class TeamAnalyser extends WorldAnalyser{
    public ObjectMap<Team,TeamStats> teamstats = new ObjectMap<>();
    @Override
    public void init(WorldMapper w){
        Vars.state.teams.getActive().each(t->{
            teamstats.put(t.team,new TeamStats(t.team));
        });
    }

    @Override
    public void consumeTile(Tile t){

    }

    @Override
    public void onBuildPlaced(Building b){

    }

    @Override
    public void onBuildRemoved(Building b){

    }

    @Override
    public void update(){
        Vars.state.teams.getActive().each(t->{
            if(teamstats.get(t.team)!=null){
                teamstats.get(t.team).update();
            }
        });
    }
    public TeamStats get(Team team){
        return teamstats.get(team);
    }

    public class TeamStats{
        public Team team;
        public ObjectMap<Item,ItemStats> itemstats = new ObjectMap<>();

        TeamStats(Team team){
            this.team=team;
            Vars.content.items().each(item->{
                itemstats.put(item,new ItemStats(item));
            });
        }

        void update(){
            if(team.core()==null){
                itemstats.each((item,amount)->{amount.amount=0;amount.pushNewAmount(0);});
                return;
            }
            ItemModule items = team.core().items;
            items.each((item,amount)->{
                ItemStats stat = itemstats.get(item);
                if(amount>0 && !stat.unlocked){
                    stat.unlocked = true;
                }
                if(stat.unlocked){
                    stat.pushNewAmount(amount);
                }
            });
        }
        public ItemStats get(Item item){
            return itemstats.get(item);
        }

        public Seq<ItemStats> filter(Boolf<ItemStats> filter){
            Seq<ItemStats> output = new Seq<>();
            Vars.content.items().each(item->{
                if(filter.get(itemstats.get(item))){
                    output.add(itemstats.get(item));
                }
            });
            return output;
        }



        public ItemStats min(Floatf<ItemStats> filter){
            final float[] min = {Float.MAX_VALUE};
            final ItemStats[] output = {null};
            Vars.content.items().each(item->{
                float t = filter.get(itemstats.get(item));
                if(t< min[0]){
                    min[0] = t;
                    output[0]=(itemstats.get(item));
                }
            });
            return output[0];
        }




        public class ItemStats{
            public Item item;
            public int amount;
            // the maximum of this item that was ever stored
            public int maxAmount;
            // whether the item has existed b4
            public boolean unlocked;
            //average over like 30 second
            public MovingAverage income;
            //average over like 5 minutes
            public MovingAverage longtermIncome;


            public ItemStats(Item item){
                this.item = item;
                income = new MovingAverage(60,30);
                longtermIncome= new MovingAverage(600,30);
            }

            public void pushNewAmount(int amount){
                float rawincome = (amount-this.amount)*60f;
                income.push(rawincome);
                longtermIncome.push(rawincome);
                this.amount=amount;
                this.maxAmount=Math.max(maxAmount,amount);
            }

            public float getIncome(){
                return income.average;
            }
            public float getLongTermIncome(){
                return longtermIncome.average;
            }
            public float getTrend(){
                return income.average-longtermIncome.average;
            }

            @Override
            public String toString(){
                return "ItemStats{" +
                "item=" + item +
                ", amount=" + amount +
                ", maxAmount=" + maxAmount +
                ", unlocked=" + unlocked +
                ", income=" + income.average +
                ", longtermIncome=" + longtermIncome.average +
                '}';
            }
        }
    }
}


