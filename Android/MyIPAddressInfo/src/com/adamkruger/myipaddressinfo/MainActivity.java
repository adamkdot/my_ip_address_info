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

import java.util.ArrayList;
import java.util.Collections;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.purplebrain.adbuddiz.sdk.AdBuddiz;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.viewpagerindicator.CirclePageIndicator;

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;

public class MainActivity extends ActionBarActivity {

    private static final String TAG_IP_ADDRESS_INFO_FRAGMENT = "ip_address_info_fragment";
    private IPAddressInfoFragment mIPAddressInfoFragment;
    private NetworkInfoFragment mNetworkInfoFragment;
    private ProxySettingsFragment mProxySettingsFragment;
    private GeoIPProviderSettingsFragment mGeoIPProviderSettingsFragment;
    private ViewPager mAdditionalInfoPager;
    private AdditionalInfoFragmentPagerAdapter mAdditionalInfoPagerAdapter;
    private CirclePageIndicator mAdditionalInfoPageIndicator;
    private AdView mAdMobBannerAdView;
    private InterstitialAd mAdMobInterstitialAd;

    public static class AdditionalInfoFragmentPagerAdapter extends FragmentPagerAdapter {

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

    public static class AdditionalInfoPageChangeListener implements ViewPager.OnPageChangeListener {

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
            Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putInt(context.getResources().getString(R.string.PREFERENCE_ADDITIONAL_INFO_PAGE), position);
            editor.commit();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fragmentManager = getSupportFragmentManager();
        mIPAddressInfoFragment = (IPAddressInfoFragment) fragmentManager.findFragmentByTag(TAG_IP_ADDRESS_INFO_FRAGMENT);
        mAdditionalInfoPager = (ViewPager) findViewById(R.id.other_info_container);
        mAdditionalInfoPageIndicator = (CirclePageIndicator) findViewById(R.id.other_info_indicator);

        if (mIPAddressInfoFragment == null) {
            mIPAddressInfoFragment = new IPAddressInfoFragment();
            fragmentManager.beginTransaction()
                    .add(R.id.ip_address_info_container, mIPAddressInfoFragment, TAG_IP_ADDRESS_INFO_FRAGMENT).commit();
        }

        // NOTE that this hack assumes the fragment positions in the ViewPager
        // corresponding to the FragmentPagerAdapter's SetPages call below
        mNetworkInfoFragment = (NetworkInfoFragment) fragmentManager.findFragmentByTag("android:switcher:"
                + R.id.other_info_container + ":" + 0);
        mProxySettingsFragment = (ProxySettingsFragment) fragmentManager.findFragmentByTag("android:switcher:"
                + R.id.other_info_container + ":" + 1);
        mGeoIPProviderSettingsFragment = (GeoIPProviderSettingsFragment) fragmentManager.findFragmentByTag("android:switcher:"
                + R.id.other_info_container + ":" + 2);

        if (mNetworkInfoFragment == null) {
            mNetworkInfoFragment = new NetworkInfoFragment();
        }

        if (mProxySettingsFragment == null) {
            mProxySettingsFragment = new ProxySettingsFragment();
        }

        if (mGeoIPProviderSettingsFragment == null) {
            mGeoIPProviderSettingsFragment = new GeoIPProviderSettingsFragment();
        }

        mAdditionalInfoPagerAdapter = new AdditionalInfoFragmentPagerAdapter(fragmentManager);
        mAdditionalInfoPagerAdapter.SetPages(new Fragment[] { mNetworkInfoFragment, mProxySettingsFragment,
                mGeoIPProviderSettingsFragment });
        mAdditionalInfoPager.setAdapter(mAdditionalInfoPagerAdapter);
        mAdditionalInfoPageIndicator.setViewPager(mAdditionalInfoPager);
        mAdditionalInfoPageIndicator.setOnPageChangeListener(new AdditionalInfoPageChangeListener(this));

        int additionalInfoCurrentPage = PreferenceManager.getDefaultSharedPreferences(this).getInt(
                getResources().getString(R.string.PREFERENCE_ADDITIONAL_INFO_PAGE), 0);
        mAdditionalInfoPager.setCurrentItem(additionalInfoCurrentPage);

