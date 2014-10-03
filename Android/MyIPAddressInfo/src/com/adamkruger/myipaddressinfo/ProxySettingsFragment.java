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

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

public class ProxySettingsFragment extends Fragment implements OnCheckedChangeListener {

    private int HTTP_PROXY_PORT_INVALID = 0;
    private int HTTP_PROXY_PORT_TOR = 8118;
    private int HTTP_PROXY_PORT_PSIPHON = 8080;

    private RadioGroup mProxySettingsRadioGroup;
    private TextView mCustomProxyPort;

    public ProxySettingsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_proxy_settings, container, false);
        mProxySettingsRadioGroup = (RadioGroup) rootView.findViewById(R.id.proxySettingsRadioGroup);
        mProxySettingsRadioGroup.setOnCheckedChangeListener(this);
        mCustomProxyPort = (TextView) rootView.findViewById(R.id.proxySettingsCustomPort);
        mCustomProxyPort.addTextChangedListener(onCustomPortChanged);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean useProxy = preferences.getBoolean(getResources().getString(R.string.PREFERENCE_USE_PROXY), false);
        int proxyPort = preferences.getInt(getResources().getString(R.string.PREFERENCE_PROXY_PORT), HTTP_PROXY_PORT_INVALID);
        ((RadioButton) rootView.findViewById(R.id.proxySettingsRadioNone)).setChecked(!useProxy);
        ((RadioButton) rootView.findViewById(R.id.proxySettingsRadioTor)).setChecked(useProxy
                && (proxyPort == HTTP_PROXY_PORT_TOR));
        ((RadioButton) rootView.findViewById(R.id.proxySettingsRadioPsiphon)).setChecked(useProxy
                && (proxyPort == HTTP_PROXY_PORT_PSIPHON));
        ((RadioButton) rootView.findViewById(R.id.proxySettingsRadioCustom)).setChecked(useProxy
                && (proxyPort != HTTP_PROXY_PORT_TOR) && (proxyPort != HTTP_PROXY_PORT_PSIPHON));
        if (proxyPort != HTTP_PROXY_PORT_INVALID && proxyPort != HTTP_PROXY_PORT_TOR && proxyPort != HTTP_PROXY_PORT_PSIPHON) {
            mCustomProxyPort.setText(Integer.toString(proxyPort));
        }

        enableCustomPortEdit();

        return rootView;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        updateProxyPreferences();
        enableCustomPortEdit();
    }

    private void enableCustomPortEdit() {
        mCustomProxyPort.setEnabled(mProxySettingsRadioGroup.getCheckedRadioButtonId() == R.id.proxySettingsRadioCustom);
    }

    private TextWatcher onCustomPortChanged = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            updateProxyPreferences();
        }
    };

    private void updateProxyPreferences() {
        boolean useProxy = mProxySettingsRadioGroup.getCheckedRadioButtonId() != R.id.proxySettingsRadioNone;
        int proxyPort = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt(
                getResources().getString(R.string.PREFERENCE_PROXY_PORT), HTTP_PROXY_PORT_INVALID);

        int checkedRadioButtonId = mProxySettingsRadioGroup.getCheckedRadioButtonId();
        if (checkedRadioButtonId == R.id.proxySettingsRadioTor) {
            proxyPort = HTTP_PROXY_PORT_TOR;
        } else if (checkedRadioButtonId == R.id.proxySettingsRadioPsiphon) {
            proxyPort = HTTP_PROXY_PORT_PSIPHON;
        } else if (checkedRadioButtonId == R.id.proxySettingsRadioCustom) {
            try {
                proxyPort = Integer.parseInt(mCustomProxyPort.getText().toString());
                if (proxyPort < 1 || proxyPort > 65535) {
                    proxyPort = HTTP_PROXY_PORT_INVALID;
                }
            } catch (NumberFormatException e) {
                proxyPort = HTTP_PROXY_PORT_INVALID;
            }
        }

        Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        editor.putBoolean(getResources().getString(R.string.PREFERENCE_USE_PROXY), useProxy);
        editor.putInt(getResources().getString(R.string.PREFERENCE_PROXY_PORT), proxyPort);
        editor.commit();
    }
}
