package com.cor3.bluetoothpanel;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.Log;

import java.util.AbstractMap;
import java.util.Set;

/**
 * Created by Victor Condino on 7/1/2016.
 */
public class Preset extends AbstractMap<String, String> implements Parcelable {
    private ArrayMap<String, String> mValues;

    public Preset()
    {
        mValues = new ArrayMap<>();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return mValues.entrySet();
    }

    public Preset(Iterable<Parameter> from)
    {
        this();
        for(Parameter p : from)
        {
            if(p.kind == Parameter.FLOAT_PARAM)
                mValues.put(p.name, String.format("%f", p.val));
            else if(p.kind == Parameter.INT_PARAM)
                mValues.put(p.name, String.format("%d", (int)p.val));
            else if (p.kind == Parameter.BOOL_PARAM)
                mValues.put(p.name, String.format("%d", (p.val != 0.0f) ? 1 : 0));
            else if (p.kind == Parameter.NULL_PARAM)
                mValues.put(p.name, "null");
            else
                Log.e("Preset", "Error in Iterable<Parameter> constructor... invalid Parameter.kind for parameter '"+p.name+"'!");
        }
    }

    public static Preset createFrom(ParameterListAdapter src)
    {
        return new Preset(src.getItems());
    }

    public void applyTo(ParameterListAdapter dest)
    {
        boolean need_notify = false;
        for (ArrayMap.Entry<String, String> entry : mValues.entrySet())
        {
            int destIndex = dest.getIndex(entry.getKey());
            if(destIndex >= 0)
            {
                Parameter destItem = (Parameter) dest.getItem(destIndex);
                if(destItem.setValue(entry.getValue())) {
                    need_notify = true;
                    dest.onParameterChanged(destItem);
                }
            }
        }
        if(need_notify)
            dest.notifyDataSetChanged();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        Bundle b = this.toBundle();
        out.writeBundle(b);
    }

    public static final Parcelable.Creator<Preset> CREATOR = new Parcelable.Creator<Preset>() {
        public Preset createFromParcel(Parcel in) {
            return Preset.fromBundle(in.readBundle());
    }
        public Preset[] newArray(int size) {
            return new Preset[size];
        }
    };

    public static Preset fromBundle(Bundle b) {
        if(b == null) {
            return null;
        }
        final Preset preset = new Preset();
        for(final String k : b.keySet())
        {
            if(b.getString(k, "null") != "null")
                preset.put(k, b.getString(k));
            else
                Log.e("Preset", "fromBundle("+b.toString()+"): cannot unbundle key '"+k+"'");
        }
        return preset;
    }

    public final Bundle toBundle()
    {
        Bundle b = new Bundle();
        for(ArrayMap.Entry<String, String> it : this.mValues.entrySet()) {
            b.putString(it.getKey(), it.getValue());
        }
        return b;
    }

    @Override
    public String put(String key, String value) {
        return mValues.put(key, value);
    }
}
