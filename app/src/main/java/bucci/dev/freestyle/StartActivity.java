package bucci.dev.freestyle;

import android.content.Intent;
import android.graphics.Typeface;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class StartActivity extends FragmentActivity {
    private static final String TAG = "BCC|StartActivity";

    public static final String TIMER_TYPE = "timer_type";
    public static final int TIMER_TYPE_COUNT = 3;

    public static final char TYPE_BATTLE = 'B';
    public static final char TYPE_ROUTINE = 'R';
    public static final char TYPE_PRACTICE = 'P';


    BattleTypeAdapter mAdapter;
    ViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_start);

        mAdapter = new BattleTypeAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void goToTimer(View view) {
        Intent intent = new Intent(this, TimerActivity.class);

        Character battleType = (Character) view.getTag();

        switch (battleType) {
            case TYPE_BATTLE:
                intent.putExtra(TIMER_TYPE, TYPE_BATTLE);
                break;
            case TYPE_ROUTINE:
                intent.putExtra(TIMER_TYPE, TYPE_ROUTINE);
                break;
            case TYPE_PRACTICE:
                intent.putExtra(TIMER_TYPE, TYPE_PRACTICE);
                break;
        }

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

            char timerTypeForGivenPosition = 'X';

            switch (position) {
                case 0:
                    timerTypeForGivenPosition = TYPE_BATTLE;
                    break;
                case 1:
                    timerTypeForGivenPosition = TYPE_ROUTINE;
                    break;
                case 2:
                    timerTypeForGivenPosition = TYPE_PRACTICE;
                    break;
            }

            bundle.putChar(TIMER_TYPE, timerTypeForGivenPosition);

            fragment.setArguments(bundle);

            return fragment;

        }


        @Override
        public int getCount() {
            return TIMER_TYPE_COUNT;
        }
    }

    public static class PageFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View swipeView = inflater.inflate(R.layout.page_fragment, container, false);
            Bundle bundle = getArguments();
            char timerType = bundle.getChar(TIMER_TYPE);
            ImageView timerTypeImage = (ImageView) swipeView.findViewById(R.id.timer_type_image);
            TextView timerTypeText = (TextView) swipeView.findViewById(R.id.timer_type_text);

            switch (timerType) {
                case TYPE_BATTLE:
                    timerTypeImage.setImageResource(R.drawable.battle);
                    timerTypeText.setText("Battle");
                    break;
                case TYPE_ROUTINE:
                    timerTypeImage.setImageResource(R.drawable.routine);
                    timerTypeText.setText("Routine");
                    break;
                case TYPE_PRACTICE:
                    timerTypeImage.setImageResource(R.drawable.practice);
                    timerTypeText.setText("Practice");
                    break;
            }

            timerTypeImage.setTag(timerType);

            return swipeView;
        }

    }
}
