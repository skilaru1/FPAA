package hasler.fpaaapp.utils;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.os.Handler;
import android.os.Environment;

import hasler.fpaaapp.ControllerActivity;
import hasler.fpaaapp.R;


public class targetProgram {

    ProgressBar progressBar;
    Driver driver;

    public Map<String, byte[]> zipped_files;
    public String[] targetListInstruct;

    public targetProgram(Driver driver, ProgressBar progressBar) {
        this.driver = driver;
        this.progressBar = progressBar;
    }

     /*==========================================================================================
        TARGETLIST (File inputFile, ProgressBar p)
        ----------------------------------------------------------------------------------------
        Description: Used to copy content from targetlist.txt found in GetTargetList method below
                     into an array of strings (targetListInstruct).
        ----------------------------------------------------------------------------------------
        Arguments: inputFile is the zip file of RASP30 instructions we downloaded from Github.
                   p is the progressbar we are updating
        ----------------------------------------------------------------------------------------
        Result: return true when we have finished copying lines of the targetlist.txt into a
                text file
        ========================================================================================
     */

    public void TARGETLIST(File inputFile) {
        //make File of targetList
        targetListInstruct = GetTargetList(inputFile);
        //extract the lines in targetList
        //Map<String, byte[]> zipped_files = null;
        if (targetListInstruct != null) {
            zipped_files = Utils.getZipContents(inputFile.getAbsolutePath());
            //Utils.debugLine("keyset?", true);
            if (zipped_files.keySet().isEmpty()) {
                Utils.debugLine("null keyset", true);
            }
            List<String> l = new ArrayList<String>(zipped_files.keySet());
            for (String key : l) {
                //Utils.debugLine(key, true);
            }
        }
    }
        /*==========================================================================================
        GetTargetList (File file)
        ----------------------------------------------------------------------------------------
        Description: used to find targetlist.txt from the zip file and then copy its instructions
                     to an array
        ----------------------------------------------------------------------------------------
        Arguments: file is the zip file we downloaded
        ----------------------------------------------------------------------------------------
        Result: returns a String array with line-by-line instruction copied from targetlist.txt
        ========================================================================================
     */

