package com.cor3.bluetoothpanel;

import android.bluetooth.*;
import android.os.*;
import android.util.*;
import java.io.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;

public abstract class BluetoothSerialThread extends Thread
{
    protected final String TAG = "BluetoothSerialThread";

    public void retryNow()
    {
        if(this.retryWait)
        {
            this.handler.sendEmptyMessage(BluetoothSerialThread.RETRY_MESSAGE);
        }
    }

    public void retryAfter(long delayMs)
    {
        if(this.retryWait)
        {
            this.handler.sendEmptyMessageDelayed(BluetoothSerialThread.RETRY_MESSAGE, delayMs);
        }
    }

    protected void onDisconnected() {
        this.retryAfter(100);
    }

    protected void onNoBluetooth() {
        this.retryAfter(1000);
    }

    protected void onNoRemoteDevice() {
        this.retryAfter(250);
    }

    protected void onNoConnection() {
        this.retryAfter(500);
    }

    protected abstract void onConnected();
    protected abstract void onConnecting();
    protected abstract void onRead(String line);

    public BluetoothSerialThread(String address)
    {
        this.address = address;
        this.btAdapter = null;
        this.btDevice = null;
        this.btSocket = null;
        this.closing = false;
        this.closed = false;
        this.handler = new BluetoothSerialThread.MyHandler(this);
    }

    public void retry()
    {
    }

    /**
     * @brief  shut down the connection and stop the thread
     */

    public void close()
    {
        Log.i(this.TAG, "...entering close()...");
        this.closing = true;
        try
        {
            this.btSocket.close();
        } 
        catch (IOException e2) {
            Log.d(this.TAG, "IOException in ConnectedThread.close()... " + e2.getMessage() + ".");
        }
        try
        {
            synchronized(wait) { wait.notifyAll(); }
            this.join();
        }
        catch (InterruptedException e)
        {
            Log.w(this.TAG, "ConnectedThread.join() in ConnectedThread.close() interrupted...");
        }
        Log.i(this.TAG, "...exiting close()...");
        this.closed = true;
    }

