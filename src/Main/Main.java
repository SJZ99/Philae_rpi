package Main;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import device.Magnetometer;
import device.Mpu9250;
import module.AHRS;
import module.tool.Timer;

import java.io.IOException;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        Mpu9250 mpu9250 = new Mpu9250();
        Magnetometer mag = new Magnetometer(0x0c, mpu9250.getScale(), Magnetometer.MagMode.MAG_MODE_100HZ);
        Timer t = new Timer();
//        System.out.println(Arrays.toString(mpu9250.read16Bit((byte)Mpu9250.Registers.SELF_TEST_X_GYRO.getAddress(), 3)));


        while(true){
//            mpu9250.updateGyroscope();
//            System.out.println(mpu9250.getGyro_z());
            mag.update();
            t.spinLock(100000000);
            System.out.println(Arrays.toString(mag.getMag()));
        }
//        AHRS ahrs = new AHRS(mpu9250);
//        mpu9250.sleep(200);
//        System.out.println(Arrays.toString(mpu9250.read16Bit((byte)Mpu9250.Registers.GYRO_XOUT_H.getAddress(), 3)));
//        ahrs.startUpdate();

    }
}
