package aibot.util;

public interface Task{
    void doTask();
    boolean isFinished();
    float amountDone();
    float computeWeight();
    void abort();
    String name();
}
