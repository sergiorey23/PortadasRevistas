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
import android.view.animation.Animation;
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
    private AlertDialog alertDialogNoConn;
    private InterstitialAd interstitialAd;
    private ViewPager2 mViewPager2;
    private ViewPagerAdapter mSectionsPagerAdapter;
    private boolean clicked = false;
    private MaterialDatePicker<Long> datePicker;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.FRANCE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = prefs.getString("theme", "default");
        boolean isDark = false;
        switch (theme) {
            case "default":
                if ((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                    setTheme(R.style.AppThemeDark);
                    isDark = true;
                }
                break;
            case "dark":
                setTheme(R.style.AppThemeDark);
                isDark = true;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portada_detalle);

        int showAd = getIntent().getIntExtra("showAd", 0);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        toolbar.setNavigationContentDescription(R.string.back);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        prefsPer = getSharedPreferences("revistas", Context.MODE_PRIVATE);

        Animation rotateOpen = AnimationUtils.loadAnimation(this, R.anim.rotate_open_anim);
        Animation rotateClose = AnimationUtils.loadAnimation(this, R.anim.rotate_close_anim);
        Animation fromBottom = AnimationUtils.loadAnimation(this, R.anim.from_bottom_anim);
        Animation toBottom = AnimationUtils.loadAnimation(this, R.anim.to_bottom_anim);

        FloatingActionButton fabWeb = findViewById(R.id.httpButton);
        boolean finalIsDark = isDark;
        fabWeb.setOnClickListener(v -> {
            if (portada.getWebRevista().isEmpty()) {
                Toast.makeText(this, "No existe la página web", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                int colorResId;
                if (finalIsDark) {
                    colorResId = R.color.colorPrimaryDark;
                } else {
                    colorResId = R.color.colorPrimary;
                }
                String url = "https://www." + portada.getWebRevista();
                CustomTabColorSchemeParams customTabColorSchemeParams = new CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(getResources().getColor(colorResId)).build();
                CustomTabsIntent intent = new CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .setDefaultColorSchemeParams(customTabColorSchemeParams)
                        .build();
                intent.launchUrl(PortadaDetalle.this, Uri.parse(url));
            } catch (ActivityNotFoundException e) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://www." + portada.getWebRevista()));
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        FloatingActionButton favfab = findViewById(R.id.favButton);
        favfab.setOnDragListener((view, dragEvent) -> false);
        favfab.setOnClickListener(view -> {
            SharedPreferences.Editor editor = prefsPer.edit();
            if (prefsPer.contains(portada.getTitle())) {
                editor.remove(portada.getTitle());
                favfab.setImageResource(R.drawable.ic_favorite_border_black_24dp);
            } else {
                editor.putString(portada.getTitle(), portada.getRevista());
                favfab.setImageResource(R.drawable.ic_favorite_black_24dp);
            }
            editor.apply();
        });

        FloatingActionButton addFab = findViewById(R.id.addFab);
        addFab.setOnClickListener(v -> {
            if (!clicked) {
                fabWeb.setVisibility(View.VISIBLE);
                favfab.setVisibility(View.VISIBLE);
                fabWeb.startAnimation(fromBottom);
                favfab.startAnimation(fromBottom);
                addFab.startAnimation(rotateOpen);
                fabWeb.setClickable(true);
                favfab.setClickable(true);
            } else {
                fabWeb.setVisibility(View.INVISIBLE);
                favfab.setVisibility(View.INVISIBLE);
                fabWeb.startAnimation(toBottom);
                favfab.startAnimation(toBottom);
                addFab.startAnimation(rotateClose);
                fabWeb.setClickable(false);
                favfab.setClickable(false);
            }
            clicked = !clicked;
        });

        loadSectionsAdapter();


        prefs = PreferenceManager.getDefaultSharedPreferences(this);
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

    private void loadSectionsAdapter() {
        if (!isOnline()) {
            if (alertDialogNoConn == null)
                alertDialogNoConn = createNoConnectionDialog();
            alertDialogNoConn.show();
        } else {
            mViewPager2 = findViewById(R.id.viewpager2);
            String[] portadas = getIntent().getStringArrayExtra("Portadas");
            String selectedRevistaId = getIntent().getStringExtra("Id");
            mSectionsPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
            int pos = 0;
            if (portadas != null) {
                for (int i = 0; i < portadas.length; i++) {
                    String[] portadaArray = portadas[i].split(":");
                    String webRevista = "";
                    int pages = 100;
                    String scale = "74";
                    if (portadaArray.length > 2) {
                        webRevista = portadaArray[2];
                        if (portadaArray.length > 3) {
                            pages = Integer.parseInt(portadaArray[3]);
                            scale = portadaArray[4];
                        }
                    }
                    Portada portada = new Portada(portadaArray[0], portadaArray[1], webRevista, portadas[i]);
                    if (portada.getId().equals(selectedRevistaId)) {
                        pos = i;
                        this.portada = portada;
                    }
                    mSectionsPagerAdapter.addFragment(PortadaDetalleFragment.newInstance(portada.getId(), portada.getTitle(), pages, scale));
                    mSectionsPagerAdapter.addPortada(portada);
                }
            }

            mViewPager2.setAdapter(mSectionsPagerAdapter);
            mViewPager2.setCurrentItem(pos, false);
        }
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
                    if (portada.getCurrentDate() != null) {
                        setMaterialDatePicker();
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
                    setMaterialDatePicker();
                } catch (Exception e) {
                    e.printStackTrace();
                    result = "Error: " + e.getMessage();
                    Log.e("HTTP Error", result);
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Portada no encontrada", Toast.LENGTH_LONG).show());
                }
            }).start();

        }

        private void setMaterialDatePicker() {
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

            datePicker = MaterialDatePicker.Builder
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
                            return portada.getCalendar().contains(dateString);
                        }
                    }).build()).build();
            datePicker.addOnPositiveButtonClickListener(
                    longSelectedDate -> {
                        String selectedDate = sdf.format(new Date(longSelectedDate));
                        if (!Objects.equals(currentDate, selectedDate)) {
                            mSectionsPagerAdapter.getPortada(mViewPager2.getCurrentItem()).setCurrentDate(selectedDate);
                            SharedPreferences sp = getSharedPreferences(portada.getId(), MODE_PRIVATE);
                            SharedPreferences.Editor editor = sp.edit();
                            editor.remove("currentPage");
                            editor.apply();
                            mSectionsPagerAdapter.createFragment(mViewPager2.getCurrentItem()).setCoverByDate(selectedDate);
                        }
                    });
        }
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
            if (datePicker == null) {
                mSectionsPagerAdapter.setMaterialDatePicker();
            }
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

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public AlertDialog createNoConnectionDialog() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Error de conexión");
        alertDialogBuilder.setIcon(R.mipmap.magazines_icon);
        alertDialogBuilder.setOnCancelListener(dialogInterface -> onBackPressed());
        alertDialogBuilder.setPositiveButton("Reintentar", (dialog, id) -> {
                    if (isOnline())
                        loadSectionsAdapter();
                    else
                        alertDialogBuilder.show();
                })
                .setNegativeButton("Cancelar", (dialog, id) -> onBackPressed());
        alertDialogBuilder.setMessage("No hay conexión a internet. Por favor, comprueba tu conexión");
        return alertDialogBuilder.create();
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
