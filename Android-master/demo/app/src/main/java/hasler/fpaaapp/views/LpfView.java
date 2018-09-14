package hasler.fpaaapp.views;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import hasler.fpaaapp.R;
import hasler.fpaaapp.models.ProgramModel;
import hasler.fpaaapp.utils.Configuration;
import hasler.fpaaapp.utils.DriverFragment;
import hasler.fpaaapp.utils.ReadTimeOutException;
import hasler.fpaaapp.utils.Utils;

import static java.lang.Thread.sleep;

public class LpfView extends DriverFragment {
    private final String TAG = "LpfView";

    protected Handler mHandler = new Handler();
    protected ProgressBar progressBar;
    protected GraphView graph;

    public static LpfView newInstance() {
        return new LpfView();
    }
    public LpfView() { /* Required empty public constructor */ }


    public static void plot(GraphView graph, long[][]... vals) {
        List<LineGraphSeries<DataPoint>> series = new ArrayList<>();
        for (long[][] val : vals) {
            DataPoint[] dataPoints = new DataPoint[val[0].length];
            for (int i = 0; i < val[0].length; i++) {
                dataPoints[i] = new DataPoint(val[0][i], val[1][i]);
            }

            LineGraphSeries<DataPoint> graphSeries = new LineGraphSeries<>(dataPoints);
            series.add(graphSeries);
        }
        plotSeries(graph, series);
    }

    public static void plot(GraphView graph, double[][]... vals) {
        List<LineGraphSeries<DataPoint>> series = new ArrayList<>();
        for (double[][] val : vals) {
            DataPoint[] dataPoints = new DataPoint[val[0].length];
            for (int i = 0; i < val[0].length; i++) {
                dataPoints[i] = new DataPoint(val[0][i], val[1][i]);
            }

            LineGraphSeries<DataPoint> graphSeries = new LineGraphSeries<>(dataPoints);
            series.add(graphSeries);
        }
        plotSeries(graph, series);
    }

    public static void plot(GraphView graph, double[] xvals, double[] yvals) {
        graph.removeAllSeries();
        DataPoint[] dataPoints = new DataPoint[xvals.length];
        for (int i = 0; i < xvals.length; i++) {
            dataPoints[i] = new DataPoint(xvals[i], yvals[i]);
        }
        LineGraphSeries<DataPoint> graphSeries = new LineGraphSeries<>(dataPoints);
        graph.addSeries(graphSeries);
    }

    protected static final int[] COLORS = { Color.BLUE, Color.RED, Color.GREEN, Color.CYAN, Color.BLACK };

