package com.example.flp_.codeweekcommuncation;

import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Communication Example";
    private static final String DEVICE = "android";
    private static final String SENSOR = "gyro";
    private static final String DATA = "rotation";

    private CommunicationHandler _comHandler;

    private EditText _ipField;
    private EditText _portField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        _ipField    = (EditText)findViewById(R.id.server_ip);
        _portField = (EditText)findViewById(R.id.port);
    }

    @Override
    protected void onStop() {
        super.onStop();
        _comHandler.closeConnection();
    }

    public void handleEstablishConnectionClick(View v){
        String ip   = _ipField.getText().toString();
        int port = Integer.parseInt(_portField.getText().toString());

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

    private class SocketWorker extends Thread{

        public void run(){
            try{
                int server_port = 1202;
                DatagramSocket s = new DatagramSocket();
                InetAddress local = InetAddress.getByName("192.168.1.9");
                String id = System.currentTimeMillis()+"";
                for(int i=0;i<25;i++){
                    String messageStr="[$]tracking|id="+id+"|,[$$]android,[$$$]gyro,rotation,10,20,30;";
                    int msg_length=messageStr.length();
                    byte[] message = messageStr.getBytes();
                    DatagramPacket p = new DatagramPacket(message, msg_length,local,server_port);
                    s.send(p);
                    Thread.sleep(500);
                    Log.i(TAG,"sending... "+messageStr);
                }
                Log.i(TAG,"data sent");
            }catch(Exception e){
                e.printStackTrace();
            }
        }

    }
}
