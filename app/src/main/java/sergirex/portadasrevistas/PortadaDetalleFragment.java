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

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PortadaDetalleFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PortadaDetalleFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private View rootView;
    private PhotoView photoView;
    private static final String ARG_ID = "id";
    private static final String ARG_TITLE = "title";
    private static final String ARG_PAGES = "pages";
    private static final String ARG_SCALE = "scale";

    // TODO: Rename and change types of parameters
    private String id;
    private String title;
    private int pages;
    private String scale;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param id    Parameter 1.
     * @param title Parameter 2.
     * @param pages Parameter 3.
     * @param scale Parameter 4.
     * @return A new instance of fragment PortadaDetalleFragment.
     */
    // TODO: Rename and change types and number of parameters
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
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_portada_detalle, container, false);
        setCacheCover();
        return rootView;
    }

    public void setCacheCover() {
        SavePortada savePortada = new SavePortada(rootView.getContext());
        Bitmap portadaBM = savePortada.getFile(title + 't');
        photoView = rootView.findViewById(R.id.imagen_extendida);
        photoView.setImageBitmap(portadaBM);
    }

    public void setCoverByDate(String date) {

        ProgressBar progressBar = rootView.findViewById(R.id.progressBar);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);
        String strUrl = "https://t.prcdn.co/img?file=" + id + date + "00000000001001&scale=171";

        new Thread(() -> {
            try {
                URL url = new URL(strUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream is = connection.getInputStream();
                Bitmap portadaBM = BitmapFactory.decodeStream(is);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        photoView.setImageBitmap(portadaBM);
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.requireActivity());
                        photoView.setOnClickListener(v -> {
                            if (prefs.getBoolean("premium_sub_id", false)) {
                                Intent intent = new Intent(getActivity(), RevistaPagerActivity.class);
                                intent.putExtra("id", id);
                                intent.putExtra("date", date);
                                intent.putExtra("title", title);
                                intent.putExtra("pages", pages);
                                intent.putExtra("scale", scale);
                                startActivity(intent);
                            } else {
                                PurchaseInAppProduct purchaseInAppProduct = new PurchaseInAppProduct(getActivity(), "premium_sub_id", BillingClient.ProductType.SUBS, getString(R.string.subscribe), requireActivity().findViewById(R.id.bannerContainerDetail));
                                purchaseInAppProduct.launchInAppProduct();
                            }
                        });

                        progressBar.setVisibility(View.GONE);
                    });
                }
            } catch (IOException e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getActivity(), "Portada no encontrada", Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.GONE);
                    });
                }
            }
        }).start();
    }


    public String getCoverId() {
        return id;
    }

    @Override
    public void onResume() {
        super.onResume();
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(title);
    }
}