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
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.http.conn.util.InetAddressUtils;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
            addTableRow(context, mNetworkInfoTableLayout, mNetworkInfo.getTypeName(), state);
            addTableRow(context, mNetworkInfoTableLayout, mNetworkInfo.getSubtypeName(), mNetworkInfo.getExtraInfo());
            addTableRow(context, mNetworkInfoTableLayout, reason, detailedState);
        }

        String valueColumn = "";

        for (NetworkInterfaceInfo networkInterfaceInfo : mNetworkInterfaceInfos) {
            mNetworkInfoTableLayout.addView(makeTableRowSpacer(context));
            valueColumn = "";
            for (String ipAddress : networkInterfaceInfo.ipAddresses) {
                valueColumn += ipAddress + "\n";
            }
            if (networkInterfaceInfo.MAC.length() > 0) {
                valueColumn += networkInterfaceInfo.MAC + "\n";
            }
            if (networkInterfaceInfo.MTU != -1) {
                valueColumn += String.format("%s: %d", context.getString(R.string.network_info_label_MTU),
                        networkInterfaceInfo.MTU);
            }
            addTableRow(context, mNetworkInfoTableLayout, networkInterfaceInfo.name, valueColumn);
        }

        if (mDNSes.size() > 0) {
            mNetworkInfoTableLayout.addView(makeTableRowSpacer(context));
            valueColumn = "";
            for (String DNS : mDNSes) {
                valueColumn += DNS + "\n";
            }
            valueColumn = valueColumn.trim();
            addTableRow(context, mNetworkInfoTableLayout, context.getString(R.string.network_info_label_DNS), valueColumn);
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
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
        labelTextView.setSingleLine(false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            labelTextView.setTextIsSelectable(true);
        }

        TextView valueTextView = new TextView(context);
        valueTextView.setText(value);
        valueTextView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT));
        valueTextView.setTextAppearance(context, android.R.attr.textAppearanceSmall);
        valueTextView.setTextColor(getResources().getColor(R.color.dark_text_color));
        valueTextView.setPadding(textPadding, 0, 0, 0);
        valueTextView.setSingleLine(false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            valueTextView.setTextIsSelectable(true);
        }

        tableRow.addView(labelTextView);
        tableRow.addView(valueTextView);
        return tableRow;
    }

    private TableRow makeTableRowSpacer(Context context) {
        TableRow tableRow = new TableRow(context);
        View spacerView = new View(context);
        spacerView.setLayoutParams(new TableRow.LayoutParams(1, 16));
        tableRow.addView(spacerView);
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
}
