package com.example.wificallv2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity {

    TextView textView;
    String ip="";
    boolean IN_CALL = false;
    EditText edit;
    Button buttonCall;
    Button buttonEndCal;
    public final static String EXTRA_IP = "my_ip";
    static final String LOG_TAG = "WiFi Call";
    private boolean LISTEN = false;
    private static final int LISTENER_PORT = 50003;
    private static final int BUF_SIZE = 1024;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialWork();

        //Request Permission For Recording
        requestRecordAudioPermission();

        //Display Your Ip in Text View
        getMyIpAddress();

        //Start Listen For incoming Call
        startCallListener();

        buttonCall.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {

               // Send this information to the MakeCallActivity and start that activity
               Intent intent = new Intent(MainActivity.this, ActivityMakeCall.class);
               intent.putExtra(EXTRA_IP, edit.getText().toString());
               startActivity(intent);
           }
       });



    }

    private void getMyIpAddress() {
        WifiManager wm = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        textView.setText("Ip Address:"+ip);
    }

    private void initialWork() {
        textView =findViewById(R.id.textView);
         buttonCall = findViewById(R.id.buttonCall);
         buttonEndCal = findViewById(R.id.buttonEndCall);
         buttonEndCal = findViewById(R.id.buttonEndCall);
         edit = findViewById(R.id.editTextIp);
    }


    private void startCallListener() {
        // Creates the listener thread
        LISTEN = true;
        Thread listener = new Thread(new Runnable() {

            @Override
            public void run() {

                try {
                    // Set up the socket and packet to receive
                    Log.i(LOG_TAG, "Incoming call listener started");
                    DatagramSocket socket = new DatagramSocket(LISTENER_PORT);
                    socket.setSoTimeout(1000);
                    byte[] buffer = new byte[BUF_SIZE];
                    DatagramPacket packet = new DatagramPacket(buffer, BUF_SIZE);
                    while(LISTEN) {
                        // Listen for incoming call requests
                        try {
                            Log.i(LOG_TAG, "Listening for incoming calls");
                            socket.receive(packet);
                            String data = new String(buffer, 0, packet.getLength());
                            Log.i(LOG_TAG, "Packet received from "+ packet.getAddress() +" with contents: " + data);
                            String action = data.substring(0, 4);
                            if(action.equals("CAL:")) {
                                // Received a call request. Start the ReceiveCallActivity
                                String address = packet.getAddress().toString();


                                Intent intent = new Intent(MainActivity.this, RecivieCall.class);

                                intent.putExtra(EXTRA_IP, address.substring(1, address.length()));
                                IN_CALL = true;

                                startActivity(intent);
                            }
                            else {
                                // Received an invalid request
                                Log.w(LOG_TAG, packet.getAddress() + " sent invalid message: " + data);
                            }
                        }
                        catch(Exception e) {}
                    }
                    Log.i(LOG_TAG, "Call Listener ending");
                    socket.disconnect();
                    socket.close();
                }
                catch(SocketException e) {

                    Log.e(LOG_TAG, "SocketException in listener " + e);
                }
            }
        });
        listener.start();
    }

    private void requestRecordAudioPermission() {
        //check API version, do nothing if API version < 23!
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion > android.os.Build.VERSION_CODES.LOLLIPOP){

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.d("Activity", "Granted!");

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("Activity", "Denied!");
                    finish();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
