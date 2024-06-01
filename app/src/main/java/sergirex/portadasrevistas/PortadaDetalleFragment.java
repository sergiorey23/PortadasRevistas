package sergirex.portadasrevistas;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
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
    private static final String ARG_ID = "id";
    private static final String ARG_TITLE = "title";

    // TODO: Rename and change types of parameters
    private String id;
    private String title;
    private String date;
    private int tries = 0;

    public PortadaDetalleFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param id Parameter 1.
     * @param title Parameter 2.
     * @return A new instance of fragment PortadaDetalleFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PortadaDetalleFragment newInstance(String id, String title) {
        PortadaDetalleFragment fragment = new PortadaDetalleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, id);
        args.putString(ARG_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            id = getArguments().getString(ARG_ID);
            title = getArguments().getString(ARG_TITLE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_portada_detalle, container, false);

        getCover();
        return rootView;
    }

    public void getCover(){
        SavePortada savePortada = new SavePortada(rootView.getContext());
        Bitmap portadaBM = savePortada.getFile(title + 't');
        PhotoView photoView = rootView.findViewById(R.id.imagen_extendida);
        photoView.setImageBitmap(portadaBM);
        getThread(new Date().getTime(), photoView, null);
    }

    public void getCoverByDate(Long date) {
        ProgressDialog pd = new ProgressDialog(rootView.getContext(),R.style.DialogCustom);
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.setCancelable(false);
        pd.setMessage(getString(R.string.loading));
        pd.setIndeterminate(true);
        pd.show();

        PhotoView imageView = rootView.findViewById(R.id.imagen_extendida);

        getThread(date, imageView, pd);
    }

    private void getThread(Long longDate, PhotoView imageView, ProgressDialog pd) {

        DateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.FRANCE);
        Date date = new Date(longDate);
        String dateStr = formatter.format(date);
        String strUrl = "https://t.prcdn.co/img?file="+ id + dateStr +"00000000001001&scale=171";

        new Thread(() -> {
            try {
                URL url = new URL(strUrl);
                InputStream is;
                is = (InputStream) url.getContent();
                Bitmap portadaBM = BitmapFactory.decodeStream(is);
                requireActivity().runOnUiThread(() -> {
                    this.date = dateStr;
                    imageView.setImageBitmap(portadaBM);
                    /*imageView.setOnClickListener(v -> {
                        Intent intent = new Intent(getActivity(), RevistaDetalle.class);
                        intent.putExtra("id", id);
                        intent.putExtra("date", date);
                        startActivity(intent);
                    });*/
                    tries = 0;
                    if(pd != null) {
                        pd.cancel();
                        pd.dismiss();
                    }
                });
            } catch (IOException e) {
                if(tries > 31) {
                    if(pd != null) {
                        requireActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Portada no encontrada", Toast.LENGTH_LONG).show());
                        pd.cancel();
                        pd.dismiss();
                    }
                }else {
                    tries++;
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);
                    calendar.add(Calendar.DATE, -1);
                    getThread(calendar.getTimeInMillis(), imageView, pd);
                }
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(title);
    }
}