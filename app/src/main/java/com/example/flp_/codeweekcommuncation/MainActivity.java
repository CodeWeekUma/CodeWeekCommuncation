package com.example.flp_.codeweekcommuncation;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ToggleButton;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity  implements SensorEventListener {

    private static final String TAG = "Communication Example";
    private static final String DEVICE = "android";
    private static final String SENSOR = "orientation";
    private static final String DATA = "angles";

    private CommunicationHandler _comHandler;

    private EditText _ipField;
    private EditText _portField;
    private EditText _frequencyField;


    private SensorManager _sensorManager;
    private Sensor _sensor_gyro;
    private Sensor _sensor_magnetic;

    private  float[] _accelerometerReading = new float[3];
    private  float[] _magnetometerReading  = new float[3];

    private  float[] _rotationMatrix       = new float[9];
    private  float[] _orientationAngles    = new float[3];

    private ContinuousWorkerThread _continuousWorkerThread;

    private final static String IP_KEY="server_ip";
    private final static String PORT_KEY="server_port";

    SharedPreferences _prefs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _prefs  = this.getSharedPreferences(
                "com.example.flp_.codeweekcommunication", Context.MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        _ipField        = (EditText)findViewById(R.id.server_ip);
        _portField      = (EditText)findViewById(R.id.port);
        _frequencyField = (EditText)findViewById(R.id.frequency);

        String ip   = _prefs.getString(IP_KEY,"");
        String port = _prefs.getString(PORT_KEY,"");

        _ipField.setText(ip.toString());
        _portField.setText(port.toString());

        _sensorManager   = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        _sensor_gyro     = _sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        _sensor_magnetic = _sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        _sensorManager.registerListener(this, _sensor_gyro, SensorManager.SENSOR_DELAY_NORMAL);
        _sensorManager.registerListener(this, _sensor_magnetic, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(_comHandler!=null)
          _comHandler.closeConnection();

        if(_continuousWorkerThread!=null){
            _continuousWorkerThread.stopSendingData();
        }

        if(_sensorManager!=null){
            _sensorManager.unregisterListener(this);
        }
    }

    public void handleEstablishConnectionClick(View v){
        String ip   = _ipField.getText().toString();
        int port = Integer.parseInt(_portField.getText().toString());

        _prefs.edit().putString(IP_KEY,ip).apply();
        _prefs.edit().putString(PORT_KEY,port+"").apply();

        try {
            _comHandler = new CommunicationHandler(ip,port,DEVICE,SENSOR,DATA);
            _comHandler.establishConnection();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void handleSendDataButton(View v){
        try {
            float value_1 = (float) (90*Math.random());
            float value_2 = (float) (90*Math.random());
            float value_3 = (float) (90*Math.random());

            _comHandler.sendMessage(value_1+"."+value_2+","+value_3);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleONOFFClick(View v){

        String ip   = _ipField.getText().toString();
        int port = Integer.parseInt(_portField.getText().toString());
        _prefs.edit().putString(IP_KEY,ip).apply();
        _prefs.edit().putString(PORT_KEY,port+"").apply();
        int frequency = Integer.parseInt(_frequencyField.getText().toString());
        int sleepTime = Math.round((1/(float)frequency)*1000);

       if(((ToggleButton)v).isChecked()){
           _continuousWorkerThread = new ContinuousWorkerThread(ip,port,DEVICE,SENSOR,DATA,sleepTime);
           _continuousWorkerThread.start();
           _continuousWorkerThread.startSendingData();
        }else{
            _continuousWorkerThread.stopSendingData();
       }
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, _accelerometerReading,
                    0, _accelerometerReading.length);
        }
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, _magnetometerReading,
                    0, _magnetometerReading.length);
        }

        updateOrientationAngles();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        _sensorManager.getRotationMatrix(_rotationMatrix, null,
                _accelerometerReading, _magnetometerReading);

        // "mRotationMatrix" now has up-to-date information.

        _sensorManager.getOrientation(_rotationMatrix, _orientationAngles);


    }

    private class ContinuousWorkerThread extends Thread {


        private  InetAddress _serverAdress;
        private  DatagramSocket  _socket;
        private  String  _id;
        private  String _ip;
        private  int _port;
        private  String _sensor;
        private  String _data;
        private  String _device;
        private  long _sleepTime;
        private  boolean _continuousSocketRunning  = false;


        public ContinuousWorkerThread(String ip, int port, String device, String sensor, String data, long sleepTime){
            Log.i(TAG,"ContinuousWorkerThread Created");
            _ip        = ip;
            _port      = port;
            _device    = device;
            _sensor    = sensor;
            _data      = data;
            _sleepTime = sleepTime;

            try {
                _socket = new DatagramSocket();
                _serverAdress = InetAddress.getByName(ip);
                _id = System.currentTimeMillis() + "";
                Log.i(TAG,"Connection established");

            }catch(Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public void run(){

            while(_continuousSocketRunning){
                try {
                    Log.i(TAG,"sending data");
                    String messageStr="[$]tracking|id="+_id+"|,[$$]"+_device+",[$$$]"+_sensor+","+_data+","+_orientationAngles[0]+","+_orientationAngles[1]+","+_orientationAngles[2]+";";
                    DatagramPacket p = new DatagramPacket(messageStr.getBytes(), messageStr.length(),_serverAdress,_port);
                    _socket.send(p);

                    Thread.sleep(_sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }

        public void startSendingData(){
            _continuousSocketRunning = true;
        }

        public void stopSendingData(){
            _continuousSocketRunning = false;
            _socket.close();
        }
    }

}
