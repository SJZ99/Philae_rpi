package module.tool;

public class Timer {
    long last = -1l;

    public void start(){
        last = System.nanoTime();
    }

    public long getPass(){
        if(last < 0){
            return 0;
        }
        return System.nanoTime() - last;
    }

    public long reset(){
        long pass = System.nanoTime() - last;
        start();
        return pass;
    }

    public void spinLock(long nano){
        start();
        while(getPass() < nano);
        return;
    }

    public double getResolution(){
        return (1d / 1000000000d);
    }
}
