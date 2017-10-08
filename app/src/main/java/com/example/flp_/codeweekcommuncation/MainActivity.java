package com.example.flp_.codeweekcommuncation;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Communication Example";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    @Override
    protected void onResume() {
        super.onResume();
        new SocketWorker().start();

    }

    private class SocketWorker extends Thread{

        public void run(){
            try{
                int server_port = 1202;
                DatagramSocket s = new DatagramSocket();
                InetAddress local = InetAddress.getByName("192.168.1.9");
                for(int i=0;i<25;i++){
                    String messageStr="[$]tracking|id="+System.currentTimeMillis()+"|,[$$]android,[$$$]gyro,rotation,10,20,30;";
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
