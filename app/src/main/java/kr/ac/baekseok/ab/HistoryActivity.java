package kr.ac.baekseok.ab;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

// 분류 히스토리 화면 - UI만 담당, 데이터 로드/삭제는 HistoryViewModel에 위임
public class HistoryActivity extends AppCompatActivity {

    private HistoryViewModel viewModel;
    private TtsHelper tts;
    private RecyclerView recyclerView;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        tts = new TtsHelper(this);
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        String userId = getIntent().getStringExtra(SubActivity.KEY_USER_ID);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                tts.speakAndThen("뒤로 가기", this::finish));
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_clear) {
                viewModel.deleteAll(); // ViewModel에서 삭제 후 LiveData 즉시 갱신
                return true;
            }
            return false;
        });

        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // LiveData 관찰 - 삭제 시 빈 리스트가 전달되어 자동으로 emptyView 표시
        viewModel.historyList.observe(this, records -> {
            if (records == null || records.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            } else {
                emptyView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                recyclerView.setAdapter(new HistoryAdapter(records));
            }
        });

        // 해당 사용자의 히스토리만 로드 - 다른 사용자 기록이 섞이지 않도록 userId 전달
        viewModel.loadHistory(userId);
    }

    @Override
    protected void onDestroy() {
        tts.shutdown();
        super.onDestroy();
    }
}
