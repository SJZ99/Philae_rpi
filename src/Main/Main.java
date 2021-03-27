package Main;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import device.Mpu9250;
import module.AHRS;

import java.io.IOException;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        Mpu9250 mpu9250 = new Mpu9250();
        AHRS ahrs = new AHRS(mpu9250);
        mpu9250.sleep(200);
        System.out.println(Arrays.toString(mpu9250.read16Bit((byte)Mpu9250.Registers.GYRO_XOUT_H.getAddress(), 3)));


    }
}
