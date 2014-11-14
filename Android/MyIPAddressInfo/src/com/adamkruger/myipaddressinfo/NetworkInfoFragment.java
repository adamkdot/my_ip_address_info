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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.http.conn.util.InetAddressUtils;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class NetworkInfoFragment extends Fragment {

    private NetworkInfo mNetworkInfo;
    private List<NetworkInterfaceInfo> mNetworkInterfaceInfos;
    private List<String> mDNSes;
    private boolean mWifiEnabled;
    private WifiInfo mWifiInfo;
    private DhcpInfo mDhcpInfo;
    private TableLayout mNetworkInfoTableLayout;

    public NetworkInfoFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        refreshData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_network_info, container, false);
        mNetworkInfoTableLayout = (TableLayout) rootView.findViewById(R.id.networkInfoTableLayout);
        refreshView();
        return rootView;
    }

    public void refresh() {
        refreshData();
        refreshView();
    }

    private void refreshData() {
        Context context = getActivity();
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mNetworkInfo = connectivityManager.getActiveNetworkInfo();
        mNetworkInterfaceInfos = getNetworkInterfaceInfos();
        mDNSes = getActiveNetworkDnsResolvers(context);
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mWifiEnabled = wifiManager.isWifiEnabled();
        mWifiInfo = wifiManager.getConnectionInfo();
        mDhcpInfo = wifiManager.getDhcpInfo();
    }

    private void refreshView() {
        Context context = getActivity();
        mNetworkInfoTableLayout.removeAllViewsInLayout();

        if (mNetworkInfo != null) {
            String state = mNetworkInfo.getState().toString();
            String detailedState = mNetworkInfo.getDetailedState().toString();
            String reason = mNetworkInfo.getReason();
            if (detailedState.compareToIgnoreCase(state) == 0) {
                detailedState = "";
            }
            if (reason == null || reason.compareToIgnoreCase(state) == 0 || reason.compareToIgnoreCase(detailedState) == 0) {
                reason = "";
            }
            addTableRowTitle(context.getString(R.string.network_info_subtitle_active_network));
            addTableRow(new Row().addLine(mNetworkInfo.getTypeName(), state)
                    .addLine(mNetworkInfo.getSubtypeName(), mNetworkInfo.getExtraInfo()).addLine(reason, detailedState));
        }

        if (mWifiEnabled) {
            if (mWifiInfo != null) {
                String wifiSSID = mWifiInfo.getSSID();
                if (wifiSSID.length() > 0) {
                    addTableRowSpacer();
                    addTableRowTitle(context.getString(R.string.network_info_subtitle_wifi_info));
                    addTableRow(new Row().addLine(
                            wifiSSID,
                            mWifiInfo.getLinkSpeed() + WifiInfo.LINK_SPEED_UNITS + " ("
                                    + calculateSignalLevel(mWifiInfo.getRssi(), 100) + "%)").addLine(
                            mWifiInfo.getHiddenSSID() ? context.getString(R.string.network_info_hidden_network) : "", ""));
                }
            }
        
            if (mDhcpInfo != null) {
                String dhcpIPAddress = intToHostAddress(mDhcpInfo.ipAddress);
                if (dhcpIPAddress.length() > 0) {
                    addTableRowSpacer();
                    addTableRowTitle(context.getString(R.string.network_info_subtitle_dhcp_info));
                    addTableRow(new Row()
                            .addLineIfValue(context.getString(R.string.network_info_label_ip_address), dhcpIPAddress)
                            .addLineIfValue(context.getString(R.string.network_info_label_gateway),
                                    intToHostAddress(mDhcpInfo.gateway))
                            // TODO: netmask conversion doesn't work
                            .addLineIfValue(context.getString(R.string.network_info_label_netmask),
                                    intToHostAddress(mDhcpInfo.netmask))
                            .addLineIfValue(context.getString(R.string.network_info_label_dns), intToHostAddress(mDhcpInfo.dns1))
                            .addLineIfValue(context.getString(R.string.network_info_label_dns), intToHostAddress(mDhcpInfo.dns2))
                            .addLineIfValue(context.getString(R.string.network_info_label_dhcp_server),
                                    intToHostAddress(mDhcpInfo.serverAddress))
                            .addLineIfValue(context.getString(R.string.network_info_label_lease_duration),
                                    Integer.toString(mDhcpInfo.leaseDuration)));
                }
            }
        }
        
        if (mDNSes.size() > 0) {
            addTableRowSpacer();
            addTableRowTitle(context.getString(R.string.network_info_subtitle_active_dns));
            Row row = new Row();
            for (String DNS : mDNSes) {
                row.addLine(context.getString(R.string.network_info_label_dns), DNS);
            }
            addTableRow(row);
        }

        if (mNetworkInterfaceInfos.size() > 0) {
            addTableRowSpacer();
            addTableRowTitle(context.getString(R.string.network_info_subtitle_interfaces));

            for (NetworkInterfaceInfo networkInterfaceInfo : mNetworkInterfaceInfos) {
                String valueColumn = "";
                for (String ipAddress : networkInterfaceInfo.ipAddresses) {
                    valueColumn += ipAddress + "\n";
                }
                if (networkInterfaceInfo.MAC.length() > 0) {
                    valueColumn += networkInterfaceInfo.MAC + "\n";
                }
                if (networkInterfaceInfo.MTU != -1) {
                    valueColumn += String.format("%s: %d", context.getString(R.string.network_info_label_mtu),
                            networkInterfaceInfo.MTU);
                }
                addTableRow(new Row().addLine(networkInterfaceInfo.name, valueColumn));
            }
        }
        
        addTableRowSpacer();
    }

    private static class Row {
        public String mLabel;
        public String mValue;

        public Row() {
            mLabel = "";
            mValue = "";
        }

        public Row addLine(String label, String value) {
            if (label == null) {
                label = "";
            }
            if (value == null) {
                value = "";
            }
            if (label.length() > 0 || value.length() > 0) {
                if (mLabel.length() > 0 || mValue.length() > 0) {
                    mLabel += "\n";
                    mValue += "\n";

                }
                mLabel += label;
                mValue += value;
            }
            return this;
        }

        public Row addLineIfValue(String label, String value) {
            if (value != null && value.length() > 0) {
                addLine(label, value);
            }
            return this;
        }
    }

    private void addTableRowSpacer() {
        Context context = getActivity();
        TableRow tableRow = new TableRow(context);
        View spacerView = new View(context);
        int spacerHeight = (int) (getResources().getDisplayMetrics().density
                * getResources().getDimension(R.dimen.network_info_spacer_height) + 0.5f);
        spacerView.setLayoutParams(new TableRow.LayoutParams(1, spacerHeight));
        tableRow.addView(spacerView);
        mNetworkInfoTableLayout.addView(tableRow);
    }

    private void addTableRowTitle(String title) {
        int horizontalPadding = (int) (getResources().getDisplayMetrics().density
                * getResources().getDimension(R.dimen.subtitle_padding) + 0.5f);
        TextView titleView = makeTextView(title, getResources().getColor(R.color.subtitle_color), horizontalPadding);
        TableRow tableRow = makeTableRow(titleView, null);
        int verticalPadding = (int) (getResources().getDisplayMetrics().density
                * getResources().getDimension(R.dimen.network_info_vertical_padding) + 0.5f);
        tableRow.setPadding(0, 0, 0, verticalPadding);
        mNetworkInfoTableLayout.addView(tableRow);
    }

    private void addTableRow(Row row) {
        if (row.mLabel.length() > 0 || row.mValue.length() > 0) {
            int horizontalPadding = (int) (getResources().getDisplayMetrics().density
                    * getResources().getDimension(R.dimen.label_value_padding) + 0.5f);
            TextView labelView = makeTextView(row.mLabel, getResources().getColor(R.color.dark_text_color), horizontalPadding);
            labelView.setGravity(Gravity.RIGHT);
            TextView valueView = makeTextView(row.mValue, getResources().getColor(R.color.dark_text_color), horizontalPadding);
            TableRow tableRow = makeTableRow(labelView, valueView);
            int verticalPadding = (int) (getResources().getDisplayMetrics().density
                    * getResources().getDimension(R.dimen.network_info_vertical_padding) + 0.5f);
            tableRow.setPadding(0, verticalPadding, 0, verticalPadding);
            tableRow.setBackgroundColor(getResources().getColor(R.color.background_color_white));
            tableRow.setGravity(Gravity.CENTER_HORIZONTAL);
            mNetworkInfoTableLayout.addView(tableRow);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private TextView makeTextView(String value, int color, int horizontalPadding) {
        Context context = getActivity();
        TextView textView = new TextView(context);
        textView.setText(value);
        textView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
        textView.setTextAppearance(context, android.R.attr.textAppearanceSmall);
        textView.setTextColor(color);
        textView.setPadding(horizontalPadding, 0, horizontalPadding, 0);
        textView.setSingleLine(false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            textView.setTextIsSelectable(true);
        }
        return textView;
    }

    private TableRow makeTableRow(TextView label, TextView value) {
        TableRow tableRow = new TableRow(getActivity());
        tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
        tableRow.addView(label);
        if (value != null) {
            tableRow.addView(value);
        }
        return tableRow;
    }

    private static class NetworkInterfaceInfo {
        public String name;
        public ArrayList<String> ipAddresses;
        public String MAC;
        public int MTU;

        public NetworkInterfaceInfo() {
            name = "";
            ipAddresses = new ArrayList<String>();
            MAC = "";
            MTU = -1;
        }
    }

    // Adapted from
    // http://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private static List<NetworkInterfaceInfo> getNetworkInterfaceInfos() {
        List<NetworkInterfaceInfo> networkInterfaceInfos = new ArrayList<NetworkInterfaceInfo>();
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaces) {
                NetworkInterfaceInfo networkInterfaceInfo = new NetworkInterfaceInfo();
                networkInterfaceInfo.name = networkInterface.getDisplayName();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
                    byte[] MAC = networkInterface.getHardwareAddress();
                    if (MAC != null) {
                        StringBuilder stringBuilder = new StringBuilder(18);
                        for (byte b : MAC) {
                            if (stringBuilder.length() > 0) {
                                stringBuilder.append(':');
                            }
                            stringBuilder.append(String.format("%02x", b));
                        }
                        networkInterfaceInfo.MAC = stringBuilder.toString();
                    }

                    networkInterfaceInfo.MTU = networkInterface.getMTU();
                }
                List<InetAddress> addresses = Collections.list(networkInterface.getInetAddresses());
                for (InetAddress address : addresses) {
                    if (!address.isLoopbackAddress()) {
                        networkInterfaceInfo.ipAddresses.add(InetAddressToString(address));
                    }
                }
                if (networkInterfaceInfo.ipAddresses.size() > 0) {
                    networkInterfaceInfos.add(networkInterfaceInfo);
                }
            }
        } catch (SocketException e) {
        }

        return networkInterfaceInfos;
    }

    private static String InetAddressToString(InetAddress address) {
        String addressString = address.getHostAddress().toUpperCase(Locale.getDefault());
        if (InetAddressUtils.isIPv4Address(addressString)) {
            return addressString;
        } else {
            int suffixPosition = addressString.indexOf('%');
            return suffixPosition < 0 ? addressString : addressString.substring(0, suffixPosition);
        }
    }

    // Taken from
    // http://stackoverflow.com/questions/17055946/android-formatter-formatipaddress-deprecation-with-api-12
    private static String intToHostAddress(int addressAsInt) {
        String hostAddress = "";
        byte[] ipAddress = BigInteger.valueOf(Integer.reverseBytes(addressAsInt)).toByteArray();
        try {
            InetAddress inetAddress = InetAddress.getByAddress(ipAddress);
            hostAddress = InetAddressToString(inetAddress);
        } catch (UnknownHostException e) {
        }
        return hostAddress;
    }

    // Copied and adapted from
    // https://bitbucket.org/psiphon/psiphon-circumvention-system/src/default/Android/PsiphonAndroidLibrary/src/com/psiphon3/psiphonlibrary/Utils.java?at=default
    /*
     * Copyright (c) 2013, Psiphon Inc. All rights reserved.
     * 
     * This program is free software: you can redistribute it and/or modify it
     * under the terms of the GNU General Public License as published by the
     * Free Software Foundation, either version 3 of the License, or (at your
     * option) any later version.
     * 
     * This program is distributed in the hope that it will be useful, but
     * WITHOUT ANY WARRANTY; without even the implied warranty of
     * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
     * Public License for more details.
     * 
     * You should have received a copy of the GNU General Public License along
     * with this program. If not, see <http://www.gnu.org/licenses/>.
     */
    private static List<String> getActiveNetworkDnsResolvers(Context context) {
        ArrayList<String> dnsAddresses = new ArrayList<String>();

        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            Class<?> LinkPropertiesClass = Class.forName("android.net.LinkProperties");

            Method getActiveLinkPropertiesMethod = ConnectivityManager.class.getMethod("getActiveLinkProperties", new Class[] {});

            Object linkProperties = getActiveLinkPropertiesMethod.invoke(connectivityManager);

            Method getDnsesMethod = LinkPropertiesClass.getMethod("getDnses", new Class[] {});

            Collection<?> dnses = (Collection<?>) getDnsesMethod.invoke(linkProperties);

            for (Object dns : dnses) {
                dnsAddresses.add(InetAddressToString((InetAddress) dns));
            }
        } catch (ClassNotFoundException e) {
        } catch (NoSuchMethodException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        } catch (NullPointerException e) {
        }

        return dnsAddresses;
    }
    
    // Copied from Android sources. On Android 2.3 there is a divide-by-zero bug.
    /*
     * Copyright (C) 2008 The Android Open Source Project
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *      http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
    /** Anything worse than or equal to this will show 0 bars. */
    private static final int MIN_RSSI = -100;

    /** Anything better than or equal to this will show the max bars. */
    private static final int MAX_RSSI = -55;
    
    private static int calculateSignalLevel(int rssi, int numLevels) {
        if (rssi <= MIN_RSSI) {
            return 0;
        } else if (rssi >= MAX_RSSI) {
            return numLevels - 1;
        } else {
            float inputRange = (MAX_RSSI - MIN_RSSI);
            float outputRange = (numLevels - 1);
            return (int)((float)(rssi - MIN_RSSI) * outputRange / inputRange);
        }
    }
}
