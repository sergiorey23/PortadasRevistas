package sergirex.portadasrevistas;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GetPortadas extends AsyncTask<String, ImageButton, Boolean> {
    private final WeakReference<View> rootView;
    private final WeakReference<Context> context;
    private int portCont = 0;
    private WeakReference<LinearLayout> ly;
    private WeakReference<ProgressBar> pb;
    private String strUrl = null;

    @SuppressLint("StaticFieldLeak")
    private final SwipeRefreshLayout swipeRefreshLayout;

    public GetPortadas(View rootView, SwipeRefreshLayout swipeRefreshLayout) {
        this.rootView = new WeakReference<>(rootView);
        context = new WeakReference<>(rootView.getContext());
        this.swipeRefreshLayout = swipeRefreshLayout;
    }

    public View getRootView() {
        return rootView.get();
    }

    protected void onPreExecute() {
        ly = new WeakReference<>(rootView.get().findViewById(R.id.linearLayout));
        pb = new WeakReference<>(new ProgressBar(context.get()));
        ly.get().addView(pb.get());
    }

    @Override
    protected Boolean doInBackground(String... params) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.get());

        final SavePortada savePortada = new SavePortada(context.get());

        DateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.FRANCE);

        String fecha = formatter.format(new Date());

        boolean descargar = !fecha.equals(prefs.getString("fechaPortadas", ""));

        for (String portada : params) {
            String[] portadaArray = portada.split(":");
            String id = portadaArray[0];
            String title = portadaArray[1];
            String webRevista;
            if(portadaArray.length > 2) {
                webRevista = portadaArray[2];
            } else {
                webRevista = "";
            }

            strUrl = "https://www.kioskoymas.com/Imageresize/getLatestImage/"+id+"/424/600";
            Bitmap portadaBM;
            if(descargar || (portadaBM = savePortada.getFile(title + 't')) == null || swipeRefreshLayout != null) {
                if(prefs.getBoolean("premium_sub_id", false)) {
                    SharedPreferences sp = context.get().getSharedPreferences(id, MODE_PRIVATE);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.remove("currentPage");
                    editor.apply();
                }
                try {
                    URL url = new URL(strUrl);
                    InputStream is = (InputStream) url.getContent();
                    portadaBM = BitmapFactory.decodeStream(is);
                    savePortada.saveFile(context.get().getCacheDir(), title + "t", portadaBM);
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

            ImageButton imageButton = new ImageButton(context.get());
            paramsly.width = getHalfScreenWidth();
            paramsly.height = (portadaBM.getHeight() * paramsly.width) / portadaBM.getWidth();
            Drawable drawable = new BitmapDrawable(context.get().getResources(), portadaBM);
            imageButton.setBackground(drawable);
            imageButton.setLayoutParams(paramsly);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                imageButton.setTooltipText(title);
            }
            imageButton.setContentDescription(title);

            imageButton.setOnClickListener(view -> {
                Intent intent = new Intent(context.get(), PortadaDetalle.class);
                intent.putExtra("Portadas", params);
                intent.putExtra("Id", id);
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
                            new DownloadPortada(context.get(), savePortada, title).execute(strUrl);
                        }
                    } else if (menuItem.getItemId() == R.id.save) {
                        if (savePortada.isExternalStorageWritable()) {
                            if (!savePortada.checkPermissions())
                                return false;
                            File file;
                            if ((file = new File(savePortada.getAlbumStorageDir() + File.separator + title + "_" + (title != null ? fecha : "") + ".jpg")).exists()) {
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
                        if(webRevista.isEmpty()){
                            Toast.makeText(context.get(), "No existe la página web", Toast.LENGTH_LONG).show();
                            return true;
                        }
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse("https://www." + webRevista));
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

        if (descargar) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("fechaPortadas", fecha);
            editor.apply();
        }
        return true;
    }


    private WeakReference<LinearLayout> linearLayout;
    private int lycount = 0;

    protected void onProgressUpdate(ImageButton... ib) {
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
                ImageButton button = (ImageButton) linearLayout.get().getChildAt(0);
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

