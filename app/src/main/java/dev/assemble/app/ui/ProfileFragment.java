package dev.assemble.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import dev.assemble.app.R;

/**
 * Profile v1: campos estáticos, sem Auth, sem Firestore.
 */
public class ProfileFragment extends Fragment {

    /** Callback vazio que Nexo vai conectar. */
    public interface OnProfileListener {
        void onSaveProfile(String displayName);
    }

    private OnProfileListener listener;

    public void setOnProfileListener(OnProfileListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);
        root.findViewById(R.id.btn_save_stub).setOnClickListener(v -> {
            if (listener != null) listener.onSaveProfile("");
        });
        return root;
    }
}
