package co.onemeter.oneapp.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wowtalk.api.Database;
import org.wowtalk.api.GroupChatRoom;
import co.onemeter.oneapp.R;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-10-9
 * Time: 下午3:45
 * To change this template use File | Settings | File Templates.
 */
public class ShareRangeLimitedDepAdapter extends BaseAdapter {
    private Context contextRef;
    private ArrayList<String> selectedDepIdList;
    private Database mDb;
    private boolean isShownOnly;

    public ShareRangeLimitedDepAdapter(Context context,ArrayList<String> depIds,boolean shownOnly) {
        contextRef=context;
        selectedDepIdList=depIds;

        mDb = new Database(context);

        isShownOnly=shownOnly;
    }
    @Override
    public int getCount() {
        //each line two item
        return selectedDepIdList.size()/2+selectedDepIdList.size()%2;
    }

    @Override
    public Object getItem(int i) {
        return i;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(contextRef).inflate(R.layout.share_range_limited_dep_item, null);
        }

        TextView tv_0=(TextView) convertView.findViewById(R.id.tv_0);
        TextView tv_1=(TextView) convertView.findViewById(R.id.tv_1);
        ImageView iv_0= (ImageView) convertView.findViewById(R.id.iv_0);
        ImageView iv_1= (ImageView) convertView.findViewById(R.id.iv_1);
        RelativeLayout item_0_layout=(RelativeLayout) convertView.findViewById(R.id.ll_item_0_layout);
        RelativeLayout item_1_layout=(RelativeLayout) convertView.findViewById(R.id.ll_item_1_layout);

        final int pos_0=2*position;
        final int pos_1=pos_0+1;

        GroupChatRoom group_0=mDb.fetchGroupChatRoom(selectedDepIdList.get(pos_0));
        if (null != group_0 && !group_0.isEditable && TextUtils.isEmpty(group_0.parentGroupId)) {
            group_0.groupNameOriginal = contextRef.getString(R.string.contactsforbiz_root_group_name_display);
        }
        tv_0.setText(group_0.groupNameOriginal);
        if(isShownOnly) {
            iv_0.setVisibility(View.GONE);
            item_0_layout.setOnClickListener(null);
        } else {
            iv_0.setVisibility(View.VISIBLE);
            item_0_layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectedDepIdList.remove(pos_0);
                    notifyDataSetChanged();
                }
            });
        }

        if(pos_1 >= selectedDepIdList.size()) {
            item_1_layout.setVisibility(View.INVISIBLE);
        } else {
            item_1_layout.setVisibility(View.VISIBLE);
            GroupChatRoom group_1=mDb.fetchGroupChatRoom(selectedDepIdList.get(pos_1));
            if (null != group_1 && !group_1.isEditable && TextUtils.isEmpty(group_1.parentGroupId)) {
                group_1.groupNameOriginal = contextRef.getString(R.string.contactsforbiz_root_group_name_display);
            }
            tv_1.setText(group_1.groupNameOriginal);
            if(isShownOnly) {
                iv_1.setVisibility(View.GONE);
                item_1_layout.setOnClickListener(null);
            } else {
                iv_1.setVisibility(View.VISIBLE);
                item_1_layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        selectedDepIdList.remove(pos_1);
                        notifyDataSetChanged();
                    }
                });
            }
        }

        return convertView;
    }
}
