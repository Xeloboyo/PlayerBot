package aibot.analysers;

import aibot.*;
import arc.func.*;
import arc.math.*;
import arc.struct.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.defense.turrets.BaseTurret.*;
import mindustry.world.blocks.defense.turrets.LaserTurret.*;
import mindustry.world.blocks.defense.turrets.PowerTurret.*;
import mindustry.world.blocks.defense.turrets.TractorBeamTurret.*;
import mindustry.world.blocks.defense.turrets.Turret.*;

import java.util.*;

public class ThreatAnalyser extends WorldAnalyser{
    int tinstanceid = 0;
    public class TurretInstance{
        public BaseTurretBuild turret;
        public boolean ground,air;
        public float dps;
        public float bulletspeed = 0;
        public final int id = tinstanceid++;

        public TurretInstance(BaseTurretBuild turret){
            this.turret = turret;
            refreshDps();
        }
        public void refreshDps(){
            bulletspeed = 0;
            dps = 0;
            if(turret instanceof TractorBeamBuild){
                TractorBeamBuild tbb = (TractorBeamBuild)turret;
                TractorBeamTurret tb = (TractorBeamTurret)turret.block;
                if(tb.hasPower){
                    dps = tb.damage*tbb.power.status;
                }else{
                    dps = tb.damage;
                }
                bulletspeed = 999999f;
                ground = tb.targetGround;
                air = tb.targetAir;
            }else if(turret instanceof LaserTurretBuild){
                LaserTurret tu = (LaserTurret)turret.block;
                LaserTurretBuild tb = (LaserTurretBuild)turret;
                if(tb.hasAmmo()){
                    BulletType bul = tb.peekAmmo();
                    dps = bul.estimateDPS() / (tu.reloadTime/60);
                    dps *= tb.power.status;
                }
                bulletspeed = 999999f;
                ground = tu.targetGround;
                air = tu.targetAir;
            }else if(turret instanceof PowerTurretBuild){
                PowerTurret tu = (PowerTurret)turret.block;
                PowerTurretBuild tb = (PowerTurretBuild)turret;
                if(tb.hasAmmo()){
                    BulletType bul = tb.peekAmmo();
                    dps = bul.estimateDPS() / (tu.reloadTime/60);
                    dps *= tb.power.status;
                    bulletspeed = bul.speed==0?999999f:bul.speed;
                }
                ground = tu.targetGround;
                air = tu.targetAir;
            }else if(turret instanceof TurretBuild){
                Turret tu = (Turret)turret.block;
                TurretBuild tb = (TurretBuild)turret;
                if(tb.hasAmmo()){
                    BulletType bul = tb.peekAmmo();
                    dps = bul.estimateDPS() / (tu.reloadTime/60);
                    bulletspeed = bul.speed==0?999999f:bul.speed;
                }
                ground = tu.targetGround;
                air = tu.targetAir;
            }
        }
    }
    public class ThreatTileTurretData{
        public TurretInstance turret;
        public ThreatTile tile;
        float distance;

        public ThreatTileTurretData(BaseTurretBuild turret, ThreatTile tile){
            this.turret = new TurretInstance(turret);
            this.tile = tile;
            this.distance = Mathf.dst(turret().x,turret().y,tile.x*Vars.tilesize,tile.y*Vars.tilesize);
        }

        public BaseTurretBuild turret(){
            return turret.turret;
        }
    }
    //ai().mapper.analyser("threat").threatening[29][33].getGroundDps()
    public class ThreatTile{
        public Seq<ThreatTileTurretData> list = new Seq<>();
        public float grounddps,airdps;
        public int lastUpdated = -99999;
        public int x,y;

        public ThreatTile(int x, int y){
            this.x = x;
            this.y = y;
        }

        public void remove(BaseTurretBuild b){
            list.filter(turretInstance -> turretInstance.turret()!=b);
        }

        public void add(BaseTurretBuild b){
            list.add(new ThreatTileTurretData(b,this));
        }

        public float getDps(Unit c){
            if(c.isFlying()){
                return getAirDps(c.team,true);
            }
            return getGroundDps(c.team,true);
        }

        public float getDps(Unit unit, IntMap<Float> times, float currenttime){
            if(lastUpdated+16<mapper.tick){
                refreshDps();
            }
            float total = 0;
            for(ThreatTileTurretData c:list){
                if(((unit.isFlying() && c.turret.air)||(!unit.isFlying() && c.turret.ground)) && (c.turret().team!=unit.team)){
                    if(times.get(c.turret.id)==null){
                        times.put(c.turret.id,currenttime);
                    }
                    float bullettravel = (currenttime-times.get(c.turret.id))*c.turret.bulletspeed;
                    if(bullettravel < c.distance){
                        continue;
                    }

                    total += c.turret.dps;
                }
            }
            return total;
        }

