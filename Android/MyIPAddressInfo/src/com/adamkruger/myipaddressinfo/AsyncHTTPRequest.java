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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;

import android.os.AsyncTask;
import android.os.SystemClock;

public class AsyncHTTPRequest extends AsyncTask<String, String, String> {

    interface RequestTaskCaller {
        void onPreExecute();

        void onPostExecute(Result result);
    }
    
    public class Result {
        final public String mResponseBody;
        final public int mExpectedContentLength;
        final public long mStartTime;
        final public long mElapsedTime;
        final public boolean mFailed;
        final public boolean mTimedOut;
        public Result(String responseBody, int expectedContentLength, long startTime, long elapsedTime, boolean failed, boolean timedOut) {
            mResponseBody = responseBody;
            mExpectedContentLength = expectedContentLength;
            mStartTime = startTime;
            mElapsedTime = elapsedTime;
            mFailed = failed;
            mTimedOut = timedOut;
        }
    }

    static final int CONNECTION_TIMEOUT_SECONDS = 20;
    static final int READ_TIMEOUT_SECONDS = 10;

    private long mStartTime = 0;
    private long mElapsedTime = 0;
    private boolean mFailed = true;
    private boolean mTimedOut = false;
    private int mExpectedContentLength = 0;
    private WeakReference<RequestTaskCaller> mCallerWeakRef;
    private Proxy mProxySettings;

    public AsyncHTTPRequest(RequestTaskCaller caller, Proxy proxySettings) {
        this.mCallerWeakRef = new WeakReference<RequestTaskCaller>(caller);
        mProxySettings = proxySettings;
    }

    @Override
    protected String doInBackground(String... uri) {
        URL url = null;
        HttpURLConnection httpUrlConnection = null;
        StringBuilder responseStringBuilder = new StringBuilder();
        mStartTime = System.currentTimeMillis();
        long startTime = SystemClock.elapsedRealtime();
        try {
            url = new URL(uri[0]);
            httpUrlConnection = (HttpURLConnection) url.openConnection(mProxySettings);
            httpUrlConnection.setConnectTimeout(CONNECTION_TIMEOUT_SECONDS * 1000);
            httpUrlConnection.setReadTimeout(READ_TIMEOUT_SECONDS * 1000);
            mExpectedContentLength = httpUrlConnection.getContentLength();
            BufferedReader reader = new BufferedReader(new InputStreamReader(httpUrlConnection.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                responseStringBuilder.append(line + "\n");
            }
            mFailed = false;
        } catch (SocketTimeoutException e) {
            mTimedOut = true;
            // return empty string
        } catch (MalformedURLException e) {
            // do nothing, return empty string
        } catch (IOException e) {
            // do nothing, return empty string
        } finally {
            httpUrlConnection.disconnect();
            mElapsedTime = SystemClock.elapsedRealtime() - startTime;
        }
        return responseStringBuilder.toString();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        RequestTaskCaller caller = mCallerWeakRef.get();
        if (caller != null) {
            caller.onPreExecute();
        }
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        RequestTaskCaller caller = mCallerWeakRef.get();
        if (caller != null) {
            caller.onPostExecute(new Result(result, mExpectedContentLength, mStartTime, mElapsedTime, mFailed, mTimedOut));
        }
    }
}
