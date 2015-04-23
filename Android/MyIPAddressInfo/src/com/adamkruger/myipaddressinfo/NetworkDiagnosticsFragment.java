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

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import com.adamkruger.myipaddressinfo.AsyncHTTPRequest.Result;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.AsyncTask.Status;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;

public class NetworkDiagnosticsFragment extends Fragment implements OnItemSelectedListener, OnClickListener {

    static String getNetworkDiagnosticsPreference(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(context.getResources().getString(R.string.PREFERENCE_NETWORK_DIAGNOSTICS_CHOICE),
                context.getResources().getString(R.string.PREFERENCE_NETWORK_DIAGNOSTICS_CHOICE_ALL_TESTS));
    }
    
    static String networkDiagnosticsPreferenceToSelection(Resources resources, String preference) {
        String selection = resources.getString(R.string.network_diagnostics_choice_all_tests);
        if (preference.equals(resources.getString(R.string.PREFERENCE_NETWORK_DIAGNOSTICS_CHOICE_SMALL_DOWNLOAD))) {
            selection = resources.getString(R.string.network_diagnostics_choice_small_download);
        } else if (preference.equals(resources.getString(R.string.PREFERENCE_NETWORK_DIAGNOSTICS_CHOICE_MEDIUM_DOWNLOAD))) {
            selection = resources.getString(R.string.network_diagnostics_choice_medium_download);
        } else if (preference.equals(resources.getString(R.string.PREFERENCE_NETWORK_DIAGNOSTICS_CHOICE_LARGE_DOWNLOAD))) {
            selection = resources.getString(R.string.network_diagnostics_choice_large_download);
        }
        return selection;
    }
    
