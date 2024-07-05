package sergirex.portadasrevistas.categorias;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import sergirex.portadasrevistas.GetPortadas;
import sergirex.portadasrevistas.R;

/**
 * Created by sergio on 01/10/2019.
 * Nothing else to add
 */
public class Favoritos extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.portada_layout, container, false);
        SharedPreferences prefs = rootView.getContext().getSharedPreferences("revistas", Context.MODE_PRIVATE);
        if(prefs.getAll().isEmpty())
            return rootView;
        GetPortadas getPortadas = new GetPortadas(rootView, getClass().getSimpleName(), null);
        String[] favRevistas = prefs.getAll().values().toArray(new String[0]);
        getPortadas.execute(favRevistas);
        SwipeRefreshLayout swipeRefreshLayout = rootView.findViewById(R.id.refreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshFragment(rootView, swipeRefreshLayout, prefs);
            swipeRefreshLayout.setRefreshing(false);
        });
        return getPortadas.getRootView();
    }

    private void refreshFragment(View rootView, SwipeRefreshLayout swipeRefreshLayout, SharedPreferences prefs) {
        ViewGroup viewGroup = rootView.findViewById(R.id.linearLayout);
        viewGroup.removeAllViews();
        String[] favRevistas = prefs.getAll().values().toArray(new String[0]);
        GetPortadas getPts = new GetPortadas(rootView, getClass().getSimpleName(), swipeRefreshLayout);
        getPts.execute(favRevistas);
    }

}
