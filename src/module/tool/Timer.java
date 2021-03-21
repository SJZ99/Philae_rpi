package module.tool;

public class Timer {
    long last = 0l;

    public void start(){
        last = System.nanoTime();
    }

    public long getPass(){
        return System.nanoTime() - last;
    }

    public long reset(){
        long pass = System.nanoTime() - last;
        start();
        return pass;
    }

    public double getResolution(){
        return (1d / 1000000000d);
    }
}
