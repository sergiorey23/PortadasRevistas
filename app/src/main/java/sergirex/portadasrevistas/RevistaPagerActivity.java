package sergirex.portadasrevistas;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

public class RevistaPagerActivity extends FragmentActivity {

    FragmentStateAdapter pagerAdapter;
    ViewPager2 viewPager;
    String title;
    SharedPreferences sp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = prefs.getString("theme", "default");
        switch (theme) {
            case "default":
                if ((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                    setTheme(R.style.AppThemeDark);
                }
                break;
            case "dark":
                setTheme(R.style.AppThemeDark);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.revista_pager_activity);


        Toolbar toolbar = findViewById(R.id.toolbar);
        title = getIntent().getStringExtra("title");
        toolbar.setTitle(title);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        sp = getSharedPreferences(title, MODE_PRIVATE);

        String id = getIntent().getStringExtra("id");
        String date = getIntent().getStringExtra("date");
        int pages = getIntent().getIntExtra("pages", 100);
        String scale = getIntent().getStringExtra("scale");
        viewPager = findViewById(R.id.viewPagerRevista);
        pagerAdapter = new ScreenSlidePagerAdapter(this, id, date, pages, scale);
        viewPager.setAdapter(pagerAdapter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            viewPager.setPageTransformer(new DepthPageTransformer());
        }
        int currentPage = sp.getInt("currentPage",0);
        if(currentPage > 0){
            viewPager.setCurrentItem(currentPage, false);
        }
    }


    @Override
    public void onBackPressed() {
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("currentPage",viewPager.getCurrentItem());
        editor.apply();
        super.onBackPressed();
    }

    private static class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        private final String id;
        private final String date;
        private final int size;
        private final String scale;

        public ScreenSlidePagerAdapter(FragmentActivity fa, String id, String date, int size, String scale) {
            super(fa);
            this.id = id;
            this.date = date;
            this.size = size;
            this.scale = scale;
            createFragment(0);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return RevistaFragmentPage.newInstance(id, date, position + 1, scale);
        }

        @Override
        public int getItemCount() {
            return size;
        }
    }

    @RequiresApi(21)
    public static class DepthPageTransformer implements ViewPager2.PageTransformer {
        private static final float MIN_SCALE = 0.75f;

        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0f);

            } else if (position <= 0) { // [-1,0]
                // Use the default slide transition when moving to the left page.
                view.setAlpha(1f);
                view.setTranslationX(0f);
                view.setTranslationZ(0f);
                view.setScaleX(1f);
                view.setScaleY(1f);

            } else if (position <= 1) { // (0,1]
                // Fade the page out.
                view.setAlpha(1 - position);

                // Counteract the default slide transition.
                view.setTranslationX(pageWidth * -position);
                // Move it behind the left page
                view.setTranslationZ(-1f);

                // Scale the page down (between MIN_SCALE and 1).
                float scaleFactor = MIN_SCALE
                        + (1 - MIN_SCALE) * (1 - Math.abs(position));
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0f);
            }
        }
    }


}