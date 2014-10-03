/*
 * Copyright (c) 2014 Adam Kruger
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.adamkruger.myipaddressinfo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class NetworkInfoFragment extends Fragment {

    private TableLayout mNetworkInfoTableLayout;

    public NetworkInfoFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_network_info, container, false);
        mNetworkInfoTableLayout = (TableLayout) rootView.findViewById(R.id.networkInfoTableLayout);
        populateInfo();
        return rootView;
    }

    public void populateInfo() {
        Context context = getActivity();
        mNetworkInfoTableLayout.removeAllViewsInLayout();

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo != null) {
            String state = networkInfo.getState().toString();
            String detailedState = networkInfo.getDetailedState().toString();
            String reason = networkInfo.getReason();
            if (detailedState.compareToIgnoreCase(state) == 0) {
                detailedState = "";
            }
            if (reason == null || reason.compareToIgnoreCase(state) == 0 || reason.compareToIgnoreCase(detailedState) == 0) {
                reason = "";
            }
            addTableRow(context, mNetworkInfoTableLayout, networkInfo.getTypeName(), state);
            addTableRow(context, mNetworkInfoTableLayout, networkInfo.getSubtypeName(), detailedState);
            addTableRow(context, mNetworkInfoTableLayout, networkInfo.getExtraInfo(), reason);
        }
    }

    private void addTableRow(Context context, TableLayout tableLayout, String label, String value) {
        if (label == null) {
            label = "";
        }
        if (value == null) {
            value = "";
        }
        if (label.length() > 0 || value.length() > 0) {
            tableLayout.addView(makeTableRow(context, label, value));
        }
    }

    private TableRow makeTableRow(Context context, String label, String value) {
        TableRow tableRow = new TableRow(context);
        tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));

        int textPadding = (int) (getResources().getDisplayMetrics().density
                * getResources().getDimension(R.dimen.label_value_padding) + 0.5f);

        TextView labelTextView = new TextView(context);
        labelTextView.setText(label);
        labelTextView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT));
        labelTextView.setTextAppearance(context, android.R.attr.textAppearanceSmall);
        labelTextView.setTextColor(getResources().getColor(R.color.dark_text_color));
        labelTextView.setPadding(0, 0, textPadding, 0);

        TextView valueTextView = new TextView(context);
        valueTextView.setText(value);
        valueTextView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT));
        valueTextView.setTextAppearance(context, android.R.attr.textAppearanceSmall);
        valueTextView.setTextColor(getResources().getColor(R.color.dark_text_color));
        valueTextView.setPadding(textPadding, 0, 0, 0);

        tableRow.addView(labelTextView);
        tableRow.addView(valueTextView);
        return tableRow;
    }
}
