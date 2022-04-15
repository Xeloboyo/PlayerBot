package aibot.util;

public class StopWatch{
    long millis;

    public StopWatch(){
        millis = System.currentTimeMillis();
    }

    //idk what to call it
    public long click(){
        long diff = System.currentTimeMillis() - millis;
        millis += diff;
        return diff;
    }
}
