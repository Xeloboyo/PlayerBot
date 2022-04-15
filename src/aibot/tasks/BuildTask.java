package aibot.tasks;

import aibot.*;
import aibot.util.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.*;
import mindustry.entities.units.*;
import mindustry.game.Schematic.*;
import mindustry.gen.*;
import mindustry.world.*;

///js ai().ais.get(Team.sharded).controllers.get(0).player.build(Blocks.copperWall, 24,186)
public class BuildTask extends AITask{
    public Seq<BuildPlot> buildPlots = new Seq<>();
    public Seq<BuildPlan> deconstruction = new Seq<>();

    public BuildTask(AIPlayer player, Stile destination){
        super(player, new Vec2(destination.x * Vars.tilesize,destination.y * Vars.tilesize));
        buildPlots.add(new BuildPlot(destination));
        arrivalDistance = Vars.buildingRange*0.5f;
    }

    public BuildTask(AIPlayer player, Stile[] destination){
        super(player, new Vec2(destination[0].x * Vars.tilesize,destination[0].y * Vars.tilesize));
        for(Stile st:destination){
            buildPlots.add(new BuildPlot(st));
        }
        arrivalDistance = Vars.buildingRange*0.5f;
    }

    @Override
    public void onArrive(){
        player.building = true;
    }

    public boolean plannedForDeconstruction(Building b){
        for(BuildPlan bp:deconstruction){
            if(bp.x == b.tile.x && bp.y == b.tile.y){
                return true;
            }
        }
        return false;
    }

    public class BuildPlot{
        public Stile tobuild;
        public BuildPlan plan;
        public boolean impossible = false;
        public boolean complete = false;
        public boolean active = false;
        public Vec2 location = new Vec2();
        public Seq<BuildPlan> deconstructions = new Seq<>();

        public BuildPlot(Stile tobuild){
            this.tobuild=tobuild;
            plan = new BuildPlan( tobuild.x, tobuild.y, tobuild.rotation, tobuild.block, tobuild.config);
            location = new Vec2(plan.x*Vars.tilesize,plan.y*Vars.tilesize);
        }

        public boolean checkComplete(){
            Tile t = Vars.world.tile(tobuild.x,tobuild.y);
            if(t.build!=null){
                if(t.build.block == tobuild.block && t.build.tile == t){
                    complete = true;
                    plan.progress=1;
                    return true;
                }
            }
            return complete;
        }

        public void deconstructObstacles(){
            Utils.coveredTiles(tobuild.block, tobuild.x, tobuild.y, tile -> {
                if(tile.build != null){
                    if(!plannedForDeconstruction(tile.build)){
                        BuildPlan bp = new BuildPlan(tile.build.tile.x, tile.build.tile.y);
                        deconstruction.add(bp);
                        deconstructions.add(bp);
                        player.buildPlans.add(bp);
                    }
                }
            });
            deconstruction.filter(bp -> player.buildPlans.contains(bp));
            if(Build.validPlace(tobuild.block, player.team(),tobuild.x, tobuild.y,tobuild.rotation)){
                if(!deconstructions.isEmpty()){
                    deconstruction.filter(decon->{
                        for(BuildPlan decon2:deconstructions){
                            if(decon==decon2){
                                return false;
                            }
                        }
                        return true;
                    });
                    deconstructions.clear();
                }
            }else{
                if(deconstruction.isEmpty()){
                    impossible = true;
                    return;
                }
            }
        }
    }

    @Override
    public void doTask(){
        boolean allComplete = true;
        boolean alreadypathed = !player.movement.path.isEmpty();
        for(BuildPlot plot:buildPlots){
            plot.checkComplete();
            if(plot.complete){
                continue;
            }
            allComplete = false;

            if(!alreadypathed && plot.location.dst(player.pos)>Vars.buildingRange-40){
                player.pathTo(Utils.randVec(Vars.buildingRange*0.5f).add(plot.location));
                alreadypathed = true;
            }

            if(!plot.active){
                plot.deconstructObstacles();
                if(plot.deconstructions.isEmpty()){
                    player.buildPlans.add(plot.plan);
                    plot.active = true;
                }
            }
            if(plot.plan.initialized && Vars.world.tile(plot.tobuild.x, plot.tobuild.y).build == null){
                plot.plan.initialized = false;
            }
            if(plot.plan.progress>=1){
                plot.complete = true;
            }
        }

        if(allComplete){
            this.taskActive=false;
            player.building = false;
        }
    }

    @Override
    public void interrupt(){
        player.building = false;
    }
}
