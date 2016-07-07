package com.cor3.bluetoothpanel;

import java.util.Objects;
import java.util.regex.Pattern;

public class Parameter
{
    public static final int FLOAT_PARAM = 1;
    public static final int INT_PARAM = 2;
    public static final int BOOL_PARAM = 3;
    public static final int NULL_PARAM = 0;

    public Parameter() {
        this.name = null;
        this.kind = Parameter.NULL_PARAM;
    }

    public Parameter(String name, int kind, float val, float def) {
        this.bounded = false;
        this.def = def;
        this.kind = kind;
        this.name = name;
        this.val = val;
    }

    public Parameter(String name, int kind, float val, float def, float min, float max) {
        this.bounded = true;
        this.def = def;
        this.kind = kind;
        this.max = max;
        this.min = min;
        this.name = name;
        this.val = val;
    }

    String name;
    int kind;
    boolean bounded;
    float min, max, def, val;

    public float getValue()
    {
        return val;
    }

    public String formatValue()
    {
        if(kind == FLOAT_PARAM)
            return String.format("%f", val);
        else if(kind == INT_PARAM)
            return String.format("%d", val);
        else if(kind == BOOL_PARAM)
            return String.format("%d", (val > 0f) ? 1f : 0f);
        else
            return "null";
    }

    public boolean setValue(String v)
    {
        float fv = 0f;
        try {
            fv = Float.parseFloat(v);
        } catch (NumberFormatException e) {
            return false;
        }
        return setValue(fv);
    }

    public boolean setValue(float fv)
    {
        if(kind == FLOAT_PARAM)
        {
            if (bounded)
                if (fv < min)
                    fv = min;
                else if (fv > max)
                    fv = max;
        }
        else if(kind == INT_PARAM)
        {
            fv = (float)Math.round(fv);
            if (bounded)
                if (fv < Math.round(min))
                    fv = Math.round(min);
                else if (fv > Math.round(max))
                    fv = Math.round(max);
        }
        else if(kind == BOOL_PARAM)
        {
                if (fv > 0f)
                    fv = 1f;
                else
                    fv = 0f;
        }
        else
        {
            return false;
        }
        val = fv;
        return true;
    }

    public static Parameter parse(String desc)
    {
        String[] words=Pattern.compile("\\s+").split(desc);
        Parameter p = new Parameter();

        // kind name min max def val
        if(words.length >= 4)
        {
            try {
                if(Objects.equals(words[0], "float"))
                    p.kind = Parameter.FLOAT_PARAM;
                else if(Objects.equals(words[0], "int"))
                    p.kind = Parameter.INT_PARAM;
                else if(Objects.equals(words[0], "bool"))
                    p.kind = Parameter.BOOL_PARAM;
                else
                    return null;

                p.name = words[1];
                if(words.length >= 6)
                {
                    p.bounded = true;
                    try {
                        p.min = Float.parseFloat(words[2]);
                        p.max = Float.parseFloat(words[3]);
                    } catch(NumberFormatException e) {
                        p.bounded = false;
                    }
                    p.def = Float.parseFloat(words[4]);
                    p.val = Float.parseFloat(words[5]);
                }
                else
                {
                    p.bounded = false;
                    p.def = Float.parseFloat(words[2]);
                    p.val = Float.parseFloat(words[3]);
                }
            } 
            catch(NumberFormatException e)
            {
                return null;
            }
            catch(IndexOutOfBoundsException e)
            {
                return null;
            }
            return p;
        }
        return null;
    }
}
