package com.cor3.bluetoothpanel;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.cor3.bluetoothpanel.R.id;
import com.cor3.bluetoothpanel.R.layout;

import java.util.ArrayList;
import java.util.Collection;

public abstract class ParameterListAdapter extends BaseAdapter
{
    private Context context;
    private LayoutInflater inflater;
    private ArrayList<Parameter> items;

    public ParameterListAdapter(Context c, LayoutInflater layoutInflater)
    {
        this.items = new ArrayList<Parameter>();
        this.context = c;
        this.inflater = layoutInflater; //(LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    
    public int getIndex(String name)
    {
        for(int i = 0; i < this.items.size(); i ++)
        {
            Parameter p = this.items.get(i);
            if(p.name.equals(name))
                return i;
        }
        return -1;
    }

    public boolean add(Parameter p)
    {
        if(p == null || p.name.equals(""))
            return false;
        int i = this.getIndex(p.name);
        if(i >= 0)
        {
            this.items.set(i, p);
            this.notifyDataSetChanged();
        }
        else {
            this.items.add(p);
            this.notifyDataSetChanged();
        }
        return false;
    }
    
    public void clear()
    {
        this.items.clear();
        this.notifyDataSetChanged();
    }
    
    public void replaceAll(Collection<? extends Parameter> with)
    {
        this.items.clear();
        this.items.addAll(with);
        this.notifyDataSetChanged();
    }
    
    @Override
    public int getViewTypeCount()
    {
        return 3;
    }
    
    @Override
    public int getCount()
    {
        return this.items.size();
    }

    @Override
    public Object getItem(int p1)
    {
        try {
            return this.items.get(p1);
        }
        catch(IndexOutOfBoundsException e)
        {}
        return null;
    }

    @Override
    public long getItemId(int p1)
    {
        return p1;
    }

    @Override
    public int getItemViewType(int position)
    {
        Parameter param = (Parameter) this.getItem(position);
        if(param != null)
            return param.kind - 1;
        return 0;
    }
    
    public abstract void onParameterChanged(Parameter p);

    @Override
    public View getView(int p1, View p2, ViewGroup p3)
    {
        Log.i("ParameterListAdapter", "in getView(" + String.format("%d", p1) + ")");
        Parameter param = (Parameter) this.getItem(p1);
        Log.i("ParameterListAdapter", "in getView(" + String.format("%d", p1) + "): param.name is '" + param.name + "'");

        if(p2 == null)
        {
            if(param.kind == Parameter.FLOAT_PARAM)
                p2 = this.inflater.inflate(layout.float_param_list_item, p3, false);
            else if(param.kind == Parameter.INT_PARAM)
                p2 = this.inflater.inflate(layout.int_param_list_item, p3, false);
            else if(param.kind == Parameter.BOOL_PARAM)
                p2 = this.inflater.inflate(layout.bool_param_list_item, p3, false);
            else
                p2 = null;
        }
        else
        {
            p2.setTag(R.id.tag_param, null);
        }
        
        View v = p2;
        
        if(param.kind == Parameter.FLOAT_PARAM ||
           param.kind == Parameter.INT_PARAM)
        {
            TextView nameTextView = (TextView) p2.findViewById(id.param_name);
            TextView valueTextView = (TextView) p2.findViewById(id.param_value);
            SeekBar valueControl = (SeekBar) p2.findViewById(id.param_control);

            valueControl.setOnSeekBarChangeListener(null);

            valueControl.setTag(R.id.tag_param, param);
            valueControl.setTag(R.id.tag_listadapter, this);
            valueControl.setTag(R.id.tag_value_textview, valueTextView);

            nameTextView.setText(param.name);
            if(param.kind == Parameter.INT_PARAM) {
                valueTextView.setText(String.format("%d", (int)param.val));
            }
            else {
                valueTextView.setText(String.format("%f", param.val));
            }
            valueControl.setMax(1000);
            float val = param.val;
            if(param.kind == Parameter.INT_PARAM) {
                val = Math.round(val);
            }
            float pval = (param.val - param.min) / (param.max - param.min);
            valueControl.setProgress(Math.round(pval * 1000.0f));
            valueControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            
                    @Override
                    public void onProgressChanged(SeekBar p1, int p2, boolean p3)
                    {
                        Parameter p = (Parameter)p1.getTag(R.id.tag_param);
                        ParameterListAdapter pla = (ParameterListAdapter)p1.getTag(R.id.tag_listadapter);
                        TextView vtv = (TextView)p1.getTag(R.id.tag_value_textview);

                        float value = (float) p1.getProgress() / (float) p1.getMax();
                        value = value * (p.max - p.min) + p.min;
                        if(p.kind == Parameter.INT_PARAM)
                            value = Math.round(value);

                        p.setValue(value);

                        if (p.kind == Parameter.INT_PARAM)
                            vtv.setText(String.format("%d", (int)p.val));
                        else
                            vtv.setText(String.format("%f", p.val));

                        if(p3)
                            pla.onParameterChanged(p);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar p1)
                    {
                        Parameter p = (Parameter) p1.getTag(R.id.tag_param);
                        ParameterListAdapter pla = (ParameterListAdapter) p1.getTag(R.id.tag_listadapter);

                        //pla.tracking.add(p.name);
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar p1)
                    {
                        Parameter p = (Parameter) p1.getTag(R.id.tag_param);
                        ParameterListAdapter pla = (ParameterListAdapter) p1.getTag(R.id.tag_listadapter);

                        onProgressChanged(p1, p1.getProgress(), true);
                    }
            });
        }
        else if(param.kind == Parameter.BOOL_PARAM)
        {
            TextView nameTextView = (TextView) p2.findViewById(id.param_name);
            Switch valueControl = (Switch) p2.findViewById(id.param_control);

            valueControl.setTag(R.id.tag_param, param);
            valueControl.setTag(R.id.tag_listadapter, this);

            nameTextView.setText(param.name);
            valueControl.setChecked(param.val != 0.0f);
            valueControl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton p1, boolean p2)
                    {
                        Parameter p = (Parameter)p1.getTag(id.tag_param);
                        ParameterListAdapter pla = (ParameterListAdapter)p1.getTag(id.tag_listadapter);

                        float value = p2 ? 1.0f : 0.0f;
                        p.setValue(value);
                        pla.onParameterChanged(p);
                    }
                   
            });
        }
        
        return v;
    }


    public ArrayList<Parameter> getItems() {
        return items;
    }
}
