package hasler.fpaaapp.utils;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import java.util.Arrays;

import hasler.fpaaapp.ControllerActivity;

public class Driver extends GenericDriver {
    private final String TAG = "Driver";

    /* Original */
    D2xxManager d2xxManager;
    FT_Device ftDev = null;
    int devCount = -1;
    int currentIndex = -1;
    int openIndex = 1;

    /* Local variables */
    int baudRate = 115200;
    byte stopBit = 1;
    byte dataBit = 8;
    byte parity = 0;
    byte flowControl = 0;

    /* Parameters and more local variables */
    boolean bReadThreadGoing = false;
    boolean uartConfigured = false;

    private ControllerActivity parentContext;

    public Driver(ControllerActivity parentContext) {
        this.parentContext = parentContext;
        d2xxManager = parentContext.getDeviceManager();
    }


    public void setConfig(int baud, byte dataBits, byte stopBits, byte parity, byte flowControl) {
        if (!ftDev.isOpen()) {
            Toast.makeText(parentContext, "FT device is not open", Toast.LENGTH_SHORT).show();
            return;
        }

        // Configure to our port
        ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);
        ftDev.setBaudRate(baud);

        // Configure data bits
        switch (dataBits) {
            case 7:
                dataBits = D2xxManager.FT_DATA_BITS_7;
                break;
            case 8:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
            default:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
        }

        // Configure stop bits
        switch (stopBits) {
            case 1:
                stopBit = D2xxManager.FT_STOP_BITS_1;
                break;
            case 2:
                stopBits = D2xxManager.FT_STOP_BITS_2;
                break;
            default:
                stopBits = D2xxManager.FT_STOP_BITS_1;
                break;
        }

        // Configure parity
        switch (parity) {
            case 0:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
            case 1:
                parity = D2xxManager.FT_PARITY_ODD;
                break;
            case 2:
                parity = D2xxManager.FT_PARITY_EVEN;
                break;
            case 3:
                parity = D2xxManager.FT_PARITY_MARK;
                break;
            default:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
        }

        // Set data characteristics
        ftDev.setDataCharacteristics(dataBits, stopBits, parity);

        short flowControlSetting;
        switch (flowControl) {
            case 0:
                flowControlSetting = D2xxManager.FT_FLOW_NONE;
                break;
            case 1:
                flowControlSetting = D2xxManager.FT_FLOW_RTS_CTS;
                break;
            case 2:
                flowControlSetting = D2xxManager.FT_FLOW_DTR_DSR;
                break;
            case 3:
                flowControlSetting = D2xxManager.FT_FLOW_XON_XOFF;
                break;
            default:
                flowControlSetting = D2xxManager.FT_FLOW_NONE;
                break;
        }

        // Shouldn't be hard coded, but I don't know the correct way
        ftDev.setFlowControl(flowControlSetting, (byte) 0x0b, (byte) 0x0d);

