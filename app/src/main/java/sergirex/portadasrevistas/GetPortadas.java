package sergirex.portadasrevistas;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

class GetPortadas extends AsyncTask<String, Button, Boolean> {
    private final WeakReference<View> rootView;
    private final WeakReference<Context> context;
    private int portCont = 0;
    private List<Portada> portadasList;
    private WeakReference<LinearLayout> ly;
    private WeakReference<ProgressBar> pb;
    private final SharedPreferences fechasSP;
    private String strUrl = null;

    @SuppressLint("StaticFieldLeak")
    private final SwipeRefreshLayout swipeRefreshLayout;

    GetPortadas(View rootView, String simpleName, SwipeRefreshLayout swipeRefreshLayout) {
        this.rootView = new WeakReference<>(rootView);
        context = new WeakReference<>(rootView.getContext());
        fechasSP = context.get().getSharedPreferences("Fechas" + simpleName, MODE_PRIVATE);
        this.swipeRefreshLayout = swipeRefreshLayout;
    }

    View getRootView() {
        return rootView.get();
    }

    protected void onPreExecute() {
        ly = new WeakReference<>(rootView.get().findViewById(R.id.linearLayout));
        pb = new WeakReference<>(new ProgressBar(context.get()));
        portadasList = new ArrayList<>();
        ly.get().addView(pb.get());
    }

