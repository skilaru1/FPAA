package hasler.fpaaapp.views;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.RequiresPermission;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import hasler.fpaaapp.R;
import hasler.fpaaapp.utils.DriverFragment;
import hasler.fpaaapp.utils.ReadTimeOutException;
import hasler.fpaaapp.utils.Utils;
import hasler.fpaaapp.utils.targetProgram;

//import hasler.fpaaapp.utils.Configuration;

/*----------------------------------------------------------------------------------*/
/*----------------------------------MAIN_APP_VIEW-----------------------------------*/
/*----------------------------------------------------------------------------------*/
public class OscopeView extends DriverFragment {
    private final String TAG = "OscopeView";

    protected Handler mHandler = new Handler();
    
    //indicator of progress through current button
    protected ProgressBar progressBar;
    
    //graph for recorded data
    protected GraphView graph;
    
    //Alert to select wavFile in PlayButton
    protected AlertDialog.Builder builder;
    
    //name of selectedWavFile
    protected String selectedKey;
    
    //url of zip file with programming data
    protected String url = "Url not found!!!";
    
    //protected String url = Configuration.DAC_ADC_WAV_LOCATION;
    //protected String selection;
    
    //location of unzipped wavfile
    protected String selectionPath;

    //initialize View
    public static OscopeView newInstance() {
        return new OscopeView();
    }

    public OscopeView() { /* Required empty public constructor */ }

    //MediaPlayer to play wavFiles
    private MediaPlayer mediaPlayer;

/*----------------------------------------------------------------------------------*/
/*-------------------------------------METHODS--------------------------------------*/
/*----------------------------------------------------------------------------------*/

    //removes the wav file from the zip file and stores it in a temporary file
    private void makeTempWaveFile(Map<String,byte[]> zipped_files, String selectedKey){
        byte[] wavFile = zipped_files.get(selectedKey);
        //selectionPath = new FileDescriptor();
        try{
            //initializes  file
            File unzippedWav = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/selection.wav");

            //writes wavFile byte array into new location
            FileOutputStream fos = new FileOutputStream(unzippedWav);
            fos.write(wavFile);
            fos.close();

            //location of wavFile
            selectionPath = unzippedWav.getAbsolutePath();
        }
        catch(IOException e){
            Utils.debugLine("makeTempWaveFile Exception: " + e.getMessage(),true);
        }

    }

    //wait for a bit to see if we can connect to the FPAA
    public boolean checkConnection(){
        try{
            if (!driver.connect()) {
                int count = 0;
                while(count < 10){
                    if(driver.connect()){
                        count = 10;
                    }
                    else {
                        count++;
                        Utils.debugLine("checkConnection count: " + count, true);
                    }
                }
                Utils.debugLine("not connected to driver", true);
                return false;
            }
        }
        catch(Exception e){
            Utils.debugLine("checkConnection (driver.connect) error: " + e.getMessage(),true);
            return false;
        }
        //Utils.debugLine("connected to driver",true);
        return true;
    }

    //displays data on graph
    //name[] is an array of names of the series i.e. {voltage, current, ...}
    //long[0][i] = x values, long[1][i] = y values
    public static void plot(GraphView graph, String name[], long[][]... vals) {
        //array of series
        List<LineGraphSeries<DataPoint>> seriesList = new ArrayList<>();
        int loopCounter = 0;
        for (long[][] val : vals) {
            DataPoint[] dataPoints = new DataPoint[val[0].length];
            for (int i = 0; i < val[0].length; i++) {
                dataPoints[i] = new DataPoint(val[0][i], val[1][i]);
            }
            //creates a new series to store dataPoints
            LineGraphSeries<DataPoint> graphSeries = new LineGraphSeries<>(dataPoints);
            //names this new series in legend
            graphSeries.setTitle(name[loopCounter++]);
            //adds this graphSeries to the list
            seriesList.add(graphSeries);

        }
        plotSeries(graph, seriesList);
    }

    //same as above, but double instead of long
    public static void plot(GraphView graph, String name[], double[][]... vals) {
        List<LineGraphSeries<DataPoint>> series = new ArrayList<>();
        int loopcounter = 0;
        for (double[][] val : vals) {
            DataPoint[] dataPoints = new DataPoint[val[0].length];
            for (int i = 0; i < val[0].length; i++) {
                dataPoints[i] = new DataPoint(val[0][i], val[1][i]);
            }

            LineGraphSeries<DataPoint> graphSeries = new LineGraphSeries<>(dataPoints);
            graphSeries.setTitle(name[loopcounter++]);
            series.add(graphSeries);
        }
        plotSeries(graph, series);
    }

    //clears graph, then plots a single series, (xvals,yvals), with title name
    public static void plot(GraphView graph, double[] xvals, double[] yvals, String name) {
        graph.removeAllSeries();
        DataPoint[] dataPoints = new DataPoint[xvals.length];
        for (int i = 0; i < xvals.length; i++) {
            dataPoints[i] = new DataPoint(xvals[i], yvals[i]);
        }
        LineGraphSeries<DataPoint> graphSeries = new LineGraphSeries<>(dataPoints);
        graphSeries.setTitle(name);
        graph.addSeries(graphSeries);
    }

    //order of the colors of series i.e. first series is blue, second is red......
    protected static final int[] COLORS = {Color.BLUE, Color.RED, Color.GREEN, Color.CYAN, Color.BLACK};

    //clears graph, then plots each graphSeries in seriesList
    public static void plotSeries(GraphView graph, List<LineGraphSeries<DataPoint>> seriesList) {
        graph.removeAllSeries();
        for (int i = 0; i < seriesList.size(); i++) {
            LineGraphSeries<DataPoint> series = seriesList.get(i);
            series.setColor(COLORS[i % COLORS.length]);
            graph.addSeries(series);
        }
    }

    //formats the graph according to a program design button press
    public static void formatGraphProgramDesign(GraphView graph, TextView graphTitle, String title){
        graphTitle.setText(title);

        graph.setTitle("Program Design");

        GridLabelRenderer glr = graph.getGridLabelRenderer();
        glr.setHorizontalAxisTitle("time (seconds)");

        LegendRenderer lr = graph.getLegendRenderer();
        lr.setVisible(true);
    }

    //formats the graph according to a getData button press
    public static void formatGraphGetData(GraphView graph){
        graph.setTitle("Get Data");

        GridLabelRenderer glr = graph.getGridLabelRenderer();
        glr.setHorizontalAxisTitle("time (seconds)");
        glr.setVerticalAxisTitle("Voltage (unknown unit)");

        LegendRenderer lr = graph.getLegendRenderer();
        lr.setVisible(true);
    }


