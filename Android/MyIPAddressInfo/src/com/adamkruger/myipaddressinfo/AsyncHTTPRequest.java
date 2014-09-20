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

		void onPostExecute(String result, long elapsedTime, boolean timedOut);
	}

	static final int CONNECTION_TIMEOUT_SECONDS = 20;
	static final int READ_TIMEOUT_SECONDS = 10;

	private long mElapsedTime = 0;
	private boolean mTimedOut = false;
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
		long startTime = SystemClock.elapsedRealtime();
		try {
			url = new URL(uri[0]);
			httpUrlConnection = (HttpURLConnection) url.openConnection(mProxySettings);
			httpUrlConnection
					.setConnectTimeout(CONNECTION_TIMEOUT_SECONDS * 1000);
			httpUrlConnection.setReadTimeout(READ_TIMEOUT_SECONDS * 1000);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					httpUrlConnection.getInputStream()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				responseStringBuilder.append(line + "\n");
			}
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
			caller.onPostExecute(result, mElapsedTime, mTimedOut);
		}
	}
}
