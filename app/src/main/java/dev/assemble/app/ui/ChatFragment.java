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
 * Chat v1: estados presentes, sem mensagens reais.
 * Transição score→conexão/chat NÃO implementada (pendente por contrato).
 */
public class ChatFragment extends Fragment {

    /** Callback vazio que Nexo vai conectar. */
    public interface OnMessageListener {
        void onSendMessage(String characterId, String text);
    }

    private OnMessageListener listener;

    public void setOnMessageListener(OnMessageListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_chat, container, false);
        root.findViewById(R.id.btn_send_stub).setOnClickListener(v -> {
            if (listener != null) listener.onSendMessage("static-local-001", "");
        });
        return root;
    }
}