    public static void plotSeries(GraphView graph, List<LineGraphSeries<DataPoint>> graphSeries) {
        graph.removeAllSeries();
        for (int i = 0; i < graphSeries.size(); i++) {
            LineGraphSeries<DataPoint> series = graphSeries.get(i);
            series.setColor(COLORS[i % COLORS.length]);
            graph.addSeries(series);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) return null;
        super.onCreate(savedInstanceState);

        // Inflate the view XML file
        final View view = inflater.inflate(R.layout.fragment_lpf, container, false);

        // Progress bar (used by both processes)
        progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        graph = (GraphView) view.findViewById(R.id.graph);
        final Button programDesignButton = (Button) view.findViewById(R.id.program_design_button);
        final Button getDataButton = (Button) view.findViewById(R.id.get_data_button);

        /***********
         * PROGRAM *
         ***********/

        programDesignButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("StaticFieldLeak")
            @Override
            public void onClick(View v) {
                new ThreadRunnable() {
                    @Override
                    protected void onPreExecute() {
                        getDataButton.setEnabled(false);
                        programDesignButton.setEnabled(false);
                        progressBar.setProgress(0);
                    }

                    @TargetApi(Build.VERSION_CODES.KITKAT)
                    @Override
                    public Boolean doInBackground(Void... params) {
                        programmer = new ProgramModel("lpf_meas.zip", driver, progressBar);

//                        try {
//                            programmer.program();
//                        } catch (Exception e) {
//                            makeToastMessage(e.getMessage());
//                            return false;
//                        }

                        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        File file = new File(path, "lpf_meas.zip");

                        if (!download(file)) return false;
                        makeToastMessage("Download Complete");

                        Map<String, byte[]> zipped_files = Utils.getZipContents(file.getAbsolutePath());

                        try {
                            Log.i("LPF View", "Tunnel Programming...");
                            programmer.tunnelProgram(zipped_files);
                            Log.i("LPF View", "Tunnel Program Complete");

                            makeToastMessage("Tunnel Program Complete");

                            Log.i("LPF View", "Switch Programming...");
                            programmer.switchProgram(zipped_files);

                            makeToastMessage("Switch Program Complete");
                            Log.i("LPF View", "Switch Program Complete");

                            sleep(10000);

                            Log.i("LPF View", "Target Programming...");
                            programmer.targetProgram(zipped_files);
                            Log.i("LPF View", "Target Program Complete");

                            makeToastMessage("Target Program Complete");

//                            programmer.runMode(zipped_files);
//                            makeToastMessage("Run Mode Complete");
                        } catch (Exception e) {
                            makeToastMessage(e.getMessage());
                            Log.e("LPF View", e.getMessage());
                            return false;
                        }

                        return true;
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        super.onPostExecute(result);

                        if (result == null || !result) {
                            makeToastMessage("Error while trying to program the design");
                        }

                        progressBar.setProgress(100);
                        getDataButton.setEnabled(true);
                        programDesignButton.setEnabled(true);
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

        /************
         * GET DATA *
         ************/

        getDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ThreadRunnable() {
                    @Override
                    protected void onPreExecute() {
                        getDataButton.setEnabled(false);
                        programDesignButton.setEnabled(false);
                        progressBar.setProgress(0);
                    }

                    @TargetApi(Build.VERSION_CODES.KITKAT)
                    @Override
                    public Boolean doInBackground(Void... params) {
                        try {
                            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                            File file = new File(path, "lpf_meas.zip");
                            if (!download(file)) return null;

                            if (!driver.connect()) return null;

                            Map<String, byte[]> zipped_files = Utils.getZipContents(file.getAbsolutePath());
                            updateProgressBar(10);

                            if (!writeAscii(zipped_files, 0x4300, "input_vector")) return null;
                            if (!writeAscii(zipped_files, 0x4200, "output_info")) return null;
                            updateProgressBar(30);

                            if (!driver.runWithoutData()) return null;
                            updateProgressBar(50);

                            driver.sleep(40 * 1000);
                            updateProgressBar(100);

                            long[] d = Utils.toInts(2, driver.readMem(0x6000, 1000));
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
                                updateGraph(new long[][]{Utils.linspace(1, d.length, d.length), d});
                            } else {
                                long[] a = sp.get(sp.size() - 1);
                                double[] b = new double[a.length];
                                for (int i = 0; i < a.length; i++)
                                    b[i] = (a[i] - 5000) * 2.5 / 5000.0;
                                updateGraph(new double[][]{Utils.linspace(1.0, b.length, b.length), b},
                                        new double[][]{Utils.linspace(1.0, n.length, l.length), n});
                            }

                            return true;
                        } catch (ReadTimeOutException e) {
                            return true;
                        }
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        super.onPostExecute(result);

                        if (result == null || !result) {
                            makeToastMessage("Error while trying to program the design");
                        }

                        progressBar.setProgress(100);
                        getDataButton.setEnabled(true);
                        programDesignButton.setEnabled(true);
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

        return view;
    }

    protected abstract class ThreadRunnable extends AsyncTask<Void, Void, Boolean> {
        ProgramModel programmer;

        public void updateProgressBar(final int i) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    progressBar.setProgress(i);
                }
            });
        }

        public void updateGraph(final long[][]... data) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    plot(graph, data);
                }
            });
        }

        public void updateGraph(final double[][]... data) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    plot(graph, data);
                }
            });
        }

        public void updateGraph(final double[] xdata, final double[] ydata) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    plot(graph, xdata, ydata);
                }
            });
        }

        public void makeToastMessage(final String text) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(parentContext, text, Toast.LENGTH_SHORT).show();
                }
            });
        }

        protected boolean compileAndProgram(Map<String, byte[]> loc, String name, int wait_ms) {
            byte[] data;

            if (!loc.containsKey(name)) {
                makeToastMessage("Zipped file does not contain file name \"" + name + "\"");
                return false;
            }

            try {
                data = Utils.compileElf(loc.get(name));
            } catch (Exception e) { return false; }

            try {
                driver.programData(data);
            } catch (Exception e) {
                return false;
            }

            driver.sleep(wait_ms);
            return true;
        }

        protected boolean writeAscii(Map<String, byte[]> loc, int address, String name) {
            if (!loc.containsKey(name)) {
                makeToastMessage("Zipped file does not contain file name \"" + name + "\"");
                return false;
            }

            byte[] data = Utils.parseHexAscii(loc.get(name));
            data = Utils.swapBytes(data);
            boolean b = driver.writeMem(address, data);
            if (b) driver.sleep(1000);
            return b;
        }

        protected boolean download(File file) {
            // Download the file if it doesn't exist
            if (!file.exists()) {
                // String url = Configuration.LPF_LOCATION;
                String url = Configuration.DAC_ADC_LOCATION;
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setTitle("lpf_meas.zip");
                request.setDescription("Downloading the LPF programming file");

                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "lpf_meas.zip");

                DownloadManager manager = (DownloadManager) parentContext.getSystemService(Context.DOWNLOAD_SERVICE);
                manager.enqueue(request);

                // Check to see if the file downloaded
                int counter = 0, MAX_COUNTER = 100;
                while (counter <= MAX_COUNTER && !file.exists()) {
                    counter++;
                    driver.sleep(10);
                }
                if (counter > MAX_COUNTER) {
                    makeToastMessage("Downloading programming files failed (or is taking too long)");
                    return false;
                }
            }

            return true;
        }
    }

}
