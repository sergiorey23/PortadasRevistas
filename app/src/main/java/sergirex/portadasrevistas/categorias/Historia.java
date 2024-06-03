package sergirex.portadasrevistas.categorias;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import sergirex.portadasrevistas.GetPortadas;
import sergirex.portadasrevistas.R;
import sergirex.portadasrevistas.Revistas;

/**
 * Created by Sergio on 12/02/2017.
 * Clase de revistas Corazon
 */

public class Historia extends Fragment {


    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.portada_layout, container, false);
        GetPortadas getPortadas = new GetPortadas(rootView, null);
        getPortadas.execute(Revistas.historia);
        SwipeRefreshLayout swipeRefreshLayout = rootView.findViewById(R.id.refreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshFragment(rootView,swipeRefreshLayout);
            swipeRefreshLayout.setRefreshing(false);
        });

        return getPortadas.getRootView();
    }

    private void refreshFragment(View rootView, SwipeRefreshLayout swipeRefreshLayout) {
        ViewGroup viewGroup = rootView.findViewById(R.id.linearLayout);
        viewGroup.removeAllViews();
        GetPortadas getPts = new GetPortadas(rootView, swipeRefreshLayout);
        getPts.execute(Revistas.historia);
    }

}
