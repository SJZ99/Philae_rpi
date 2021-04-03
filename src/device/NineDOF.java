package device;

public interface NineDOF {
    public int[] getGyroData();
    public int[] getAccelData();

    public void updateGyroscope();
    public void updateAccelerometer();
    public void updateMagnetometer();

    public int getGyroSampleRate();

    public double getGyroResolution();
    public double getAccelResolution();
    /**
     * Sleep specified length of time.
     * @param mills mills that will sleep
     */
    public default void sleep(long mills){
        try{
            Thread.sleep(mills);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
