package com.example.flp_.codeweekcommuncation;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by flp_ on 15/10/2017.
 */
public class CommunicationHandler {

    private String _sensor;
    private String _data;
    private String _device;
    private String _ip;
    private int _port;

    private String _id;
    private DatagramSocket _socket;
    private InetAddress _serverAdress;

    public CommunicationHandler(String ip, int port){

        _ip   = ip;
        _port = port;
    }

    public CommunicationHandler(String ip, int port, String device){

        _ip     = ip;
        _port   = port;
        _device = device;
    }

    public CommunicationHandler(String ip, int port, String device,String sensor){

        _ip     = ip;
        _port   = port;
        _device = device;
        _sensor = sensor;
    }

    public CommunicationHandler(String ip, int port, String device,String sensor, String data){

        _ip     = ip;
        _port   = port;
        _device = device;
        _sensor = sensor;
        _data   = data;
    }

    public void establishConnection()throws SocketException,UnknownHostException{

        _socket       = new DatagramSocket();
        _serverAdress = InetAddress.getByName(_ip);
        _id           = System.currentTimeMillis()+"";

    }

    public void setDevice(String device){
        _device = device;
    }

    public void setSensor(String _sensor) {
        this._sensor = _sensor;
    }

    public void setData(String _data) {
        this._data = _data;
    }

    public void sendMessage(String message) throws IOException{
        String messageStr="[$]tracking|id="+_id+"|,[$$]"+_device+",[$$$]"+_sensor+","+_data+","+message+";";
        int msg_length=messageStr.length();
        byte[] msg = messageStr.getBytes();
        DatagramPacket p = new DatagramPacket(msg, msg_length,_serverAdress,_port);

        new CommunicationWorker(p,_socket).start();
    }

    public void closeConnection(){
        _socket.close();
    }

    private class CommunicationWorker extends Thread{

        private final DatagramPacket _packet;
        private final DatagramSocket _worker;

        public CommunicationWorker(DatagramPacket packet, DatagramSocket socket ){
            _packet = packet;
            _worker = socket;
        }

        @Override
        public void run(){
            try {
                _worker.send(_packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
