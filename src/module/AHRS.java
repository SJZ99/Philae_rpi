package module;

import device.NineDOF;
import module.tool.Timer;

public class AHRS {
    NineDOF sensor;
    Timer timer = new Timer();
    private double yaw = 0, roll = 0, pitch = 0;
    public AHRS(NineDOF sensor){
        this.sensor = sensor;
        Thread update = new Thread(new Update());
        update.start();
    }

    /**
     * Getter for gyro yaw value (position)
     * @return Angles of z axis
     */
    public double getRawYaw(){
        return yaw;
    }

    /**
     * Getter for gyro roll value (position)
     * @return Angles of y axis
     */
    public double getRawRoll(){
        return roll;
    }

    /**
     * Getter for gyro pitch value (position)
     * @return Angles of x axis
     */
    public double getRawPitch(){
        return pitch;
    }

    /**
     * Run a thread to keep update integral value.
     */
    private class Update implements Runnable{
        @Override
        public void run() {
            sensor.updateAccelerometer();
            sensor.updateGyroscope();
            sensor.updateMagnetometer();
            double deltaT;
            while(!Thread.interrupted()) {
                deltaT = timer.getPass() * timer.getResolution();
                pitch += sensor.getGyro_x() * deltaT;
                roll  += sensor.getGyro_y() * deltaT;
                yaw   += sensor.getGyro_z() * deltaT;
                timer.spinLock(1000000000 / sensor.getGyroSampleRate());
            }
        }
    }
}