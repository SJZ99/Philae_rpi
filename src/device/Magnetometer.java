package device;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class Magnetometer {
    I2CDevice magnetometer;
    double mag[] = new double[3];
    float lastRawMagX, lastRawMagY, lastRawMagZ;
    Scale scale;
    MagMode magMode;
    double[] bias = new double[3];

    public Magnetometer(int address, Scale scale, MagMode mode){
        this.scale = scale;
        this.magMode = mode;
        try {
            I2CBus i2c = I2CFactory.getInstance(1);
            magnetometer = i2c.getDevice(address);
        } catch (I2CFactory.UnsupportedBusNumberException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        init();
    }
    public void sleep(long mills){
        try{
            Thread.sleep(mills);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void init() {
        write(Mpu9250.Registers.AK8963_CNTL.getAddress(),(byte) 0x00); // Power down magnetometer
        sleep(10);
        write(Mpu9250.Registers.AK8963_CNTL.getAddress(), (byte)0x0F); // Enter Fuse ROM access mode
        sleep(10);
        short rawData[] = read(Mpu9250.Registers.AK8963_ASAX.getAddress(), 3);  // Read the x-, y-, and z-axis calibration values
        scale.getMag().getScaling()[0] =  (float)(rawData[0] - 128)/256f + 1f;   // Return x-axis sensitivity adjustment values, etc.
        scale.getMag().getScaling()[1] =  (float)(rawData[1] - 128)/256f + 1f;
        scale.getMag().getScaling()[2] =  (float)(rawData[2] - 128)/256f + 1f;
        write(Mpu9250.Registers.AK8963_CNTL.getAddress(), (byte)0x00); // Power down magnetometer
        sleep(10);
        // Configure the magnetometer for continuous read and highest resolution
        // set Mscale bit 4 to 1 (0) to enable 16 (14) bit resolution in CNTL register,
        // and enable continuous mode data acquisition Mmode (bits [3:0]), 0010 for 8 Hz and 0110 for 100 Hz sample rates
        write(Mpu9250.Registers.AK8963_CNTL.getAddress(), (byte)(scale.getMag().MFS_16BIT.getValue() << 4 | magMode.getMode())); // Set magnetometer data resolution and sample ODR
        sleep(10);
        System.out.println(Arrays.toString(scale.getMag().getScaling()));
    }

    public void update(){
        byte newMagData = (byte) (read(Mpu9250.Registers.AK8963_ST1.getAddress()) & 0x01);
        if (newMagData == 0) return;
        short[] buffer = read(Mpu9250.Registers.AK8963_ST1.getAddress(), 7);

        short c = buffer[6];
        if((c & 0x08) == 0)
        { // Check if magnetic sensor overflow set, if not then report data
            lastRawMagX = (short) ((buffer[1] << 8) | buffer[0]); // Turn the MSB and LSB into a signed 16-bit value
            lastRawMagY = (short) ((buffer[3] << 8) | buffer[2]); // Data stored as little Endian
            lastRawMagZ = (short) ((buffer[5] << 8) | buffer[4]);
            float x=lastRawMagX,y=lastRawMagY,z=lastRawMagZ;

            x *= scale.getMag().getResolution()* scale.getMag().getScaling()[0];
            y *= scale.getMag().getResolution()* scale.getMag().getScaling()[1];
            z *= scale.getMag().getResolution()* scale.getMag().getScaling()[2];

            x -= bias[0];
            y -= bias[1];
            z -= bias[2];

            mag[0] = x;
            mag[1] = y;
            mag[2] = z;
        }
        read(0x09);
    }

    public double[] getMag(){
        return mag;
    }

    /**
     * Write byte(8 bit) to mpu9250's register
     * @param address Register address
     * @param data    Data(8 bit)
     */
    public void write(int address, int data){
        try{
            magnetometer.write(address, (byte)data);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Write multiple value to consequent registers.
     * @param address        First address
     * @param registerCount  How many register want to write
     * @param data           Data that will be wrote to register
     */
    public void write(int address, int registerCount, byte[] data){
        Objects.requireNonNull(data);
        if(data.length != registerCount){
            System.err.println("Couldn't match! Mpu9250 write multiple register");
            return;
        }
        for(int i = 0; i < registerCount; ++i){
            try {
                magnetometer.write((address + i), data[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public short read(int address) {
        short registerData = 0;
        try {
            registerData = (short)magnetometer.read(address);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return registerData;
    }

    public short[] read(int address, int count) {
        short[] data = new short[count];
        try{
            for(int i = 0; i < count; ++i){
                data[i] = (short)magnetometer.read(address + i);
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }
        return data;
    }

    /**
     * Read 16 bit data.
     * @param address     Register address
     * @param groupCount  How many group you want to read.(group = how much bit / 16 bit)
     * @return            Short array that contain data.
     */
    public int[] read16Bit(int address, int groupCount){
        byte[] raw = new byte[groupCount * 2]; //16 bit is two registers.
        try{
            magnetometer.read(address, raw, 0, groupCount * 2);

        } catch (IOException e) {
            e.printStackTrace();
        }
        int[] group = new int[groupCount];
        for(int i = 0; i < groupCount; ++i){
            group[i] =((Byte.toUnsignedInt(raw[i * 2]) & 0xff) << 8 | (Byte.toUnsignedInt(raw[i * 2 + 1]) & 0xff));
            if(group[i] >= 32768){
                group[i] -= 65536;
            }
        }
        return group;
    }
    public enum MagMode
    {
        MAG_MODE_100HZ   ((byte)0x06,1500), // 6 for 100 Hz continuous magnetometer data read
        MAG_MODE_8HZ	 ((byte)0x02,128); // 2 for 8 Hz, continuous magnetometer data read

        private final byte mode;
        private final int sampleCount;

        MagMode(byte mode,int sampleCount)
        {
            this.mode = mode;
            this.sampleCount = sampleCount;
        }
        public byte getMode()
        {
            return this.mode;
        }
        public int getSampleCount()
        {
            return this.sampleCount;
        }

    }

}
