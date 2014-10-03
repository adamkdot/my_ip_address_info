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

import com.viewpagerindicator.CirclePageIndicator;

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

public class MainActivity extends ActionBarActivity {

	private static final String TAG_IP_ADDRESS_INFO_FRAGMENT = "ip_address_info_fragment";
	private IPAddressInfoFragment mIPAddressInfoFragment;
	private NetworkInfoFragment mNetworkInfoFragment;
	private ProxySettingsFragment mProxySettingsFragment;
	private ViewPager mAdditionalInfoPager;
	private AdditionalInfoFragmentPagerAdapter mAdditionalInfoPagerAdapter;
	private CirclePageIndicator mAdditionalInfoPageIndicator;

	public static class AdditionalInfoFragmentPagerAdapter extends
			FragmentPagerAdapter {

		private Fragment[] fragmentPages;

		public AdditionalInfoFragmentPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		public void SetPages(Fragment[] pages) {
			fragmentPages = pages;
		}

		@Override
		public int getCount() {
			return fragmentPages.length;
		}

		@Override
		public Fragment getItem(int position) {
			return fragmentPages[position];
		}
	}

	public static class AdditionalInfoPageChangeListener implements
			ViewPager.OnPageChangeListener {

		private Context context;

		public AdditionalInfoPageChangeListener(Context parentContext) {
			context = parentContext;
		}

		@Override
		public void onPageScrollStateChanged(int arg0) {
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageSelected(int position) {
			Editor editor = PreferenceManager.getDefaultSharedPreferences(
					context).edit();
			editor.putInt(
					context.getResources().getString(
							R.string.PREFERENCE_ADDITIONAL_INFO_PAGE), position);
			editor.commit();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		FragmentManager fragmentManager = getSupportFragmentManager();
		mIPAddressInfoFragment = (IPAddressInfoFragment) fragmentManager
				.findFragmentByTag(TAG_IP_ADDRESS_INFO_FRAGMENT);
		mAdditionalInfoPager = (ViewPager) findViewById(R.id.other_info_container);
		mAdditionalInfoPageIndicator = (CirclePageIndicator) findViewById(R.id.other_info_indicator);

		if (mIPAddressInfoFragment == null) {
			mIPAddressInfoFragment = new IPAddressInfoFragment();
			fragmentManager
					.beginTransaction()
					.add(R.id.ip_address_info_container,
							mIPAddressInfoFragment,
							TAG_IP_ADDRESS_INFO_FRAGMENT).commit();
		}

		// NOTE that this hack assumes the fragment positions in the ViewPager
		// corresponding to the FragmentPagerAdapter's SetPages call below
		mNetworkInfoFragment = (NetworkInfoFragment) fragmentManager
				.findFragmentByTag("android:switcher:"
						+ R.id.other_info_container + ":" + 0);
		mProxySettingsFragment = (ProxySettingsFragment) fragmentManager
				.findFragmentByTag("android:switcher:"
						+ R.id.other_info_container + ":" + 1);

		if (mNetworkInfoFragment == null) {
			mNetworkInfoFragment = new NetworkInfoFragment();
		}

		if (mProxySettingsFragment == null) {
			mProxySettingsFragment = new ProxySettingsFragment();
		}

		mAdditionalInfoPagerAdapter = new AdditionalInfoFragmentPagerAdapter(
				fragmentManager);
		mAdditionalInfoPagerAdapter.SetPages(new Fragment[] {
				mNetworkInfoFragment, mProxySettingsFragment });
		mAdditionalInfoPager.setAdapter(mAdditionalInfoPagerAdapter);
		mAdditionalInfoPageIndicator.setViewPager(mAdditionalInfoPager);
		mAdditionalInfoPageIndicator
				.setOnPageChangeListener(new AdditionalInfoPageChangeListener(
						this));

		int additionalInfoCurrentPage = PreferenceManager
				.getDefaultSharedPreferences(this).getInt(
						getResources().getString(
								R.string.PREFERENCE_ADDITIONAL_INFO_PAGE), 0);
		mAdditionalInfoPager.setCurrentItem(additionalInfoCurrentPage);
	}

	@Override
	public void onResume() {
		super.onResume();

		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
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
			refreshNetworkInfo();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void refreshIpAddressInfo() {
		if (mIPAddressInfoFragment != null
				&& mIPAddressInfoFragment.isVisible()) {
			mIPAddressInfoFragment.makeIPAddressInfoRequest();
		}
	}

	public void refreshNetworkInfo() {
		if (mNetworkInfoFragment != null && mNetworkInfoFragment.isVisible()) {
			mNetworkInfoFragment.populateInfo();
		}
	}
}
