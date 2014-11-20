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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class GeoIPProviderSettingsFragment extends Fragment implements OnCheckedChangeListener {

    static String getProviderPreference(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(context.getResources().getString(R.string.PREFERENCE_GEOIP_PROVIDER),
                context.getResources().getString(R.string.PREFERENCE_GEOIP_PROVIDER_AUTO));
    }
    
    private RadioGroup mProviderSettingsRadioGroup;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_geoip_provider_settings, container, false);
        mProviderSettingsRadioGroup = (RadioGroup) rootView.findViewById(R.id.geoipProviderSettingsRadioGroup);
        mProviderSettingsRadioGroup.setOnCheckedChangeListener(this);

        String providerPreference = getProviderPreference(getActivity());
        ((RadioButton) rootView.findViewById(R.id.geoipProviderSettingsRadioAuto)).setChecked(
                providerPreference.equals(getResources().getString(R.string.PREFERENCE_GEOIP_PROVIDER_AUTO)));
        ((RadioButton) rootView.findViewById(R.id.geoipProviderSettingsRadioFreeGeoIPDotNet)).setChecked(
                providerPreference.equals(getResources().getString(R.string.PREFERENCE_GEOIP_PROVIDER_FREEGEOIPDOTNET)));
        ((RadioButton) rootView.findViewById(R.id.geoipProviderSettingsRadioTelizeDotCom)).setChecked(
                providerPreference.equals(getResources().getString(R.string.PREFERENCE_GEOIP_PROVIDER_TELIZEDOTCOM)));

        return rootView;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        updateProviderPreferences();
    }

    private void updateProviderPreferences() {
        String providerPreference = getResources().getString(R.string.PREFERENCE_GEOIP_PROVIDER_AUTO);
        int checkedRadioButtonId = mProviderSettingsRadioGroup.getCheckedRadioButtonId();
        if (checkedRadioButtonId == R.id.geoipProviderSettingsRadioFreeGeoIPDotNet) {
            providerPreference = getResources().getString(R.string.PREFERENCE_GEOIP_PROVIDER_FREEGEOIPDOTNET);
        } else if (checkedRadioButtonId == R.id.geoipProviderSettingsRadioTelizeDotCom) {
            providerPreference = getResources().getString(R.string.PREFERENCE_GEOIP_PROVIDER_TELIZEDOTCOM);
        }

        Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        editor.putString(getResources().getString(R.string.PREFERENCE_GEOIP_PROVIDER), providerPreference);
        editor.commit();
    }
}
