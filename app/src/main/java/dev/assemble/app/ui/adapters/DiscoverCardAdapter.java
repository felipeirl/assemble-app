package dev.assemble.app.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import dev.assemble.app.R;

/**
 * Adapter estático v1: 1 item local. Sem modelo de dados (Nexo fornece depois).
 */
public class DiscoverCardAdapter extends RecyclerView.Adapter<DiscoverCardAdapter.CardViewHolder> {

    /** Callback vazio para decisão; botão e gesto chamam o mesmo stub no Fragment. */
    public interface OnCardDecision {
        void onDecision(String choice); // PASS|ASSEMBLE
    }

    private final OnCardDecision callback;

    public DiscoverCardAdapter(OnCardDecision callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_character_card, parent, false);
        return new CardViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        holder.name.setText(holder.itemView.getContext().getString(R.string.card_static_name));
        holder.summary.setText(holder.itemView.getContext().getString(R.string.card_static_summary));
        holder.btnPass.setOnClickListener(v -> callback.onDecision("PASS"));
        holder.btnAssemble.setOnClickListener(v -> callback.onDecision("ASSEMBLE"));
    }

    @Override
    public int getItemCount() {
        return 1; // scaffold: card estático único
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView summary;
        final View btnPass;
        final View btnAssemble;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.card_name);
            summary = itemView.findViewById(R.id.card_summary);
            btnPass = itemView.findViewById(R.id.btn_pass);
            btnAssemble = itemView.findViewById(R.id.btn_assemble);
        }
    }
}
