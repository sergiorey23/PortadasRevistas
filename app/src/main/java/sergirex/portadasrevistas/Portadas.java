package sergirex.portadasrevistas;

import static sergirex.portadasrevistas.SavePortada.permission;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.android.billingclient.api.BillingClient;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.facebook.ads.AudienceNetworkAds;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import sergirex.portadasrevistas.categorias.Cocina;
import sergirex.portadasrevistas.categorias.Corazon;
import sergirex.portadasrevistas.categorias.Cultura;
import sergirex.portadasrevistas.categorias.Deportes;
import sergirex.portadasrevistas.categorias.Favoritos;
import sergirex.portadasrevistas.categorias.Femeninas;
import sergirex.portadasrevistas.categorias.Historia;
import sergirex.portadasrevistas.categorias.LifeStyle;
import sergirex.portadasrevistas.categorias.Masculinas;
import sergirex.portadasrevistas.categorias.Motor;
import sergirex.portadasrevistas.categorias.Tecnologia;

public class Portadas extends AppCompatActivity {
    static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 0;
    static final int MY_PERMISSIONS_REQUEST_POST_NOTIFICATION = 1;
    private DrawerLayout mDrawerLayout;
    private ViewPager2 mViewPager;
    private List<String> categorias;
    private AlertDialog alertDialog;
    private LinearLayout adContainer;
    private AlertDialog alertDialogNoConn;
    private TabLayout tabLayout;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private SharedPreferences prefs;
    private SharedPreferences prefsPor;
    private int favsCount = 0;
    private boolean dark = false;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = prefs.getString("theme", "default");
        switch (theme) {
            case "default":
                switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                    case Configuration.UI_MODE_NIGHT_YES:
                        setTheme(R.style.AppThemeDark);
                        dark = true;
                        break;
                    case Configuration.UI_MODE_NIGHT_NO:
                        setTheme(R.style.AppTheme);
                        break;
                }
                break;
            case "light":
                setTheme(R.style.AppTheme);
                break;
            case "dark":
                setTheme(R.style.AppThemeDark);
                dark = true;
        }
        super.onCreate(savedInstanceState);


        DateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.FRANCE);
        String fecha = formatter.format(new Date());
        boolean descargar = !fecha.equals(prefs.getString("fechaPortadas", ""));

        setContentView(R.layout.activity_portadas);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDrawerLayout = findViewById(R.id.drawerLayout);
        NavigationView mNavigationView = findViewById(R.id.navView);

        tabLayout = findViewById(R.id.tabs);
        prefsPor = getSharedPreferences("revistas", Context.MODE_PRIVATE);

        int rateDialog = prefs.getInt("rate", 0);
        if (rateDialog == 2) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("rate", 0);
            editor.apply();
        }

        if (descargar && !isOnline()) {
            if (alertDialogNoConn == null)
                alertDialogNoConn = createNoConnectionDialog();
            alertDialogNoConn.show();
        } else {
            loadSectionsAdapter();
        }


        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);

        mNavigationView.setNavigationItemSelectedListener(menuItem -> {
            mDrawerLayout.closeDrawers();
            int itemId = menuItem.getItemId();
            if (itemId == R.id.nav_help)
                showDialog().show();
            else if (itemId == R.id.nav_subscribe) {
                PurchaseInAppProduct purchaseInAppProduct = new PurchaseInAppProduct(this, "premium_sub_id", BillingClient.ProductType.SUBS, getString(R.string.subscribe), adContainer);
                purchaseInAppProduct.launchInAppProduct();
            } else if (itemId == R.id.nav_remove_ads) {
                PurchaseInAppProduct purchaseInAppProduct = new PurchaseInAppProduct(this, "remove_ads_id", BillingClient.ProductType.INAPP, getString(R.string.purchase), adContainer);
                purchaseInAppProduct.launchInAppProduct();
            } else if (itemId == R.id.nav_rate)
                launchMarket();
            else if (itemId == R.id.nav_share) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_full_name));
                String sAux = "Descarga la app de Portadas gratis!\n\nhttps://play.google.com/store/apps/details?id=" + getPackageName();
                i.putExtra(Intent.EXTRA_TEXT, sAux);
                startActivity(Intent.createChooser(i, null));
            } else if (itemId == R.id.nav_about) {
                showAboutInfo();
            } else if (itemId == R.id.nav_settings) {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
            }
            return true;
        });

        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.open_menu,
                R.string.close_menu);

        mDrawerLayout.addDrawerListener(mDrawerToggle);

        mDrawerToggle.syncState();

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT > 33) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        MY_PERMISSIONS_REQUEST_POST_NOTIFICATION);
            }
        } else {
            if (Build.VERSION.SDK_INT <= 33) {
                startAlarmBroadcastReceiver(this);
            }
        }


        if (!prefs.getBoolean("remove_ads_id", false) && !prefs.getBoolean("premium_sub_id", false)) {
            AudienceNetworkAds.initialize(this);
            //AdSettings.setTestMode(true);
            // Instantiate an AdView object.
            // NOTE: The placement ID from the Facebook Monetization Manager identifies your App.
            // To get test ads, add IMG_16_9_APP_INSTALL# to your placement id. Remove this when your app is ready to serve real ads.
            boolean isPhone = getResources().getBoolean(R.bool.isPhone);
            AdView bottomBanner;
            if (isPhone) {
                bottomBanner = new AdView(this, "483584450995789_487598283927739", AdSize.BANNER_HEIGHT_50);
            } else {
                bottomBanner = new AdView(this, "483584450995789_487598283927739", AdSize.BANNER_HEIGHT_90);
            }

            // Find the Ad Container
            adContainer = findViewById(R.id.bannerContainer);
            adContainer.addView(bottomBanner);
            bottomBanner.loadAd();
        }
        PurchaseInAppProduct subProduct = new PurchaseInAppProduct(this, "premium_sub_id", BillingClient.ProductType.SUBS, getString(R.string.subscribe), adContainer);
        subProduct.restorePurchases();

        if (prefs.getBoolean("remove_ads_id", false)) {
            hideRemoveAdsMenuItem(mNavigationView, R.id.nav_remove_ads);
        }
    }

    private void hideRemoveAdsMenuItem(NavigationView mNavigationView, int resource) {
        Menu nav_Menu = mNavigationView.getMenu();
        nav_Menu.findItem(resource).setVisible(false);
    }

    private void setFavoriteIconVisible(boolean visible) {
        if (menu != null)
            menu.findItem(R.id.fav).setVisible(visible);
    }


    public void startAlarmBroadcastReceiver(Context context) {
        AlarmBroadcastReceiver alarmBroadcastReceiver = new AlarmBroadcastReceiver();
        alarmBroadcastReceiver.startAlarmBroadcastReceiver(context, false);
    }

    void loadSectionsAdapter() {
        mViewPager = findViewById(R.id.viewpager);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), getLifecycle());
        if (!prefsPor.getAll().isEmpty()) {
            mSectionsPagerAdapter.addFragment(getString(R.string.fav_tab), new Favoritos());
            favsCount = prefsPor.getAll().size();
        }
        mSectionsPagerAdapter.addFragment(getString(R.string.first_tab), new Corazon());
        mSectionsPagerAdapter.addFragment(getString(R.string.second_tab), new Cultura());
        mSectionsPagerAdapter.addFragment(getString(R.string.third_tab), new Deportes());
        mSectionsPagerAdapter.addFragment(getString(R.string.fourth_tab), new LifeStyle());
        mSectionsPagerAdapter.addFragment(getString(R.string.fifth_tab), new Femeninas());
        mSectionsPagerAdapter.addFragment(getString(R.string.sixth_tab), new Masculinas());
        mSectionsPagerAdapter.addFragment(getString(R.string.seventh_tab), new Motor());
        mSectionsPagerAdapter.addFragment(getString(R.string.eight_tab), new Tecnologia());
        mSectionsPagerAdapter.addFragment(getString(R.string.nineth_tab), new Cocina());
        mSectionsPagerAdapter.addFragment(getString(R.string.tenth_tab), new Historia());

        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(5);
        new TabLayoutMediator(tabLayout, mViewPager,
                (tab, position) -> tab.setText(categorias.get(position))
        ).attach();
        int lpValue = Integer.parseInt(prefs.getString("init_category", "1"));
        if (prefsPor.getAll().isEmpty()) {
            mViewPager.setCurrentItem(lpValue - 1, false);
        } else {
            mViewPager.setCurrentItem(lpValue, false);
        }
    }

    void showAboutInfo() {
        View aboutLayout = getLayoutInflater().inflate(R.layout.about, mDrawerLayout, false);
        int theme = dark ? R.style.AppThemeDark : R.style.AppTheme;
        AlertDialog.Builder builder = new AlertDialog.Builder(this, theme);
        builder.setPositiveButton(R.string.close, null);

        builder.setView(aboutLayout);
        TextView appSource = aboutLayout.findViewById(R.id.appSource);
        TextView tv = aboutLayout.findViewById(R.id.appVersion);

        tv.setText("v".concat(BuildConfig.VERSION_NAME));
        appSource.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://www.lasportadas.es/"));
            startActivity(intent);
        });

        TextView privacy = aboutLayout.findViewById(R.id.privacy_policy);
        privacy.setOnClickListener(view -> {
            Uri uri = Uri.parse("https://www.lasportadas.es/privacy.php");
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
            try {
                startActivity(browserIntent);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            }
        });
