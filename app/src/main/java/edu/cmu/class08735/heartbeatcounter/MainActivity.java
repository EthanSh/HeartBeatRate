package edu.cmu.class08735.heartbeatcounter;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.TextView;


import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    static final String TAG = "Heartbeat Sensor";
    private HeartBeatCameraView mOpenCVCameraView;
    GraphView graph;
    LineGraphSeries<DataPoint> series;
    TextView text;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mOpenCVCameraView = (HeartBeatCameraView) findViewById(R.id.OpenCVCameraView);
        mOpenCVCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCVCameraView.setMaxFrameSize(200, 200);
        mOpenCVCameraView.setCvCameraViewListener(this);
        graph = (GraphView) findViewById(R.id.graph);
        series = new LineGraphSeries<>();
        graph.addSeries(series);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(40);
        text = (TextView) findViewById(R.id.text2);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        mOpenCVCameraView.turnOffFlashlight();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mOpenCVCameraView != null)
            mOpenCVCameraView.disableView();
    }

    int index =0;
    int count =0;
    double[] arr = new double[128];
    float hz;
    public double[] hanningWindow(double[] recordedData) {

        // iterate until the last line of the data buffer
        for (int n = 1; n < recordedData.length; n++) {
            // reduce unnecessarily performed frequency part of each and every frequency
            recordedData[n] *= 0.5 * (1 - Math.cos((2 * Math.PI * n)
                    / (recordedData.length - 1)));
        }
        // return modified buffer to the FFT function
        return recordedData;
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mOpenCVCameraView.turnOnFlashlight();

        Mat currentFrame = inputFrame.rgba();

        Imgproc.GaussianBlur(currentFrame, currentFrame, new Size(0,0), 0.2);

        Scalar scalar = Core.mean(currentFrame);

        double center = scalar.val[0];

        double topLeft = currentFrame.get(0, 0)[1];
        double topRight = currentFrame.get(0, currentFrame.cols()-1)[1];
        double bottomLeft = currentFrame.get(currentFrame.rows()-1, 0)[1];
        double bottomRight = currentFrame.get(currentFrame.rows()-1, currentFrame.cols()-1)[1];

        boolean isFullyTouched = Math.abs(topLeft-bottomRight+topRight-bottomLeft) < 10;
        if(!isFullyTouched){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    text.setText("put your finger to cover camera!");
                }
            });
            return currentFrame;
        }
        if(true){
            if(count <128){
                arr[count] = center;
                count++;
            }else{
                count = 0;
                double max = 0;
                float index = 0f;
//                double sum = 0.0;
//                for(int i = 0;i<128;i++){
//                    sum += arr[i];
//                }
//                for(int i =0;i<128;i++){
//                    arr[i] = arr[i] - sum/128;
//
//
//                }

//                for(int i =0;i<256;i++){
//                }
                double[] arrt = arr.clone();
                Complex[] complexes= new FastFourierTransformer(DftNormalization.STANDARD).transform(arr, TransformType.FORWARD);

                for(int i= 5;i<complexes.length/2;i++){
                    Log.d("hertz", String.valueOf(complexes[i].abs()));

                    if(max< complexes[i].abs()){
                        max = complexes[i].abs();
                        index = (float) i;
                    }
                }

                hz = index*14 /128;

                Log.d("hertz", String.valueOf(hz*60));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        text.setText("Your heart rate is:"+ String.valueOf(hz*60));
                    }
                });
            }
        }
        series.appendData(new DataPoint(index++, center ), true, 40);

//        int cannyThreshold=50;

        return currentFrame;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCVCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
}
