package com.vhallyun.rtc.otoInteractive.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.vhallyun.rtc.Member;
import com.vhallyun.rtc.R;
import com.vhallyun.rtc.otoInteractive.activity.OTOActivity;
import com.vhallyun.rtc.otoInteractive.adapter.MemberAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zwp on 2019-12-11
 */
public class MemberListFragment extends Fragment {

    View rootView;
    RecyclerView rcvMemberList;
    SwipeRefreshLayout refreshLayout;
    LinearLayoutManager layoutManager;
    private List<Member> members = new ArrayList<>();
    private MemberAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_member_list, container, false);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        initView();
    }

    private void initView() {
        refreshLayout = rootView.findViewById(R.id.sr_refresh);
        rcvMemberList = rootView.findViewById(R.id.rcv_members);
        layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rcvMemberList.setLayoutManager(layoutManager);
        adapter = new MemberAdapter(members);
        rcvMemberList.setAdapter(adapter);
        adapter.setCallBtnClickListener(callBtnClickListener);

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshLayout.setRefreshing(true);
                ((OTOActivity) getActivity()).getMembers();
            }
        });
    }

    public void setMembers(List<Member> members) {
        this.members = members;
        if (adapter != null) {
            adapter.setMemberList(members);
        }
        if (refreshLayout != null) {
            refreshLayout.setRefreshing(false);
        }
    }

    private MemberAdapter.onCallBtnClickListener callBtnClickListener = new MemberAdapter.onCallBtnClickListener() {
        @Override
        public void onClick(int position) {
            ((OTOActivity) getActivity()).otoCall(position);
        }
    };
}
