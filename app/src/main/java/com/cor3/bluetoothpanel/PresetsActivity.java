package com.cor3.bluetoothpanel;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.ArrayMap;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

public class PresetsActivity extends Activity {

    private Menu mOptionsMenu;
    private ActionBar mActionBar;

    private Preset mNewPresetValues;
    private Intent mIntent;

    private ListView mListView;
    private ArrayAdapter<Pair<String, Preset>> mListAdapter;
    private View mEmptyView;

    private SharedPreferences mPreferences;
    public static final int REQUEST_ADD_PRESET = 1969;
    public static final int REQUEST_SHOW_PRESETS = 1970;

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_presets);

        this.mActionBar = this.getActionBar();
        if(this.mActionBar != null)
        {
            this.mActionBar.setDisplayHomeAsUpEnabled(true);
            //this.mActionBar.setDisplayShowHomeEnabled(true);
        }

        this.mListView = (ListView)this.findViewById(android.R.id.list);
        this.mEmptyView = (View) this.findViewById(android.R.id.empty);

        this.mListAdapter = new ArrayAdapter<Pair<String, Preset>>(this, android.R.layout.simple_selectable_list_item) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if(convertView == null) {
                    convertView = PresetsActivity.this.getLayoutInflater().inflate(android.R.layout.simple_selectable_list_item, parent, false);
                }
                Pair<String, Preset> item = this.getItem(position);
                if(item != null)
                {
                    convertView.setTag(item);
                    TextView textView = (TextView)convertView.findViewById(android.R.id.text1);
                    textView.setText(item.first);
                }
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PresetsActivity.this.onListItemClicked(view);
                    }
                });
                return convertView;
            }
        };

        this.mListView.setAdapter(this.mListAdapter);

        // load presets from json stored in preferences
        this.populateListAdapter();


        mIntent = getIntent();
        if(mIntent.hasExtra("newPresetValues"))
        {
            mNewPresetValues = Preset.fromBundle(mIntent.getBundleExtra("newPresetValues"));
        }


    }

    public void onListItemClicked(View item)
    {
        Pair<String, Preset> name_and_preset = (Pair<String, Preset>)(item.getTag());
        if(name_and_preset != null)
        {
            mIntent.putExtra("chosenPreset", name_and_preset.second.toBundle());
            mIntent.putExtra("chosenPresetName", name_and_preset.first);
            setResult(RESULT_OK, mIntent);
            finish();
        }
    }

    private void populateListAdapter() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        String presetData = preferences.getString("presets", null);
        ArrayMap<String, Preset> presets = getPresetsMapFromJSON(presetData);
        if(presets != null) {
            this.mListAdapter.clear();
            for (ArrayMap.Entry<String, Preset> it : presets.entrySet()) {
                this.mListAdapter.add(new Pair<String, Preset>(it.getKey(), it.getValue()));
            }
            this.mListAdapter.notifyDataSetChanged();
        }

        if(this.mListAdapter.isEmpty())
        {
            this.mListView.setVisibility(View.GONE);
            this.mEmptyView.setVisibility(View.VISIBLE);
        } else {
            this.mEmptyView.setVisibility(View.GONE);
            this.mListView.setVisibility(View.VISIBLE);
        }
    }

    private void addPresetToPreferences(String name, Preset preset) {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        String presetData = preferences.getString("presets", null);
        ArrayMap<String, Preset> presets = getPresetsMapFromJSON(presetData);
        if (presets == null) {
            presets = new ArrayMap<>();
        }
        presets.put(name, preset);
        JSONObject newJSON = getJSONFromPresetMap(presets);
        preferences.edit().putString("presets", newJSON.toString()).apply();
    }

    private static ArrayMap<String, Preset> getPresetsMapFromJSON(String jsonData) {
        ArrayMap<String, Preset> presets = null;
        if(jsonData != null)
        {
            // ArrayMap<String, ArrayMap<String, String>> data;
            JSONObject presetDict = null;
            try { presetDict = new JSONObject(jsonData); } catch (JSONException e) { presetDict = null; }
            if(presetDict != null) {
                presets = getPresetsMapFromJSON(presetDict);

            }
        }
        return presets;
    }

    private static ArrayMap<String, Preset> getPresetsMapFromJSON(JSONObject jsonObject) {
        ArrayMap<String, Preset> presets = new ArrayMap<>();
        for (Iterator<String> presetDictIter = jsonObject.keys(); presetDictIter.hasNext(); ) {
            String presetDictKey = presetDictIter.next();
            JSONObject presetDictValue = null;
            try { presetDictValue = jsonObject.getJSONObject(presetDictKey); } catch (JSONException e) { continue; }
            Preset preset = getPresetFromJSON(presetDictValue);
            presets.put(presetDictKey, preset);
        }
        return presets;
    }

    private static Preset getPresetFromJSON(JSONObject jsonObject) {
        Preset preset = new Preset();
        for(Iterator<String> presetDictValueIter = jsonObject.keys(); presetDictValueIter.hasNext(); ) {
            String presetDictValueKey = presetDictValueIter.next();
            String presetDictValueValue = null;
            try { presetDictValueValue = jsonObject.getString(presetDictValueKey); } catch (JSONException e) { continue; }
            preset.put(presetDictValueKey, presetDictValueValue);
        }
        return preset;
    }

    private static JSONObject getJSONFromPreset(Preset preset)
    {
        JSONObject jsonObject = new JSONObject();
        if(preset != null)
        {
            for(ArrayMap.Entry<String, String> it : preset.entrySet())
            {
                try {
                    jsonObject.put(it.getKey(), it.getValue());
                } catch (JSONException e) {
                    continue;
                }
            }
        }
        return jsonObject;
    }

    private static JSONObject getJSONFromPresetMap(Map<String, Preset> presets)
    {
        JSONObject jsonObject = new JSONObject();
        if(presets != null)
        {
            for(Map.Entry<String, Preset> it : presets.entrySet())
            {
                try {
                    jsonObject.put(it.getKey(), getJSONFromPreset(it.getValue()));
                } catch (JSONException e) {
                    continue;
                }
            }
        }
        return jsonObject;
    }

    @Override
    public void onBackPressed() {
        this.setResult(Activity.RESULT_CANCELED);
        this.finish();
        super.onBackPressed();
    }

    @Override
    public final boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        this.mOptionsMenu = menu;
        this.getMenuInflater().inflate(R.menu.activity_presets_options_menu, menu);

        // get intent, and enable or disable the "Add Preset" action depending on whether the
        // parent activity sent us newPresetValues to use.
        MenuItem addMenuItem = this.mOptionsMenu.findItem(R.id.activity_presets_add);
        if (addMenuItem != null) {
            addMenuItem.setEnabled(mNewPresetValues != null);
        }

        return true;
    }

    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.activity_presets_add) {
            showAddPresetDialog();
        } else if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void showAddPresetDialog() {
        final String[] dialogText = new String[1];

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Title");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PresetsActivity.this.addPresetToPreferences(input.getText().toString(), PresetsActivity.this.mNewPresetValues);
                dialog.dismiss();
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }
}
