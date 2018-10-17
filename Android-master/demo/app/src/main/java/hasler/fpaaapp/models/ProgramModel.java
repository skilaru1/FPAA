package hasler.fpaaapp.models;

import android.os.Environment;
import android.util.Log;
import android.widget.ProgressBar;

import java.io.File;
import java.util.Map;

import hasler.fpaaapp.utils.Driver;
import hasler.fpaaapp.utils.Utils;
import hasler.fpaaapp.utils.targetProgram;

/**
 * Created by Brian on 10/4/17.
 */

public class ProgramModel {

    private File zipFile;
    private Driver driver;

    ProgressBar progressBar;

    public ProgramModel() { /* Required empty public constructor */ }

    public ProgramModel(String zipFileName, Driver driver, ProgressBar progressBar) {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        zipFile = new File(path, zipFileName);

        this.driver = driver;
        this.progressBar = progressBar;

    }

    public void program() throws Exception {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(path, "lpf_meas.zip");

        Map<String, byte[]> zipped_files = Utils.getZipContents(file.getAbsolutePath());

        progressBar.setProgress(10);
        tunnelProgram(zipped_files);
        progressBar.setProgress(20);


        switchProgram(zipped_files);
        progressBar.setProgress(30);

        try {
            targetProgram(zipped_files);
        } catch(Exception e) {
            throw new Exception("Program Error: Unable to target program. Details: " + e.getMessage());
        }

        progressBar.setProgress(70);

        // Run mode
        runMode(zipped_files);
        progressBar.setProgress(100);
    }

    public void tunnelProgram(Map<String, byte[]> zipped_files) throws Exception {
        compileAndProgram(zipped_files, "tunnel_revtun_SWC_CAB.elf", 10 * 1000);
    }

    public void switchProgram(Map<String, byte[]> zipped_files) throws Exception {

//        Log.i("Switch Program", "Writing to Memory");
//        if (!writeAscii(zipped_files, 0x5500, "switch_info")) {
//            throw new Exception("Program Error: Unable to complete switch_info");
//        }
//
//        Log.i("Switch Program", "Compiling ELF");
//        // compileAndProgram(zipped_files, "switch_program.elf", 70 * 1000);
        writeAscii(zipped_files, 0x7000, "switch_info");
        compileAndProgram(zipped_files, "switch_program.elf", 1000);
//        writeAscii(zipped_files, 0x7000, "target_info_aboveVt_mite");//line 27
//        compileAndProgram(zipped_files, "recover_inject_aboveVt_CAB_mite.elf", 20 * 1000);//line 29

        /*
        Utils.debugLine("n_target_aboveVt_mite", true);
        writeAscii(zipped_files, 0x7000, "target_info_aboveVt_mite");//line 27
        //Utils.debugLine("1",true);
        writeAscii(zipped_files, 0x6800, "pulse_width_table_mite");//line 28
        //Utils.debugLine("2",true);
        compileAndProgram(zipped_files, "recover_inject_aboveVt_CAB_mite.elf", 20 * 1000);//line 29
        //Utils.debugLine("3",true);

        // coarse inj
        //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_mite")) return false; //line 32
        //Utils.debugLine("4",true);
        compileAndProgram(zipped_files, "first_coarse_program_aboveVt_CAB_mite.elf", 20 * 1000);//line 33
        //Utils.debugLine("5",true);
        //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_mite")) return false;//line 36
        //Utils.debugLine("6",true);
        compileAndProgram(zipped_files, "measured_coarse_program_aboveVt_CAB_mite.elf", 20 * 1000);//line 37
        //Utils.debugLine("7",true);

        // fine inj
        //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_mite")) return false;//line 39
        //Utils.debugLine("8",true);
        writeAscii(zipped_files, 0x6800, "Vd_table_30mV");//line 40
        //Utils.debugLine("9",true);
        compileAndProgram(zipped_files, "fine_program_aboveVt_m_ave_04_CAB_mite.elf", 20 * 1000);//line 41
        //Utils.debugLine("10",true);
        */

    }

    public void runMode(Map<String, byte[]> zipped_files) throws Exception {
        if (!writeAscii(zipped_files, 0x4300, "input_vector")) {
            throw new Exception("Program Error: Unable to complete input_vector");
        }
        if (!writeAscii(zipped_files, 0x4200, "output_info")) {
            throw new Exception("Program Error: Unable to complete output_info");
        }

        compileAndProgram(zipped_files, "voltage_meas.elf", 70 * 1000);
    }

    public void compileAndProgram(Map<String, byte[]> loc, String name, int wait_ms) throws Exception {
        byte[] data;

        if (!loc.containsKey(name.trim())) {
            Utils.debugLine("Zipped file does not contain file name \"" + name + "\"",true);
            throw new Exception("Zip file does not contain: " + name);
        }

        try {
            Log.i("Program Model", "Compiling Elf...");
            data = Utils.compileElf(loc.get(name));
        } catch (Exception e) {
            throw new Exception("Compile Elf failed");
        }

        try {
            Log.i("Program Model", "Programming Data...");
            driver.programData(data);
        } catch (Exception e) {
            throw new Exception("Driver programming failed");
        }

        // driver.sleep(wait_ms);
    }

    public void targetProgram(Map<String, byte[]> zipped_files) throws Exception {
        targetProgram tp = new targetProgram(driver, progressBar);

        progressBar.setProgress(50);

        //get the lines of the text file targetList
        tp.TARGETLIST(zipFile);

        for(String instruct : tp.targetListInstruct){
            Utils.debugLine(instruct,true);
        }
        //program the FPAA based on the lines of targetList
        tp.targetProgram();
    }

    private boolean writeAscii(Map<String, byte[]> loc, int address, String name) throws Exception{
        if (!loc.containsKey(name)) {
            throw new Exception("Zipped file does not contain file name \"" + name + "\"");
        }

        byte[] data = Utils.parseHexAscii(loc.get(name));
        data = Utils.swapBytes(data);
        boolean b = driver.writeMem(address, data);
        if (b) driver.sleep(1000);
        return b;
    }
}
