package device;

public interface NineDOF {
    public double getGyro_x();
    public double getGyro_y();
    public double getGyro_z();

    public void updateGyroscope();
    public void updateAccelerometer();
    public void updateMagnetometer();

    public int getGyroSampleRate();

    public double getGyroResolution();
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
