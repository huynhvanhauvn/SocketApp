package com.sbro.socketapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ListView listView1;
    private ArrayAdapter<String> listAdapter1;
    private ArrayList<String> items;
    private String INET_ADDR = "224.0.0.3";
    private int PORT = 8888;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // You must use the WifiManager to create a multicast lock in order to receive
        // multicast packets. Only do this while you're actively receiving data, because
        // it decreases battery life.
        // See: https://bugreports.qt.io/browse/QTBUG-34111
        WifiManager wifi = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null){
            WifiManager.MulticastLock lock = wifi.createMulticastLock("HelloAndroid");
            lock.acquire();
        }

        // Standard code for initializing a ListView from an ArrayList
        listView1 = (ListView) findViewById(R.id.listView1);
        items = new ArrayList<String>();
        listAdapter1 = new ArrayAdapter<String>(this, R.layout.simple_list_row, items);
        listView1.setAdapter(listAdapter1);

        // This thread receives the packets, as you can't do it from the main thread
        runThread();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // http://stackoverflow.com/questions/11140285/how-to-use-runonuithread
    private void runThread() {

        new Thread() {
            public void run() {
                MulticastSocket socket = null;
                InetAddress group = null;

                try {
                    socket = new MulticastSocket(PORT);
                    group = InetAddress.getByName(INET_ADDR);
                    socket.joinGroup(group);

                    DatagramPacket packet;
                    while (true) {
                        byte[] buf = new byte[256];
                        packet = new DatagramPacket(buf, buf.length);
                        socket.receive(packet);

                        // Java byte values are signed. Convert to an int so we don't have to deal with negative values for bytes >= 0x7f (unsigned).
                        int[] valueBuf = new int[2];
                        for (int ii = 0; ii < valueBuf.length; ii++) {
                            valueBuf[ii] = (buf[ii] >= 0) ? (int) buf[ii] : (int) buf[ii] + 256;
                        }

                        final int value = (valueBuf[0] << 8) | valueBuf[1];

                        String s = new String(packet.getData());
                        Log.d(TAG, s);

                        synchronized (items) {
                            items.add(s);
                        }

                        // We're running on a worker thread here, but we need to update the list view from the main thread
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                synchronized (items) {
                                    listAdapter1.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                }
                catch(IOException e) {
                    System.out.println(e.toString());
                }
                finally {
                    if (socket != null) {
                        try {
                            if (group != null) {
                                socket.leaveGroup(group);
                            }
                            socket.close();
                        }
                        catch(IOException e) {

                        }
                    }
                }
            }


        }.start();
    }
}
