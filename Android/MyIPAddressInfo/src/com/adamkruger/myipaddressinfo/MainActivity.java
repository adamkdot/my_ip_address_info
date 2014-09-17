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

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity {

	private static final String TAG_IP_ADDRESS_INFO_FRAGMENT = "ip_address_info_fragment";
	private IPAddressInfoFragment mIPAddressInfoFragment;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        FragmentManager fragmentManager = getSupportFragmentManager();
        mIPAddressInfoFragment = (IPAddressInfoFragment)fragmentManager.findFragmentByTag(TAG_IP_ADDRESS_INFO_FRAGMENT);

        if (mIPAddressInfoFragment == null) {
        	mIPAddressInfoFragment = new IPAddressInfoFragment();
        	fragmentManager.beginTransaction()
                    .add(R.id.container, mIPAddressInfoFragment, TAG_IP_ADDRESS_INFO_FRAGMENT)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
        	refreshIpAddressInfo();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void refreshIpAddressInfo() {
    	if (mIPAddressInfoFragment != null && mIPAddressInfoFragment.isVisible()) {
    		mIPAddressInfoFragment.makeIPAddressInfoRequest();
    	}
    }
}
