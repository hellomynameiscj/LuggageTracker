package sensors;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.punchthrough.bean.sdk.Bean;
import com.punchthrough.bean.sdk.message.Acceleration;
import com.punchthrough.bean.sdk.message.Callback;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import mecs.hci.luggagetracker.CurrentBean;
import mecs.hci.luggagetracker.R;


public class AccelerometerFragment extends Fragment {

    public static String TAG = "AccelerometerFragment";

    Bean bean;

    private TextView  XTextView;
    private TextView  YTextView;
    private TextView  ZTextView;
    private TextView warningLevel;

    private Timer timer;

    public AccelerometerFragment() {
        // Required empty public constructor
    }


    public static AccelerometerFragment newInstance(String param1, String param2) {
        AccelerometerFragment fragment = new AccelerometerFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_accelerometer, container, false);
        XTextView = (TextView)rootView.findViewById(R.id.xAxisAccelerationTextView);
        YTextView = (TextView)rootView.findViewById(R.id.yAxisAccelerationTextView);
        ZTextView = (TextView)rootView.findViewById(R.id.zAxisAccelerationTextView);
        warningLevel = (TextView)rootView.findViewById(R.id.warningLevel);

        bean = CurrentBean.getBean();

        if (bean != null) {
            startMonitoringAccelerometer();
        } else {
            Toast.makeText(getActivity(), "Bean not connected, sensor data will not work",
                    Toast.LENGTH_LONG).show();
            startFakeAccelerometer();
        }

        return rootView;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void startMonitoringAccelerometer(){
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                bean.readAcceleration(new Callback<Acceleration>() {
                    @Override
                    public void onResult(final Acceleration result) {
                        getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                XTextView.setText(Double.toString(result.x()));
                                YTextView.setText(Double.toString(result.y()));
                                ZTextView.setText(Double.toString(result.z()));
                            }
                        });
                    }
                });
            }
        }, 0, 250);
    }

    private Random r = new Random();
    private Acceleration previousAccel;
    private void startFakeAccelerometer(){
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // fake results
                //r.nextInt(80 - 75) + 75;
                double x = (r.nextDouble()*2) - 1;
                double y = (r.nextDouble()*2) - 1;
                double z = (r.nextDouble()*2) - 1;
                final Acceleration result = new AccelerationFake(x,y,z);

                // detect movement
                if (previousAccel != null) {
                    if (isKnocked(result, previousAccel)) {
                        getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                warningLevel.setText("BANG!");
                            }
                        });
                    } else if (accelerometerMoving(result, previousAccel)) {
                        getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                warningLevel.setText("MOVING");
                            }
                        });
                    } else {
                         getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                warningLevel.setText("STILL");
                            }
                        });
                    }
                }
                previousAccel = result;
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        XTextView.setText(String.format(" %.2f ", result.x()));
                        YTextView.setText(String.format(" %.2f ", result.y()));
                        ZTextView.setText(String.format(" %.2f ", result.z()));
                    }
                });
            }
        }, 0, 750);
    }

    private double difference(double one, double two) {
        if (one > two) {
            return one - two;
        }
        return two - one;
    }

    // compares if the axis values are different which indicates its moving
    // currently if there is a big enough difference on any axis it gives true
    private boolean accelerometerMoving(Acceleration one, Acceleration two) {
        double threshold = 0.3;
        if (difference(one.x(), two.x()) > threshold) {
            return true;
        }
        if (difference(one.y(), two.y()) > threshold) {
            return true;
        }
        if (difference(one.z(), two.z()) > threshold) {
            return true;
        }
        return false;
    }

    private boolean isKnocked(Acceleration one, Acceleration two) {
        double threshold = 0.5;
        return (difference(one.x(), two.x()) > threshold &&
                difference(one.y(), two.y()) > threshold &&
                difference(one.z(), two.z()) > threshold);
    }

    @Override
    public void onPause() {
        if (timer !=  null) {
            timer.cancel();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        if (timer !=  null) {
            timer.cancel();
        }
        if (bean != null) {
            startMonitoringAccelerometer();
        } else {
            Toast.makeText(getActivity(), "Bean not connected, sensor data will not work",
                    Toast.LENGTH_LONG).show();
            startFakeAccelerometer();
        }
        super.onResume();
    }
}
