package com.vhallyun.rtc.otoInteractive.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.vhallyun.rtc.Member;
import com.vhallyun.rtc.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zwp on 2019-12-11
 */
public class MemberAdapter extends RecyclerView.Adapter {

    private onCallBtnClickListener callBtnClickListener;
    private List<Member> memberList = new ArrayList<>();

    public MemberAdapter(List<Member> memberList) {
        this.memberList = memberList;
    }

    public void setCallBtnClickListener(onCallBtnClickListener listener) {
        callBtnClickListener = listener;
    }

    public void setMemberList(List<Member> memberList) {
        this.memberList = memberList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_member_oto, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.tvContent.setText(memberList.get(i).userid);
        if(memberList.get(i).status == 1){
            holder.btnCall.setVisibility(View.GONE);
        }else{
            holder.btnCall.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvContent;
        Button btnCall;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_item_member_name);
            btnCall = itemView.findViewById(R.id.btn_item_member_call);

            btnCall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (callBtnClickListener != null) {
                        callBtnClickListener.onClick(getAdapterPosition());
                    }
                }
            });
        }
    }

    public interface onCallBtnClickListener {
        void onClick(int position);
    }
}
