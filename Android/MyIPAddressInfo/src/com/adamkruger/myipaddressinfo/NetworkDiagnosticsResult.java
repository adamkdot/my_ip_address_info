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

import java.text.DateFormat;
import java.util.Date;

import com.adamkruger.myipaddressinfo.AsyncHTTPRequest.Result;

import android.content.Context;

class NetworkDiagnosticsResult {
    
    private final Context mContext;
    private final String mTitle;
    private final Result mAsyncHTTPRequestResult;
    private boolean mCollapsed;
    
    public NetworkDiagnosticsResult (Context context, String title, Result asyncHTTPRequestResult) {
        mContext = context;
        mTitle = title;
        mAsyncHTTPRequestResult = asyncHTTPRequestResult;
        mCollapsed = true;
    }
    
    public String timestamp() {
        return DateFormat.getDateTimeInstance().format(new Date(mAsyncHTTPRequestResult.mStartTime));
    }
    
    public String title() {
        return mTitle;
    }
    
    public String summary() {
        if (mAsyncHTTPRequestResult.mTimedOut) {
            return String.format(mContext.getResources().getString(R.string.last_update_status_timeout),
                    mAsyncHTTPRequestResult.mElapsedTime / 1000);
        } else if (mAsyncHTTPRequestResult.mFailed) {
            return String.format(mContext.getResources().getString(R.string.last_update_status_fail),
                    mAsyncHTTPRequestResult.mElapsedTime);
        } else {
            return String.format(mContext.getResources().getString(R.string.last_update_status_success),
                    mAsyncHTTPRequestResult.mElapsedTime);
        }
    }
    
    public String formattedExpectedBytes() {
        // TODO: res
        return String.format("ExpectedContentLength: %d", mAsyncHTTPRequestResult.mExpectedContentLength);
    }
    
    public String formattedActualBytes() {
        // TODO: res
        return String.format("ActualContentLength: %d", mAsyncHTTPRequestResult.mResponseBody.length());
    }
    
    public String formattedSpeedResult() {
        // TODO: KB/s / MB/s
        float speed = 0;
        if (!mAsyncHTTPRequestResult.mFailed) {
            speed = (float)mAsyncHTTPRequestResult.mExpectedContentLength / 1024 /
                    ((float)mAsyncHTTPRequestResult.mElapsedTime / 1000);
        }
        return String.format("%.2f KB/s", speed);
                
    }
    
    public void toggleCollapsed() {
        mCollapsed = !mCollapsed;
    }
    
    public boolean getCollapsed() {
        return mCollapsed;
    }
}
