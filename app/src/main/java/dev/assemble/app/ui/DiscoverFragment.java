package dev.assemble.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import dev.assemble.app.R;

/**
 * Discover v1: 1 card estático local com X/coração.
 * Botão e gesto (swipe) chamam o mesmo stub {@link #onDecisionStub(String)}.
 * Sem lógica de dados, sem score, sem transição para chat (pendente por contrato).
 */
public class DiscoverFragment extends Fragment {

    /** Callback vazio que Nexo vai conectar à camada de dados. */
    public interface OnDecisionListener {
        void onDecision(String characterId, String choice); // choice: PASS|ASSEMBLE
    }

    private OnDecisionListener listener; // null no scaffold; Nexo injeta depois

    public void setOnDecisionListener(OnDecisionListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_discover, container, false);

        Button btnPass = root.findViewById(R.id.btn_pass);
        Button btnAssemble = root.findViewById(R.id.btn_assemble);
        btnPass.setOnClickListener(v -> onDecisionStub("static-local-001", "PASS"));
        btnAssemble.setOnClickListener(v -> onDecisionStub("static-local-001", "ASSEMBLE"));

        // Gesto swipe chama o MESMO stub (sem duplicar lógica).
        root.findViewById(R.id.card_static).setOnTouchListener(new SwipeStubListener(
                () -> onDecisionStub("static-local-001", "PASS"),
                () -> onDecisionStub("static-local-001", "ASSEMBLE")));

        TextView pending = root.findViewById(R.id.text_pending);
        pending.setText(getString(R.string.state_pending));

        return root;
    }

    /**
     * Stub único de decisão. Não calcula score, não abre chat.
     * Resultado Assemble fica como estado registrado/pendente.
     */
    private void onDecisionStub(String characterId, String choice) {
        if (listener != null) {
            listener.onDecision(characterId, choice);
        }
        // Scaffold: sem efeito além do callback. Nexo implementa depois.
    }

    /** Swipe esquerda=Pass, direita=Assemble; delega aos mesmos stubs. */
    private static class SwipeStubListener implements View.OnTouchListener {
        private final Runnable onSwipeLeft;
        private final Runnable onSwipeRight;
        private float downX;

        SwipeStubListener(Runnable onSwipeLeft, Runnable onSwipeRight) {
            this.onSwipeLeft = onSwipeLeft;
            this.onSwipeRight = onSwipeRight;
        }

        @Override
        public boolean onTouch(View v, android.view.MotionEvent event) {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    downX = event.getX();
                    return true;
                case android.view.MotionEvent.ACTION_UP:
                    float dx = event.getX() - downX;
                    if (dx < -120) onSwipeLeft.run();
                    else if (dx > 120) onSwipeRight.run();
                    v.performClick();
                    return true;
                default:
                    return false;
            }
        }
    }
}
