/*
 Copyright Michal Buczek, 2014
 All rights reserved.

This file is part of Freestyle Timer.

    Freestyle Timer is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    Freestyle Timer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Freestyle Timer; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */


package bucci.dev.freestyle;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class StartActivity extends FragmentActivity {
    private static final boolean DEBUG = false;
    private static final String TAG = "BCC|StartActivity";

    public static final String TIMER_TYPE = "timer_type";
    public static final int TIMER_MODES_COUNT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_start);

        BattleTypeAdapter mAdapter = new BattleTypeAdapter(getSupportFragmentManager());
        ViewPager mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    public void goToTimer(View view) {
        Intent intent = new Intent(this, TimerActivity.class);
        TimerType battleType = (TimerType) view.getTag();

        intent.putExtra(TIMER_TYPE, battleType);
        startActivity(intent);
    }


    public class BattleTypeAdapter extends FragmentPagerAdapter {
        public BattleTypeAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = new PageFragment();
            Bundle bundle = new Bundle();

            //Timer type positions are hardcoded, hence the 'magic numbers' bad practice
            //TODO Change hardcoded number to something better
            switch (position) {
                case 0:
                    bundle.putSerializable(TIMER_TYPE, TimerType.BATTLE);
                    break;
                case 1:
                    bundle.putSerializable(TIMER_TYPE, TimerType.QUALIFICATION);
                    break;
                case 2:
                    bundle.putSerializable(TIMER_TYPE, TimerType.ROUTINE);
                    break;
                default:
                    if (DEBUG) Log.e(TAG, "BattleTypeAdapter getItem() error with setting timer type to position");
                    break;
            }

            fragment.setArguments(bundle);

            return fragment;

        }


        @Override
        public int getCount() {
            return TIMER_MODES_COUNT;
        }
    }

    public static class PageFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View swipeView = inflater.inflate(R.layout.page_fragment, container, false);
            Bundle bundle = getArguments();
            ImageView timerTypeImageView = (ImageView) swipeView.findViewById(R.id.timer_type_image);
            TextView timerTypeText = (TextView) swipeView.findViewById(R.id.timer_type_text);

            TimerType timerType = (TimerType) bundle.getSerializable(TIMER_TYPE);

            switch (timerType) {
                case BATTLE:
                    timerTypeImageView.setImageResource(R.drawable.battle);
                    timerTypeText.setText(getString(R.string.timer_type_battle));
                    break;
                case QUALIFICATION:
                    timerTypeImageView.setImageResource(R.drawable.qualification);
                    timerTypeText.setText(getString(R.string.timer_type_qualification));
                    break;
                case ROUTINE:
                    timerTypeImageView.setImageResource(R.drawable.routine);
                    timerTypeText.setText(getString(R.string.timer_type_routine));
                    break;
            }

            timerTypeImageView.setTag(timerType);

            return swipeView;
        }

    }
}
