package kr.ac.baekseok.ab;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

// 분류 히스토리 목록을 RecyclerView에 표시하는 어댑터
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<HistoryRecord> records;

    public HistoryAdapter(List<HistoryRecord> records) {
        this.records = records;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryRecord record = records.get(position);
        holder.source.setText(record.source);
        holder.result.setText(record.result);
        holder.probability.setText(String.format(Locale.KOREAN, "%.1f%%", record.probability * 100));
        holder.timestamp.setText(record.timestamp);
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView source, result, probability, timestamp;

        ViewHolder(View view) {
            super(view);
            source = view.findViewById(R.id.histSource);
            result = view.findViewById(R.id.histResult);
            probability = view.findViewById(R.id.histProbability);
            timestamp = view.findViewById(R.id.histTimestamp);
        }
    }
}
