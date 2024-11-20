package sergirex.portadasrevistas;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.Objects;

public class NoConnectionDialogFragment extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(requireActivity())
                .setTitle("Error de conexión")
                .setMessage("No hay conexión a internet. Por favor, comprueba tu conexión")
                .setIcon(R.mipmap.magazines_icon)
                .setPositiveButton("Reintentar", (dialog, which) -> ((PortadaDetalle) requireActivity()).loadSectionsAdapter())
                .setNegativeButton("Cancelar", (dialog, which) -> getActivity().onBackPressed())
                .create();
    }
}