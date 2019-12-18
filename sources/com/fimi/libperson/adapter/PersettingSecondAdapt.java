package com.fimi.libperson.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.fimi.kernel.utils.FontUtil;
import com.fimi.libperson.R;
import com.fimi.libperson.entity.PersonSetting;
import java.util.List;

public class PersettingSecondAdapt extends BaseAdapter {
    private Context mContext;
    private List<PersonSetting> mList;

    public enum State {
        SERVICE,
        LANGUAGE
    }

    private class ViewHolder {
        ImageView mIvArrow;
        RelativeLayout mRlBg;
        TextView mTvContent;
        TextView mTvItemTitle;

        private ViewHolder() {
        }

        public View initView(ViewGroup parent) {
            View view = LayoutInflater.from(PersettingSecondAdapt.this.mContext).inflate(R.layout.libperson_adapt_person_new_setting, parent, false);
            this.mRlBg = (RelativeLayout) view.findViewById(R.id.rl_bg);
            this.mTvItemTitle = (TextView) view.findViewById(R.id.tv_item_title);
            this.mIvArrow = (ImageView) view.findViewById(R.id.iv_arrow);
            this.mTvContent = (TextView) view.findViewById(R.id.tv_content);
            FontUtil.changeFontLanTing(PersettingSecondAdapt.this.mContext.getAssets(), this.mTvContent, this.mTvItemTitle);
            return view;
        }
    }

    public PersettingSecondAdapt(Context context) {
        this.mContext = context;
    }

    public int getCount() {
        return this.mList == null ? 0 : this.mList.size();
    }

    public Object getItem(int position) {
        return Integer.valueOf(position);
    }

    public long getItemId(int position) {
        return (long) position;
    }

    public void setData(List<PersonSetting> list) {
        this.mList = list;
        notifyDataSetChanged();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = holder.initView(parent);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (this.mList != null) {
            resetDefaultView(holder, convertView.getLayoutParams());
            State positionIndex = ((PersonSetting) this.mList.get(position)).getSecondAdapt();
            if (positionIndex == State.SERVICE) {
                holder.mTvItemTitle.setText(R.string.libperson_service_setting_title);
            } else if (positionIndex == State.LANGUAGE) {
                holder.mTvItemTitle.setText(R.string.person_setting_language);
            }
        }
        return convertView;
    }

    private void resetDefaultView(ViewHolder holder, LayoutParams params) {
        holder.mIvArrow.setVisibility(0);
        holder.mTvContent.setText("");
        params.height = (int) this.mContext.getResources().getDimension(R.dimen.person_setting_height);
        holder.mRlBg.setLayoutParams(params);
        holder.mRlBg.setBackgroundResource(R.drawable.person_listview_item_shape_enable);
    }
}
