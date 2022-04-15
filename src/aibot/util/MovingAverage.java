package aibot.util;

public class MovingAverage{
    float[] values;
    int index = 0;
    public float average = 0;
    float total = 0;

    int delay = 0, tick = 0;
    float accum = 0;

    public MovingAverage(int length, int delay){
        values = new float[length];
        this.delay = delay;
    }

    public void push(float value){
        tick--;
        if(tick >= 0){
            accum += value;
            return;
        }
        tick = delay;
        accum /= (float)(delay + 1);


        index++;
        if(index >= values.length){
            index = 0;
        }
        total -= values[index];
        values[index] = accum;
        total += accum;

        average = total / values.length;
        accum = 0;
    }

    public float getTotal(){
        return total;
    }
}