        uartConfigured = true;
    }

    protected void createDeviceList() {
        int tempDevCount = d2xxManager.createDeviceInfoList(parentContext);
        if (tempDevCount > 0) {
            if (devCount != tempDevCount) {
                devCount = tempDevCount;
            }
        } else {
            devCount = -1;
            currentIndex = -1;
        }
    }

    public void disconnect() {
        devCount = -1;
        currentIndex = -1;
        bReadThreadGoing = false;

        // Sleep for 50 milliseconds
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Close the FT device, if it is open
        if (ftDev != null) {
            synchronized (ftDev) {
                if (ftDev.isOpen()) {
                    ftDev.close();
                }
            }
        }

        if (thread != null) {
            thread.cancel(false);
        }
    }

    private int readTime = 5000;

    private boolean readComplete;

    private byte[] data;
    private int readAmount, readLength;

    public void setReadTime(int time) {
        readTime = time;
    }

    protected byte[] read(int length) throws ReadTimeOutException {
        return read(length, readTime);
    }

    protected ReadThread thread;

    protected byte[] read(int length, int n_millis) throws ReadTimeOutException {
        readAmount = 0;
        readLength = length;
        data = new byte[readLength];
        readComplete = false;

        if (thread == null || thread.isCancelled()) {
            thread = new ReadThread();
            thread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        // wait for the thread to complete
        long startTime = System.currentTimeMillis();
        while (!readComplete) {
            if (System.currentTimeMillis() - startTime > n_millis) { // cancel after 5 seconds
                Log.d(TAG, "Read Thread Error");
                thread.cancel(false);
                thread = null;


                throw new ReadTimeOutException("Cannot read device");
//                data[0] = -128;
//                return data;
                // break;
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return data;
    }

    protected boolean write(byte... outData) {
        if (!ftDev.isOpen()) {
            Log.d(TAG, "FT Device is not open");
            return false;
        }

        int result = ftDev.write(outData, outData.length, false);

        return result == outData.length;
    }

    private class ReadThread extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            final int BUFFER_LENGTH = 70000;
            byte[] buf = new byte[BUFFER_LENGTH];

            int avail;
            while (!isCancelled()) {
                if (readComplete) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                while (readAmount < readLength) {
                    if (isCancelled()) {
                        break;
                    }

                    avail = ftDev.getQueueStatus();
                    if (avail > 0) {
                        if (avail > BUFFER_LENGTH) {
                            avail = BUFFER_LENGTH;
                        }

                        ftDev.read(buf, avail);
                        System.arraycopy(buf, 0, data, readAmount, Math.min(avail, readLength - readAmount));
                        readAmount += avail;

                        if (readAmount == readLength) {
                            ftDev.purge((byte) 1);
                        }
                    }

                    if (readAmount < readLength) {
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                readComplete = true;
            }

            return null;
        }
    }

    public boolean connect() {
        Utils.debugLine("start connect", true);
        if(parentContext == null){
            Utils.debugLine("null parentContext", true);
        }
        if (devCount <= 0) {
            createDeviceList();
        }

        if (devCount <= 0) {
            Utils.debugLine("devicecount <= 0", true);
            return false;
        }

        if (currentIndex != openIndex) {
            if (ftDev == null) {
                ftDev = d2xxManager.openByIndex(parentContext, openIndex);
            } else {
                synchronized (ftDev) {
                    ftDev = d2xxManager.openByIndex(parentContext, openIndex);
                }
            }
            uartConfigured = false;
        }

//        if (ftDev == null) {
//            Utils.debugLine("ftDev == null", true);
//            return false;
//        }

        if (ftDev.isOpen()) {
            currentIndex = openIndex;
        } else {
            Utils.debugLine("ftDev is closed", true);
            return false;
        }

        if (!uartConfigured) {
            setConfig(baudRate, dataBit, stopBit, parity, flowControl);
            uartConfigured = true;
            Utils.debugLine("reconfigure UART", true);
        }
        Utils.debugLine("" + uartConfigured, true);
        return uartConfigured;

    }

    /**
     * Program data to the device
     * @param data The data to program
     * @return Whether or not the data was programmed
     */
    public void programData(byte[] data) throws Exception{
        int cpu_stat_check = Utils.toInt(readRegister("CPU_STAT"));

        sendSynchronizationFrame();
        if (!connectToDevice()) {
            throw new Exception("Not connected to device");
        }

        // Number of bytes
        int byte_size = data.length;

        // POR and halt the CPU
        executePorHalt();

        // Write the program to memory
        int startAddress = 0x10000 - byte_size;
        Utils.debugLine("programdata writeburst", true);
        writeBurst(startAddress, data);
        sleep(500);

        // Verify that the data was written correctly
        if (!verifyMemory(startAddress, data)) {
            throw new Exception("Unable to verify if data was written correctly");
        }

        // Run the CPU
        int cpuCtlOrg = Utils.toInt(readRegister("CPU_CTL"));
        writeRegister("CPU_CTL", cpuCtlOrg | 0x02);

        boolean cpuIsRunning = true;

        sleep(3000);
        while (cpuIsRunning) {
            try {
                byte[] cpu_stat_val = readRegister("CPU_STAT");
                cpuIsRunning = false;
            } catch (ReadTimeOutException e) {
                Log.e("Driver", "Error 1");
                sleep(3000);
            }

        }

    }

}