    /*==========================================================================================
     onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    ----------------------------------------------------------------------------------------
    Description: picks out specific elf file from the zip file and writes the instruction enclosed
    to a particular address of the FPAA. Waits a certain amount of time for the FPAA to program
    properly
    ----------------------------------------------------------------------------------------
    Arguments:

    inflater: an object that helps turn your XML file into a View object so that the programmer can
    access visual elements from XML file

    container: holds the View, base class for Layout

    savedInstanceState: used to create a Fragment
    ----------------------------------------------------------------------------------------
    Result: returns a View. This basically means we update the tablet screen with new outputs
    ========================================================================================
    */

    /*----------------------------------------------------------------------------------*/
    /*---------------------------------------VIEW---------------------------------------*/
    /*----------------------------------------------------------------------------------*/
    //initialize the View (what people see)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) return null;
        super.onCreate(savedInstanceState);

        // Inflate the view XML file (translates the elements into what we see on the screen)
        final View view = inflater.inflate(R.layout.fragment_oview, container, false);

        //visual elements from XML file: buttons are pressable, textview displays text(sometimes editable),
        // graph is a graph, alertdialog builder is used to create a window with various selectable options
        progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        final Button programDesignButton = (Button) view.findViewById(R.id.program_design_button);
        final Button getDataButton = (Button) view.findViewById(R.id.get_data_button);
        final Button playButton = (Button) view.findViewById(R.id.play_button);
        final Button pauseButton = (Button) view.findViewById(R.id.pause_button);
        final TextView graphTitle = (TextView) view.findViewById(R.id.textView2);
        //title wiped, will be set during each button press
        graphTitle.setText("Empty Title");
        graph = (GraphView) view.findViewById(R.id.graph);
        builder = new AlertDialog.Builder(getContext());


