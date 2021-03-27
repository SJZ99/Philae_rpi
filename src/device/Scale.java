package device;

public class Scale {
    private AccScale acc;
    private GyroScale gyro;
    private MagScale mag;

    public Scale(AccScale acc, GyroScale gyro, MagScale mag){
        this.acc = acc;
        this.gyro = gyro;
        this.mag = mag;
    }

    public Scale(AccScale acc, GyroScale gyro){
        this(acc, gyro, null);
    }

    public boolean haveMag(){
        return (mag != null);
    }

    public AccScale getAcc(){
        return acc;
    }

    public GyroScale getGyro(){
        return gyro;
    }

    public MagScale getMag(){
        return mag;
    }

    enum MagScale
    {
        MFS_14BIT((byte)0x00,10f*4912f/8190f), //mscale val = 0, 14 bit will be shifted 4 left
        MFS_16BIT((byte)0x01,10f*4912f/32760f); //mscale val = 1, 16 bit will be shifted 4 left

        private final byte value;
        private final float res;
        MagScale(byte value, float res)
        {
            this.value = value;
            this.res = res;
        }
        public byte getValue()
        {
            return value;
        }
        public float getResolution()
        {
            return res;
        }
        public int getMinMax()
        {
            return 4800;
        }
    }

    enum AccScale
    {
        AFS_2G(0x00,2),
        AFS_4G(0x08,4),
        AFS_8G(0x10,8),
        AFS_16G(0x18,16);

        private final int value;
        private final int minMax;
        AccScale(int value, int minMax)
        {
            this.value = value;
            this.minMax = minMax;
        }
        public byte getValue()
        {
            return (byte)value;
        }
        public double getResolution()
        {
            return (double)minMax/32768.0;
        }
        public int getMinMax()
        {
            return minMax;
        }
    }

    enum GyroScale
    {
        GFS_250DPS(0x00,250),
        GFS_500DPS(0x08,500),
        GFS_1000DPS(0x10,1000),
        GFS_2000DPS(0x18,2000);


        private final int value;
        private final int minMax;
        GyroScale(int value, int minMax)
        {
            this.value = value;
            this.minMax = minMax;
        }
        public byte getValue()
        {
            return (byte)value;
        }
        public double getResolution()
        {
            return (double)minMax * 2.0 / 65536.0;
        }
        public int getMinMax()
        {
            return minMax;
        }
    }

}