    private String[] GetTargetList(File file) {
        //Utils.debugLine("GetTargetList", true);
        progressBar.setSecondaryProgress(0);
        InputStream is;
        String[] strArr = new String[19];
        String[] entryNames = new String[14];
        int entryCount = 0;
        ZipEntry entry;


        try {
            progressBar.setSecondaryProgress(10);
            is = new FileInputStream(file);
            progressBar.setSecondaryProgress(20);
            ZipInputStream zis = new ZipInputStream(is);
            progressBar.setSecondaryProgress(30);
            entry = zis.getNextEntry();
            if (entry == null) {
                throw new Exception();
            }
            progressBar.setSecondaryProgress(40);
            String entryName;
            //Utils.debugLine("%s" + "%n",entryName);
            progressBar.setSecondaryProgress(50);

            while (entry != null) {
                entryName = entry.getName();
                //Utils.debugLine(entryName, true);
                if (entryName.equals("target_list")) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(zis, "UTF-8"));
                    int count = 0;
                    while (count < 19){
                        Utils.debugLine("" + count,true);
                        strArr[count] = br.readLine();
                        count++;
                        //Toast.makeText(strArr[count]);
                    }
                    br.close();
                    if (count != 19) {
                        Utils.debugLine("not enough lines " + count, true);
                        return null;
                    }
                    progressBar.setSecondaryProgress(100);
                    //Utils.debugLine("execution complete", true);
                    int returnCount = 0;
                    //Utils.debugLine("target_list lines: ", true);
                    return strArr;
                }
                entryNames[entryCount] = entryName;
                entryCount++;
                entry = zis.getNextEntry();
            }
            zis.close();
            is.close();
        } catch (FileNotFoundException e) {
            Utils.debugLine(e.getMessage(), true);
            progressBar.setProgress(25);
            return null;
        } catch (IOException e) {
            Utils.debugLine(e.getMessage(), true);
            progressBar.setSecondaryProgress(50);
            return null;
        } catch (Exception e) {
            Utils.debugLine("get_target_list general exception: " + e.getMessage(), true);
            progressBar.setSecondaryProgress(75);
            return null;
        }
        Utils.debugLine("target_list not found", true);
        progressBar.setSecondaryProgress(0);
        return entryNames;
    }

    public void targetProgram() throws Exception {
        Utils.debugLine("start TProgram", true);
        //reads target list and operates on it
        String n_target_highaboveVT_swc = targetListInstruct[1].trim();
        String n_target_highaboveVt_ota = targetListInstruct[2].trim();
        String n_target_aboveVt_swc = targetListInstruct[3].trim();
        String n_target_aboveVt_ota = targetListInstruct[4].trim();
        String n_target_aboveVt_otaref = targetListInstruct[5].trim();
        String n_target_aboveVt_mite = targetListInstruct[6].trim();
        String n_target_aboveVt_dirswc = targetListInstruct[7].trim();
        String n_target_subVt_swc = targetListInstruct[8].trim();
        String n_target_subVt_ota = targetListInstruct[9].trim();
        String n_target_subVt_otaref = targetListInstruct[10].trim();
        String n_target_subVt_mite = targetListInstruct[11].trim();
        String n_target_subVt_dirswc = targetListInstruct[12].trim();
        String n_target_lowsubVt_swc = targetListInstruct[13].trim();
        String n_target_lowsubVt_ota = targetListInstruct[14].trim();
        String n_target_lowsubVt_otaref = targetListInstruct[15].trim();
        String n_target_lowsubVt_mite = targetListInstruct[16].trim();
        String n_target_lowsubVt_dirswc = targetListInstruct[17].trim();
        Utils.debugLine("assigned targetList Values", true);

        if (!n_target_highaboveVT_swc.equals("0.000000000000000")) {
            Utils.debugLine("highaboveVt_swc", true);
            writeAscii(zipped_files, 0x7000, "target_info_highaboveVt_swc");
            writeAscii(zipped_files, 0x6800, "pulse_width_table_highaboveVt_swc");
            compileAndProgram(zipped_files,"recover_inject_highaboveVt_SWC.elf", 20 * 1000);

            writeAscii(zipped_files, 0x7000, "target_info_highaboveVt_swc");
            compileAndProgram(zipped_files, "measured_coarse_program_highaboveVt_SWC.elf", 20 * 1000);

            writeAscii(zipped_files, 0x7000, "target_info_highaboveVt_swc");
            writeAscii(zipped_files, 0x6800, "Vd_table_30mV");
            compileAndProgram(zipped_files, "fine_program_highaboveVt_m_ave_04_SWC.elf", 20 * 1000);
        }
        //progressBar3.setProgress(25);

        if (!n_target_aboveVt_swc.equals("0.000000000000000")) {
            Utils.debugLine("aboveVt_swc\naboveVt_swc: " + n_target_aboveVt_swc, true);
            Utils.debugLine("zeros:       " + "0.000000000000000",true);
            Utils.debugLine("aboveVt_swc_length: " + n_target_aboveVt_swc.length(),true);
            Utils.debugLine("zeros length:       " + new String("0.000000000000000").length(),true);
            Utils.debugLine("char comparison", true);
            char[] aboveVt_swcArr = n_target_aboveVt_swc.toCharArray();
            char[] zerosArr = "0.000000000000000".toCharArray();
            if(n_target_aboveVt_swc.length() == new String("0.000000000000000").length()){
                for(int i = 0; i < n_target_aboveVt_swc.length(); i++){
                    Utils.debugLine("" + (aboveVt_swcArr[i] == zerosArr[i]),true);
                }
            }
            writeAscii(zipped_files, 0x7000, "target_info_aboveVt_swc");
            writeAscii(zipped_files, 0x6800, "pulse_width_table_swc");
            compileAndProgram(zipped_files, "recover_inject_aboveVt_SWC.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_swc")) return false;
            compileAndProgram(zipped_files, "first_coarse_program_aboveVt_SWC.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_swc")) return false;
            compileAndProgram(zipped_files, "measured_coarse_program_aboveVt_SWC.elf", 20 * 1000);


            //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_swc")) return false;
            writeAscii(zipped_files, 0x6800, "Vd_table_30mV");
            compileAndProgram(zipped_files, "fine_program_aboveVt_m_ave_04_SWC.elf", 20 * 1000);
        }

        //progressBar3.setProgress(10);

        if (!n_target_subVt_swc.equals("0.000000000000000")) {
            Utils.debugLine("subVt_swc", true);
            writeAscii(zipped_files, 0x7000, "target_info_subVt_swc");
            writeAscii(zipped_files, 0x6800, "pulse_width_table_swc");
            compileAndProgram(zipped_files,"recover_inject_subVt_SWC.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_swc")) return false;
            compileAndProgram(zipped_files, "first_coarse_program_subVt_SWC.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_swc")) return false;
            compileAndProgram(zipped_files, "measured_coarse_program_subVt_SWC.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_swc")) return false;
            writeAscii(zipped_files, 0x6800, "Vd_table_30mV");
            compileAndProgram(zipped_files, "fine_program_subVt_m_ave_04_SWC.elf", 20 * 1000);
        }
        //progressBar3.setProgress(15);

        if (!n_target_lowsubVt_swc.equals("0.000000000000000")) {
            Utils.debugLine("lowsubVt_swc", true);
            writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_swc");
            writeAscii(zipped_files, 0x6800, "pulse_width_table_lowsubVt_swc");
            compileAndProgram(zipped_files,"recover_inject_lowsubVt_SWC.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_swc")) return false;
            compileAndProgram(zipped_files, "first_coarse_program_lowsubVt_SWC.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_swc")) return false;
            compileAndProgram(zipped_files, "measured_coarse_program_lowsubVt_SWC.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_swc")) return false;
            writeAscii(zipped_files, 0x6800, "Vd_table_30mV");
            compileAndProgram(zipped_files, "fine_program_lowsubVt_m_ave_04_SWC.elf", 20 * 1000);
        }

        if (!n_target_highaboveVt_ota.equals("0.000000000000000")) {
            Utils.debugLine("highaboveVt_ota", true);
            writeAscii(zipped_files, 0x7000, "target_info_highaboveVt_ota");
            writeAscii(zipped_files, 0x6800, "pulse_width_table_highaboveVt_ota");
            compileAndProgram(zipped_files, "recover_inject_highaboveVt_CAB_ota.elf", 20 * 1000);

            writeAscii(zipped_files, 0x7000, "target_info_highaboveVt_ota");
            compileAndProgram(zipped_files, "first_coarse_program_highaboveVt_CAB_ota.elf", 20 * 1000);

            writeAscii(zipped_files, 0x7000, "target_info_highaboveVt_ota");
            compileAndProgram(zipped_files, "measured_coarse_program_highaboveVt_CAB_ota.elf", 20 * 1000);

            writeAscii(zipped_files, 0x7000, "target_info_highaboveVt_ota");
            writeAscii(zipped_files, 0x6800, "Vd_table_30mV");
            compileAndProgram(zipped_files, "fine_program_highaboveVt_m_ave_04_CAB_ota.elf", 20 * 1000);
        }

        //progressBar3.setProgress(20);
        if (!n_target_aboveVt_ota.equals("0.000000000000000")) {
            Utils.debugLine("aboveVt_ota", true);
            writeAscii(zipped_files, 0x7000, "target_info_aboveVt_ota" );
            writeAscii(zipped_files, 0x6800, "pulse_width_table_ota");
            compileAndProgram(zipped_files, "recover_inject_aboveVt_CAB_ota.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_ota")) return false;
            compileAndProgram(zipped_files, "first_coarse_program_aboveVt_CAB_ota.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_ota")) return false;
            compileAndProgram(zipped_files, "measured_coarse_program_aboveVt_CAB_ota.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_ota")) return false;
            writeAscii(zipped_files, 0x6800, "Vd_table_30mV");
            compileAndProgram(zipped_files,"fine_program_aboveVt_m_ave_04_CAB_ota.elf", 20 * 1000);
        }
        //progressBar3.setProgress(30);

        if (!n_target_subVt_ota.equals("0.000000000000000")) {
            Utils.debugLine("subVt_ota", true);
            writeAscii(zipped_files, 0x7000, "target_info_subVt_ota" );
            writeAscii(zipped_files, 0x6800, "pulse_width_table_ota");
            compileAndProgram(zipped_files, "recover_inject_subVt_CAB_ota.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_ota")) return false;
            compileAndProgram(zipped_files, "first_coarse_program_subVt_CAB_ota.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_ota")) return false;
            compileAndProgram(zipped_files, "measured_coarse_program_subVt_CAB_ota.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_ota")) return false;
            writeAscii(zipped_files, 0x6800, "Vd_table_30mV");
            compileAndProgram(zipped_files,"fine_program_subVt_m_ave_04_CAB_ota.elf", 20 * 1000);
        }
        //progressBar3.setProgress(35);

        if (!n_target_lowsubVt_ota.equals("0.000000000000000")) {
            Utils.debugLine("lowsubVt_ota", true);
            //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_ota" )) return false;
            writeAscii(zipped_files, 0x6800, "pulse_width_table_lowsubVt_ota");
            compileAndProgram(zipped_files, "recover_inject_lowsubVt_CAB_ota.elf",20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_ota")) return false;
            compileAndProgram(zipped_files, "first_coarse_program_lowsubVt_CAB_ota.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_ota")) return false;
            compileAndProgram(zipped_files, "measured_coarse_program_lowsubVt_CAB_ota.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_ota")) return false;
            writeAscii(zipped_files, 0x6800, "Vd_table_30mV");
            compileAndProgram(zipped_files,"fine_program_lowsubVt_m_ave_04_CAB_ota.elf", 20 * 1000);
        }
        //progressBar3.setProgress(40);

        if (!n_target_aboveVt_otaref.equals("0.000000000000000")) {
            Utils.debugLine("aboveVt_otaref", true);
            writeAscii(zipped_files, 0x7000, "target_info_aboveVt_otaref" );
            writeAscii(zipped_files, 0x6800, "pulse_width_table_otaref");
            compileAndProgram(zipped_files, "recover_inject_aboveVt_CAB_ota_ref.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_otaref")) return false;
            compileAndProgram(zipped_files, "first_coarse_program_aboveVt_CAB_ota_ref.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_otaref")) return false;
            compileAndProgram(zipped_files, "measured_coarse_program_aboveVt_CAB_ota_ref.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_otaref")) return false;
            writeAscii(zipped_files, 0x6800, "Vd_table_30mV");
            compileAndProgram(zipped_files,"fine_program_aboveVt_m_ave_04_CAB_ota_ref.elf", 20 * 1000);
        }
        //progressBar3.setProgress(45);

        if (!n_target_subVt_otaref.equals("0.000000000000000")) {
            Utils.debugLine("subVt_otaref", true);
            writeAscii(zipped_files, 0x7000, "target_info_subVt_otaref" );
            writeAscii(zipped_files, 0x6800, "pulse_width_table_otaref");
            compileAndProgram(zipped_files, "recover_inject_subVt_CAB_ota_ref.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_otaref")) return false;
            compileAndProgram(zipped_files, "first_coarse_program_subVt_CAB_ota_ref.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_otaref")) return false;
            compileAndProgram(zipped_files, "measured_coarse_program_subVt_CAB_ota_ref.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_otaref")) return false;
            writeAscii(zipped_files, 0x6800, "Vd_table_30mV");
            compileAndProgram(zipped_files,"fine_program_subVt_m_ave_04_CAB_ota_ref.elf", 20 * 1000);
        }
        //progressBar3.setProgress(50);

        if (!n_target_lowsubVt_otaref.equals("0.000000000000000")) {
            Utils.debugLine("lowsubVt_otaref", true);
            writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_otaref");
            writeAscii(zipped_files, 0x6800, "pulse_width_table_lowsubVt_otaref");
            compileAndProgram(zipped_files, "recover_inject_lowsubVt_CAB_ota_ref.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_otaref")) return false;
            compileAndProgram(zipped_files, "first_coarse_program_lowsubVt_CAB_ota_ref.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_otaref")) return false;
            compileAndProgram(zipped_files, "measured_coarse_program_lowsubVt_CAB_ota_ref.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_otaref")) return false;
            writeAscii(zipped_files, 0x6800, "Vd_table_30mV");
            compileAndProgram(zipped_files, "fine_program_lowsubVt_m_ave_04_CAB_ota_ref.elf", 20 * 1000);
        }
        //progressBar3.setProgress(55);

        if (!n_target_aboveVt_mite.equals("0.000000000000000")) {
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
        }
        //progressBar3.setProgress(60);

        if (!n_target_subVt_mite.equals("0.000000000000000")) {
            Utils.debugLine("n_target_subVt_mite", true);
            writeAscii(zipped_files, 0x7000, "target_info_subVt_mite");
            writeAscii(zipped_files, 0x6800, "pulseh_table_mite");
            compileAndProgram(zipped_files, "recover_inject_subVt_CAB_mite.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_mite")) return false;
            compileAndProgram(zipped_files, "first_coarse_program_subVt_CAB_mite.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_mite")) return false;
            compileAndProgram(zipped_files, "measured_coarse_program_subVt_CAB_mite.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_mite")) return false;
            writeAscii(zipped_files, 0x6800, "Vd_table_30mV");
            compileAndProgram(zipped_files, "fine_program_subVt_m_ave_04_CAB_mite.elf", 20 * 1000);
        }
        //progressBar3.setProgress(65);

        if (!n_target_lowsubVt_mite.equals("0.000000000000000")) {
            Utils.debugLine("n_target_lowsubVt_mite", true);
            writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_mite");
            writeAscii(zipped_files, 0x6800, "pulse_width_table_lowsubVt_mite");
            compileAndProgram(zipped_files, "recover_inject_lowsubVt_CAB_mite.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_mite")) return false;
            compileAndProgram(zipped_files, "first_coarse_program_lowsubVt_CAB_mite.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_mite")) return false;
            compileAndProgram(zipped_files, "measured_coarse_program_lowsubVt_CAB_mite.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_mite")) return false;
            writeAscii(zipped_files, 0x6800, "Vd_table_30mV");
            compileAndProgram(zipped_files, "fine_program_lowsubVt_m_ave_04_CAB_mite.elf", 20 * 1000);
        }
        //progressBar3.setProgress(70);

        if (!n_target_aboveVt_dirswc.equals("0.000000000000000")) {
            Utils.debugLine("n_target_aboveVt_dirswc", true);
            writeAscii(zipped_files, 0x7000, "target_info_aboveVt_dirswc");
            writeAscii(zipped_files, 0x6800, "pulse_width_table_dirswc");
            compileAndProgram(zipped_files, "recover_inject_aboveVt_DIRSWC.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_dirswc")) return false;
            compileAndProgram(zipped_files, "first_coarse_program_aboveVt_DIRSWC.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_dirswc")) return false;
            compileAndProgram(zipped_files, "measured_coarse_program_aboveVt_DIRSWC.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_dirswc")) return false;
            writeAscii(zipped_files, 0x6800, "Vd_table_30mV");
            compileAndProgram(zipped_files, "fine_program_aboveVt_m_ave_04_DIRSWC.elf", 20 * 1000);
        }
        //progressBar3.setProgress(75);

        if (!n_target_subVt_dirswc.equals("0.000000000000000")) {
            Utils.debugLine("n_target_subVt_dirswc", true);
            writeAscii(zipped_files, 0x7000, "target_info_subVt_dirswc");
            writeAscii(zipped_files, 0x6800, "pulse_width_table_dirswc");
            compileAndProgram(zipped_files, "recover_inject_subVt_DIRSWC.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_dirswc")) return false;
            compileAndProgram(zipped_files, "first_coarse_program_subVt_DIRSWC.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_dirswc")) return false;
            compileAndProgram(zipped_files, "measured_coarse_program_subVt_DIRSWC.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_dirswc")) return false;
            writeAscii(zipped_files, 0x6800, "Vd_table_30mV");
            compileAndProgram(zipped_files, "fine_program_subVt_m_ave_04_DIRSWC.elf", 20 * 1000);
        }
        //progressBar3.setProgress(80);

        if (!n_target_lowsubVt_dirswc.equals("0.000000000000000")) {
            Utils.debugLine("n_target_lowsubVt_dirswc", true);
            writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_dirswc");
            writeAscii(zipped_files, 0x6800, "pulse_width_table_lowsubVt_dirswc");
            compileAndProgram(zipped_files, "recover_inject_lowsubVt_DIRSWC.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_dirswc")) return false;
            compileAndProgram(zipped_files, "first_coarse_program_lowsubVt_DIRSWC.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_dirswc")) return false;
            compileAndProgram(zipped_files, "measured_coarse_program_lowsubVt_DIRSWC.elf", 20 * 1000);

            //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_dirswc")) return false;
            writeAscii(zipped_files, 0x6800, "Vd_table_30mV");
            compileAndProgram(zipped_files, "fine_program_lowsubVt_m_ave_04_DIRSWC.elf", 20 * 1000);
        }
        //progressBar3.setProgress(100);
    }

    public void compileAndProgram(Map<String, byte[]> loc, String name, int wait_ms) throws Exception {
        byte[] data;

        if (!loc.containsKey(name.trim())) {
            Utils.debugLine("Zipped file does not contain file name \"" + name + "\"",true);
            throw new Exception("Zip file does not contain: " + name);
        }

        try {
            data = Utils.compileElf(loc.get(name));
        } catch (Exception e) {
            throw new Exception("Compile Elf failed");
        }

        try {
            driver.programData(data);
        } catch (Exception e) {
            throw new Exception("Driver programming failed");
        }

        driver.sleep(wait_ms);
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