//        Button paypal = aboutLayout.findViewById(R.id.paypal);
//        paypal.setOnClickListener(view -> {
//            Uri uri = Uri.parse("https://www.paypal.com/donate?hosted_button_id=4H8LT2PVZEE78");
//            Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
//            browserIntent.setPackage("com.instagram.android.p2pmobile");
//
//            try {
//                startActivity(browserIntent);
//            } catch (ActivityNotFoundException e) {
//                startActivity(new Intent(Intent.ACTION_VIEW, uri));
//            }
//        });

        Button contact = aboutLayout.findViewById(R.id.contact);
        contact.setOnClickListener(view -> {
            Uri uri = Uri.parse("mailto:sssergiooo23@gmail.com");
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, uri);
            emailIntent.setData(uri);
            startActivity(emailIntent);
        });
        Button portadas_periodicos = aboutLayout.findViewById(R.id.portadas_periodicos);
        portadas_periodicos.setOnClickListener(v -> {
            Uri uri = Uri.parse("market://details?id=sergirex.portadasperiodicos");
            Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
            try {
                startActivity(myAppLinkToMarket);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, " unable to find market app", Toast.LENGTH_LONG).show();
            }
        });
        builder.create();
        builder.show();
    }

    private AlertDialog showDialog() {
        if (alertDialog == null) {
            int theme = dark ? R.style.AppThemeDark : R.style.AppTheme;
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this, theme);
            alertDialogBuilder.setTitle(R.string.help);
            alertDialogBuilder.setIcon(R.mipmap.magazines_icon);
            alertDialogBuilder.setPositiveButton("OK", null);
            alertDialog = alertDialogBuilder.create();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                alertDialog.setMessage(Html.fromHtml("<Big>" + getString(R.string.help_description) + "<br/><br/>" + getString(R.string.help_description2) + "</Big>" + "<br/><br/>" + getString(R.string.rate_app), Html.FROM_HTML_MODE_LEGACY));
            else
                alertDialog.setMessage(Html.fromHtml("<Big>" + getString(R.string.help_description) + "<br/><br/>" + getString(R.string.help_description2) + "</Big>" + "<br/><br/>" + getString(R.string.rate_app)));
        }
        return alertDialog;
    }

    private void launchMarket() {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(myAppLinkToMarket);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, " unable to find market app", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * A {@link FragmentStateAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    private class SectionsPagerAdapter extends FragmentStateAdapter {
        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<Long> fragmentsIds = new ArrayList<>();

        void addFragment(String title, Fragment fragment) {
            categorias.add(title);
            mFragments.add(fragment);
            fragmentsIds.add((long) fragment.hashCode());
        }

        void addFirstFragment(String title, Fragment fragment) {
            categorias.add(0, title);
            mFragments.add(0, fragment);
            new TabLayoutMediator(tabLayout, mViewPager,
                    (tab, position) -> tab.setText(categorias.get(position))
            ).attach();
            fragmentsIds.add(0, (long) fragment.hashCode());
        }

        @Override
        public long getItemId(int position) {
            return fragmentsIds.get(position);
        }

        @Override
        public boolean containsItem(long itemId) {
            return fragmentsIds.contains(itemId);
        }

        void removeFragment() {
            tabLayout.removeTabAt(0);
            categorias.remove(0);
            mFragments.remove(0);
            fragmentsIds.remove(0);
        }

        public SectionsPagerAdapter(FragmentManager fm, Lifecycle lifecycle) {
            super(fm, lifecycle);
            categorias = new ArrayList<>();
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getItemCount() {
            return categorias.size();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        this.menu = menu;
        if (!prefsPor.getAll().isEmpty()) {
            setFavoriteIconVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.fav) {
            mViewPager.setCurrentItem(0);
            return true;
        }
        return true;
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static void scanFile(Context ctxt, File f, String mimeType) {
        MediaScannerConnection
                .scanFile(ctxt, new String[]{f.getAbsolutePath()},
                        new String[]{mimeType}, null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            final Uri contentUri = Uri.fromFile(f);
            scanIntent.setData(contentUri);
            ctxt.sendBroadcast(scanIntent);
        } else {
            final Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory()));
            ctxt.sendBroadcast(intent);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_STORAGE) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (permission == 1) {
                    Toast.makeText(this, "Permiso concedido. Vuelva a relizar la operacíon", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Para poder compartir y guardar fotos, es necesario otorgar permisos de almacenamiento", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == MY_PERMISSIONS_REQUEST_POST_NOTIFICATION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startAlarmBroadcastReceiver(getApplicationContext());
            }
            requestWritePermission();
        }
    }

    public void requestWritePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
            }
        }
    }

    public AlertDialog createNoConnectionDialog() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(" Error de conexión");
        alertDialogBuilder.setIcon(R.mipmap.magazines_icon);
        alertDialogBuilder.setPositiveButton("Reintentar", (dialog, id) -> {
                    if (isOnline())
                        loadSectionsAdapter();
                    else
                        alertDialogBuilder.show();
                })
                .setNegativeButton("Cencelar", (dialog, id) -> alertDialogNoConn.dismiss());
        alertDialogBuilder.setMessage("No hay conexión a internet. Por favor, comprueba tu conexión");
        return alertDialogBuilder.create();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String theme = prefs.getString("theme", "default");

        if (theme.equals("dark") && !dark || theme.equals("light") && dark) {
            recreate();
        } else if (theme.equals("default")) {
            switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                case Configuration.UI_MODE_NIGHT_YES:
                    if (!dark) {
                        recreate();
                    }
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                    if (dark) {
                        recreate();
                    }
            }
        }
        /*if (mBottomBanner != null) {
            mBottomBanner.resume();
        }*/

        if (mSectionsPagerAdapter == null) {
            return;
        }
        int count = prefsPor.getAll().size();
        if (count > 0) {
            if (!categorias.get(0).equals(getString(R.string.fav_tab))) {
                mSectionsPagerAdapter.addFirstFragment(getString(R.string.fav_tab), new Favoritos());
                mSectionsPagerAdapter.notifyItemInserted(0);
                setFavoriteIconVisible(true);
                mViewPager.setCurrentItem(0);
            } else if (favsCount != count) {
                mSectionsPagerAdapter.removeFragment();
                mSectionsPagerAdapter.addFirstFragment(getString(R.string.fav_tab), new Favoritos());
                mSectionsPagerAdapter.notifyItemChanged(0);
                favsCount = count;
            }
        } else if (mSectionsPagerAdapter.getItemCount() > 10) {
            mSectionsPagerAdapter.removeFragment();
            mSectionsPagerAdapter.notifyItemRemoved(0);
            setFavoriteIconVisible(false);
        }

        if (prefs.getBoolean("premium_sub_id", false)) {
            if (adContainer != null)
                adContainer.removeAllViews();
            NavigationView mNavigationView = findViewById(R.id.navView);
            hideRemoveAdsMenuItem(mNavigationView, R.id.nav_remove_ads);
            hideRemoveAdsMenuItem(mNavigationView, R.id.nav_subscribe);
        }
    }
    /*@Override
    protected void onPause() {
        super.onPause();
        if (bottomBanner != null) {
            bottomBanner.pause();
        }
    }*/


    @Override
    protected void onDestroy() {
        AdView adView = (AdView) adContainer.getChildAt(0);
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }
}
