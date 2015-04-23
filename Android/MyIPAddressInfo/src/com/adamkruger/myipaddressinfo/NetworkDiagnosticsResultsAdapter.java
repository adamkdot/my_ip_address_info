/*
 * Copyright (c) 2015 Adam Kruger
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

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

class NetworkDiagnosticsResultsAdapter extends ArrayAdapter<NetworkDiagnosticsResult> {

    private final Context mContext;
    private final ArrayList<NetworkDiagnosticsResult> mNetworkDiagnosticsResultsArrayList;

    public NetworkDiagnosticsResultsAdapter(Context context, ArrayList<NetworkDiagnosticsResult> networkDiagnosticsResultsArrayList) {
        super(context, R.layout.network_diagnostics_result_row, networkDiagnosticsResultsArrayList);
        mContext = context;
        mNetworkDiagnosticsResultsArrayList = networkDiagnosticsResultsArrayList;
    }

    public static class RowViewHolder {
        public int mIndex;
        public TextView mTimestamp;
        public TextView mTitle;
        public TextView mSummary;
        public TextView mExpectedLength;
        public TextView mActualLength;
        public TextView mSpeed;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final RowViewHolder rowViewHolder;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.network_diagnostics_result_row, parent, false);

            rowViewHolder = new RowViewHolder();
            rowViewHolder.mIndex = position;
            rowViewHolder.mTimestamp =
                    (TextView)convertView.findViewById(R.id.network_diagnostics_result_row_timestamp);
            rowViewHolder.mTitle =
                    (TextView)convertView.findViewById(R.id.network_diagnostics_result_row_title);
            rowViewHolder.mSummary =
                    (TextView)convertView.findViewById(R.id.network_diagnostics_result_row_summary);
            rowViewHolder.mExpectedLength =
                    (TextView)convertView.findViewById(R.id.network_diagnostics_result_row_expected_length);
            rowViewHolder.mActualLength =
                    (TextView)convertView.findViewById(R.id.network_diagnostics_result_row_actual_length);
            rowViewHolder.mSpeed =
                    (TextView)convertView.findViewById(R.id.network_diagnostics_result_row_speed);

            convertView.setOnClickListener(new View.OnClickListener() {
                private void toggleVisibility(View v) {
                    if (v.getVisibility() == View.GONE) {
                        v.setVisibility(View.VISIBLE);
                    } else {
                        v.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onClick(View v) {
                    if (mNetworkDiagnosticsResultsArrayList.size() > 0) {
                        NetworkDiagnosticsResult result = mNetworkDiagnosticsResultsArrayList.get(rowViewHolder.mIndex);
                        result.toggleCollapsed();
                        toggleVisibility(rowViewHolder.mSummary);
                        // TODO: these don't match
                        //toggleVisibility(rowViewHolder.mExpectedLength);
                        //toggleVisibility(rowViewHolder.mActualLength);
                    }
                }
            });

            convertView.setTag(rowViewHolder);
        } else {
            rowViewHolder = (RowViewHolder)convertView.getTag();
        }

        if (mNetworkDiagnosticsResultsArrayList.size() > 0) {
            NetworkDiagnosticsResult result = mNetworkDiagnosticsResultsArrayList.get(position);
            rowViewHolder.mIndex = position;
            rowViewHolder.mTimestamp.setText(result.timestamp());
            rowViewHolder.mTitle.setText(result.title());
            rowViewHolder.mSummary.setText(result.summary());
            rowViewHolder.mExpectedLength.setText(result.formattedExpectedBytes());
            rowViewHolder.mActualLength.setText(result.formattedActualBytes());
            rowViewHolder.mSpeed.setText(result.formattedSpeedResult());

            // Collapsed by default
            rowViewHolder.mSummary.setVisibility(
                    result.getCollapsed() ? View.GONE : View.VISIBLE);
            rowViewHolder.mExpectedLength.setVisibility(View.GONE);
            rowViewHolder.mActualLength.setVisibility(View.GONE);
        }

        return convertView;
    }
}
