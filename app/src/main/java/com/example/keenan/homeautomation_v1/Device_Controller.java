package com.example.keenan.homeautomation_v1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

//This code connects to an esp8266 in AP mode and then sets the password for the selected router
//then the esp8266 goes into STATION mode and connects to the selected router. Now the phone can
//control the esp8266's led from any where in the world with an internet connection. This is
//assuming the esp8266 is port forwarded on the router
public class Device_Controller extends ActionBarActivity {

    private final IntentFilter intentFilter = new IntentFilter();
    TextView CurrentStatusText,PortForwardText;
    EditText IPaddressText,PortNumberText,SsidText,PasswordText,DeviceNumberText;
    Button SetPasswordButton,StatusButton,OnButton,OffButton;
    WifiConfiguration WifiConfig;
    WifiManager wifi;
    WifiScanReceiver wifiReceiver;
    WifiInfo wifiInformation;
    String IPaddressString, RouterSsid, RouterPassword,EspNetworkSSID ="test1",EspNetworkPass ="12345678",wifis[];
    int PortNumberInt,TimeThrough=0;
    int SetPasswordFlag=0,SetIPAddressForwardFlag=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device__controller);

        IPaddressText = (EditText) findViewById(R.id.IPaddressText);
        PortNumberText = (EditText) findViewById(R.id.PortNumberText);
        SsidText = (EditText) findViewById(R.id.SsidText);
        PasswordText = (EditText) findViewById(R.id.PasswordText);
        SetPasswordButton = (Button) findViewById(R.id.SetPasswordButton);
        DeviceNumberText = (EditText) findViewById(R.id.DeviceNumberText);
        StatusButton = (Button) findViewById(R.id.StatusButton);
        CurrentStatusText = (TextView) findViewById(R.id.CurrentStatusText);
        OnButton = (Button) findViewById(R.id.OnButton);
        OffButton = (Button) findViewById(R.id.OffButton);
        PortForwardText = (TextView) findViewById(R.id.PortForwardText);

        /////////Intents added/////////////////
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_device__controller, menu);
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

    protected void onPause() {
        super.onPause();
        unregisterReceiver(wifiReceiver);
    }

    protected void onResume() {
        super.onResume();
        wifi=(WifiManager)getSystemService(Context.WIFI_SERVICE);
        wifiReceiver = new WifiScanReceiver();
        WifiConfig = new WifiConfiguration();

        registerReceiver(wifiReceiver, intentFilter);//registers receiver and enables onReceive method
        //registerReceiver(wifiReceiver, intentFilter);
        //wifiReceiver = new WifiScanReceiver();

    }

    private class WifiScanReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {



            ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo current = connManager.getActiveNetworkInfo();

            wifiInformation = wifi.getConnectionInfo();
            //Toast.makeText(getApplicationContext(),wifiInformation.getSSID() + "?=" + "\"" + EspNetworkSSID + "\"", Toast.LENGTH_LONG).show();

            boolean isWifi = current != null && current.getType() == ConnectivityManager.TYPE_WIFI && new String("\"" + EspNetworkSSID + "\"").equals(wifiInformation.getSSID());
            if (isWifi && SetPasswordFlag==1) {
                SetPasswordFlag=0;
                //Toast.makeText(getApplicationContext(), "wifi is connected if statement: " + String.valueOf(isConnectedViaWifi()), Toast.LENGTH_LONG).show();
                wifiInformation = wifi.getConnectionInfo();
                Toast.makeText(getApplicationContext(), wifiInformation.getSSID() + " has been connected to", Toast.LENGTH_LONG).show();

                //This is the code that transmits to the esp8266 after wifi connection is established/////
                Runnable r = new Runnable() {
                    @Override
                    public void run() {

                        runOnUiThread(new Runnable() {
                            public void run() {
                                SetPasswordButton.setText("");
                            }
                        });

                        String IPaddressDefault = "192.168.4.1";
                        int DefaultPortNumber = 60002;

                        RouterSsid = SsidText.getText().toString();
                        String SsidAmount = String.format("%02d", RouterSsid.length());
                        RouterPassword = PasswordText.getText().toString();
                        String PasswordAmount = String.format("%02d", RouterPassword.length());
                        final String RouterInfo = "ssid:" + SsidAmount + PasswordAmount + RouterSsid + RouterPassword;

                        runOnUiThread(new Runnable() {
                            public void run() {
                                CurrentStatusText.setText(RouterInfo);
                            }
                        });

                        try {
                            //get a datagram socket
                            DatagramSocket socket = new DatagramSocket();
                            //send request
                            byte[] buf = new byte[256];
                            byte[] bufReceive = new byte[256];
                            buf = RouterInfo.getBytes();
                            InetAddress address = InetAddress.getByName(IPaddressDefault);
                            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, DefaultPortNumber);
                            socket.send(packet);
                            //get response
                            packet = new DatagramPacket(bufReceive, bufReceive.length);
                            socket.receive(packet);//the thread hangs here
                            // display response
                            final String received = new String(packet.getData(), 0, packet.getLength());
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    SetPasswordButton.setText("Data Received: " + received);
                                }
                            });
                            socket.close();
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                };
                Thread myThread = new Thread(r);
                myThread.start();
                //////////////////////////////////////////////////////////////////////////////////////////
            }

            if (isWifi && SetIPAddressForwardFlag==1) {
                SetIPAddressForwardFlag=0;
                //Toast.makeText(getApplicationContext(), "wifi is connected if statement: " + String.valueOf(isConnectedViaWifi()), Toast.LENGTH_LONG).show();
                wifiInformation = wifi.getConnectionInfo();
                Toast.makeText(getApplicationContext(), wifiInformation.getSSID() + " has been connected to", Toast.LENGTH_LONG).show();

                //This is the code that transmits to the esp8266 after wifi connection is established/////
                Runnable r = new Runnable() {
                    @Override
                    public void run() {

                        runOnUiThread(new Runnable(){
                            public void run(){
                                PortForwardText.setText("");
                            }
                        });

                        int IPAddress = 4;
                        String IPaddressDefault = "192.168.4.1";
                        int PortNumberDefault = 60002;
                        String Data = DeviceNumberText.getText().toString() + IPAddress;


                        try{
                            //get a datagram socket
                            DatagramSocket socket = new DatagramSocket();
                            //send request
                            byte[] buf = new byte[256];
                            byte[] bufReceive = new byte[256];
                            buf = Data.getBytes();
                            InetAddress address = InetAddress.getByName(IPaddressDefault);
                            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, PortNumberDefault);
                            socket.send(packet);

                            //get response
                            packet = new DatagramPacket(bufReceive, bufReceive.length);
                            socket.receive(packet);
                            // display response
                            final String received = new String(packet.getData(), 0, packet.getLength());

                            runOnUiThread(new Runnable(){
                                public void run(){
                                    PortForwardText.setText("Port Forward: " + received);
                                }
                            });
                            socket.close();
                        }catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                };
                Thread myThread = new Thread(r);
                myThread.start();
                //////////////////////////////////////////////////////////////////////////////////////////
            }


            String action = intent.getAction();
            if(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                List<ScanResult> wifiScanList = wifi.getScanResults();
                wifis = new String[wifiScanList.size()];
                for (int i = 0; i < wifiScanList.size(); i++) {//scans for all available access points
                    wifis[i] = ((wifiScanList.get(i)).toString());
                }
                //lv.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, wifis));
                //Toast.makeText(getApplicationContext(),"scanning for wifi", Toast.LENGTH_LONG).show();

            }
        }
    }

    private boolean isConnectedViaWifi() {//method that checks if wifi is connected
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnected();
    }



    public void SetPasswordButtonClick(View view) {

        /////////////////////This part of the code connects to the esp8266 access point//////////////////////////////////////////////
        /*wifi=(WifiManager)getSystemService(Context.WIFI_SERVICE);
        wifiReceiver = new WifiScanReceiver();
        WifiConfig = new WifiConfiguration();
*/
        //adds esp ssid and password to config
        WifiConfig.SSID = "\"" + EspNetworkSSID + "\"";
        WifiConfig.preSharedKey = "\"" + EspNetworkPass + "\"";
        wifi.addNetwork(WifiConfig);

        //registerReceiver(wifiReceiver, intentFilter);//registers receiver and enables onReceive method

        List<WifiConfiguration> list = wifi.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + EspNetworkSSID + "\"")) {
                wifi.disconnect();
                wifi.enableNetwork(i.networkId, true);
                wifi.reconnect();
                SetPasswordFlag=1;
                break;
            }
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    public void IPAddressForwardButtonClick(View view) {
        /////////////////////This part of the code connects to the esp8266 access point//////////////////////////////////////////////
        /*wifi=(WifiManager)getSystemService(Context.WIFI_SERVICE);
        wifiReceiver = new WifiScanReceiver();
        WifiConfig = new WifiConfiguration();
*/
        //adds esp ssid and password to config
        WifiConfig.SSID = "\"" + EspNetworkSSID + "\"";
        WifiConfig.preSharedKey = "\"" + EspNetworkPass + "\"";
        wifi.addNetwork(WifiConfig);

        //registerReceiver(wifiReceiver, intentFilter);//registers receiver and enables onReceive method

        List<WifiConfiguration> list = wifi.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + EspNetworkSSID + "\"")) {
                wifi.disconnect();
                wifi.enableNetwork(i.networkId, true);
                wifi.reconnect();
                SetIPAddressForwardFlag=1;
                break;
            }
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    public void OnButtonClick(View view) {
        Runnable r = new Runnable() {
            @Override
            public void run() {

                runOnUiThread(new Runnable(){
                    public void run(){
                        CurrentStatusText.setText("");
                    }
                });

                int On = 1;
                IPaddressString = IPaddressText.getText().toString();
                PortNumberInt = Integer.parseInt(PortNumberText.getText().toString());
                String Data = DeviceNumberText.getText().toString() + On;


                try{
                    //get a datagram socket
                    DatagramSocket socket = new DatagramSocket();
                    //send request
                    byte[] buf = new byte[256];
                    byte[] bufReceive = new byte[256];
                    buf = Data.getBytes();
                    InetAddress address = InetAddress.getByName(IPaddressString);
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, address, PortNumberInt);
                    socket.send(packet);

                    //get response
                    packet = new DatagramPacket(bufReceive, bufReceive.length);
                    socket.receive(packet);
                    // display response
                    final String received = new String(packet.getData(), 0, packet.getLength());

                    runOnUiThread(new Runnable(){
                        public void run(){
                            CurrentStatusText.setText("Data Received: " + received);
                        }
                    });
                    socket.close();
                }catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };
        Thread myThread = new Thread(r);
        myThread.start();
    }

    public void OffButtonClick(View view) {
        Runnable r = new Runnable() {
            @Override
            public void run() {

                runOnUiThread(new Runnable(){
                    public void run(){
                        CurrentStatusText.setText("");
                    }
                });

                int Off = 2;
                IPaddressString = IPaddressText.getText().toString();
                PortNumberInt = Integer.parseInt(PortNumberText.getText().toString());
                String Data = DeviceNumberText.getText().toString() + Off;


                try{
                    //get a datagram socket
                    DatagramSocket socket = new DatagramSocket();
                    //send request
                    byte[] buf = new byte[256];
                    byte[] bufReceive = new byte[256];
                    buf = Data.getBytes();
                    InetAddress address = InetAddress.getByName(IPaddressString);
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, address, PortNumberInt);
                    socket.send(packet);

                    //get response
                    packet = new DatagramPacket(bufReceive, bufReceive.length);
                    socket.receive(packet);
                    // display response
                    final String received = new String(packet.getData(), 0, packet.getLength());

                    runOnUiThread(new Runnable(){
                        public void run(){
                            CurrentStatusText.setText("Data Received: " + received);
                        }
                    });
                    socket.close();
                }catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };
        Thread myThread = new Thread(r);
        myThread.start();
    }

    public void StatusButtonClick(View view) {
        Runnable r = new Runnable() {
            @Override
            public void run() {

                runOnUiThread(new Runnable(){
                    public void run(){
                        CurrentStatusText.setText("");
                    }
                });

                int Status = 3;
                IPaddressString = IPaddressText.getText().toString();
                PortNumberInt = Integer.parseInt(PortNumberText.getText().toString());
                String Data = DeviceNumberText.getText().toString() + Status;


                try{
                    //get a datagram socket
                    DatagramSocket socket = new DatagramSocket();
                    //send request
                    byte[] buf = new byte[256];
                    byte[] bufReceive = new byte[256];
                    buf = Data.getBytes();
                    InetAddress address = InetAddress.getByName(IPaddressString);
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, address, PortNumberInt);
                    socket.send(packet);

                    //get response
                    packet = new DatagramPacket(bufReceive, bufReceive.length);
                    socket.receive(packet);
                    // display response
                    final String received = new String(packet.getData(), 0, packet.getLength());

                    runOnUiThread(new Runnable(){
                        public void run(){
                            CurrentStatusText.setText("Data Received: " + received);
                        }
                    });
                    socket.close();
                }catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };
        Thread myThread = new Thread(r);
        myThread.start();
    }
}