    private Spinner mNetworkDiagnosticsSpinner;
    private final ArrayList<NetworkDiagnosticsResult> mResults = new ArrayList<NetworkDiagnosticsResult>();
    private NetworkDiagnosticsResultsAdapter mResultsAdapter;
    private ProgressBar mSpinningProgressBar;
    private Button mStartButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_network_diagnostics, container, false);
        
        mNetworkDiagnosticsSpinner = (Spinner) rootView.findViewById(R.id.networkDiagnosticsSpinner);
        ArrayAdapter<CharSequence> testListAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.network_diagnostics_tests, android.R.layout.simple_spinner_item);
        testListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mNetworkDiagnosticsSpinner.setAdapter(testListAdapter);
        mNetworkDiagnosticsSpinner.setOnItemSelectedListener(this);
        String networkDiagnosticsPreference = getNetworkDiagnosticsPreference(getActivity());
        mNetworkDiagnosticsSpinner.setSelection(testListAdapter.getPosition(
                networkDiagnosticsPreferenceToSelection(getResources(), networkDiagnosticsPreference)));
        
        mStartButton = (Button) rootView.findViewById(R.id.networkDiagnosticsStart);
        mStartButton.setOnClickListener(this);
        
        mResultsAdapter = new NetworkDiagnosticsResultsAdapter(getActivity(), mResults);
        ListView networkDiagnosticsResultsListView = (ListView) rootView.findViewById(R.id.networkDiagnosticsOutputListView);
        networkDiagnosticsResultsListView.setAdapter(mResultsAdapter);
        networkDiagnosticsResultsListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        
        mSpinningProgressBar = (ProgressBar) rootView.findViewById(R.id.networkDiagnosticsSpinningProgressBar);
        updateUIState();
        
        return rootView;
    }
    
    private void updateUIState() {
        NetworkDiagnosticsHTTPRequest nextTest = testQueue.peek();
        if (nextTest != null && nextTest.requestTaskIsPendingOrRunning()) {
            // If a test is underway, show the progress spinner and disable the Start button
            mSpinningProgressBar.setVisibility(View.VISIBLE);
            mStartButton.setEnabled(false);
        } else {
            mSpinningProgressBar.setVisibility(View.GONE);
            mStartButton.setEnabled(true);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long idd) {
        switch(parent.getId()) {
        case R.id.networkDiagnosticsSpinner:
            updateNetworkDiagnosticsPreference(parent.getItemAtPosition(pos).toString());
            break;
        }
    }
    
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        switch(parent.getId()) {
        case R.id.networkDiagnosticsSpinner:
            updateNetworkDiagnosticsPreference(null);
            break;
        }
    }
    
    private void updateNetworkDiagnosticsPreference(String selection) {
        String networkDiagnosticsPreference = getResources().getString(R.string.PREFERENCE_NETWORK_DIAGNOSTICS_CHOICE_ALL_TESTS);
        if (selection != null) {
            if (selection.equals(getResources().getString(R.string.network_diagnostics_choice_small_download))) {
                networkDiagnosticsPreference = getResources().getString(R.string.PREFERENCE_NETWORK_DIAGNOSTICS_CHOICE_SMALL_DOWNLOAD);
            } else if (selection.equals(getResources().getString(R.string.network_diagnostics_choice_medium_download))) {
                networkDiagnosticsPreference = getResources().getString(R.string.PREFERENCE_NETWORK_DIAGNOSTICS_CHOICE_MEDIUM_DOWNLOAD);
            } else if (selection.equals(getResources().getString(R.string.network_diagnostics_choice_large_download))) {
                networkDiagnosticsPreference = getResources().getString(R.string.PREFERENCE_NETWORK_DIAGNOSTICS_CHOICE_LARGE_DOWNLOAD);
            }
        }
        
        Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        editor.putString(getResources().getString(R.string.PREFERENCE_NETWORK_DIAGNOSTICS_CHOICE), networkDiagnosticsPreference);
        editor.commit();
    }
    
    @Override
    public void onClick(View view) {
        switch(view.getId()) {
        case R.id.networkDiagnosticsStart:
            start();
            break;
        }
    }
    
    private void start() {
        String networkDiagnosticsPreference = getNetworkDiagnosticsPreference(getActivity());
        if (networkDiagnosticsPreference.equals(getResources().getString(R.string.PREFERENCE_NETWORK_DIAGNOSTICS_CHOICE_ALL_TESTS))) {
            startAllTests();
        } else if (networkDiagnosticsPreference.equals(getResources().getString(R.string.PREFERENCE_NETWORK_DIAGNOSTICS_CHOICE_SMALL_DOWNLOAD))) {
            startSmallDownloadTest();
        } else if (networkDiagnosticsPreference.equals(getResources().getString(R.string.PREFERENCE_NETWORK_DIAGNOSTICS_CHOICE_MEDIUM_DOWNLOAD))) {
            startMediumDownloadTest();
        } else if (networkDiagnosticsPreference.equals(getResources().getString(R.string.PREFERENCE_NETWORK_DIAGNOSTICS_CHOICE_LARGE_DOWNLOAD))) {
            startLargeDownloadTest();
        }
    }
    
    private void startAllTests() {
        startSmallDownloadTest();
        startMediumDownloadTest();
        startLargeDownloadTest();
    }
    
    // TODO: http and https tests
    
    private final Queue<NetworkDiagnosticsHTTPRequest> testQueue = new LinkedList<NetworkDiagnosticsHTTPRequest>();
    private synchronized void consumeTestQueue() {
        NetworkDiagnosticsHTTPRequest nextTest = testQueue.peek();
        if (nextTest != null && !nextTest.requestTaskIsPendingOrRunning()) {
            nextTest.makeRequest();
        }
    }
    
    static final String SMALL_DOWNLOAD_TEST_URL = "https://raw.githubusercontent.com/adamkdot/my_ip_address_info/gh-pages/test-files/small-random-data";
    private void startSmallDownloadTest() {
        testQueue.add(new NetworkDiagnosticsHTTPRequest(
                getResources().getString(R.string.network_diagnostics_choice_small_download), SMALL_DOWNLOAD_TEST_URL));
        consumeTestQueue();
    }
    
    static final String MEDIUM_DOWNLOAD_TEST_URL = "https://raw.githubusercontent.com/adamkdot/my_ip_address_info/gh-pages/test-files/medium-random-data";
    private void startMediumDownloadTest() {
        testQueue.add(new NetworkDiagnosticsHTTPRequest(
                getResources().getString(R.string.network_diagnostics_choice_medium_download), MEDIUM_DOWNLOAD_TEST_URL));
        consumeTestQueue();
    }
    
    static final String LARGE_DOWNLOAD_TEST_URL = "https://raw.githubusercontent.com/adamkdot/my_ip_address_info/gh-pages/test-files/large-random-data";
    private void startLargeDownloadTest() {
        testQueue.add(new NetworkDiagnosticsHTTPRequest(
                getResources().getString(R.string.network_diagnostics_choice_large_download), LARGE_DOWNLOAD_TEST_URL));
        consumeTestQueue();
    }

    private class NetworkDiagnosticsHTTPRequest implements AsyncHTTPRequest.RequestTaskCaller {
        private WeakReference<AsyncHTTPRequest> mRequestTaskWeakRef;
        private String mDiagnosticLabel;
        private String mRequestURL;
        
        public NetworkDiagnosticsHTTPRequest(String diagnosticLabel, String requestURL) {
            mDiagnosticLabel = diagnosticLabel;
            mRequestURL = requestURL;
        }
        
        public synchronized void makeRequest() {
            if (requestTaskIsPendingOrRunning()) {
                return;
            }

            AsyncHTTPRequest requestTask = new AsyncHTTPRequest(this, getProxySettings());
            mRequestTaskWeakRef = new WeakReference<AsyncHTTPRequest>(requestTask);
            requestTask.execute(mRequestURL);
        }

        private boolean requestTaskIsPendingOrRunning() {
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
            updateUIState();
        }

        @Override
        public void onPostExecute(Result result) {
            if (getActivity() == null) {
                return;
            }
            
            // TODO: detect failures
            // TODO: check response code
            // TODO: compare file for integrity
            // TODO: compare expectedContentLength
            
            mResults.add(new NetworkDiagnosticsResult(getActivity(), mDiagnosticLabel, result));
            mResultsAdapter.notifyDataSetChanged();
            
            testQueue.poll();
            if (testQueue.peek() == null) {
                // Only show an ad if the queue is empty to avoid additional network activity during a test
                ((MainActivity)getActivity()).showFullScreenAd();
            }
            updateUIState();
            consumeTestQueue();
        }
    }
}
