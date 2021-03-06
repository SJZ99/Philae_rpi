package device;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

import device.Scale.*;


import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class Mpu9250 implements NineDOF{
    private static final byte[] MPU9250_ADDRESS = {
            (byte)0x68,
            (byte)0x69
    };
    /** Sensor reference */
    private I2CDevice mpu9250 = null;
    /** scale */
    private Scale scale;
    /** Gyro data */
    private int[] gyro = new int[3];
    /** Accel data */
    private int[] accel = new int[3];
    /** Mag data */
    private int[] mag = new int[3];

    public Mpu9250() {
        this(false);
    }

    public Mpu9250(boolean isAD0High) {
        this(new Scale(Scale.AccScale.AFS_4G, GyroScale.GFS_250DPS, Scale.MagScale.MFS_16BIT),
                isAD0High);
    }

    public Mpu9250(Scale scale, boolean isAD0High) {
        try{
            I2CBus i2c = I2CFactory.getInstance(1);
            if(isAD0High){
                mpu9250 = i2c.getDevice(MPU9250_ADDRESS[1]);
            }else{
                mpu9250 = i2c.getDevice(MPU9250_ADDRESS[0]);
            }
        }catch (IOException e) {
            e.printStackTrace();
        } catch (I2CFactory.UnsupportedBusNumberException e) {
            e.printStackTrace();
        }

        this.scale = scale;
        init();
    }

    /**
     * Init Gyroscope and Accelerometer.
     */
    public void init(){
        short temp = read(Mpu9250.Registers.PWR_MGMT_1.getAddress());

        temp = (byte)(temp & 0x06);  //clear [6](sleep mode), set [1](auto select clock source)
        write(Mpu9250.Registers.PWR_MGMT_1.getAddress(), temp);
        sleep(150);

        temp = (byte)(temp | 0x01);
        write(Mpu9250.Registers.PWR_MGMT_1.getAddress(), temp);
        sleep(200);

        writeOffset((byte) Mpu9250.Registers.XG_OFFSET_H.getAddress(), new int[]{0, 0, 0});

        write(Mpu9250.Registers.CONFIG.getAddress(), (byte)0x03); //Gyroscope 41kHz, Temperature 42kHz

        write(Mpu9250.Registers.SMPLRT_DIV.getAddress(), (byte)0x04); //Gyroscope 41k * 1 / (1 + SMPLRT_DIV) = 8k(Hz)

        temp = (byte)0;
        write(Mpu9250.Registers.GYRO_CONFIG.getAddress(), (byte)(temp | scale.getGyro().getValue())); //Gyroscope default 250 (deg / sec)

        write(Mpu9250.Registers.ACCEL_CONFIG.getAddress(), (byte)(temp | scale.getAcc().getValue())); //Accelerometer default 4g

        temp = read(Mpu9250.Registers.ACCEL_CONFIG.address);
        temp = (byte)(temp & ~0x0F);
        temp = (byte)(temp | 0x03);
        write(Mpu9250.Registers.ACCEL_CONFIG2.getAddress(), temp);

        write(Mpu9250.Registers.INT_PIN_CFG.getAddress(), (byte)0x22);  // INT is 50 microsecond pulse and any read to clear - as per MPUBASICAHRS_T3
        write(Mpu9250.Registers.INT_ENABLE.getAddress(), (byte)0x01);  // Enable data ready (bit 0) interrupt
        sleep(100);

        int[] offset = calibrate((byte) Mpu9250.Registers.GYRO_XOUT_H.getAddress(), 4);
        System.out.println(Arrays.toString(offset));
        writeOffset((byte) Mpu9250.Registers.XG_OFFSET_H.getAddress(), offset);

    }

    public int[] getAccelData(){
        return accel;
    }

    public int[] getGyroData(){
        return gyro;
    }
    /**
     * Getter for gyro x value (velocity)
     * @return Angular velocity of x axis
     */
    public double getGyro_x(){
        return gyro[0];
    }

    /**
     * Getter for gyro y value (velocity)
     * @return Angular velocity of y axis
     */
    public double getGyro_y(){
        return gyro[1];
    }

    /**
     * Getter for gyro z value (velocity)
     * @return Angular velocity of z axis
     */
    public double getGyro_z(){
        return gyro[2];
    }

    @Override
    public void updateGyroscope() {
        gyro = read16Bit(Registers.GYRO_XOUT_H.getAddress(), 3);
    }

    @Override
    public void updateAccelerometer() {
        accel = read16Bit(Registers.ACCEL_XOUT_H.getAddress(), 3);
    }

    @Override
    public void updateMagnetometer() {

    }

    public Scale getScale(){
        return scale;
    }

    /**
     * Get bias of sensor (3 DOF)
     * @param startAddress Sensor data address
     * @param iteration    Times of Iteration (1~5)
     * @return bias of sensor
     */
    public int[] calibrate(byte startAddress, int iteration){
        float kP = 0;
        float kI = 0;
        float rate = (float)((100 - (20 - (iteration / 5.0 * 20))) / 100);
        int[] offset = new int[]{0, 0, 0};
        int[] errorSum = new int[]{0, 0, 0};
        if(startAddress == Mpu9250.Registers.GYRO_XOUT_H.getAddress()){
            kP = 0.6f;
            kI = 0.02f;
        }else if(startAddress == Mpu9250.Registers.ACCEL_XOUT_H.getAddress()){
            kP = 0.3f;
            kI = 0.02f;
        }
        kP *= rate;
        kI *= rate;
        //run PID
        for(int i = 0; i < iteration; ++i){
            for(int j = 0; j < 100; ++j){
                int[] err = read16Bit(startAddress, 3);
                for(int k = 0; k < 3; ++k){
                    err[k] -= offset[k];
                    errorSum[k] += err[k];
                    offset[k] += (short) Math.round(err[k] * kP + errorSum[k] * kI);
//                    System.out.print(offset + ", ");
                }
//                System.out.println();
                if(j % 5 == 0){
                    for(int k = 0; k < 3; ++k){
                        errorSum[k] = 0;
                    }
                }
            }
            kP *= 0.85;
            kI *= 0.85;
        }
        return offset;
    }

    /**
     * Write offset to hardware register.
     * @param address First register address
     * @param offset  Offset array, should contain three value(x, y, z)
     */
    public void writeOffset(byte address, int[] offset){
        byte[] buffer = new byte[6];
        if(address == Mpu9250.Registers.XG_OFFSET_H.getAddress()){
            //Divide 4 to match the registers of offset expect.
            buffer[0] = (byte)(((-offset[0] / 4) >> 8) & 0xFF);
            buffer[1] = (byte)((-offset[0] / 4) & 0xFF);
            buffer[2] = (byte)(((-offset[1] / 4) >> 8) & 0xFF);
            buffer[3] = (byte)((-offset[1] / 4) & 0xFF);
            buffer[4] = (byte)(((-offset[2] / 4) >> 8) & 0xFF);
            buffer[5] = (byte)((-offset[2] / 4) & 0xFF);

            write(Mpu9250.Registers.XG_OFFSET_H.getAddress(), 6, buffer);
        }
    }

    /**
     * Write byte(8 bit) to mpu9250's register
     * @param address Register address
     * @param data    Data(8 bit)
     */
    public void write(int address, int data){
        try{
            mpu9250.write(address, (byte)data);
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
                mpu9250.write((address + i), data[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Read a data(8 bit)
     * @param  address Register address
     * @return Register data
     */
    public short read(int address) {
        short registerData = 0;
        try {
            registerData = (short)mpu9250.read(address);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return registerData;
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
            mpu9250.read(address, raw, 0, groupCount * 2);

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

    /**
     * Get gyro sample rate
     * @return 8000 (Hz)
     */
    public int getGyroSampleRate(){
        return 8000;
    }

    public double getGyroResolution(){
        return scale.getGyro().getResolution();
    }
    public double getAccelResolution(){
        return scale.getAcc().getResolution();
    }


    enum MagMode
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

    public enum Registers
    {
        AK8963_ADDRESS   (0x0C), // i2c bus address

        WHO_AM_I_AK8963  (0x00), // should return (0x48
        INFO             (0x01),
        AK8963_ST1       (0x02),  // data ready status bit 0
        AK8963_XOUT_L    (0x03),  // data
        AK8963_XOUT_H    (0x04),
        AK8963_YOUT_L    (0x05),
        AK8963_YOUT_H    (0x06),
        AK8963_ZOUT_L    (0x07),
        AK8963_ZOUT_H    (0x08),
        AK8963_ST2       (0x09),  // Data overflow bit 3 and data read error status bit 2
        AK8963_CNTL      (0x0A),  // Power down (0000), single-measurement (0001), self-test (1000) and Fuse ROM (1111) modes on bits 3:0
        AK8963_ASTC      (0x0C),  // Self test control
        AK8963_I2CDIS    (0x0F),  // I2C disable
        AK8963_ASAX      (0x10),  // Fuse ROM x-axis sensitivity adjustment address
        AK8963_ASAY      (0x11),  // Fuse ROM y-axis sensitivity adjustment address
        AK8963_ASAZ      (0x12),  // Fuse ROM z-axis sensitivity adjustment address

        SELF_TEST_X_GYRO (0x00),
        SELF_TEST_Y_GYRO (0x01),
        SELF_TEST_Z_GYRO (0x02),

        SELF_TEST_X_ACCEL(0x0D),
        SELF_TEST_Y_ACCEL(0x0E),
        SELF_TEST_Z_ACCEL(0x0F),

        XG_OFFSET_H      (0x13),  // User-defined trim values for gyroscope
        XG_OFFSET_L      (0x14),
        YG_OFFSET_H      (0x15),
        YG_OFFSET_L      (0x16),
        ZG_OFFSET_H      (0x17),
        ZG_OFFSET_L      (0x18),
        SMPLRT_DIV       (0x19),
        CONFIG           (0x1A),
        GYRO_CONFIG      (0x1B),
        ACCEL_CONFIG     (0x1C),
        ACCEL_CONFIG2    (0x1D),
        LP_ACCEL_ODR     (0x1E),
        WOM_THR          (0x1F),
        MOT_DUR          (0x20),  // Duration counter threshold for motion interrupt generation, 1 kHz rate, LSB = 1 ms
        ZMOT_THR         (0x21),  // Zero-motion detection threshold bits [7:0]
        ZRMOT_DUR        (0x22),  // Duration counter threshold for zero motion interrupt generation, 16 Hz rate, LSB = 64 ms
        FIFO_EN          (0x23),
        I2C_MST_CTRL     (0x24),
        I2C_SLV0_ADDR    (0x25),
        I2C_SLV0_REG     (0x26),
        I2C_SLV0_CTRL    (0x27),
        I2C_SLV1_ADDR    (0x28),
        I2C_SLV1_REG     (0x29),
        I2C_SLV1_CTRL    (0x2A),
        I2C_SLV2_ADDR    (0x2B),
        I2C_SLV2_REG     (0x2C),
        I2C_SLV2_CTRL    (0x2D),
        I2C_SLV3_ADDR    (0x2E),
        I2C_SLV3_REG     (0x2F),
        I2C_SLV3_CTRL    (0x30),
        I2C_SLV4_ADDR    (0x31),
        I2C_SLV4_REG     (0x32),
        I2C_SLV4_DO      (0x33),
        I2C_SLV4_CTRL    (0x34),
        I2C_SLV4_DI      (0x35),
        I2C_MST_STATUS   (0x36),
        INT_PIN_CFG      (0x37),
        INT_ENABLE       (0x38),
        DMP_INT_STATUS   (0x39),  // Check DMP interrupt
        INT_STATUS       (0x3A),
        ACCEL_XOUT_H     (0x3B),
        ACCEL_XOUT_L     (0x3C),
        ACCEL_YOUT_H     (0x3D),
        ACCEL_YOUT_L     (0x3E),
        ACCEL_ZOUT_H     (0x3F),
        ACCEL_ZOUT_L     (0x40),
        TEMP_OUT_H       (0x41),
        TEMP_OUT_L       (0x42),
        GYRO_XOUT_H      (0x43),
        GYRO_XOUT_L      (0x44),
        GYRO_YOUT_H      (0x45),
        GYRO_YOUT_L      (0x46),
        GYRO_ZOUT_H      (0x47),
        GYRO_ZOUT_L      (0x48),
        EXT_SENS_DATA_00 (0x49),
        EXT_SENS_DATA_01 (0x4A),
        EXT_SENS_DATA_02 (0x4B),
        EXT_SENS_DATA_03 (0x4C),
        EXT_SENS_DATA_04 (0x4D),
        EXT_SENS_DATA_05 (0x4E),
        EXT_SENS_DATA_06 (0x4F),
        EXT_SENS_DATA_07 (0x50),
        EXT_SENS_DATA_08 (0x51),
        EXT_SENS_DATA_09 (0x52),
        EXT_SENS_DATA_10 (0x53),
        EXT_SENS_DATA_11 (0x54),
        EXT_SENS_DATA_12 (0x55),
        EXT_SENS_DATA_13 (0x56),
        EXT_SENS_DATA_14 (0x57),
        EXT_SENS_DATA_15 (0x58),
        EXT_SENS_DATA_16 (0x59),
        EXT_SENS_DATA_17 (0x5A),
        EXT_SENS_DATA_18 (0x5B),
        EXT_SENS_DATA_19 (0x5C),
        EXT_SENS_DATA_20 (0x5D),
        EXT_SENS_DATA_21 (0x5E),
        EXT_SENS_DATA_22 (0x5F),
        EXT_SENS_DATA_23 (0x60),
        MOT_DETECT_STATUS (0x61),
        I2C_SLV0_DO      (0x63),
        I2C_SLV1_DO      (0x64),
        I2C_SLV2_DO      (0x65),
        I2C_SLV3_DO      (0x66),
        I2C_MST_DELAY_CTRL (0x67),
        SIGNAL_PATH_RESET  (0x68),
        MOT_DETECT_CTRL  (0x69),
        USER_CTRL        (0x6A),  // Bit 7 enable DMP, bit 3 reset DMP
        PWR_MGMT_1       (0x6B), // Device defaults to the SLEEP mode
        PWR_MGMT_2       (0x6C),
        DMP_BANK         (0x6D),  // Activates a specific bank in the DMP
        DMP_RW_PNT       (0x6E),  // Set read/write pointer to a specific start address in specified DMP bank
        DMP_REG          (0x6F),  // Register in DMP from which to read or to which to write
        DMP_REG_1        (0x70),
        DMP_REG_2        (0x71),
        FIFO_COUNTH      (0x72),
        FIFO_COUNTL      (0x73),
        FIFO_R_W         (0x74),
        WHO_AM_I_MPU9250 (0x75), // Should return (0x71
        XA_OFFSET_H      (0x77),
        XA_OFFSET_L      (0x78),
        YA_OFFSET_H      (0x7A),
        YA_OFFSET_L      (0x7B),
        ZA_OFFSET_H      (0x7D),
        ZA_OFFSET_L      (0x7E),


        SELF_TEST_A      (0x10);


        private final int address;
        Registers(int addr)
        {
            this.address = addr;
        }
        public int getAddress()
        {
            return address;
        }
    }
}
