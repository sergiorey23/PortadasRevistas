package sergirex.portadasrevistas;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.github.chrisbanes.photoview.PhotoView;

import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PortadaDetalleFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PortadaDetalleFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_ID = "id";
    private static final String ARG_TITLE = "title";

    // TODO: Rename and change types of parameters
    private String id;
    private String title;
    //private ImageView imageView;
    //private ProgressDialog pd;

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
        View rootView = inflater.inflate(R.layout.fragment_portada_detalle, container, false);

        /*pd = new ProgressDialog(rootView.getContext(),R.style.DialogCustom);
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.setCancelable(false);
        pd.setMessage(getString(R.string.loading));
        pd.setIndeterminate(true);
        pd.show();*/
        getCover(rootView);
        return rootView;
    }

    public void getCover(View rootView){
            /*ImageView imageView = rootView.findViewById(R.id.imagen_extendida);
            String url = "https://www.kioskoymas.com/Imageresize/getLatestImage/"+id+"/424/600";
            Picasso.get().load(url)
                    .into(imageView, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            pd.cancel();
                            pd.dismiss();
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });*/
        SavePortada savePortada = new SavePortada(rootView.getContext());
        Bitmap portadaBM = savePortada.getFile(title + 't');
        PhotoView photoView = rootView.findViewById(R.id.imagen_extendida);
        photoView.setImageBitmap(portadaBM);

    }

    @Override
    public void onResume() {
        super.onResume();
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(title);
    }
}