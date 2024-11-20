package sergirex.portadasrevistas;

import static sergirex.portadasrevistas.Portadas.MY_PERMISSIONS_REQUEST_WRITE_STORAGE;
import static sergirex.portadasrevistas.Portadas.scanFile;
import static sergirex.portadasrevistas.SavePortada.permission;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


public class PortadaDetalle extends AppCompatActivity {

    private Portada portada;
    private SharedPreferences prefsPer;
    private InterstitialAd interstitialAd;
    private ViewPager2 mViewPager2;
    private ViewPagerAdapter mSectionsPagerAdapter;
    private boolean clicked = false;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.FRANCE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupTheme();
        setContentView(R.layout.activity_portada_detalle);
        setupToolbar();

        int showAd = getIntent().getIntExtra("showAd", 0);

        prefsPer = getSharedPreferences("revistas", Context.MODE_PRIVATE);

        setupFabButtons();
        loadSectionsAdapter();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        handleAdsAndRating(prefs, showAd);
    }

    private void setupTheme() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = prefs.getString("theme", "default");
        boolean isDark = (theme.equals("default") && (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
                || theme.equals("dark");

        setTheme(isDark ? R.style.AppThemeDark : R.style.AppTheme);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        toolbar.setNavigationContentDescription(R.string.back);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupFabButtons() {
        FloatingActionButton webBtn = findViewById(R.id.httpButton);
        FloatingActionButton favBtn = findViewById(R.id.favButton);
        FloatingActionButton addBtn = findViewById(R.id.addButton);

        webBtn.setOnClickListener(v -> openWebsite());
        favBtn.setOnClickListener(v -> toggleFavorite(favBtn));
        addBtn.setOnClickListener(v -> toggleFabMenu(webBtn, favBtn, addBtn));
    }

    private void openWebsite() {
        if (portada == null || portada.getWebRevista().isEmpty()) {
            Toast.makeText(this, "No existe la página web", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int colorResId = isDarkMode() ? R.color.colorPrimaryDark : R.color.colorPrimary;
            String url = "https://www." + portada.getWebRevista();

            CustomTabColorSchemeParams colorScheme = new CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(getResources().getColor(colorResId))
                    .build();
            CustomTabsIntent intent = new CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .setDefaultColorSchemeParams(colorScheme)
                    .build();

            intent.launchUrl(this, Uri.parse(url));
        } catch (ActivityNotFoundException e) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www." + portada.getWebRevista()));
            startActivity(intent);
        }
    }

    private void toggleFavorite(FloatingActionButton favFab) {
        SharedPreferences.Editor editor = prefsPer.edit();
        if (prefsPer.contains(portada.getTitle())) {
            editor.remove(portada.getTitle());
            favFab.setImageResource(R.drawable.ic_favorite_border_black_24dp);
        } else {
            editor.putString(portada.getTitle(), portada.getRevista());
            favFab.setImageResource(R.drawable.ic_favorite_black_24dp);
        }
        editor.apply();
    }

    private void toggleFabMenu(FloatingActionButton webBtn, FloatingActionButton favBtn, FloatingActionButton addBtn) {
        int visibility = clicked ? View.INVISIBLE : View.VISIBLE;
        webBtn.setVisibility(visibility);
        favBtn.setVisibility(visibility);

        webBtn.setClickable(!clicked);
        favBtn.setClickable(!clicked);

        if (!clicked) {
            webBtn.startAnimation(AnimationUtils.loadAnimation(this, R.anim.from_bottom_anim));
            favBtn.startAnimation(AnimationUtils.loadAnimation(this, R.anim.from_bottom_anim));
            addBtn.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_open_anim));
        } else {
            webBtn.startAnimation(AnimationUtils.loadAnimation(this, R.anim.to_bottom_anim));
            favBtn.startAnimation(AnimationUtils.loadAnimation(this, R.anim.to_bottom_anim));
            addBtn.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_close_anim));
        }
        clicked = !clicked;
    }

    private boolean isDarkMode() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = prefs.getString("theme", "default");
        return theme.equals("dark") || (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private void handleAdsAndRating(SharedPreferences prefs, int showAd) {
        int rate = prefs.getInt("rate", 0);
        if (rate == 0 && showAd % 2 == 0) {
            SharedPreferences.Editor editor = prefs.edit();
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.rateTitle)
                    .setMessage(R.string.rateDescription)
                    .setIcon(R.mipmap.magazines_icon)
                    .setNeutralButton("No", (dialog, which) -> {
                        editor.putInt("rate", 1);
                        editor.apply();
                    }).setNegativeButton(R.string.later, (dialog, which) -> {
                        editor.putInt("rate", 2);
                        editor.apply();
                    }).setPositiveButton(R.string.sure, (dialog, which) -> {
                        editor.putInt("rate", 1);
                        editor.apply();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            ReviewManager manager = ReviewManagerFactory.create(this);
                            Task<ReviewInfo> request = manager.requestReviewFlow();
                            request.addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    // We can get the ReviewInfo object
                                    ReviewInfo reviewInfo = task.getResult();
                                    Task<Void> flow = manager.launchReviewFlow(this, reviewInfo);
                                    flow.addOnCompleteListener(t -> Toast.makeText(this, "¡Gracias!", Toast.LENGTH_LONG).show());
                                } else {
                                    // There was some problem, log or handle the error code.
                                    Log.e("Review Error", task.getException().getMessage());
                                }
                            });
                        } else {
                            Uri uri = Uri.parse("market://details?id=" + getPackageName());
                            Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
                            try {
                                startActivity(myAppLinkToMarket);
                            } catch (ActivityNotFoundException e) {
                                Toast.makeText(this, " unable to find market app", Toast.LENGTH_LONG).show();
                            }
                        }
                    }).show();
        }
        if (!prefs.getBoolean("remove_ads_id", false) && !prefs.getBoolean("premium_sub_id", false)) {
            AudienceNetworkAds.initialize(this);
            //AdSettings.setTestMode(true);
            AdView bottomBanner = new AdView(this, "483584450995789_487598390594395", AdSize.BANNER_HEIGHT_50);
            // Find the Ad Container
            LinearLayout adContainer = findViewById(R.id.bannerContainerDetail);
            adContainer.addView(bottomBanner);
            bottomBanner.loadAd();
            if (showAd % 3 == 0) {
                interstitialAd = new InterstitialAd(this, "483584450995789_487598570594377");
                InterstitialAdListener interstitialAdListener = new InterstitialAdListener() {

                    @Override
                    public void onInterstitialDisplayed(Ad ad) {
                        // Interstitial ad displayed callback
                    }

                    @Override
                    public void onInterstitialDismissed(Ad ad) {
                        // Interstitial dismissed callback
                    }

                    @Override
                    public void onError(Ad ad, AdError adError) {
                        // Ad error callback
                        Log.e("AD", "Interstitial ad failed to load: " + adError.getErrorMessage());
                    }

                    @Override
                    public void onAdLoaded(Ad ad) {
                        // Interstitial ad is loaded and ready to be displayed
                        // Show the ad
                        interstitialAd.show();
                    }

                    @Override
                    public void onAdClicked(Ad ad) {
                        // Ad clicked callback
                    }

                    @Override
                    public void onLoggingImpression(Ad ad) {
                        // Ad impression logged callback
                    }
                };

                interstitialAd.loadAd(interstitialAd.buildLoadAdConfig()
                        .withAdListener(interstitialAdListener)
                        .build());
            }
        }
    }

    void loadSectionsAdapter() {
        if (!isOnline()) {
            new NoConnectionDialogFragment().show(getSupportFragmentManager(), "no_connection_dialog");
            return;
        }

        mViewPager2 = findViewById(R.id.viewpager2);
        String[] portadas = getIntent().getStringArrayExtra("Portadas");
        String selectedRevistaId = getIntent().getStringExtra("Id");

        mSectionsPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        int selectedPos = setupPortadas(portadas, selectedRevistaId);

        mViewPager2.setAdapter(mSectionsPagerAdapter);
        mViewPager2.setCurrentItem(selectedPos, false);
    }

    private int setupPortadas(String[] portadas, String selectedId) {
        int pos = 0;
        if (portadas != null) {
            for (int i = 0; i < portadas.length; i++) {
                Portada portada = parsePortada(portadas[i]);
                if (portada.getId().equals(selectedId)) {
                    pos = i;
                    this.portada = portada;
                }
                mSectionsPagerAdapter.addFragment(PortadaDetalleFragment.newInstance(portada.getId(), portada.getTitle(), 100, "74"));
                mSectionsPagerAdapter.addPortada(portada);
            }
        }
        return pos;
    }

    private Portada parsePortada(String portadaStr) {
        String[] parts = portadaStr.split(":");
        String id = parts[0];
        String title = parts[1];
        String webMagazine = parts.length > 2 ? parts[2] : "";
        int pages = parts.length > 3 ? Integer.parseInt(parts[3]) : 100;
        String scale = parts.length > 4 ? parts[4] : "74";

        return new Portada(id, title, webMagazine, portadaStr, pages, scale);
    }

    public class ViewPagerAdapter extends FragmentStateAdapter {
        private final List<PortadaDetalleFragment> fragments = new ArrayList<>();
        private final List<Portada> portadas = new ArrayList<>();

        public ViewPagerAdapter(@NonNull FragmentManager fm, @NonNull Lifecycle lifecycle) {
            super(fm, lifecycle);
            fm.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @Nullable Bundle savedInstanceState) {
                    super.onFragmentCreated(fm, f, savedInstanceState);
                    if (getPortada(mViewPager2.getCurrentItem()).getCurrentDate() == null)
                        getHistoryDates();
                }

                @Override
                public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                    super.onFragmentResumed(fm, f);
                    portada = getPortada(mViewPager2.getCurrentItem());
                    FloatingActionButton fabfav = findViewById(R.id.favButton);
                    if (prefsPer.contains(portada.getTitle())) {
                        fabfav.setImageResource(R.drawable.ic_favorite_black_24dp);
                    } else {
                        fabfav.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                    }
                }
            }, true);
        }

        public void addFragment(Fragment fragment) {
            fragments.add((PortadaDetalleFragment) fragment);
        }

        public void addPortada(Portada portada) {
            portadas.add(portada);
        }

        @NonNull
        @Override
        public PortadaDetalleFragment createFragment(int position) {
            return fragments.get(position);
        }

        @Override
        public int getItemCount() {
            return fragments.size();
        }

        public Portada getPortada(int pos) {
            return portadas.get(pos);
        }

    }

    private void getHistoryDates() {
        new Thread(() -> {
            String result;
            try {
                URL url = new URL("https://www.kioskoymas.com/Api_postcalendar/getHistory/" + portada.getId());

                // Lee la respuesta del servidor
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                result = reader.readLine(); // Leer solo la primera línea de respuesta
                String[] dates = result.split("\\|");
                //portada = getPortada(mViewPager2.getCurrentItem());
                portada.setCalendar(dates);
                reader.close();
                runOnUiThread(() -> mSectionsPagerAdapter.createFragment(mViewPager2.getCurrentItem()).setCoverByDate(portada.getLastDate()));
            } catch (Exception e) {
                e.printStackTrace();
                result = "Error: " + e.getMessage();
                Log.e("HTTP Error", result);
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Portada no encontrada", Toast.LENGTH_LONG).show());
            }
        }).start();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("premium_sub_id", false)) {
            String date = sdf.format(new Date(portada.getCurrentDate()));
            if (!date.equals(prefs.getString("fechaPortadas", ""))) {
                SharedPreferences sp = getSharedPreferences(portada.getId(), MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.remove("currentPage");
                editor.apply();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_portada, menu);
        return true;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.date) {
            long longCurrentDate;
            String currentDate = portada.getCurrentDate();
            try {
                Date date = currentDate == null || currentDate.isEmpty() ?
                        new Date() : sdf.parse(currentDate);
                assert date != null;
                longCurrentDate = date.getTime();
            } catch (ParseException e) {
                longCurrentDate = new Date().getTime();
            }

            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder
                    .datePicker()
                    .setTitleText("Select date").setSelection(longCurrentDate)
                    .setCalendarConstraints(new CalendarConstraints.Builder().setValidator(new CalendarConstraints.DateValidator() {
                        @Override
                        public int describeContents() {
                            return 0;
                        }

                        @Override
                        public void writeToParcel(@NonNull Parcel dest, int flags) {

                        }

                        @Override
                        public boolean isValid(long date) {
                            String dateString = sdf.format(new Date(date));
                            List<String> calendar = portada.getCalendar();
                            return calendar != null && calendar.contains(dateString);
                        }
                    }).build()).build();
            datePicker.addOnPositiveButtonClickListener(
                    longSelectedDate -> {
                        String selectedDate = sdf.format(new Date(longSelectedDate));
                        if (!Objects.equals(currentDate, selectedDate)) {
                            portada.setCurrentDate(selectedDate);
                            SharedPreferences sp = getSharedPreferences(portada.getId(), MODE_PRIVATE);
                            SharedPreferences.Editor editor = sp.edit();
                            editor.remove("currentPage");
                            editor.apply();
                            mSectionsPagerAdapter.createFragment(mViewPager2.getCurrentItem()).setCoverByDate(selectedDate);
                        }
                    });
            datePicker.show(getSupportFragmentManager(), "MATERIAL_DATE_PICKER");
        } else if (itemId == R.id.share) {
            SavePortada savePortada = new SavePortada(this);
            ImageView imageView = findViewById(R.id.imagen_extendida);
            Uri fileURI = savePortada.saveFile(portada.getTitle(), ((BitmapDrawable) imageView.getDrawable()).getBitmap());
            Intent i = new Intent(Intent.ACTION_SEND);
            i.putExtra(Intent.EXTRA_STREAM, fileURI);
            i.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=" + getPackageName());
            i.setType("image/jpeg");
            Intent chooser = Intent.createChooser(i, "Compartir portada");
            startActivity(chooser);
            return true;
        } else if (itemId == R.id.save) {
            SavePortada savePortada = new SavePortada(this);
            if (savePortada.isExternalStorageWritable()) {
                if (!savePortada.checkPermissions())
                    return false;
                File file;
                if ((file = new File(savePortada.getAlbumStorageDir() + File.separator + portada.getTitle() + ".jpg")).exists()) {
                    Snackbar sb = Snackbar.make(findViewById(R.id.imagen_extendida), "Ya se ha guardado la portada", Snackbar.LENGTH_SHORT);
                    sb.setAnimationMode(Snackbar.ANIMATION_MODE_FADE);
                    sb.show();
                    return true;
                }
                ImageView imageView = findViewById(R.id.imagen_extendida);
                savePortada.saveFile(savePortada.getAlbumStorageDir(), portada.getTitle(), ((BitmapDrawable) imageView.getDrawable()).getBitmap());

                scanFile(this, file, "images/jp" +
                        "eg");

                Snackbar sb = Snackbar.make(findViewById(R.id.imagen_extendida), "Portada guarda correctamente en " + file.getAbsolutePath(), Snackbar.LENGTH_SHORT);
                sb.setAnimationMode(Snackbar.ANIMATION_MODE_FADE);
                sb.show();
            } else {
                Toast.makeText(this, "Internal Storage unreadable", Toast.LENGTH_LONG).show();
            }
            return true;
        }

        return false;
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_STORAGE) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (permission == 1) {
                    Snackbar sb = Snackbar.make(findViewById(R.id.imagen_extendida), "Permiso concedido. Vuelva a relizar la operacíon", Snackbar.LENGTH_LONG);
                    sb.setAnimationMode(Snackbar.ANIMATION_MODE_FADE);
                    sb.show();
                }
            } else {
                Snackbar sb = Snackbar.make(findViewById(R.id.imagen_extendida), "Para poder compartir y guardar fotos, es necesario otorgar permisos de almacenamiento", Snackbar.LENGTH_LONG);
                sb.setAnimationMode(Snackbar.ANIMATION_MODE_FADE);
                sb.show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (interstitialAd != null) {
            interstitialAd.destroy();
        }
    }
}