    @Override
    protected Boolean doInBackground(String... params) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.get());

        final SavePortada savePortada = new SavePortada(context.get());

        Calendar calendar = Calendar.getInstance();
        Date today = new Date();
        calendar.setTime(today);
        if (calendar.get(Calendar.HOUR_OF_DAY) < 7) {
            calendar.add(Calendar.DATE, -1);
        }

        DateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.FRANCE);

        String fecha = formatter.format(calendar.getTime());

        String fechaPortadas = fechasSP.getString("fechaPortadas", "");
        boolean descargar = !fechaPortadas.equals(fecha);

        SharedPreferences.Editor editor = fechasSP.edit();

        try {
            if (descargar || swipeRefreshLayout != null) {
                Document doc = Jsoup.connect("https://www.kioskoymas.com/Catalogo/filtro/revistas").get();
                for (String portada : params) {
                    String[] values = portada.split(":");
                    Log.e("PORTADA",values[0]);
                    Elements elements = doc.getElementsByAttributeValue("data-filter",values[0]);
                    if(elements.isEmpty()) continue;
                    String webRevista = "";
                    if(values.length > 1){
                        webRevista = values[1];
                    }
                    portadasList.add(new Portada(elements.get(0).attr("id"),values[0],webRevista,portada));
                }
            } else {
                Document doc = null;
                for (String portada : params) {
                    String[] values = portada.split(":");
                    String id = fechasSP.getString(values[0], "");
                    if(id.isEmpty()){
                        if(doc == null) {
                            doc = Jsoup.connect("https://www.kioskoymas.com/Catalogo/filtro/revistas").get();
                        }
                        Elements elements = doc.getElementsByAttributeValue("data-filter",values[0]);
                        if(elements.isEmpty()) continue;
                        id = elements.get(0).attr("id");
                    }
                    String webRevista = "";
                    if(values.length > 1){
                        webRevista = values[1];
                    }
                    portadasList.add(new Portada(id,values[0],webRevista,portada));
                }
            }
            for (Portada revista : portadasList) {

                strUrl = "https://www.kioskoymas.com/Imageresize/getLatestImage/"+revista.getId()+"/424/600";
                Bitmap portadaBM = savePortada.getThumbFile(revista.getTitle() + 't');
                if(descargar || portadaBM == null || swipeRefreshLayout != null) {
                    try {
                        URL url = new URL(strUrl);
                        InputStream is = (InputStream) url.getContent();
                        portadaBM = BitmapFactory.decodeStream(is);
                        savePortada.saveFile(context.get().getCacheDir(), revista.getTitle() + "t", portadaBM);
                        editor.putString(revista.getTitle(), revista.getId());

                    } catch (FileNotFoundException fne) {
                        fne.printStackTrace();
                        break;
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }

                LinearLayout.LayoutParams paramsly = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                Button imageButton = new Button(context.get());
                paramsly.width = getHalfScreenWidth();
                paramsly.height = (portadaBM.getHeight() * paramsly.width) / portadaBM.getWidth();
                Drawable drawable = new BitmapDrawable(context.get().getResources(), portadaBM);
                imageButton.setBackground(drawable);
                imageButton.setLayoutParams(paramsly);

                imageButton.setOnClickListener(view -> {
                    Intent intent = new Intent(context.get(), PortadaDetalle.class);
                    intent.putExtra("Portadas",(Serializable) portadasList);
                    intent.putExtra("Id", revista.getId());
                    int adCount = prefs.getInt("adCount", 0) + 1;
                    SharedPreferences.Editor edit = prefs.edit();
                    edit.putInt("adCount", adCount);
                    edit.apply();
                    intent.putExtra("showAd", adCount);
                    context.get().startActivity(intent);
                });
                imageButton.setOnLongClickListener(view -> {
                    PopupMenu popup = new PopupMenu(view.getContext(), view);
                    popup.getMenuInflater().inflate(R.menu.menu_portada_list, popup.getMenu());
                    popup.setOnMenuItemClickListener(menuItem -> {
                        if (menuItem.getItemId() == R.id.share) {
                            if (savePortada.isExternalStorageWritable()) {
                                if (!savePortada.checkPermissions())
                                    return false;
                                new DownloadPortada(context.get(), savePortada, revista.getTitle()).execute(strUrl);
                            }
                        } else if (menuItem.getItemId() == R.id.save) {
                            if (savePortada.isExternalStorageWritable()) {
                                if (!savePortada.checkPermissions())
                                    return false;
                                File file;
                                if ((file = new File(savePortada.getAlbumStorageDir() + File.separator + revista.getTitle() + "_" + (revista.getTitle() != null ? fecha : "") + ".jpg")).exists()) {
                                    Toast.makeText(context.get(), "Ya se ha guardado la portada.", Toast.LENGTH_LONG).show();
                                    return true;
                                }
                                DownloadPortada dp = new DownloadPortada(context.get(), file);
                                dp.execute(strUrl);
                            } else {
                                Toast.makeText(context.get(), "Internal Storage unreadable", Toast.LENGTH_LONG).show();
                            }
                        } else if (menuItem.getItemId() == R.id.web) {
                            //menuItem.setTitle(menuItem.getTitle() + url1);
                            if(revista.getWebRevista().isEmpty()){
                                Toast.makeText(context.get(), "No existe la página web", Toast.LENGTH_LONG).show();
                                return true;
                            }
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse("https://www." + revista.getWebRevista()));
                            context.get().startActivity(i);
                        }
                        return true;
                    });
                    MenuPopupHelper menuHelper = new MenuPopupHelper(context.get(), (MenuBuilder) popup.getMenu(), view);
                    menuHelper.setForceShowIcon(true);
                    menuHelper.show();
                    return true;
                });

                publishProgress(imageButton);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (descargar) {
            editor.putString("fechaPortadas", fecha);
            editor.apply();
        }
        return true;
    }


    private WeakReference<LinearLayout> linearLayout;
    private int lycount = 0;

    protected void onProgressUpdate(Button... ib) {
        if (portCont % 2 == 0) {
            linearLayout = new WeakReference<>(new LinearLayout(context.get()));
            linearLayout.get().setOrientation(LinearLayout.HORIZONTAL);
            linearLayout.get().setHorizontalGravity(Gravity.CENTER_HORIZONTAL);
            linearLayout.get().addView(ib[0]);
            if (ly.get() != null)
                ly.get().addView(linearLayout.get(), lycount);
            lycount++;
        } else {
            if (linearLayout.get() != null) {
                Button button = (Button) linearLayout.get().getChildAt(0);
                LinearLayout.LayoutParams paramsly = (LinearLayout.LayoutParams) button.getLayoutParams();
                if (paramsly.height > ib[0].getLayoutParams().height) {
                    ib[0].setLayoutParams(paramsly);
                } else {
                    paramsly.height = ib[0].getLayoutParams().height;
                    button.setLayoutParams(paramsly);
                }
                linearLayout.get().addView(ib[0]);
            }
        }
        portCont++;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        if (ly.get() != null) {
            ly.get().removeView(pb.get());
        }
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
        super.onPostExecute(aBoolean);
    }

    private static int getHalfScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels / 2;
    }
}

