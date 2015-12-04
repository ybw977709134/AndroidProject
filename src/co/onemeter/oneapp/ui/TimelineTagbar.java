package co.onemeter.oneapp.ui;

import org.wowtalk.api.Database;
import org.wowtalk.api.PrefUtil;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import co.onemeter.oneapp.R;

import com.androidquery.AQuery;

/**
 * <p>获取动态标签选择器视图，并处理用户交互。</p>
 * Created by pzy on 10/19/14.
 */
public class TimelineTagbar {
    private final View view;
    private OnTimelineFilterChangedListener listener;
    private View[] indicates;
    private static Database dbHelper;

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int index = 0;
            switch (v.getId()) {
                case R.id.btn_tag_all:
                    index = 0;
                    break;
                case R.id.btn_tag_notice:
                    index = 1;
                    break;
                case R.id.btn_tag_qa:
                    index = 2;
                    break;
                case R.id.btn_tag_study:
                    index = 3;
                    break;
                case R.id.btn_tag_life:
                    index = 4;
                    break;
                case R.id.btn_tag_survey:
                    index = 5;
                    break;
                case R.id.btn_tag_video:
                	index = 6;
                    break;
            }

            updateIndicatesVisibility(index);

            if (listener != null) {
                listener.onTagChanged(index);
            }
        }
    };

    public void updateIndicatesVisibility(int selectedIndex) {
        for (int i = 0; i < indicates.length; ++i) {
            indicates[i].setVisibility(i == selectedIndex ? View.VISIBLE : View.INVISIBLE);
        }
    }

    /**
     * 显示老师成长日志的bar显示
     * @param context
     */
//    public TimelineTagbar(Context context) {
//        view = LayoutInflater.from(context)
//                .inflate(R.layout.timeline_tag_tabbar, null);
//
//        AQuery q = new AQuery(view);
//        q.find(R.id.btn_tag_all).clicked(onClickListener);
//        q.find(R.id.btn_tag_notice).clicked(onClickListener);
//        q.find(R.id.btn_tag_qa).clicked(onClickListener);
//        q.find(R.id.btn_tag_study).clicked(onClickListener);
//        q.find(R.id.btn_tag_life).clicked(onClickListener);
//        q.find(R.id.btn_tag_survey).clicked(onClickListener);
//        q.find(R.id.btn_tag_video).clicked(onClickListener);
//
//        indicates = new View[7];
//        indicates[0] = view.findViewById(R.id.img_tag_all);
//        indicates[1] = view.findViewById(R.id.img_tag_notice);
//        indicates[2] = view.findViewById(R.id.img_tag_qa);
//        indicates[3] = view.findViewById(R.id.img_tag_study);
//        indicates[4] = view.findViewById(R.id.img_tag_life);
//        indicates[5] = view.findViewById(R.id.img_tag_survey);
//        indicates[6] = view.findViewById(R.id.img_tag_vedio);
//        
//   
//    }
    
    /**
     * 通过flag 传入的值判断是老师还是学生//1为学生，2为老师
     * @param context
     * @param flag
     * @author hutianfeng
     * @date 2014-12-26
     */
    public TimelineTagbar(Context context,int flag) {
        view = LayoutInflater.from(context)
                .inflate(R.layout.timeline_tag_tabbar, null);

        AQuery q = new AQuery(view);
        q.find(R.id.btn_tag_all).clicked(onClickListener);
        q.find(R.id.btn_tag_notice).clicked(onClickListener);
        q.find(R.id.btn_tag_qa).clicked(onClickListener);
        q.find(R.id.btn_tag_study).clicked(onClickListener);
        q.find(R.id.btn_tag_life).clicked(onClickListener);
        q.find(R.id.btn_tag_survey).clicked(onClickListener);
        q.find(R.id.btn_tag_video).clicked(onClickListener);

        indicates = new View[7];
        indicates[0] = view.findViewById(R.id.img_tag_all);
        indicates[1] = view.findViewById(R.id.img_tag_notice);
        indicates[2] = view.findViewById(R.id.img_tag_qa);
        indicates[3] = view.findViewById(R.id.img_tag_study);
        indicates[4] = view.findViewById(R.id.img_tag_life);
        indicates[5] = view.findViewById(R.id.img_tag_survey);
        indicates[6] = view.findViewById(R.id.img_tag_vedio);
        
    	if (flag == 1) {//如果为学生，通知和视频不能显示
//    		view.findViewById(R.id.layout_timebar_notice).setVisibility(View.GONE);
        	view.findViewById(R.id.layout_timebar_video).setVisibility(View.GONE);
    	}
    	view.findViewById(R.id.btn_tag_qa).setVisibility(View.GONE);
    	view.findViewById(R.id.layout_timebar_notice).setVisibility(View.GONE);

    }
      

    public View getView() {
        return view;
    }

    public void setListener(OnTimelineFilterChangedListener l) {
        listener = l;
    }
}