        findViewById(R.id.ip_address_info_container).getViewTreeObserver().addOnGlobalLayoutListener(
                new OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // Compute the best panel height so that as much as
                        // possible of both the IPAddressInfoFragment and the
                        // SlidingUpPanelLayout is showing, without wasting
                        // empty space
                        SlidingUpPanelLayout slidingPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_panel_layout);
                        if (slidingPanelLayout != null) {
                            int ipAddressFragmentHeight = mIPAddressInfoFragment.getOptimalHeight();
                            // TODO: dimen
                            int minimumPanelHeight = (int) (60 * getResources().getDisplayMetrics().density + 0.5f);

                            int optimalSlidingPanelHeight = slidingPanelLayout.getBottom() - ipAddressFragmentHeight;
                            if (mAdMobBannerAdView != null) {
                                optimalSlidingPanelHeight -= mAdMobBannerAdView.getHeight();
                            }
                            if (optimalSlidingPanelHeight > minimumPanelHeight) {
                                slidingPanelLayout.setPanelHeight(optimalSlidingPanelHeight);
                                // Sometimes changing the sliding panel height
                                // leaves a shadow artifact.
                                // This forces the IP Address fragment to
                                // redraw.
                                mIPAddressInfoFragment.getView().invalidate();
                            }
                        }
                    }
                });
        
        AdBuddiz.setPublisherKey(getString(R.string.ad_buddiz_publisher_key));
        AdBuddiz.cacheAds(this);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        if (mAdMobBannerAdView != null) {
            mAdMobBannerAdView.pause();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        initAds();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        if (mAdMobBannerAdView != null) {
            mAdMobBannerAdView.resume();
        }
    }

    @Override
    public void onDestroy() {
        if (mAdMobBannerAdView != null) {
            mAdMobBannerAdView.destroy();
        }
        super.onDestroy();
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
        if (mIPAddressInfoFragment != null && mIPAddressInfoFragment.isVisible()) {
            mIPAddressInfoFragment.makeIPAddressInfoRequest();
        }
        showFullScreenAd();
    }

    public void refreshNetworkInfo() {
        if (mNetworkInfoFragment != null && mNetworkInfoFragment.isVisible()) {
            mNetworkInfoFragment.refresh();
        }
    }

    @Override
    public void onBackPressed() {
        SlidingUpPanelLayout slidingPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_panel_layout);
        if (slidingPanelLayout != null && slidingPanelLayout.isPanelExpanded()) {
                slidingPanelLayout.collapsePanel();
        } else {
            super.onBackPressed();
        }
    }
    
    private int mFullScreenAdCounter = 0;
    private void showFullScreenAd() {
        mFullScreenAdCounter++;
        if (mFullScreenAdCounter % 2 != 0) {
            return;
        }
        
        ArrayList<FullScreenAd> fullScreenAds = new ArrayList<FullScreenAd>();
        fullScreenAds.add(new AdBuddizFullScreenAd());
        fullScreenAds.add(new AdMobFullScreenAd());
        
        Collections.shuffle(fullScreenAds);
        for (FullScreenAd fullScreenAd : fullScreenAds) {
            boolean shown = fullScreenAd.show();
            if (shown) {
                break;
            }
        }
    }
    
    interface FullScreenAd {
        public boolean show();
    }
    
    private class AdMobFullScreenAd implements FullScreenAd {
        public boolean show() { 
            if (mAdMobInterstitialAd != null && mAdMobInterstitialAd.isLoaded()) {
                mAdMobInterstitialAd.show();
                return true;
            }
            return false;
        }
    }
    
    private class AdBuddizFullScreenAd implements FullScreenAd {
        public boolean show() {
            if (AdBuddiz.isReadyToShowAd(MainActivity.this)) {
                AdBuddiz.showAd(MainActivity.this);
                return true;
            }
            return false;
        }
    }
    
    private void initAds() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
            return;
        }
        
        if (mAdMobBannerAdView == null) {
            mAdMobBannerAdView = new AdView(this);
            mAdMobBannerAdView.setAdSize(AdSize.SMART_BANNER);
            mAdMobBannerAdView.setAdUnitId(getString(R.string.banner_ad_unit_id));
            mAdMobBannerAdView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded()
                {
                    View stubView = findViewById(R.id.bannerAdViewStub);
                    if (stubView != null) {
                        ViewGroup parent = (ViewGroup)stubView.getParent();
                        if (parent != null) {
                            int index = parent.indexOfChild(stubView);
                            parent.removeView(stubView);
                            parent.addView(mAdMobBannerAdView, index);
                        }
                    }
                }
                @Override
                public void onAdFailedToLoad(int errorCode)
                {
                    // Set to null so it will be re-initialized the next time
                    mAdMobBannerAdView = null;
                }
            });
            AdRequest bannerAdRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("AFF55E9917949EF5CDAB182729BC72A1")
                .addTestDevice("04CEF367A9A9433242C8C8DCF41D13BC")
                .build();
            mAdMobBannerAdView.loadAd(bannerAdRequest);
        }
        
        initAdMobInterstitial();
    }
    
    private void initAdMobInterstitial() {
        if (mAdMobInterstitialAd == null) {
            mAdMobInterstitialAd = new InterstitialAd(this);
            mAdMobInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
            mAdMobInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(int errorCode) {
                    // Set to null so it will be re-initialized the next time
                    mAdMobInterstitialAd = null;
                }
                @Override
                public void onAdOpened() {
                    loadAdMobInterstitial();
                }
            });
            loadAdMobInterstitial();
        }
    }
    
    private void loadAdMobInterstitial() {
        if (mAdMobInterstitialAd != null) {
            AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("AFF55E9917949EF5CDAB182729BC72A1")
                .addTestDevice("04CEF367A9A9433242C8C8DCF41D13BC")
                .build();
            mAdMobInterstitialAd.loadAd(adRequest);
        }
    }
}