    @Override
    public void run()
    {
        Log.i(this.TAG, "...entering run()...");
        this.retryWait = false;
        while(!this.closing)
        {
            if(this.retryWait)
            {
                synchronized(wait) {
                    try {
                        this.wait.wait();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "wait() in thread run() loop interrupted!", e);
                    }
                }
                continue;
            }

            this.btAdapter = BluetoothAdapter.getDefaultAdapter();

            if(this.btAdapter == null || !this.btAdapter.isEnabled())
            {
                Log.e(this.TAG, "...Bluetooth adapter OFF or not found...");
                this.retryWait = true;
                this.handler.obtainMessage(BluetoothSerialThread.NO_BLUETOOTH_MESSAGE).sendToTarget();
                continue;
            }

            // Set up a pointer to the remote node using it's address.
            this.btDevice = this.btAdapter.getRemoteDevice(this.address);

            if(this.btDevice == null)
            {
                Log.w(this.TAG, "...btAdapter.getRemoteDevice("+ this.address +") returned null...");
                this.retryWait = true;
                this.handler.obtainMessage(BluetoothSerialThread.NO_REMOTEDEVICE_MESSAGE).sendToTarget();
                continue;
            }

            this.handler.obtainMessage(BluetoothSerialThread.CONNECTING_MESSAGE).sendToTarget();

            // Two things are needed to make a connection:
            //   A MAC address, which we got above.
            //   A Service ID or UUID.  In this case we are using the
            //     UUID for SPP.

            try 
            {
                this.btSocket = null;
                if (Build.VERSION.SDK_INT >= 10)
                {
                    try
                    {
                        Method  m = this.btDevice.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
                        this.btSocket = (BluetoothSocket) m.invoke(this.btDevice, BluetoothSerialThread.MY_UUID);
                    }
                    catch (Exception e)
                    {
                        Log.e(this.TAG, "Could not create Insecure RFComm Connection", e);
                        //continue;
                    }
                }
                if(this.btSocket == null)
                {
                    this.btSocket = this.btDevice.createRfcommSocketToServiceRecord(BluetoothSerialThread.MY_UUID);
                }
            } 
            catch (IOException e) 
            {
                Log.w(this.TAG, "In onResume() and socket create failed: " + e.getMessage() + ".");
                this.retryWait = true;
                this.handler.obtainMessage(BluetoothSerialThread.NO_CONNECTION_MESSAGE).sendToTarget();
                continue;
            }

            // Discovery is resource intensive.  Make sure it isn't going on
            // when you attempt to connect and pass your message.
            this.btAdapter.cancelDiscovery();

            // Establish the connection.  This will block until it connects.
            Log.i(this.TAG, "...Connecting...");
            try
            {
                this.btSocket.connect();
                Log.i(this.TAG, "....Connection ok...");
            }
            catch (IOException e)
            {
                Log.e(this.TAG, "...Connection failure: btSocket.connect() raised...", e);
                try
                {
                    this.btSocket.close();
                }
                catch (IOException e2)
                {
                    Log.w(this.TAG, " unable to close socket during connection failure ", e2);
                }
                this.retryWait = true;
                this.handler.obtainMessage(BluetoothSerialThread.NO_CONNECTION_MESSAGE).sendToTarget();
                continue;
            }

            // Create a data stream so we can talk to server.
            Log.i(this.TAG, "...Create Socket...");

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try
            {
                tmpIn = this.btSocket.getInputStream();
                tmpOut = this.btSocket.getOutputStream();
            }
            catch (IOException e)
            {
                Log.e(this.TAG, "...Connection failure: btSocket.getInputStream() raised", e);
                try {
                    this.btSocket.close();
                } catch (IOException e2) {
                    Log.w(this.TAG, "unable to close socket during connection failure", e2);
                }
                this.retryWait = true;
                this.handler.obtainMessage(BluetoothSerialThread.NO_CONNECTION_MESSAGE).sendToTarget();
                continue;
            }

            this.mmInStream = tmpIn;
            this.mmOutStream = tmpOut;

            this.handler.obtainMessage(BluetoothSerialThread.CONNECTED_MESSAGE).sendToTarget();

            byte[] buffer = new byte[4096];  // buffer store for the stream
            int bytes; // bytes returned from read()
            StringBuilder sbuf = new StringBuilder();

            // Keep listening to the InputStream until an exception occurs
            while (true)
            {
                try
                {
                    // Read from the InputStream
                    bytes = this.mmInStream.read(buffer);    // Get number of bytes and message in "buffer"
                    String strb = new String(buffer, 0, bytes);         // create string from bytes array
                    sbuf.append(strb);                        // append string
                    int endOfLineIndex = sbuf.indexOf("\n");              // determine the end-of-line
                    while (endOfLineIndex >= 0)
                    {                       // if end-of-line,
                        String line = sbuf.substring(0, endOfLineIndex);        // extract string
                        sbuf.delete(0, endOfLineIndex + 1);                   // and clear
                        endOfLineIndex = sbuf.indexOf("\n");
                        this.handler.obtainMessage(BluetoothSerialThread.RECEIVE_MESSAGE, line.length(), -1, line.getBytes()).sendToTarget();    // Send to message queue Handler
                    }
                }
                catch (IOException e)
                {
                    Log.d(this.TAG, "Exiting read loop on exception", e);
                    break;
                }
            }

            Log.i(this.TAG, "At end of loop body in run()... closing socket");

            try
            {
                this.btSocket.close();
            }
            catch (IOException e)
            {
                Log.w(this.TAG, "Error in btSocket.close() at end of ConnectedThread.run() loop" + e.getMessage());
            }
            this.retryWait = true;
            this.handler.obtainMessage(BluetoothSerialThread.DISCONNECTED_MESSAGE).sendToTarget();
        }
    }

    /* Call this from the main activity to send data to the remote device */
    public boolean write(String message)
    {
        Log.i(this.TAG, "...Data to send: " + message + "...");
        byte[] msgBuffer = message.getBytes();
        try
        {
            this.mmOutStream.write(msgBuffer);
        }
        catch (IOException e)
        {
            Log.d(this.TAG, "...Error data send: " + e.getMessage() + "...");
            return false;
        }
        return true;
    }

    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private BluetoothSocket btSocket;
    private BluetoothDevice btDevice;
    private BluetoothAdapter btAdapter;
    private boolean closing, closed;
    private final String address;
    private final Handler handler;
    private final Object wait = new Object();
    private boolean retryWait = false;

    private static final int NO_CONNECTION_MESSAGE = 7;
    private static final int NO_BLUETOOTH_MESSAGE = 5;
    private static final int NO_REMOTEDEVICE_MESSAGE = 6;
    private static final int CONNECTING_MESSAGE = 4; // Trying to reestablish SPP connection   <------'
    private static final int CONNECTED_MESSAGE = 2;  // Established (or reestablished upon resume) SPP connection
    private static final int RECEIVE_MESSAGE = 1;    // Received a complete line of data over SPP connection
    private static final int DISCONNECTED_MESSAGE = 3; // Lost or shut down SPP connection. Triggers -.
    private static final int RETRY_MESSAGE = 10;

    private static class MyHandler extends Handler {
        private final WeakReference<BluetoothSerialThread> bluetoothSerialThreadWeakReference;

        public MyHandler(BluetoothSerialThread bluetoothSerialThread) {
            bluetoothSerialThreadWeakReference = new WeakReference<BluetoothSerialThread>(bluetoothSerialThread);
        }

        @Override
        public void handleMessage(Message msg)
        {
            BluetoothSerialThread bluetoothSerialThread = bluetoothSerialThreadWeakReference.get();
            if(bluetoothSerialThread != null)
            {
                switch (msg.what)
                {
                    case BluetoothSerialThread.NO_BLUETOOTH_MESSAGE:
                        Log.i(bluetoothSerialThread.TAG, "...NO_BLUETOOTH_MESSAGE...");
                        bluetoothSerialThread.onNoBluetooth();
                        break;

                    case BluetoothSerialThread.NO_REMOTEDEVICE_MESSAGE:
                        Log.i(bluetoothSerialThread.TAG, "...NO_REMOTEDEVICE_MESSAGE...");
                        bluetoothSerialThread.onNoRemoteDevice();
                        break;

                    case BluetoothSerialThread.NO_CONNECTION_MESSAGE:
                        Log.i(bluetoothSerialThread.TAG, "...NO_CONNECTION_MESSAGE...");
                        bluetoothSerialThread.onNoConnection();
                        break;

                    case BluetoothSerialThread.RETRY_MESSAGE:
                        Log.i(bluetoothSerialThread.TAG, "...RETRY_MESSAGE...");
                        if (bluetoothSerialThread.retryWait)
                        {
                            bluetoothSerialThread.retryWait = false;
                        }
                        synchronized (bluetoothSerialThread.wait)
                        {
                            bluetoothSerialThread.wait.notifyAll();
                        }
                        break;

                    case BluetoothSerialThread.RECEIVE_MESSAGE:                         // if receive massage
                        byte[] readBuf = (byte[]) msg.obj;
                        String line = new String(readBuf, 0, msg.arg1);         // create string from bytes array

                        Log.i(bluetoothSerialThread.TAG, "...RECEIVE_MESSAGE: '" + line + "'...");
                        bluetoothSerialThread.onRead(line);
                        break;

                    case BluetoothSerialThread.CONNECTED_MESSAGE:
                        Log.i(bluetoothSerialThread.TAG, "...CONNECTED_MESSAGE...");
                        bluetoothSerialThread.onConnected();
                        break;

                    case BluetoothSerialThread.DISCONNECTED_MESSAGE:
                        Log.i(bluetoothSerialThread.TAG, "...DISCONNECTED_MESSAGE...");
                        bluetoothSerialThread.onDisconnected();
                        break;

                    case BluetoothSerialThread.CONNECTING_MESSAGE:
                        Log.i(bluetoothSerialThread.TAG, "...CONNECTING_MESSAGE...");
                        bluetoothSerialThread.onConnecting();
                        break;
                }
            }
        }
    }
}