        //editable url input, used to grab zip file from github
        final EditText zipAddress = (EditText) view.findViewById(R.id.zip_file_location);
        final Button zipFileLocationButton = (Button) view.findViewById(R.id.zip_file_location_button);
        zipFileLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String addressString = zipAddress.getText().toString();
                Toast.makeText(parentContext, "Working!!! " + addressString, Toast.LENGTH_SHORT).show();
                url = addressString;
            }
        });

        /*----------------------------------------------------------------------------------*/
        /*------------------------------PROGRAM_DESIGN_TO_FPAA------------------------------*/
        /*----------------------------------------------------------------------------------*/
        //programs the zipFile specified in the zipAddress bar to the FPAA
        programDesignButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ThreadRunnable() {
                    @Override
                    protected void onPreExecute() {
                        //disable buttons, set progress to 0
                        getDataButton.setEnabled(false);
                        programDesignButton.setEnabled(false);
                        playButton.setEnabled(false);
                        pauseButton.setEnabled(false);
                        progressBar.setProgress(0);
                        progressBar.setSecondaryProgress(0);
                    }

                    @TargetApi(Build.VERSION_CODES.KITKAT)
                    @Override
                    public String doInBackground(Void... params) {
                        try {
                            //check for url input
                            if (url.equals("Url not found!!!")) {
                                //SAI
                                makeToastMessage(url);
                                return url;
                            }

                            //download the zip file into path(downloads) and store it in file
                            Utils.debugLine("beginning download (start)", false);
                            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                            String title = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("."));
                            File dFile = new File(path, title.concat(".zip"));
                            //if file of the same name already exists, delete and redownload
                            if (dFile.exists()) {
                                dFile.delete();
                            }
                            if (!download(dFile)) {
                                Utils.debugLine("file not downloaded", true);
                                return "file not downloaded";
                            }
                            //map representing the zip file, keys are file names, byte[] are contents
                            Map<String, byte[]> zipped_files = Utils.getZipContents(dFile.getAbsolutePath());

                            updateProgressBar(10);
                            //check for driver connection
                            if (!checkConnection()) {
                                Utils.debugLine("not connected to driver", true);
                                return "not connected to driver";
                            }
                            Utils.debugLine("checked Connection", true);
                            //check for successful download and map
                            if (zipped_files == null) {
                                Utils.debugLine("null zipped_files", true);
                                return "null zipped_files";
                            }
                            Utils.debugLine("zip file contents", true);
                            for (String key : zipped_files.keySet()) {
                                Utils.debugLine(key, true);
                            }

                            Utils.debugLine("\nstarted compile and program", true);
                            //begin programming FPAA
                            if (!compileAndProgram(zipped_files, "tunnel_revtun_SWC_CAB.elf", 10 * 1000))
                                return "tunnel_revtun_SWC_CAB.elf not properly programmed";
                            updateProgressBar(20);
                            Utils.debugLine("first compile and program successful", true);
                            if (!writeAscii(zipped_files, 0x7000, "switch_info")) return "first writeAscii failed";
                            Utils.debugLine("first writeascii successful", true);
                            if (!compileAndProgram(zipped_files, "switch_program.elf", 70 * 1000))
                                return "switch_program.elf not properly programmed";
                            updateProgressBar(30);
                            Utils.debugLine("started target program", true);
                            //initializer targetProgram class, which gets target list from the zip file
                            targetProgram tp = new targetProgram(driver, progressBar);

                            updateProgressBar(50);

                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {
                                makeToastMessage("InterruptedException");
                                Utils.debugLine(e.getMessage(), true);
                                return "InterruptedException";
                            }
                            //get the lines of the text file targetList
                            tp.TARGETLIST(dFile);

                            for (String instruct : tp.targetListInstruct) {
                                Utils.debugLine(instruct, true);
                            }
                            //program the FPAA based on the lines of targetList
                            if (!TProgram(tp.targetListInstruct, tp.zipped_files)) return "Failed to program the FPAA based on the lines of targetList";
//                        for(int i = 0; i < 13; i++){
//                            makeToastMessage(tp.entryNames[i]);
//                            try{
//                                Thread.sleep(3000);
//                            }
//                            catch(InterruptedException e){
//                                makeToastMessage("InterruptedException");
//                            }
//                        }
                            updateProgressBar(70);

                            //finish programming the FPAA
                            if (!writeAscii(zipped_files, 0x4300, "input_vector")) return "input_vector writeAscii failed";
                            if (!writeAscii(zipped_files, 0x4200, "output_info")) return "output_info writeAscii failed";
                            if (!compileAndProgram(zipped_files, "voltage_meas.elf", 70 * 1000))
                                return "voltage_meas.elf not programmed";
                            updateProgressBar(100);

                            // plot the switches
                            long[] d = Utils.toInts(2, driver.readMem(0x5000, 100));

                            List<long[]> sp = Utils.ffsplit(d);

                            //plot the switches - for debugging use
//                        if (sp.size() < 5) {
//                            updateGraph(new String[] {"0x5000"}, new long[][]{Utils.linspace(1, d.length, d.length), d});
//                        } else {
//                            long[] a = sp.get(sp.size() - 4), b = sp.get(sp.size() - 3);
//                            updateGraph(new String[] {"0x500 ffsplit.get(size()-4)", "0x500 ffsplit.get(size()-3)"}, new long[][]{Utils.linspace(1, a.length, a.length), a},
//                                    new long[][]{Utils.linspace(1, b.length, b.length), b});
//                        }
//                        //format the graph for program design
//                        formatGraphPD(graph, graphTitle, title);
                            return "Success";
                        } catch (ReadTimeOutException e) {
                            return "ReadTimeOutException";
                        }
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        makeToastMessage(result);

                        progressBar.setProgress(100);
                        getDataButton.setEnabled(true);
                        programDesignButton.setEnabled(true);
                        playButton.setEnabled(true);
                        pauseButton.setEnabled(true);
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
        });

        /*----------------------------------------------------------------------------------*/
        /*--------------------------------GET_DATA_FROM_FPAA--------------------------------*/
        /*----------------------------------------------------------------------------------*/

        getDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ThreadRunnable() {
                    @Override
                    protected void onPreExecute() {
                        Utils.debugLine("getData (PreExecute)",false);
                        getDataButton.setEnabled(false);
                        programDesignButton.setEnabled(false);
                        playButton.setEnabled(false);
                        pauseButton.setEnabled(false);
                        progressBar.setProgress(0);
                    }

                    @TargetApi(Build.VERSION_CODES.KITKAT)
                    @Override
                    public String doInBackground(Void... params) {
                        try {
                            Utils.debugLine("\n \n \n \n \n \n Get Data (do in background)\n", false);
                            //Utils.debugLine("started getData",true);
                            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                            final String title = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("."));
                            File file = new File(path, title.concat(".zip"));
                            makeToastMessage(title.concat(".zip"));
                            if (!download(file)) return "File not downloaded";

                            if (!checkConnection()) {
                                Utils.debugLine("driver not connected", true);
                                return "driver not connected";
                            }
                            //Utils.debugLine("driver connected",true);
                            Map<String, byte[]> zipped_files = Utils.getZipContents(file.getAbsolutePath());
                            updateProgressBar(10);

                            if (!writeAscii(zipped_files, 0x4300, "input_vector")) return "input_vector writeAscii failed";
                            if (!writeAscii(zipped_files, 0x4200, "output_info")) return "output_info writeAscii failed";
                            updateProgressBar(30);

                            if (!driver.runWithoutData()) return "driver not run without data";
                            updateProgressBar(50);

                            driver.sleep(40 * 1000);
                            updateProgressBar(100);
                            if (zipped_files.containsKey("voltage_meas.elf")) {
                                //Utils.debugLine("voltage_meas.elf exists", true);
                            } else {
                                Utils.debugLine("voltage_meas.elf DNE", true);
                            }
                            if (!compileAndProgram(zipped_files, "voltage_meas.elf", 20 * 1000)) {
                                Utils.debugLine("couldn't compile and program voltage_meas.elf", true);
                                return "couldn't compile and program voltage_meas.elf";
                            }
                            //Utils.debugLine("compiled and programmed voltage_meas.elf",true);
                            long[] d = Utils.toInts(2, driver.readMem(0x6000, 1200));
                            for (int i = 0; i < 1200; i++) {
                                //Utils.debugLine("i: " + i + " d[i]: " + d[i], true);
                            }
                            List<long[]> sp = Utils.ffsplit(d);

                            // plot the input vector beside the generated vector
                            byte[] data = Utils.parseHexAscii(zipped_files.get("input_vector"));
                            long[] l = Utils.toInts(2, Utils.swapBytes(data));
                            l = Utils.reverse(Arrays.copyOfRange(l, 3, l.length - 1));

                            // scaling
                            double[] n = new double[l.length];
                            for (int i = 0; i < l.length; i++) {
                                n[i] = l[i] * 2.5 / 30720.0;
                            }

                            if (sp.size() < 2) {
                                updateGraph(new String[]{"input"}, new long[][]{Utils.linspace(1, d.length, d.length), d});
                            } else {
                                long[] a = sp.get(sp.size() - 1);
                                //Utils.debugLine("sp.size(): " + sp.size(), true);
                                double[] b = new double[a.length];
                                double[] c = new double[a.length];
                                //Utils.debugLine("a.length: " + a.length, true);
                                for (int i = 0; i < a.length; i++) {
                                    b[i] = (a[i] - 5000) * 2.5 / 5000.0;
                                    c[i] = a[i] * 1.0;
                                    //Utils.debugLine("i: " + i + " a[i]: " + a[i] + " b[i]: " + b[i] + " n[i]: " + n[i], true);
                                }
                                updateGraph(new String[]{"output", "input"}, new double[][]{Utils.linspace(1.0, b.length, b.length), b},
                                        new double[][]{Utils.linspace(1.0, n.length, l.length), n});
                            }
                            formatGraphGD(graph);
                            return "Success";
                        } catch (ReadTimeOutException e) {
                            return "ReadTimeOutException";
                        }
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        makeToastMessage(result);

                        progressBar.setProgress(100);
                        getDataButton.setEnabled(true);
                        programDesignButton.setEnabled(true);
                        playButton.setEnabled(true);
                        pauseButton.setEnabled(true);
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });
        /*----------------------------------------------------------------------------------*/
        /*-------------------------PLAY/FILTER_WAV_FILE_USING_FPAA--------------------------*/
        /*---------------------------(MUST_PROGRAM_DATA_FIRST)------------------------------*/
        /*----------------------------------------------------------------------------------*/
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ThreadRunnable() {
                    @Override
                    protected void onPreExecute() {
                        Utils.debugLine("PlayButton preExecution",false);
                        //Looper.prepare();
                        getDataButton.setEnabled(false);
                        programDesignButton.setEnabled(false);
                        playButton.setEnabled(false);
                        pauseButton.setEnabled(false);
                        progressBar.setProgress(0);
                        if(selectionPath != null){
                            File file = new File(selectionPath);
                            file.delete();
                        }
                        selectedKey = null;
                    }

                    @TargetApi(Build.VERSION_CODES.KITKAT)
                    @Override
                    public String doInBackground(Void... params) {
                        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        if (url.equals("Url not found!!!")) {
                            return url;
                        }
                        String title = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("."));
                        File downloadedZip = new File(path, title.concat(".zip"));
                        makeToastMessage(title.concat(".zip"));
                        Utils.debugLine(title.concat(".zip"),true);
                        if(downloadedZip.exists()){downloadedZip.delete();}
                        if(downloadedZip.exists()){Utils.debugLine("file.delete not working", true);}
                        if (!download(downloadedZip)) {
                            Utils.debugLine("download = false", true);
                            makeToastMessage("download = false");
                            return "Zip file not downloaded";
                        }
                        downloadedZip.deleteOnExit();

                        if (!checkConnection()) {
                            Utils.debugLine("driver not connected", true);
                            return "driver not connected";
                        }
                        //Utils.debugLine("driver connected", true);
                        Map<String, byte[]> zipped_files = Utils.getZipContents(downloadedZip.getAbsolutePath());
                        updateProgressBar(10);
                        Set<String> keys = zipped_files.keySet();
                        Set<String> wavKeys = new HashSet<String>();
                        //Utils.debugLine("Sets Created",true);
                        for (String key : keys) {
                            if (key.substring(key.length() - 4).equals(".wav")) {
                                wavKeys.add(key);
                            }
                        }
                        if(downloadedZip.exists()){
                            Utils.debugLine("downloadedZip not deleted",true);
                        }
                        else{
                            //Utils.debugLine("downloadedZip deleted",true);
                        }
                        //Utils.debugLine("For loop key: keys finished",true);
                        while (wavKeys.size() == 0) {
                            makeToastMessage("no wav files in zip file");
                            Utils.debugLine("no wav files in zip file", true);

                            //Repeating initial process
                            downloadedZip.delete();

                            makeToastMessage(title.concat(".zip"));
                            Utils.debugLine(title.concat(".zip"),true);
                            if(downloadedZip.exists()){downloadedZip.delete();}
                            if(downloadedZip.exists()){Utils.debugLine("file.delete not working", true);}
                            if (!download(downloadedZip)) {
                                Utils.debugLine("download = false", true);
                                return "Zip file not downloaded";
                            }
                            downloadedZip.deleteOnExit();

                            if (!checkConnection()) {
                                Utils.debugLine("driver not connected", true);
                                return "driver not connected";
                            }
                            //Utils.debugLine("driver connected", true);
                            zipped_files = Utils.getZipContents(downloadedZip.getAbsolutePath());
                            updateProgressBar(10);
                            keys = zipped_files.keySet();
                            wavKeys = new HashSet<String>();
                            //Utils.debugLine("Sets Created",true);
                            for (String key : keys) {
                                if (key.substring(key.length() - 4).equals(".wav")) {
                                    wavKeys.add(key);
                                }
                            }
                            if(downloadedZip.exists()){
                                Utils.debugLine("downloadedZip not deleted",true);
                            }
                            else{
                                //Utils.debugLine("downloadedZip deleted",true);
                            }
                            //Utils.debugLine("For loop key: keys finished",true);

                        }

                        final CharSequence wavSequence[] = new CharSequence[wavKeys.size()];
                        int wavKeyCount = 0;
                        for (String wavKey : wavKeys) {
                            wavSequence[wavKeyCount] = wavKey;
                            wavKeyCount++;
                        }
                        //Utils.debugLine("For loop wavKey : wavKeys finished", true);
                        if(builder == null){Utils.debugLine("null builder", true);}
                        builder.setTitle("Select a .wav file");
                        //Utils.debugLine("builder title set", true);
                        //Utils.debugLine("wavSequence Length: " + wavSequence.length, true);

                        builder.setItems(wavSequence, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int select) {
                                SetSelectedKey(wavSequence[select].toString());
                            }
                        });
                        //Utils.debugLine("before create",true);
                        //AlertDialog alert = builder.create();
                        //alert.show();
                        makeAlertDialog(builder);
                        while(selectedKey == null){
                            //Utils.debugLine("waiting",true);
                        }
                        //Utils.debugLine("alert.show finished",true);
                    if (!writeAscii(zipped_files, 0x4300, "input_vector")) {
                        Utils.debugLine("writeAscii(input_vector) = false",true);
                        return "writeAscii(input_vector) = false";
                    }
                    if (!writeAscii(zipped_files, 0x4200, "output_info")){
                        Utils.debugLine("writeAscii(output_info)",true);
                        return "writeAscii(output_info) = false";
                    }
                        updateProgressBar(30);

                    if (!driver.runWithoutData()){
                        Utils.debugLine("driver.runwithoutData() = false",true);
                        return "driver.runwithoutData() = false";
                    }
                        updateProgressBar(50);

                        //driver.sleep(40 * 1000);
                        updateProgressBar(100);
                        if (zipped_files.containsKey("voltage_meas.elf")) {
                            //Utils.debugLine("voltage_meas.elf exists", true);
                        } else {
                            Utils.debugLine("voltage_meas.elf DNE", true);
                        }
                    if (!compileAndProgram(zipped_files, "voltage_meas.elf", 20 * 1000)) {
                        Utils.debugLine("couldn't compile and program voltage_meas.elf", true);
                        return "couldn't compile and program voltage_meas.elf";
                    }


                        //@TargetApi(Build.VERSION_CODES.KITKAT);
                       /* mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        try {
                            mediaPlayer.setDataSource(url);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        */
                        makeTempWaveFile(zipped_files, selectedKey);
                        //Utils.debugLine("selectionPath created",true);
                        makeMediaFile();
                        return "Success";
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        Utils.debugLine("PostExecute Start", true);
                        makeToastMessage(result);

                        updateProgressBar(100);
                        programDesignButton.setEnabled(true);
                        getDataButton.setEnabled(true);
                        playButton.setEnabled(true);
                        pauseButton.setEnabled(true);

                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

        });

        /*----------------------------------------------------------------------------------*/
        /*---------------------------------PAUSE_WAV_FILE-----------------------------------*/
        /*----------------------------------------------------------------------------------*/
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Pausing Sound ", Toast.LENGTH_SHORT).show();
                mediaPlayer.pause();
            }
        });
        //mediaPlayer.release();
        if(mediaPlayer != null) {
            mediaPlayer.release();
        }
        return view;
    }

    /*----------------------------------------------------------------------------------*/
    /*-------------------------METHODS_CALLED_USING_MHANDLER----------------------------*/
    /*------------------(USUALLY_CALLS_METHODS_FROM_METHODS_SECTION_ABOVE)--------------*/
    /*----------------------------------------------------------------------------------*/
    protected abstract class ThreadRunnable extends AsyncTask<Void, Void, String> {

        public void updateGraph(final String name[], final long[][]... data) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    plot(graph, name, data);
                }
            });
        }

        public void updateGraph(final String[] name, final double[][]... data) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    plot(graph, name, data);
                }
            });
        }

        public void updateGraph(final String name, final double[] xdata, final double[] ydata) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    plot(graph, xdata, ydata, name);
                }
            });
        }

        public void formatGraphPD(final GraphView graph, final TextView graphTitle, final String title){
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    formatGraphProgramDesign(graph, graphTitle, title);
                }
            });
        }

        public void formatGraphGD(final GraphView graph){
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    formatGraphGetData(graph);
                }
            });
        }

        public void updateProgressBar(final int i) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    progressBar.setProgress(i);
                }
            });
        }
        public void makeToastMessage(final String text) {
            mHandler.post(new Runnable() {
                @Override
                public void run() { Toast.makeText(parentContext, text, Toast.LENGTH_SHORT).show(); }
            });
        }
        public void SetSelectedKey(String selection){
            selectedKey = selection;
        }
        public void makeAlertDialog(final AlertDialog.Builder builder){
            mHandler.post(new Runnable(){
                @Override
                public void run() {
                    AlertDialog alert = builder.create();
                    alert.show();
                }
            });
        }
        /*==========================================================================================
        makeMediaFile()
        ----------------------------------------------------------------------------------------
        Description: picks out specific elf file from the zip file and writes the instruction enclosed
        to a particular address of the FPAA. Waits a certain amount of time for the FPAA to program
        properly
        ----------------------------------------------------------------------------------------
        Arguments: none
        ----------------------------------------------------------------------------------------
        Result: nothing is returned
        ========================================================================================
        */
        public void makeMediaFile(){
            Utils.debugLine("makeMediaFile start",true);
            mHandler.post(new Runnable(){
                @Override
                public void run() {
                    if(mediaPlayer != null){
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                    mediaPlayer = MediaPlayer.create(getActivity(), R.raw.heartbeat);
                    try{
                        mediaPlayer.reset();
                        Utils.debugLine("MediaPlayer Reset",true);
                        mediaPlayer.setDataSource(selectionPath);
                        Utils.debugLine("MediaPlayer DataSource Set",true);
                        mediaPlayer.prepare();
                        Utils.debugLine("MediaPlayer Prepared",true);
                    }
                    catch(IOException e){
                        Utils.debugLine("mediaPlayerException: " + e.getMessage(),true);
                    }
                    makeToastMessage("Playing Sound");
                    //Utils.debugLine("Playing Sound", true);
                    mediaPlayer.start();//plays signal
                }
            });
        }
        /*==========================================================================================
        compileAndProgram(Map<String, byte[]> loc, String name, int wait_ms)
        ----------------------------------------------------------------------------------------
        Description: picks out specific elf file from the zip file and writes the instruction enclosed
        to a particular address of the FPAA. Waits a certain amount of time for the FPAA to program
        properly
        ----------------------------------------------------------------------------------------
        Arguments:

        loc is the zip file we downloaded stored as a Map. The byte arrays are the actual files and
        the String keys are the name of the files.

        name is the name of the elf file we want to write instruction from

        wait_ms is the wait time the FPAA requires to fully program
        ----------------------------------------------------------------------------------------
        Result: returns true when the instruction from elf file has been written to the FPAA
        ========================================================================================
        */
        protected boolean compileAndProgram(Map<String, byte[]> loc, String name, int wait_ms) {
            byte[] data;

            if (!loc.containsKey(name.trim())) {
                makeToastMessage("Zipped file does not contain file name \"" + name + "\"");
                Utils.debugLine("Zipped file does not contain file name \"" + name + "\"",true);
                return false;
            }

            try {
                data = Utils.compileElf(loc.get(name));
            } catch (Exception e) {
                return false;
            }

            try {
                driver.programData(data);
            } catch (Exception e) {
                return false;
            }


            return true;
        }
        /*==========================================================================================
       writeAscii(Map<String, byte[]> loc, int address, String name)
       ----------------------------------------------------------------------------------------
       Description: picks out specific file from the zip file and writes the instruction enclosed
       to a particular address of the FPAA
       ----------------------------------------------------------------------------------------
       Arguments:

       loc is the zip file we downloaded stored as a Map. The byte arrays are the actual files and
       the String keys are the name of the files.

       address is the location we are writing to

       name is the name of the file we want to write instruction from
       ----------------------------------------------------------------------------------------
       Result: returns true when the instruction has been written to the FPAA
       ========================================================================================
    */
        protected boolean writeAscii(Map<String, byte[]> loc, int address, String name) {
            Utils.debugLine("writeAscii start",true);
            if (!loc.containsKey(name)) {
                makeToastMessage("Zipped file does not contain file name \"" + name + "\"");
                Utils.debugLine("Zipped file does not contain file name \"" + name + "\"",true);
                return false;
            }

            byte[] data = Utils.parseHexAscii(loc.get(name));
            Utils.debugLine("parsedHexAscii",true);
            data = Utils.swapBytes(data);
            Utils.debugLine("swappedByetes",true);
            boolean b = driver.writeMem(address, data);
            Utils.debugLine("writemem done",true);
            if (b) driver.sleep(1000);
            return b;
        }

        /*==========================================================================================
           download(File file)
           ----------------------------------------------------------------------------------------
           Description: downloads the file as indicated by the url if File file does not exist yet.
           ----------------------------------------------------------------------------------------
           Arguments: file refers to the zip file path we created. It resides in the download folder
           ----------------------------------------------------------------------------------------
           Result: return true when we have finished downloading the zip file
           ========================================================================================
        */
        protected boolean download(File file) {
            Utils.debugLine("download method",true);
            // Download the file if it doesn't exist
            if (!file.exists()) {
                //String url = Configuration.C4_LOCATION;
                //DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                //String url = Configuration.DAC_ADC_LOCATION;
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                String title = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("."));
                request.setTitle(title);
                request.setDescription((("Downloading the ").concat(title)).concat(" programming file"));

                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, title.concat(".zip"));

                DownloadManager manager = (DownloadManager) parentContext.getSystemService(Context.DOWNLOAD_SERVICE);
                manager.enqueue(request);
                //////
                //new way of grabbing url
                /*
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                String title = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("."));
                request.setTitle(title);
                request.setDescription((("Downloading the ").concat(title)).concat(" programming file"));
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, title.concat(".zip"));
                DownloadManager manager = (DownloadManager) parentContext.getSystemService(Context.DOWNLOAD_SERVICE);
                manager.enqueue(request);
                */

                // Check to see if the file downloaded
                int counter = 0, MAX_COUNTER = 100;

                while (counter <= MAX_COUNTER && !file.exists()) {
                    counter++;
                    driver.sleep(150);
                    try{
                        Thread.sleep(150);
                    }
                    catch(InterruptedException e){}
                    if (counter > MAX_COUNTER) {
                        makeToastMessage("Downloading programming files failed (or is taking too long)");
                        Utils.debugLine("Downlaoding programming files failed (or is taking too long)",true);
                        return false;
                    }
                }
                driver.sleep(1000);
                try{
                    Thread.sleep(1000);
                }
                catch(InterruptedException e){}
                return true;
            }
            makeToastMessage("file already exists");
            Utils.debugLine("download file already exists", true);
            return true;
        }


    /*==========================================================================================
        TProgram (String[] targetList, Map<String, byte[]> zipped_files)
        ----------------------------------------------------------------------------------------
        Description:

        Use the targetList to program the FPAA.

        1) assign all of the lines from targetList to Strings
        2) check if each String is non zero
        3) if so, use writeAscii to program the FPAA based on the file corresponding to the String
        ----------------------------------------------------------------------------------------
        Arguments:

        targetList is the String array of the instruction from the targetlist.txt found
        in the RASP30 zip file downloaded from Github

        zipped_files is the unzipped zip file we downloaded. The bytes are the files and the keys
        are each file's name
        ----------------------------------------------------------------------------------------
        Result: returns true when the FPAA has been programmed
        ========================================================================================
     */


        public boolean TProgram(String[] targetList, Map<String, byte[]> zipped_files) {
            Utils.debugLine("start TProgram", true);
            //reads target list and operates on it
            //n_target_highaboveVt_swc=TL.readline();
            String n_target_highaboveVT_swc = targetList[1].trim();
            //n_target_highaboveVt_ota=TL.readline();
            String n_target_highaboveVt_ota = targetList[2].trim();
            //n_target_aboveVt_swc=TL.readline();
            String n_target_aboveVt_swc = targetList[3].trim();
            //n_target_aboveVt_ota=TL.readline();
            String n_target_aboveVt_ota = targetList[4].trim();
            //n_target_aboveVt_otaref=TL.readline();
            String n_target_aboveVt_otaref = targetList[5].trim();
            //n_target_aboveVt_mite=TL.readline();
            String n_target_aboveVt_mite = targetList[6].trim();
            //n_target_aboveVt_dirswc=TL.readline();
            String n_target_aboveVt_dirswc = targetList[7].trim();
            //n_target_subVt_swc=TL.readline();
            String n_target_subVt_swc = targetList[8].trim();
            //n_target_subVt_ota=TL.readline();
            String n_target_subVt_ota = targetList[9].trim();
            //n_target_subVt_otaref=TL.readline();
            String n_target_subVt_otaref = targetList[10].trim();
            //n_target_subVt_mite=TL.readline();
            String n_target_subVt_mite = targetList[11].trim();
            //n_target_subVt_dirswc=TL.readline();
            String n_target_subVt_dirswc = targetList[12].trim();
            //n_target_lowsubVt_swc=TL.readline();
            String n_target_lowsubVt_swc = targetList[13].trim();
            //n_target_lowsubVt_ota=TL.readline();
            String n_target_lowsubVt_ota = targetList[14].trim();
            //n_target_lowsubVt_otaref=TL.readline();
            String n_target_lowsubVt_otaref = targetList[15].trim();
            //n_target_lowsubVt_mite=TL.readline();
            String n_target_lowsubVt_mite = targetList[16].trim();
            //n_target_lowsubVt_dirswc=TL.readline();
            String n_target_lowsubVt_dirswc = targetList[17].trim();
            Utils.debugLine("assigned targetList Values", true);

            //progressBar3.setProgress(0);

            //put if statements here

//            if (!n_target_highaboveVT_swc.equals("0.000000000000000")) {
//                Utils.debugLine("highaboveVt_swc", true);
//                if (!writeAscii(zipped_files, 0x7000, "target_info_highaboveVt_swc")) return false;
//                ////progressBar3.setProgress(10);
//                if (!writeAscii(zipped_files, 0x6800, "pulse_width_table_highaboveVt_swc")) return false;
//                ////progressBar3.setProgress(20);
//                if (!compileAndProgram(zipped_files,"recover_inject_highaboveVt_SWC.elf", 20 * 1000)) return false;
//                //progressBar3.setProgress(30);
//
//                //if (!writeAscii(zipped_files, 0x7000, "target_info_highaboveVt_swc")) return false;
//                ////progressBar3.setProgress(40);
//                if (!compileAndProgram(zipped_files, "first_coarse_program_highaboveVt_SWC.elf", 20 * 1000)) return false;
//                ////progressBar3.setProgress(50);
//
//                //if (!writeAscii(zipped_files, 0x7000, "target_info_highaboveVt_swc")) return false;
//                ////progressBar3.setProgress(60);
//                if (!compileAndProgram(zipped_files, "measured_coarse_program_highaboveVt_SWC.elf", 20 * 1000)) return false;
//                ////progressBar3.setProgress(70);
//
//                //if (!writeAscii(zipped_files, 0x7000, "target_info_highaboveVt_swc")) return false;
//                ////progressBar3.setProgress(80);
//                if (!writeAscii(zipped_files, 0x6800, "Vd_table_30mV")) return false;
//                ////progressBar3.setProgress(90);
//                if (!compileAndProgram(zipped_files, "fine_program_highaboveVt_m_ave_04_SWC.elf", 20 * 1000)) return false;
//                ////progressBar3.setProgress(100);
//            }
            //progressBar3.setProgress(5);

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
                if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_swc")) return false;
                if (!writeAscii(zipped_files, 0x6800, "pulse_width_table_swc")) return false;
                if (!compileAndProgram(zipped_files, "recover_inject_aboveVt_SWC.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_swc")) return false;
                if (!compileAndProgram(zipped_files, "first_coarse_program_aboveVt_SWC.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_swc")) return false;
                if (!compileAndProgram(zipped_files, "measured_coarse_program_aboveVt_SWC.elf", 20 * 1000)) return false;


                //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_swc")) return false;
                if (!writeAscii(zipped_files, 0x6800, "Vd_table_30mV")) return false;
                if (!compileAndProgram(zipped_files, "fine_program_aboveVt_m_ave_04_SWC.elf", 20 * 1000)) return false;
            }
            //progressBar3.setProgress(10);

            if (!n_target_subVt_swc.equals("0.000000000000000")) {
                Utils.debugLine("subVt_swc", true);
                if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_swc")) return false;
                if (!writeAscii(zipped_files, 0x6800, "pulse_width_table_swc")) return false;
                if (!compileAndProgram(zipped_files,"recover_inject_subVt_SWC.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_swc")) return false;
                if (!compileAndProgram(zipped_files, "first_coarse_program_subVt_SWC.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_swc")) return false;
                if (!compileAndProgram(zipped_files, "measured_coarse_program_subVt_SWC.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_swc")) return false;
                if (!writeAscii(zipped_files, 0x6800, "Vd_table_30mV")) return false;
                if (!compileAndProgram(zipped_files, "fine_program_subVt_m_ave_04_SWC.elf", 20 * 1000)) return false;
            }
            //progressBar3.setProgress(15);


            if (!n_target_lowsubVt_swc.equals("0.000000000000000")) {
                Utils.debugLine("lowsubVt_swc", true);
                if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_swc")) return false;
                if (!writeAscii(zipped_files, 0x6800, "pulse_width_table_lowsubVt_swc")) return false;
                if (!compileAndProgram(zipped_files,"recover_inject_lowsubVt_SWC.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_swc")) return false;
                if (!compileAndProgram(zipped_files, "first_coarse_program_lowsubVt_SWC.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_swc")) return false;
                if (!compileAndProgram(zipped_files, "measured_coarse_program_lowsubVt_SWC.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_swc")) return false;
                if (!writeAscii(zipped_files, 0x6800, "Vd_table_30mV")) return false;
                if (!compileAndProgram(zipped_files, "fine_program_lowsubVt_m_ave_04_SWC.elf", 20 * 1000)) return false;
            }
            //progressBar3.setProgress(20);
