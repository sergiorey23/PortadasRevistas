package sergirex.portadasrevistas;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.android.billingclient.api.BillingClient;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

public class PortadaDetalleFragment extends Fragment {

    private static final String ARG_ID = "id";
    private static final String ARG_TITLE = "title";
    private static final String ARG_PAGES = "pages";
    private static final String ARG_SCALE = "scale";

    private String id;
    private String title;
    private int pages;
    private String scale;
    private View rootView;
    private PhotoView photoView;

    /**
     * Crea una nueva instancia del fragmento usando los parámetros proporcionados.
     *
     * @param id    Parameter 1.
     * @param title Parameter 2.
     * @param pages Parameter 3.
     * @param scale Parameter 4.
     * @return A new instance of fragment PortadaDetalleFragment.
     */
    public static PortadaDetalleFragment newInstance(String id, String title, int pages, String scale) {
        PortadaDetalleFragment fragment = new PortadaDetalleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, id);
        args.putString(ARG_TITLE, title);
        args.putInt(ARG_PAGES, pages);
        args.putString(ARG_SCALE, scale);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            id = getArguments().getString(ARG_ID);
            title = getArguments().getString(ARG_TITLE);
            pages = getArguments().getInt(ARG_PAGES);
            scale = getArguments().getString(ARG_SCALE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_portada_detalle, container, false);
        initializeViews();
        return rootView;
    }

    private void initializeViews() {
        photoView = rootView.findViewById(R.id.imagen_extendida);
        setCacheCover();
    }

    /**
     * Carga la portada desde el caché local, si está disponible.
     */
    private void setCacheCover() {
        SavePortada savePortada = new SavePortada(requireContext());
        Bitmap portadaBM = savePortada.getFile(title + 't');
        if (portadaBM != null) {
            photoView.setImageBitmap(portadaBM);
        }
    }

    /**
     * Descarga y establece la portada según la fecha seleccionada.
     *
     * @param date Fecha de la portada en formato `yyyyMMdd`.
     */
    public void setCoverByDate(String date) {
        ProgressBar progressBar = rootView.findViewById(R.id.progressBar);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);

        String imageUrl = String.format("https://t.prcdn.co/img?file=%s%s00000000001001&scale=171", id, date);

        new Thread(() -> {
            Bitmap portadaBM = downloadImage(imageUrl);
            requireActivity().runOnUiThread(() -> {
                if (portadaBM != null) {
                    updatePhotoView(portadaBM, date);
                } else {
                    showError("Portada no encontrada");
                }
                progressBar.setVisibility(View.GONE);
            });
        }).start();
    }

    /**
     * Descarga una imagen desde una URL.
     *
     * @param imageUrl URL de la imagen.
     * @return Bitmap de la imagen descargada o `null` si ocurre un error.
     */
    private Bitmap downloadImage(String imageUrl) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(imageUrl).openConnection();
            connection.setDoInput(true);
            connection.connect();
            try (InputStream inputStream = connection.getInputStream()) {
                return BitmapFactory.decodeStream(inputStream);
            }
        } catch (IOException e) {
            Log.e("PortadaDetalleFragment", "Error descargando imagen: " + e.getMessage());
            return null;
        }
    }

    /**
     * Actualiza el `PhotoView` con la imagen descargada.
     *
     * @param portadaBM Imagen descargada.
     * @param date      Fecha de la portada.
     */
    private void updatePhotoView(Bitmap portadaBM, String date) {
        photoView.setImageBitmap(portadaBM);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        photoView.setOnClickListener(v -> {
            if (prefs.getBoolean("premium_sub_id", false)) {
                openMagazineViewer(date);
            } else {
                launchSubscriptionFlow();
            }
        });
    }

    /**
     * Muestra un mensaje de error al usuario.
     *
     * @param message Mensaje de error.
     */
    private void showError(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }

    /**
     * Abre la actividad del visor de revistas.
     *
     * @param date Fecha de la portada.
     */
    private void openMagazineViewer(String date) {
        Intent intent = new Intent(getActivity(), RevistaPagerActivity.class);
        intent.putExtra("id", id);
        intent.putExtra("date", date);
        intent.putExtra("title", title);
        intent.putExtra("pages", pages);
        intent.putExtra("scale", scale);
        startActivity(intent);
    }

    /**
     * Inicia el flujo de compra de suscripción.
     */
    private void launchSubscriptionFlow() {
        PurchaseInAppProduct purchaseInAppProduct = new PurchaseInAppProduct(
                getActivity(),
                "premium_sub_id",
                BillingClient.ProductType.SUBS,
                getString(R.string.subscribe),
                rootView.findViewById(R.id.bannerContainerDetail)
        );
        purchaseInAppProduct.launchInAppProduct();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(title);
    }
}