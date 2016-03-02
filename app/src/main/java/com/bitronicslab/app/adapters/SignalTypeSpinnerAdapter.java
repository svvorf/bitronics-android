package com.bitronicslab.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bitronicslab.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ivan on 2/27/16.
 */
public class SignalTypeSpinnerAdapter extends BaseAdapter {
    private Context context;
    private String[] mItems;

    public SignalTypeSpinnerAdapter(Context context, String[] mItems) {
        this.context = context;
        this.mItems = mItems;
    }

    @Override
    public int getCount() {
        return mItems.length;
    }

    @Override
    public Object getItem(int position) {
        return mItems[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getDropDownView(int position, View view, ViewGroup parent) {
        if (view == null || !view.getTag().toString().equals("DROPDOWN")) {
            view = LayoutInflater.from(context).inflate(R.layout.toolbar_spinner_item_dropdown, parent, false);
            view.setTag("DROPDOWN");
        }

        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        textView.setText(getTitle(position));

        return view;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null || !view.getTag().toString().equals("NOT_DROPDOWN")) {
            view = LayoutInflater.from(context).inflate(R.layout.
                    toolbar_spinner_item, parent, false);
            view.setTag("NOT_DROPDOWN");
        }
        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        textView.setText(getTitle(position));
        return view;
    }

    private String getTitle(int position) {
        return position >= 0 && position < mItems.length ? mItems[position] : "";
    }
}
