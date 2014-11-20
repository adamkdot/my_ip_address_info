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

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.AsyncTask.Status;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class IPAddressInfoFragment extends Fragment implements OnClickListener {

    private String mIPAddress;
    private String mISP;
    private String mCountry;
    private String mCity;
    private String mRegion;
    private String mLatLong;
    private boolean mLastUpdateTimedOut;
    private boolean mLastUpdateSucceeded;
    private long mLastUpdateElapsedTimeMs;
    private String mLastUpdateTime;
    private int mDefaultLastUpdateTimeColor;
    private WeakReference<AsyncHTTPRequest> mRequestTaskWeakRef;
    private ArrayList<GeoIpServiceRequestCaller> mGeoIpProviders;
    private TelizeDotComRequestCaller mTelizeDotComRequestCaller;
    private FreegeoipDotNetRequestCaller mFreegeoipDotNetRequestCaller;
    private int mMaxFailovers;
    private int mFailovers;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mGeoIpProviders = new ArrayList<GeoIpServiceRequestCaller>();
        mTelizeDotComRequestCaller = new TelizeDotComRequestCaller();
        mFreegeoipDotNetRequestCaller = new FreegeoipDotNetRequestCaller();
        mGeoIpProviders.add(mTelizeDotComRequestCaller);
        mGeoIpProviders.add(mFreegeoipDotNetRequestCaller);
        mMaxFailovers = mGeoIpProviders.size() - 1;
        mFailovers = 0;

        makeIPAddressInfoRequest();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_ip_address_info, container, false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            ((TextView) rootView.findViewById(R.id.ipValue)).setTextIsSelectable(true);
            ((TextView) rootView.findViewById(R.id.ispValue)).setTextIsSelectable(true);
            ((TextView) rootView.findViewById(R.id.countryValue)).setTextIsSelectable(true);
            ((TextView) rootView.findViewById(R.id.cityValue)).setTextIsSelectable(true);
            ((TextView) rootView.findViewById(R.id.regionValue)).setTextIsSelectable(true);
            ((TextView) rootView.findViewById(R.id.coordinatesValue)).setTextIsSelectable(true);
        }
        rootView.findViewById(R.id.coordinatesValue).setOnClickListener(this);
        mDefaultLastUpdateTimeColor = ((TextView) rootView.findViewById(R.id.lastUpdateTime)).getTextColors().getDefaultColor();
        return rootView;
    }

    public int getOptimalHeight() {
        int height = 0;
        View view = getView();
        if (view != null) {
            height += view.findViewById(R.id.ipAddressFragmentTableLayout).getHeight();
            height += view.findViewById(R.id.lastUpdateStatus).getHeight();
            height += view.findViewById(R.id.lastUpdateTime).getHeight();
        }
        return height;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.coordinatesValue:
            onMapValueClick();
            break;
        }
    }

    private void onMapValueClick() {
        if (mLatLong.length() > 0) {
            String uri = mapLink(mLatLong, mIPAddress);
            startActivity(new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri)));
        }
    }

    private String mapLink(String latLong, String label) {
        return String.format("http://maps.google.com/?q=%s(%s)", latLong, label);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        updateViewState();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateViewState();
    }

    private void updateViewState() {
        View view = getView();
        if (view != null) {
            ((TextView) view.findViewById(R.id.ipValue)).setText(mIPAddress);
            ((TextView) view.findViewById(R.id.ispValue)).setText(mISP);
            ((TextView) view.findViewById(R.id.countryValue)).setText(mCountry);
            ((TextView) view.findViewById(R.id.cityValue)).setText(mCity);
            ((TextView) view.findViewById(R.id.regionValue)).setText(mRegion);
            ((TextView) view.findViewById(R.id.coordinatesValue)).setText(mLatLong.length() == 0 ? "" : Html.fromHtml(String
                    .format("<a href=\"%s\">%s</a>", mapLink(mLatLong, mIPAddress), mLatLong)));

            if (mLastUpdateTime != null) {
                TextView lastUpdateStatus = (TextView) view.findViewById(R.id.lastUpdateStatus);
                if (mLastUpdateTimedOut) {
                    lastUpdateStatus.setText(String.format(getResources().getString(R.string.last_update_status_timeout),
                            mLastUpdateElapsedTimeMs / 1000));
                    lastUpdateStatus.setTextColor(getResources().getColor(R.color.error_color));
                } else if (mLastUpdateSucceeded) {
                    lastUpdateStatus.setText(String.format(getResources().getString(R.string.last_update_status_success),
                            mLastUpdateElapsedTimeMs));
                    if (mLastUpdateElapsedTimeMs < 2000) {
                        lastUpdateStatus.setTextColor(getResources().getColor(R.color.success_color));
                    } else {
                        lastUpdateStatus.setTextColor(getResources().getColor(R.color.warning_color));
                    }
                } else {
                    lastUpdateStatus.setText(String.format(getResources().getString(R.string.last_update_status_fail),
                            mLastUpdateElapsedTimeMs));
                    lastUpdateStatus.setTextColor(getResources().getColor(R.color.error_color));
                }

                TextView lastUpdateTime = (TextView) view.findViewById(R.id.lastUpdateTime);
                lastUpdateTime.setText(String.format(getResources().getString(R.string.last_update_time), mLastUpdateTime));
                String currentTime = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(new Date());
                if (currentTime.equals(mLastUpdateTime)) {
                    lastUpdateTime.setTextColor(mDefaultLastUpdateTimeColor);
                } else {
                    lastUpdateTime.setTextColor(getResources().getColor(R.color.warning_color));
                }
            }
        }
    }

    private void setLoadingViewState() {
        mIPAddress = getResources().getString(R.string.loading_message);
        mISP = getResources().getString(R.string.loading_message);
        mCountry = getResources().getString(R.string.loading_message);
        mCity = getResources().getString(R.string.loading_message);
        mRegion = getResources().getString(R.string.loading_message);
        mLatLong = "";
        updateViewState();
    }

    /*
     * Simple GeoIpProvider automatic failover: Always make requests using the
     * first item in the List. On failure, move the first item to the end. If
     * mFailovers < mMaxFailovers, increment mFailovers, and make the request
     * again. Set mFailovers to 0 whenever not failing over.
     */

    public void makeIPAddressInfoRequest() {
        String providerPreference = GeoIPProviderSettingsFragment.getProviderPreference(getActivity());
        if (providerPreference.equals(getResources().getString(R.string.PREFERENCE_GEOIP_PROVIDER_AUTO))) {
            mGeoIpProviders.get(0).makeRequest();
        } else if (providerPreference.equals(getResources().getString(R.string.PREFERENCE_GEOIP_PROVIDER_FREEGEOIPDOTNET))) {
            mFreegeoipDotNetRequestCaller.makeRequest();
        } else if (providerPreference.equals(getResources().getString(R.string.PREFERENCE_GEOIP_PROVIDER_TELIZEDOTCOM))) {
            mTelizeDotComRequestCaller.makeRequest();
        }
    }

    private void onRequestCompleted() {
        if (mLastUpdateSucceeded) {
            requestsDone();
        } else if (GeoIPProviderSettingsFragment.getProviderPreference(getActivity()).equals(
                (getResources().getString(R.string.PREFERENCE_GEOIP_PROVIDER_AUTO)))) {
            mGeoIpProviders.add(mGeoIpProviders.remove(0));
            if (mFailovers < mMaxFailovers) {
                mFailovers++;
                makeIPAddressInfoRequest();
            } else {
                requestsDone();
            }
        } else {
            requestsDone();
        }
    }

    private void requestsDone() {
        mFailovers = 0;
        updateViewState();
    }

    public abstract class GeoIpServiceRequestCaller implements AsyncHTTPRequest.RequestTaskCaller {
        public synchronized void makeRequest() {
            if (IPAddressRequestTaskIsPendingOrRunning()) {
                return;
            }

            AsyncHTTPRequest ipAddressRequestTask = new AsyncHTTPRequest(this, getProxySettings());
            mRequestTaskWeakRef = new WeakReference<AsyncHTTPRequest>(ipAddressRequestTask);
            ipAddressRequestTask.execute(requestURL());
        }

        private boolean IPAddressRequestTaskIsPendingOrRunning() {
            return mRequestTaskWeakRef != null && mRequestTaskWeakRef.get() != null
                    && !mRequestTaskWeakRef.get().getStatus().equals(Status.FINISHED);
        }

        private Proxy getProxySettings() {
            Proxy proxySettings = Proxy.NO_PROXY;

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            boolean useProxy = preferences.getBoolean(getResources().getString(R.string.PREFERENCE_USE_PROXY), false);

            if (useProxy) {
                int proxyPort = preferences.getInt(getResources().getString(R.string.PREFERENCE_PROXY_PORT), 0);
                InetSocketAddress socketAddress = InetSocketAddress.createUnresolved("127.0.0.1", proxyPort);
                proxySettings = new Proxy(Proxy.Type.HTTP, socketAddress);
            }

            return proxySettings;
        }

        @Override
        public void onPreExecute() {
            setLoadingViewState();
        }

        @Override
        public void onPostExecute(String result, long elapsedTime, boolean timedOut) {
            mIPAddress = "";
            mISP = "";
            mCountry = "";
            mCity = "";
            mRegion = "";
            mLatLong = "";

            mLastUpdateTimedOut = timedOut;
            mLastUpdateSucceeded = processIPAddressInfoResponse(result);
            mLastUpdateElapsedTimeMs = elapsedTime;
            mLastUpdateTime = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(new Date());

            // Attempt to post onRequestCompleted so that this asyncTask can
            // finish in case we need to fail over and start a new asyncTask
            Handler handler = new Handler();
            boolean postSuccess = handler.post(new Runnable() {
                public void run() {
                    onRequestCompleted();
                }
            });
            if (!postSuccess) {
                onRequestCompleted();
            }
        }

        protected abstract String requestURL();

        protected abstract boolean processIPAddressInfoResponse(String response);
    }

    public class FreegeoipDotNetRequestCaller extends GeoIpServiceRequestCaller {
        @Override
        public String requestURL() {
            return "http://freegeoip.net/json/";
        }

        @Override
        protected boolean processIPAddressInfoResponse(String response) {
            try {
                JSONObject json = new JSONObject(response);
                mIPAddress = json.getString("ip");
                mISP = "";
                mCountry = json.getString("country_name");
                mCity = json.getString("city");
                mRegion = json.getString("region_name");
                mLatLong = json.getString("latitude") + "," + json.getString("longitude");
                return true;
            } catch (JSONException e) {

            }
            return false;
        }
    }

    public class TelizeDotComRequestCaller extends GeoIpServiceRequestCaller {
        @Override
        public String requestURL() {
            return "http://www.telize.com/geoip/";
        }

        @Override
        protected boolean processIPAddressInfoResponse(String response) {
            try {
                JSONObject json = new JSONObject(response);
                // If there is no IP Address, we should fail
                mIPAddress = json.getString("ip");
                mISP = safeGetJSONString(json, "isp");
                mCountry = safeGetJSONString(json, "country");
                mCity = safeGetJSONString(json, "city");
                mRegion = safeGetJSONString(json, "region");
                mLatLong = "";
                String latitude = safeGetJSONString(json, "latitude");
                String longitude = safeGetJSONString(json, "longitude");
                if (latitude.length() > 0 && longitude.length() > 0) {
                    mLatLong = latitude + "," + longitude;
                }
                return true;
            } catch (JSONException e) {

            }
            return false;
        }
    }

    private static String safeGetJSONString(JSONObject json, String key) throws JSONException {
        String jsonString = "";
        if (json.has(key)) {
            jsonString = json.getString(key);
        }
        return jsonString;
    }
}