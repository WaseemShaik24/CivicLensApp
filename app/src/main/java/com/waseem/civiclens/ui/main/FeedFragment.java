package com.waseem.civiclens.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.waseem.civiclens.R;
import com.waseem.civiclens.databinding.FragmentFeedBinding;
import com.waseem.civiclens.models.CivicReport;
import com.waseem.civiclens.ui.report.ComplaintDetailActivity;
import com.waseem.civiclens.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

public class FeedFragment extends Fragment {

    private FragmentFeedBinding binding;
    private FirebaseHelper firebaseHelper;
    private ReportAdapter adapter;
    private List<CivicReport> allReports = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFeedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseHelper = new FirebaseHelper();
        setupRecyclerView();
        setupFilterChips();

        binding.swipeRefresh.setOnRefreshListener(this::loadReports);

        loadReports();
    }

    private void setupRecyclerView() {
        adapter = new ReportAdapter(report -> {
            Intent intent = new Intent(requireContext(), ComplaintDetailActivity.class);
            intent.putExtra("report_data", report);
            startActivity(intent);
        });
        binding.rvReports.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvReports.setAdapter(adapter);
    }

    private void setupFilterChips() {
        binding.chipGroupCategory.setOnCheckedChangeListener((group, checkedId) -> filterReports(checkedId));
    }

    public void loadReports() {
        binding.swipeRefresh.setRefreshing(true);
        firebaseHelper.fetchReports(new FirebaseHelper.ReportListCallback() {
            @Override
            public void onSuccess(List<CivicReport> reports) {
                if (binding != null) {
                    binding.swipeRefresh.setRefreshing(false);
                    allReports = reports;
                    filterReports(binding.chipGroupCategory.getCheckedChipId());
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (binding != null) {
                    binding.swipeRefresh.setRefreshing(false);
                }
            }
        });
    }

    private void filterReports(int checkedChipId) {
        List<CivicReport> filtered = new ArrayList<>();
        if (checkedChipId == R.id.chipPotholes) {
            for (CivicReport r : allReports) if (r.getIssueCategory() != null && r.getIssueCategory().toLowerCase().contains("pothole")) filtered.add(r);
        } else if (checkedChipId == R.id.chipWaste) {
            for (CivicReport r : allReports) if (r.getIssueCategory() != null && r.getIssueCategory().toLowerCase().contains("waste")) filtered.add(r);
        } else if (checkedChipId == R.id.chipLights) {
            for (CivicReport r : allReports) if (r.getIssueCategory() != null && r.getIssueCategory().toLowerCase().contains("light")) filtered.add(r);
        } else if (checkedChipId == R.id.chipLeaks) {
            for (CivicReport r : allReports) if (r.getIssueCategory() != null && r.getIssueCategory().toLowerCase().contains("leak")) filtered.add(r);
        } else {
            filtered.addAll(allReports);
        }

        adapter.setReports(filtered);
        binding.tvEmptyFeed.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
