package com.cor3.bluetoothpanel;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends ListActivity 
{
    private static final String TAG = "MainActivity";

    private ParameterListAdapter params = null;
    private BluetoothSerialThread conn = null;
    private RateLimitedTaggedQueue<String, String> taggedQueue = null;
    private SharedPreferences prefs = null;
    private Menu optionsMenu = null;
    private Preset mApplyPreset = null;

    private ProgressBar mEmptyProgressBar;

    private ImageView mEmptyErrorGraphic;

    private TextView mEmptyErrorText;

    private TextView mEmptyText;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.optionsMenu = menu;
        this.getMenuInflater().inflate(R.menu.mainactivitymenu, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_load)
        {
            this.taggedQueue.add("load", "load\r\n");
            return true;
        }
        else if(item.getItemId() == R.id.menu_save)
        {
            this.taggedQueue.add("save", "save\r\n");
            return true;
        }
        else if(item.getItemId() == R.id.menu_reset)
        {
            this.taggedQueue.add("reset", "reset\r\n");
            return true;
        }
        else if(item.getItemId() == R.id.menu_choose_device)
        {
            Intent deviceChooserIntent = new Intent(this, BluetoothDeviceListActivity.class);
            startActivityForResult(deviceChooserIntent, BluetoothDeviceListActivity.REQUEST_CHOOSE_DEVICE);
            return true;
        }
        else if(item.getItemId() == R.id.menu_show_presets)
        {
            Intent presetChooserIntent = new Intent(this, PresetsActivity.class);
            Preset newPresetValues = new Preset(params.getItems());
            presetChooserIntent.putExtra("newPresetValues", newPresetValues.toBundle());
            startActivityForResult(presetChooserIntent, PresetsActivity.REQUEST_SHOW_PRESETS);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main);
        this.mEmptyProgressBar = (ProgressBar)this.findViewById(R.id.empty_progressbar);
        this.mEmptyErrorGraphic = (ImageView)this.findViewById(R.id.empty_errorgraphic);
        this.mEmptyText = (TextView)this.findViewById(R.id.empty_text);
        
        this.prefs = this.getPreferences(Context.MODE_APPEND);
        
        this.conn = null;
        
        this.taggedQueue = new RateLimitedTaggedQueue<String, String>(0.1f) {
            @Override
            protected void handle(String msg) {
                if (MainActivity.this.conn != null)
                {
                    MainActivity.this.conn.write(msg);
                }
            }
        };
        
        this.params = new ParameterListAdapter(this, MainActivity.this.getLayoutInflater()) {
            @Override
            public void onParameterChanged(Parameter p)
            {
              if(p.kind == Parameter.FLOAT_PARAM)
                  MainActivity.this.taggedQueue.add(p.name, String.format("set %s %f\r\n", p.name, p.val));
              else
                  MainActivity.this.taggedQueue.add(p.name, String.format("set %s %d\r\n", p.name, (int)p.val));
            }
        };
        
        this.setListAdapter(this.params);
    }

    public void setEmptyContent(boolean isErr, String msg)
    {
        if(msg != null)
            mEmptyText.setText(msg);
        mEmptyText.setVisibility((msg == null) ? View.GONE : View.VISIBLE);
        mEmptyErrorGraphic.setVisibility(isErr ? View.VISIBLE : View.GONE);
        mEmptyProgressBar.setVisibility((!isErr) ? View.VISIBLE : View.GONE);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == BluetoothDeviceListActivity.REQUEST_CHOOSE_DEVICE)
        {
            if(resultCode == RESULT_OK)
            {
                String address = data.getStringExtra("address");
                if(address != null)
                {
                    this.prefs.edit().putString("btaddress", address).commit();
                }
            }
        }
        else if(requestCode == PresetsActivity.REQUEST_SHOW_PRESETS)
        {
            if(resultCode == RESULT_OK) {
                if (data.hasExtra("chosenPreset")) {
                    Preset preset = Preset.fromBundle(data.getBundleExtra("chosenPreset"));
                    if (preset != null) {
                        mApplyPreset = preset;
                        this.setEmptyContent(false, getResources().getString(R.string.applying_preset_msg));
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startBluetoothConnectionThread()
    {
        // "00:06:66:49:58:FE"
        String address = this.prefs.getString("btaddress", null);
        if(address == null)
        {

        }
        else
        {
            this.conn = new BluetoothSerialThread(address) {

                private boolean listing = false;
                private boolean need_list = true;

                @Override
                protected void onDisconnected()
                {
                    this.need_list = true;
                    if(MainActivity.this.params != null)
                        MainActivity.this.params.clear();
                    this.listing = false;
                    super.onDisconnected();
                }

                @Override
                protected void onConnected()
                {
                    this.need_list = true;
                    //write("list\r\n");
                }

                @Override
                protected void onConnecting()
                {
                    this.need_list = true;
                    //params.clear();
                }

                @Override
                protected void onRead(String line)
                {
                    line = line.replaceAll("\r", "");
                    Log.i(MainActivity.TAG, "...onRead('"+line+"')");
                    if(this.need_list) {
                        MainActivity.this.taggedQueue.add("list", "list\r\n");
                        this.need_list = false;
                    }
                    //Toast.makeText(MainActivity.this, line, Toast.LENGTH_LONG).show();
                    if(Objects.equals(line, "begin"))
                    {
                        this.listing = true;
                    }
                    else if(Objects.equals(line, "end")) {
                        this.listing = false;
                        MainActivity.this.onParameterList();
                    }
                    else if(line.startsWith("!"))
                    {
                        Toast.makeText(MainActivity.this, line, Toast.LENGTH_LONG).show();
                    }
                    else if(this.listing)
                    {
                        Parameter p = Parameter.parse(line);
                        if(p != null)
                        {
                            MainActivity.this.params.add(p);
                        }
                    }

                }

            };
            this.conn.start();
        }
    }

    protected void onParameterList()
    {
        if(this.mApplyPreset != null)
        {
            this.mApplyPreset.applyTo(this.params);
            Toast.makeText(this, "Preset Applied", Toast.LENGTH_LONG).show();
            this.mApplyPreset = null;
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        if(this.conn != null)
        {
            this.conn.close();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        this.startBluetoothConnectionThread();
    }
    
    
}
