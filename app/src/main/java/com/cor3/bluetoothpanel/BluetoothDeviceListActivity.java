package com.cor3.bluetoothpanel;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Victor Condino on 6/30/2016.
 */
public class BluetoothDeviceListActivity extends ListActivity  {

    private static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_CHOOSE_DEVICE = 2001;
    private static final String TAG = "BtDevListActivity";

    private BluetoothAdapter btAdapter;
    private ArrayList<BluetoothDevice> mDiscoveredDevices = new ArrayList<>();
    private BluetoothDeviceListAdapter mAdapter;
    private ParcelUuid mDesiredUUID = new ParcelUuid(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

    private class BluetoothDeviceListAdapter extends ArrayAdapter<BluetoothDevice> {
        public BluetoothDeviceListAdapter(Context context) {
            super(context, R.layout.bluetooth_device_list_item);
        }

        @Override
        public View getView(int position, View convertView, final ViewGroup parent) {
            //convertView = super.getView(position, convertView, parent);
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.bluetooth_device_list_item, parent, false);
            }
            BluetoothDevice dev = getItem(position);
            convertView.setTag(dev);
            TextView firstLine = (TextView) (convertView.findViewById(android.R.id.text1));
            TextView secondLine = (TextView) (convertView.findViewById(android.R.id.text2));
            firstLine.setText(dev.getName());
            secondLine.setText(dev.getAddress());
            convertView.setClickable(true);
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    BluetoothDevice dev = (BluetoothDevice)(view.getTag());
                    int i = BluetoothDeviceListAdapter.this.getPosition(dev);
                    long d = BluetoothDeviceListAdapter.this.getItemId(i);
                    BluetoothDeviceListActivity.this.onListItemClick((ListView)parent, view, i, d);
                }
            });
            return convertView;
        }
    }

    @Override
    public void onListItemClick(ListView list, View view, int i, long l) {
        BluetoothDevice dev = (BluetoothDevice)(view.getTag());
        Intent intent = getIntent();
        intent.putExtra("address", dev.getAddress());
        setResult(RESULT_OK, intent);
        finish();
        // super.onListItemClick(l, v, position, id);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(android.R.layout.list_content);

        //Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_UUID);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(ActionFoundReceiver, filter); // Don't forget to unregister during onDestroy

        // Getting the Bluetooth adapter
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        //out.append("\nAdapter: " + btAdapter);

        mAdapter = new BluetoothDeviceListAdapter(this);
        setListAdapter(mAdapter);

        CheckBTState();
    }

    /* This routine is called when an activity completes.*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            CheckBTState();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (btAdapter != null) {
            btAdapter.cancelDiscovery();
        }
        unregisterReceiver(ActionFoundReceiver);
    }

    private void CheckBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // If it isn't request to turn it on
        // List paired devices
        // Emulator doesn't support Bluetooth and will return null
        if (btAdapter == null) {
            //out.append("\nBluetooth NOT supported. Aborting.");
            return;
        } else {
            if (btAdapter.isEnabled()) {
                for (BluetoothDevice dev : btAdapter.getBondedDevices()) {
                    for (ParcelUuid uuid : dev.getUuids()) {
                        if (uuid.equals(mDesiredUUID)) {
                            mAdapter.add(dev);
                        }
                    }
                }
                //out.append("\nBluetooth is enabled...");

                // Starting the device discovery
                btAdapter.startDiscovery();
            } else {
                android.content.Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }


    private final BroadcastReceiver ActionFoundReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //out.append("\n  Device: " + device.getName() + ", " + device);
                mDiscoveredDevices.add(device);
                if(!device.fetchUuidsWithSdp())
                {
                    Log.w(BluetoothDeviceListActivity.TAG, "BluetoothDevice[" + device.getName() + "].fetchUuidsWithSdp() failed!");
                }
            } else if (BluetoothDevice.ACTION_UUID.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                for (int i = 0; i < uuidExtra.length; i++) {
                    //out.append("\n  Device: " + device.getName() + ", " + device + ", Service: " + uuidExtra[i].toString());
                    if (uuidExtra[i].equals(mDesiredUUID)) {
                        mAdapter.add(device);
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //out.append("\nDiscovery Started...");
                mDiscoveredDevices.clear();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //out.append("\nDiscovery Finished");

                //for (BluetoothDevice device : mDiscoveredDevices) {
                    // Get Services for paired devices


                    //out.append("\nGetting Services for " + device.getName() + ", " + device);
                    //if (!device.fetchUuidsWithSdp()) {

                        //out.append("\nSDP Failed for " + device.getName());
                    //}

                //}


            }
        }
    };

}