        public float getAirDps(Team t,boolean invert){
            if(lastUpdated+16<mapper.tick){
                refreshDps();
            }
            float total = 0;
            for(ThreatTileTurretData c:list){
                if(c.turret.air && (c.turret().team==t ^ invert)){
                    total += c.turret.dps;
                }
            }
            return total;
        }


        public float getGroundDps(Team t,boolean invert){
            if(lastUpdated+16<mapper.tick){
                refreshDps();
            }
            float total = 0;
            for(ThreatTileTurretData c:list){
                if(c.turret.ground && (c.turret().team==t ^ invert)){
                    total += c.turret.dps;
                }
            }
            return total;
        }

        public void refreshDps(){
            lastUpdated = mapper.tick;
            float g = 0, a = 0;
            for(ThreatTileTurretData c:list){
                c.turret.refreshDps();
                if(c.turret.ground){
                    g += c.turret.dps;
                }
                if(c.turret.air){
                    a += c.turret.dps;
                }
            }
            grounddps = g;
            airdps = a;
        }
    }

    HashSet<BaseTurretBuild> contained = new HashSet<>();
    public ThreatTile[][] threatening;
    ThreatTile empty = new ThreatTile(-1,-1);

    public ThreatTile get(int x,int y){
        if(x<=0||y<=0 || x>=threatening.length|| y>=threatening[0].length){
            return empty;
        }
        if(threatening[x][y]==null){
            threatening[x][y] = new ThreatTile(x,y);
        }
        return threatening[x][y];
    }
    public ThreatTile get(float x,float y){

        return get((int)(x/Vars.tilesize),(int)(y/Vars.tilesize));
    }

    void addTurretToTile(int x, int y, BaseTurretBuild b){
        if(threatening[x][y]==null){
            threatening[x][y] = new ThreatTile(x,y);
        }
        threatening[x][y].add(b);
    }
    void removeTurretFromTile(int x, int y, BaseTurretBuild b){
        if(threatening[x][y]==null){
            return;
        }
        threatening[x][y].remove(b);
    }

    @Override
    public void init(WorldMapper w){
        threatening = new ThreatTile[w.world().width()][w.world().height()];
    }

    @Override
    public void consumeTile(Tile t){
        if(t.build instanceof BaseTurretBuild){
            onBuildPlaced(t.build);
        }
    }

    @Override
    public void onBuildPlaced(Building b){
        if(b instanceof BaseTurretBuild){
            BaseTurretBuild bt = (BaseTurretBuild)b;
            if(contained.contains(bt)){
                return;
            }
            tilesCoveredBy(bt, (x, y) -> {
                addTurretToTile(x, y, bt);
            });
            contained.add(bt);
        }
    }

    @Override
    public void onBuildRemoved(Building b){
        if(b instanceof ConstructBuild){
            if(threatening[b.tile.x][b.tile.y] == null){
                return;
            }
            for(ThreatTileTurretData c : threatening[b.tile.x][b.tile.y].list){
                if(!c.turret().added){
                    b = c.turret();
                    break;
                }
            }
        }
        if(b instanceof BaseTurretBuild){
            BaseTurretBuild bt = (BaseTurretBuild)b;
            if(!contained.contains(bt)){
                return;
            }
            tilesCoveredBy(bt, (x, y) -> {
                removeTurretFromTile(x, y, bt);
            });
            contained.remove(bt);
        }
    }

    public void displayRange(Building b){
        if(b instanceof BaseTurretBuild){
            BaseTurretBuild bt = (BaseTurretBuild)b;
            tilesCoveredBy(bt,(x,y)-> {
                Call.effect(Fx.healBlockFull, x*8, y*8, 1, bt.team.color);
            });
        }
    }

    public void tilesCoveredBy(BaseTurretBuild bt, Cons2<Integer,Integer> cons){
       Tile origin = bt.tile;
       int minX = (int)(origin.x-bt.range());
       int maxX = (int)(origin.x+bt.range());
       int minY = (int)(origin.y-bt.range());
       int maxY = (int)(origin.y+bt.range());
       float r2 = ((bt.range()+1)*(bt.range()+1))/64.0f;
       minX = Math.max(0,minX);
       minY = Math.max(0,minY);
       maxX = Math.min(Vars.world.width()-1,maxX);
       maxY = Math.min(Vars.world.height()-1,maxY);
       for(int x = minX; x<=maxX;x++){
           for(int y = minY; y<=maxY;y++){
               if(Mathf.dst2(x,y,origin.x,origin.y)<=r2){
                   cons.get(x,y);
               }
           }
       }
    }

}
