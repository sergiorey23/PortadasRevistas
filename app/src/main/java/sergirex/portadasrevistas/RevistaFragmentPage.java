package sergirex.portadasrevistas;

import androidx.fragment.app.Fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;


public class RevistaFragmentPage extends Fragment {

    private static final String ARG_ID = "id";
    private static final String ARG_DATE = "date";
    private static final String ARG_PAGE = "page";
    private static final String ARG_SCALE = "scale";

    // TODO: Rename and change types of parameters
    private String id;
    private String date;
    private int page;
    private View rootView;
    private String scale;

    public static RevistaFragmentPage newInstance(String id, String date, int page, String scale) {
        RevistaFragmentPage fragment = new RevistaFragmentPage();
        Bundle args = new Bundle();
        args.putString(ARG_ID, id);
        args.putString(ARG_DATE, date);
        args.putInt(ARG_PAGE, page);
        args.putString(ARG_SCALE, scale);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            id = getArguments().getString(ARG_ID);
            date = getArguments().getString(ARG_DATE);
            page = getArguments().getInt(ARG_PAGE);
            scale = getArguments().getString(ARG_SCALE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(
                R.layout.fragment_revista_page, container, false);
        getPage();
        return rootView;
    }
    public void getPage(){
        new Thread(() -> {
            try {
                String strUrl = "https://t.prcdn.co/img?file="+ id + date +"00000000001001&page="+page+"&scale="+scale;
                URL url = new URL(strUrl);
                InputStream is;
                is = (InputStream) url.getContent();
                Bitmap portadaBM = BitmapFactory.decodeStream(is);
                if(isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        PhotoView photoView = rootView.findViewById(R.id.pageDetailPhotoView);
                        photoView.setImageBitmap(portadaBM);
                    });
                }
            } catch (IOException e) {
                requireActivity().runOnUiThread(() -> {
                    Log.e("ERROR 404",e.getMessage());
                });
            }
        }).start();
    }

}