/*--------------------------HIGHABOVEVT_NOT_YET_IMPLEMENTED-------------------*/
//            if (!n_target_highaboveVt_ota.equals("0.000000000000000")) {
//                Utils.debugLine("highaboveVt_ota", true);
//                if (!writeAscii(zipped_files, 0x7000, "target_info_highaboveVt_ota")) return false;
//                if (!writeAscii(zipped_files, 0x6800, "pulse_width_table_highaboveVt_ota")) return false;
//                if (!compileAndProgram(zipped_files,"recover_inject_highaboveVt_CAB_ota.elf", 20 * 1000)) return false;
//
//                //if (!writeAscii(zipped_files, 0x7000, "target_info_highaboveVt_ota")) return false;
//                if (!compileAndProgram(zipped_files, "first_coarse_program_highaboveVt_CAB_ota.elf", 20 * 1000)) return false;
//
//                //if (!writeAscii(zipped_files, 0x7000, "target_info_highaboveVt_ota")) return false;
//                if (!compileAndProgram(zipped_files, "measured_coarse_program_highaboveVt_CAB_ota.elf", 20 * 1000)) return false;
//
//                //if (!writeAscii(zipped_files, 0x7000, "target_info_highaboveVt_ota")) return false;
//                if (!writeAscii(zipped_files, 0x6800, "Vd_table_30mV")) return false;
//                if (!compileAndProgram(zipped_files, "fine_program_highaboveVt_m_ave_04_CAB_ota.elf", 20 * 1000)) return false;
//            }
            //progressBar3.setProgress(25);

            if (!n_target_aboveVt_ota.equals("0.000000000000000")) {
                Utils.debugLine("aboveVt_ota", true);
                if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_ota" )) return false;
                if (!writeAscii(zipped_files, 0x6800, "pulse_width_table_ota")) return false;
                if (!compileAndProgram(zipped_files, "recover_inject_aboveVt_CAB_ota.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_ota")) return false;
                if (!compileAndProgram(zipped_files, "first_coarse_program_aboveVt_CAB_ota.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_ota")) return false;
                if (!compileAndProgram(zipped_files, "measured_coarse_program_aboveVt_CAB_ota.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_ota")) return false;
                if (!writeAscii(zipped_files, 0x6800, "Vd_table_30mV")) return false;
                if (!compileAndProgram(zipped_files,"fine_program_aboveVt_m_ave_04_CAB_ota.elf", 20 * 1000)) return false;
            }
            //progressBar3.setProgress(30);

            if (!n_target_subVt_ota.equals("0.000000000000000")) {
                Utils.debugLine("subVt_ota", true);
                if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_ota" )) return false;
                if (!writeAscii(zipped_files, 0x6800, "pulse_width_table_ota")) return false;
                if (!compileAndProgram(zipped_files, "recover_inject_subVt_CAB_ota.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_ota")) return false;
                if (!compileAndProgram(zipped_files, "first_coarse_program_subVt_CAB_ota.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_ota")) return false;
                if (!compileAndProgram(zipped_files, "measured_coarse_program_subVt_CAB_ota.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_ota")) return false;
                if (!writeAscii(zipped_files, 0x6800, "Vd_table_30mV")) return false;
                if (!compileAndProgram(zipped_files,"fine_program_subVt_m_ave_04_CAB_ota.elf", 20 * 1000)) return false;
            }
            //progressBar3.setProgress(35);

            if (!n_target_lowsubVt_ota.equals("0.000000000000000")) {
                Utils.debugLine("lowsubVt_ota", true);
                //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_ota" )) return false;
                if (!writeAscii(zipped_files, 0x6800, "pulse_width_table_lowsubVt_ota")) return false;
                if (!compileAndProgram(zipped_files, "recover_inject_lowsubVt_CAB_ota.elf",20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_ota")) return false;
                if (!compileAndProgram(zipped_files, "first_coarse_program_lowsubVt_CAB_ota.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_ota")) return false;
                if (!compileAndProgram(zipped_files, "measured_coarse_program_lowsubVt_CAB_ota.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_ota")) return false;
                if (!writeAscii(zipped_files, 0x6800, "Vd_table_30mV")) return false;
                if (!compileAndProgram(zipped_files,"fine_program_lowsubVt_m_ave_04_CAB_ota.elf", 20 * 1000)) return false;
            }
            //progressBar3.setProgress(40);

            if (!n_target_aboveVt_otaref.equals("0.000000000000000")) {
                Utils.debugLine("aboveVt_otaref", true);
                if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_otaref" )) return false;
                if (!writeAscii(zipped_files, 0x6800, "pulse_width_table_otaref")) return false;
                if (!compileAndProgram(zipped_files, "recover_inject_aboveVt_CAB_ota_ref.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_otaref")) return false;
                if (!compileAndProgram(zipped_files, "first_coarse_program_aboveVt_CAB_ota_ref.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_otaref")) return false;
                if (!compileAndProgram(zipped_files, "measured_coarse_program_aboveVt_CAB_ota_ref.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_otaref")) return false;
                if (!writeAscii(zipped_files, 0x6800, "Vd_table_30mV")) return false;
                if (!compileAndProgram(zipped_files,"fine_program_aboveVt_m_ave_04_CAB_ota_ref.elf", 20 * 1000)) return false;
            }
            //progressBar3.setProgress(45);

            if (!n_target_subVt_otaref.equals("0.000000000000000")) {
                Utils.debugLine("subVt_otaref", true);
                if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_otaref" )) return false;
                if (!writeAscii(zipped_files, 0x6800, "pulse_width_table_otaref")) return false;
                if (!compileAndProgram(zipped_files, "recover_inject_subVt_CAB_ota_ref.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_otaref")) return false;
                if (!compileAndProgram(zipped_files, "first_coarse_program_subVt_CAB_ota_ref.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_otaref")) return false;
                if (!compileAndProgram(zipped_files, "measured_coarse_program_subVt_CAB_ota_ref.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_otaref")) return false;
                if (!writeAscii(zipped_files, 0x6800, "Vd_table_30mV")) return false;
                if (!compileAndProgram(zipped_files,"fine_program_subVt_m_ave_04_CAB_ota_ref.elf", 20 * 1000)) return false;
            }
            //progressBar3.setProgress(50);

            if (!n_target_lowsubVt_otaref.equals("0.000000000000000")) {
                Utils.debugLine("lowsubVt_otaref", true);
                if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_otaref")) return false;
                if (!writeAscii(zipped_files, 0x6800, "pulse_width_table_lowsubVt_otaref")) return false;
                if (!compileAndProgram(zipped_files, "recover_inject_lowsubVt_CAB_ota_ref.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_otaref")) return false;
                if (!compileAndProgram(zipped_files, "first_coarse_program_lowsubVt_CAB_ota_ref.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_otaref")) return false;
                if (!compileAndProgram(zipped_files, "measured_coarse_program_lowsubVt_CAB_ota_ref.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_otaref")) return false;
                if (!writeAscii(zipped_files, 0x6800, "Vd_table_30mV")) return false;
                if (!compileAndProgram(zipped_files, "fine_program_lowsubVt_m_ave_04_CAB_ota_ref.elf", 20 * 1000)) return false;
            }
            //progressBar3.setProgress(55);

            if (!n_target_aboveVt_mite.equals("0.000000000000000")) {
                Utils.debugLine("n_target_aboveVt_mite", true);
                if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_mite")) return false;//line 27
                //Utils.debugLine("1",true);
                if (!writeAscii(zipped_files, 0x6800, "pulse_width_table_mite")) return false;//line 28
                //Utils.debugLine("2",true);
                if (!compileAndProgram(zipped_files, "recover_inject_aboveVt_CAB_mite.elf", 20 * 1000)) return false;//line 29
                //Utils.debugLine("3",true);

                // coarse inj
                //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_mite")) return false; //line 32
                //Utils.debugLine("4",true);
                if (!compileAndProgram(zipped_files, "first_coarse_program_aboveVt_CAB_mite.elf", 20 * 1000)) return false;//line 33
                //Utils.debugLine("5",true);
                //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_mite")) return false;//line 36
                //Utils.debugLine("6",true);
                if (!compileAndProgram(zipped_files, "measured_coarse_program_aboveVt_CAB_mite.elf", 20 * 1000)) return false;//line 37
                //Utils.debugLine("7",true);

                // fine inj
                //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_mite")) return false;//line 39
                //Utils.debugLine("8",true);
                if (!writeAscii(zipped_files, 0x6800, "Vd_table_30mV")) return false;//line 40
                //Utils.debugLine("9",true);
                if (!compileAndProgram(zipped_files, "fine_program_aboveVt_m_ave_04_CAB_mite.elf", 20 * 1000)) return false;//line 41
                //Utils.debugLine("10",true);
            }
            //progressBar3.setProgress(60);

            if (!n_target_subVt_mite.equals("0.000000000000000")) {
                Utils.debugLine("n_target_subVt_mite", true);
                if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_mite")) return false;
                if (!writeAscii(zipped_files, 0x6800, "pulse_width_table_mite")) return false;
                if (!compileAndProgram(zipped_files, "recover_inject_subVt_CAB_mite.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_mite")) return false;
                if (!compileAndProgram(zipped_files, "first_coarse_program_subVt_CAB_mite.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_mite")) return false;
                if (!compileAndProgram(zipped_files, "measured_coarse_program_subVt_CAB_mite.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_mite")) return false;
                if (!writeAscii(zipped_files, 0x6800, "Vd_table_30mV")) return false;
                if (!compileAndProgram(zipped_files, "fine_program_subVt_m_ave_04_CAB_mite.elf", 20 * 1000)) return false;
            }
            //progressBar3.setProgress(65);

            if (!n_target_lowsubVt_mite.equals("0.000000000000000")) {
                Utils.debugLine("n_target_lowsubVt_mite", true);
                if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_mite")) return false;
                if (!writeAscii(zipped_files, 0x6800, "pulse_width_table_lowsubVt_mite")) return false;
                if (!compileAndProgram(zipped_files, "recover_inject_lowsubVt_CAB_mite.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_mite")) return false;
                if (!compileAndProgram(zipped_files, "first_coarse_program_lowsubVt_CAB_mite.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_mite")) return false;
                if (!compileAndProgram(zipped_files, "measured_coarse_program_lowsubVt_CAB_mite.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_mite")) return false;
                if (!writeAscii(zipped_files, 0x6800, "Vd_table_30mV")) return false;
                if (!compileAndProgram(zipped_files, "fine_program_lowsubVt_m_ave_04_CAB_mite.elf", 20 * 1000)) return false;
            }
            //progressBar3.setProgress(70);

            if (!n_target_aboveVt_dirswc.equals("0.000000000000000")) {
                Utils.debugLine("n_target_aboveVt_dirswc", true);
                if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_dirswc")) return false;
                if (!writeAscii(zipped_files, 0x6800, "pulse_width_table_dirswc")) return false;
                if (!compileAndProgram(zipped_files, "recover_inject_aboveVt_DIRSWC.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_dirswc")) return false;
                if (!compileAndProgram(zipped_files, "first_coarse_program_aboveVt_DIRSWC.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_dirswc")) return false;
                if (!compileAndProgram(zipped_files, "measured_coarse_program_aboveVt_DIRSWC.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_dirswc")) return false;
                if (!writeAscii(zipped_files, 0x6800, "Vd_table_30mV")) return false;
                if (!compileAndProgram(zipped_files, "fine_program_aboveVt_m_ave_04_DIRSWC.elf", 20 * 1000)) return false;
            }
            //progressBar3.setProgress(75);

            if (!n_target_subVt_dirswc.equals("0.000000000000000")) {
                Utils.debugLine("n_target_subVt_dirswc", true);
                if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_dirswc")) return false;
                if (!writeAscii(zipped_files, 0x6800, "pulse_width_table_dirswc")) return false;
                if (!compileAndProgram(zipped_files, "recover_inject_subVt_DIRSWC.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_dirswc")) return false;
                if (!compileAndProgram(zipped_files, "first_coarse_program_subVt_DIRSWC.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_dirswc")) return false;
                if (!compileAndProgram(zipped_files, "measured_coarse_program_subVt_DIRSWC.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_subVt_dirswc")) return false;
                if (!writeAscii(zipped_files, 0x6800, "Vd_table_30mV")) return false;
                if (!compileAndProgram(zipped_files, "fine_program_subVt_m_ave_04_DIRSWC.elf", 20 * 1000)) return false;
            }
            //progressBar3.setProgress(80);

            if (!n_target_lowsubVt_dirswc.equals("0.000000000000000")) {
                Utils.debugLine("n_target_lowsubVt_dirswc", true);
                if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_dirswc")) return false;
                if (!writeAscii(zipped_files, 0x6800, "pulse_width_table_lowsubVt_dirswc")) return false;
                if (!compileAndProgram(zipped_files, "recover_inject_lowsubVt_DIRSWC.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_dirswc")) return false;
                if (!compileAndProgram(zipped_files, "first_coarse_program_lowsubVt_DIRSWC.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_dirswc")) return false;
                if (!compileAndProgram(zipped_files, "measured_coarse_program_lowsubVt_DIRSWC.elf", 20 * 1000)) return false;

                //if (!writeAscii(zipped_files, 0x7000, "target_info_lowsubVt_dirswc")) return false;
                if (!writeAscii(zipped_files, 0x6800, "Vd_table_30mV")) return false;
                if (!compileAndProgram(zipped_files, "fine_program_lowsubVt_m_ave_04_DIRSWC.elf", 20 * 1000)) return false;
            }
            //progressBar3.setProgress(100);
            return true;
        }
        
    }
}