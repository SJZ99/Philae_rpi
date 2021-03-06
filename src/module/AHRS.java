package module;

import device.NineDOF;
import module.tool.Timer;

public class AHRS {
    NineDOF sensor;
    Timer timer = new Timer();
    private double yaw = 0, roll = 0, pitch = 0;
    private double x, y, z, norm;
    public AHRS(NineDOF sensor){
        this.sensor = sensor;
    }
    public void startUpdate(){
        Thread update = new Thread(new Update());
        update.start();
    }
    /**
     * Getter for gyro yaw value (position)
     * on)
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
            double deltaT;
            while(!Thread.interrupted()) {
                deltaT = timer.getPass() * timer.getResolution();
                sensor.updateAccelerometer();
                sensor.updateGyroscope();
                sensor.updateMagnetometer();

                x = sensor.getAccelData()[0] * sensor.getAccelResolution();
                y = sensor.getAccelData()[1] * sensor.getAccelResolution();
                z = sensor.getAccelData()[2] * sensor.getAccelResolution();
                norm = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
                System.out.printf("x= %f, y= %f, z= %f, norm= %f", x, y, z, norm);
                /*
                pitch += sensor.getGyro_x() * deltaT * sensor.getGyroResolution();
                roll  += sensor.getGyro_y() * deltaT * sensor.getGyroResolution();
                yaw   += sensor.getGyro_z() * deltaT * sensor.getGyroResolution();
                System.out.printf("yaw= %f, roll= %f, pitch= %f\n", yaw, roll, pitch);
                timer.spinLock(1000000000 / sensor.getGyroSampleRate()/8);
                 */
            }
        }
    }
}
