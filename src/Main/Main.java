package Main;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import device.Mpu9250;

import java.io.IOException;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        Mpu9250 ahrs = new Mpu9250();
        for(int i = 0; i < 3; i++){
            short[] offset = ahrs.calibrate((byte) Mpu9250.Registers.GYRO_XOUT_H.getAddress(), 5);
            System.out.println("offset" + Arrays.toString(offset));
            System.out.println("value" + Arrays.toString(ahrs.read16Bit((byte) Mpu9250.Registers.GYRO_XOUT_H.getAddress(), 3)));
        }

    }
}
