package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;

import com.androidquery.AQuery;

import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.MessageDialog;
import org.wowtalk.ui.msg.InputBoardManager;

/**
 * <p>(时间线|分享|动态)页面。</p>
 * Created by pzy on 10/13/14.
 */
public class TimelineActivity extends FragmentActivity implements View.OnClickListener {

    /** optional moment owner's uid */
    public static final String EXTRA_UID = "uid";
    /** If page title is empty, buttons will show instead. */
    public static final String EXTRA_PAGE_TITLE = "title";
    public static final String EXTRA_BACK_FLAG = "backFlag";
    public static final int COMMENT_MOST_WORDS = 260;
    public static final int NETWORK_TAG_UPLOADING_ALBUMCOVER = 123;

    private final static int TAG_ALL_IDX=0;
    public final static int TAG_NOTICE_IDX =1;
    public final static int TAG_QA_IDX=2;
    public final static int TAG_STUDY_IDX =3;
    public final static int TAG_SURVEY_IDX=4;
    public final static int TAG_LIFE_IDX =5;
    public final static int TAG_VIDEO_IDX =6;

    public static boolean FLAG_ISPUBLIC = false;

    private static final String FRAGMENT_TAG_ALL = "all";
    private static final String FRAGMENT_TAG_MY = "my";

    private static final int REQ_CREATE_MOMENT = 124;
   /**在线的activity**/
    private static TimelineActivity instance;
    private static InputBoardManager inputMgr;
     /**所有在线动态的界面**/
    private AllTimelineFragment allTimelineFragment;
    /**我的在线的动态**/
    private MyTimelineFragment myTimelineFragment;
    /**在线的界面**/
    private static TimelineFragment currTimelineFragment;
    private View newMomentPanel;
    private AQuery q = new AQuery(this);
    private String uid;
    private String pageTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);
        instance = this;
        
        q.find(R.id.title_left).clicked(this);
        q.find(R.id.btn_all).clicked(this);
        q.find(R.id.btn_me).clicked(this);
        q.find(R.id.title_edit).clicked(this);
        q.find(R.id.vg_new_question).clicked(this);
        q.find(R.id.vg_new_study).clicked(this);
        q.find(R.id.vg_new_life).clicked(this);
        q.find(R.id.vg_new_vote).clicked(this); 
        q.find(R.id.vg_new_notice).clicked(this);
        q.find(R.id.vg_new_video).clicked(this);

        newMomentPanel = q.find(R.id.new_moment_panel).clicked(this).getView();
        if (getIntent().getBooleanExtra("isPublic",false)) {
            FLAG_ISPUBLIC = true;
            setTitle("公众号");
        }
        getData(savedInstanceState == null ? getIntent().getExtras() : savedInstanceState);
        allTimelineFragment =  new AllTimelineFragment();
        myTimelineFragment = new MyTimelineFragment();
        Bundle args = new Bundle();
        args.putString(MyTimelineFragment.EXTRA_UID,
                uid != null ? uid : PrefUtil.getInstance(this).getUid());
        myTimelineFragment.setArguments(args);
        hideUnavailableNewMomentButtons();
        hideNewMomentPanel();

        if (uid != null) {
        	if (!TextUtils.isEmpty(pageTitle)) {//好友的动态
        		setTitle(pageTitle);
        		TimelineFragment.newReviewFlag = false;
        	} else {
        		TimelineFragment.newReviewFlag = true;
        	}
        	switchToSingle();//自己的动态
        	if (getIntent().getIntExtra(EXTRA_BACK_FLAG, 0) == 1) {
        		findViewById(R.id.title_left).setVisibility(View.VISIBLE);
        	}
            
        } else {
            switchToAll();
        }

        
    }

    @Override
    protected void onDestroy() {
        //判断是否打开了键盘
        if (inputMgr != null) {
            inputMgr = null;
        }

        //重置好友圈还是公众号的标志位
        if (FLAG_ISPUBLIC) {
            FLAG_ISPUBLIC = false;
        }
        super.onDestroy();
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        if (title != null) {
            q.find(R.id.btn_layout).invisible();
            q.find(R.id.title_text).visible().text(title);
            q.find(R.id.title_edit).visibility(View.GONE);
        } else {
            q.find(R.id.btn_layout).visible();
            q.find(R.id.title_text).invisible();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("---timeline");
        // 发布动态成功后，刷新
        if (requestCode == REQ_CREATE_MOMENT && resultCode == Activity.RESULT_OK
                && currTimelineFragment != null) {
            Moment moment = data.getParcelableExtra(CreateNormalMomentWithTagActivity.EXTRA_MOMENT);
            if (moment != null)
                currTimelineFragment.insertMoment(moment, 0);
            Log.d("---timelineONresult","创建活动刷新");
        }
    }
    

    public static TimelineActivity instance() {
        return instance;
    }

    private void getData(Bundle state) {
        if (state != null) {
            uid = state.getString(EXTRA_UID);
            pageTitle = state.getString(EXTRA_PAGE_TITLE);
        }
    }

    public boolean handleBackPress() {
        if (isNewMomentPanelShowing()) {
            hideNewMomentPanel();
            return true;
        }
        if (allTimelineFragment.handleBackPress() || myTimelineFragment.handleBackPress()) {
            return true;
        }
        return false;
    }

    private boolean isNewMomentPanelShowing() {
        return newMomentPanel != null && newMomentPanel.getVisibility() == View.VISIBLE;
    }

    private void hideNewMomentPanel() {
        newMomentPanel.setVisibility(View.GONE);
    }

    private void toggleNewMomentPanel() {
        newMomentPanel.setVisibility(isNewMomentPanelShowing() ?
                View.GONE : View.VISIBLE);
    }

    private void hideUnavailableNewMomentButtons() {
        if (Buddy.ACCOUNT_TYPE_TEACHER != PrefUtil.getInstance(this).getMyAccountType()) {
            q.find(R.id.vg_new_notice).invisible();
            q.find(R.id.vg_new_video).invisible();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        	case R.id.title_left:
        		finish();
                //返回键
        		break;
            case R.id.title_edit:
                //“+”号 弹出刷选条件
                toggleNewMomentPanel();
                break;
            case R.id.new_moment_panel:
                hideNewMomentPanel();
                break;
            case R.id.btn_all:
                //分享
                switchToAll();
                break;
            case R.id.btn_me:
                //我的动态
                switchToSingle();
                break;
            case R.id.vg_new_study:
                //学习
                gotoCreateMoment(TAG_STUDY_IDX);
                hideNewMomentPanel();
                break;
            case R.id.vg_new_life:
                //生活
                gotoCreateMoment(TAG_LIFE_IDX);
                hideNewMomentPanel();
                break;
            case R.id.vg_new_notice:
                //通知
                gotoCreateMoment(TAG_NOTICE_IDX);
                hideNewMomentPanel();
                break;
            case R.id.vg_new_question:
                //问答
                gotoCreateMoment(TAG_QA_IDX);
                hideNewMomentPanel();
                break;
            case R.id.vg_new_vote:
                //投票
                gotoCreateMoment(TAG_SURVEY_IDX);
                hideNewMomentPanel();
                break;
            case R.id.vg_new_video:
                //视频
                gotoCreateMoment(TAG_VIDEO_IDX);
                hideNewMomentPanel();
                break;
        }
    }

    /**
     * 通过控制返回键来控制对评论的的输入框的显示与隐藏
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (inputMgr != null && inputMgr.isShowing()) {
                inputMgr.setSoftKeyboardVisibility(false);
                inputMgr.hide();
            } else {
                finish();
            }

        }
        return false;
    }

    private void gotoCreateMoment(int tagIdx) {
        startActivityForResult(
                new Intent(this, CreateNormalMomentWithTagActivity.class)
                        .putExtra(CreateMomentActivity.EXTRA_KEY_MOMENT_TAG_ID, tagIdx),
                REQ_CREATE_MOMENT
        );
    }

    private void switchToAll() {
        //所有的
        q.find(R.id.btn_all).background(R.drawable.tab_button_left_white_a).textColorId(R.color.blue);
        //我的
        q.find(R.id.btn_me).background(R.drawable.tab_button_right_white).textColorId(R.color.white);

        FragmentManager mgr = getSupportFragmentManager();
        FragmentTransaction trans = mgr.beginTransaction();
        if (mgr.findFragmentByTag(FRAGMENT_TAG_ALL) == null) {
            trans.add(R.id.fragment_container, allTimelineFragment, FRAGMENT_TAG_ALL);
        } else {
            trans.show(allTimelineFragment);
        }
        if (mgr.findFragmentByTag(FRAGMENT_TAG_MY) != null) {
            trans.hide(myTimelineFragment);
        }
        trans.commit();

        currTimelineFragment = allTimelineFragment;
    }

    private void switchToSingle() {
        q.find(R.id.btn_all).background(R.drawable.tab_button_left_white).textColorId(R.color.white);
        q.find(R.id.btn_me).background(R.drawable.tab_button_right_white_a).textColorId(R.color.blue);

        FragmentManager mgr = getSupportFragmentManager();
        FragmentTransaction trans = mgr.beginTransaction();
        if (mgr.findFragmentByTag(FRAGMENT_TAG_MY) == null) {
            trans.add(R.id.fragment_container, myTimelineFragment, FRAGMENT_TAG_MY);
        } else {
            trans.show(myTimelineFragment);
        }
        if (mgr.findFragmentByTag(FRAGMENT_TAG_ALL) != null) {
            trans.hide(allTimelineFragment);
        }
        trans.commit();

        currTimelineFragment = myTimelineFragment;
    }

    /**
     * 跳转到好友的动态页面
     * @param context
     * @param uid
     * @param pageTitle
     */
    public static void launch(Context context, String uid, String pageTitle) {
        Intent intent = new Intent(context, TimelineActivity.class);
        intent.putExtra(EXTRA_UID, uid);
        intent.putExtra(EXTRA_PAGE_TITLE, pageTitle);
        intent.putExtra(EXTRA_BACK_FLAG, 1);
        context.startActivity(intent);
    }
    
    /**
     * 跳转到自己的动态页面
     * @param context
     * @param uid
     * @param pageTitle
     */
    public static void launchForOwner(Context context, String uid, String pageTitle) {
        Intent intent = new Intent(context, TimelineActivity.class);
        intent.putExtra(EXTRA_UID, uid);
//        intent.putExtra(EXTRA_PAGE_TITLE, pageTitle);
        intent.putExtra(EXTRA_BACK_FLAG, 1);
        context.startActivity(intent);
    }

    public interface OnMomentReviewDeleteListener {
        void onMomentDelete(String momentId, Review review);
    }

    public static String getSelectedTagServerDesc(Context context,int tagIdx,boolean surveyMultiSelect) {
        String tag="";
        switch(tagIdx) {
            case TAG_ALL_IDX:
                break;
            case TAG_NOTICE_IDX:
                tag=Moment.SERVER_MOMENT_TAG_FOR_NOTICE;
                break;
            case TAG_QA_IDX:
                tag=Moment.SERVER_MOMENT_TAG_FOR_QA;
                break;
            case TAG_SURVEY_IDX:
                if(surveyMultiSelect) {
                    tag=Moment.SERVER_MOMENT_TAG_FOR_SURVEY_MULTI;
                } else {
                    tag=Moment.SERVER_MOMENT_TAG_FOR_SURVEY_SINGLE;
                }
                break;
            case TAG_STUDY_IDX:
                tag=Moment.SERVER_MOMENT_TAG_FOR_STUDY;
                break;
            case TAG_LIFE_IDX:
                tag=Moment.SERVER_MOMENT_TAG_FOR_LIFE;
                break;
            case TAG_VIDEO_IDX:
                tag=Moment.SERVER_MOMENT_TAG_FOR_VIDEO;
                break;
            default:
                break;
        }
        
        return tag;
    }

    public static void deleteMomentReview(final Activity activity,final String momentId, final Review replyTo,final OnMomentReviewDeleteListener momentReviewDelListener) {
        final MessageBox msgBox = new MessageBox(activity);
        msgBox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                MomentWebServerIF mMomentWeb = MomentWebServerIF.getInstance(activity);
                return mMomentWeb.fDeleteMomentReview(momentId, replyTo);
            }

            @Override
            protected void onPostExecute(Integer errno) {
                msgBox.dismissWait();

                if (errno == ErrorCode.OK) {
                    if (null != momentReviewDelListener) {
                        momentReviewDelListener.onMomentDelete(momentId, replyTo);
                    }
//                                        if(null != instance) {
//                                            instance.deleteAReview(momentId,replyTo);
//                                        }
                } else {
                    msgBox.toast(R.string.msg_operation_failed);
                }
            }
        });
    }

    /**
     * 删除自己的动态
     * @date 2015/5/7
     * @param deletedMomentId
     */
    public static void deleteMomentForOwner(String deletedMomentId){
        currTimelineFragment.refreshDeleteAdapter(deletedMomentId);
    }


    public static String getSelectedTagLocalDesc(Context context,int tagIdx) {
        String tag="";
        switch(tagIdx) {
            case TAG_ALL_IDX:
                break;
            case TAG_NOTICE_IDX:
                tag=context.getString(R.string.moment_tag_notice);
                break;
            case TAG_QA_IDX:
                tag=context.getString(R.string.moment_tag_qa);
                break;
            case TAG_SURVEY_IDX:
                tag=context.getString(R.string.moment_tag_survey);
                break;
            case TAG_STUDY_IDX:
                tag=context.getString(R.string.moment_tag_study);
                break;
            case TAG_LIFE_IDX:
                tag=context.getString(R.string.moment_tag_life);
                break;
            case TAG_VIDEO_IDX:
                tag=context.getString(R.string.moment_tag_videoStudy);
                break;
            default:
                break;
        }
        return tag;
    }

    public static void replyToMoment_helper(int momentPosition, String momentId, Review replyTo,
                                            Activity activity,
                                            InputBoardManager.InputResultHandler handler,
                                            InputBoardManager.ChangeToOtherAppsListener changeAppsListener,
                                            View.OnClickListener onLikeClickListener) {

//        InputBoardManager inputMgr = handler.getInputBoardMangager();
        inputMgr = handler.getInputBoardMangager();
        

        Database db = new Database(activity);
        final Moment moment=db.fetchMoment(momentId);

        if (inputMgr == null) {
            inputMgr = new InputBoardManager(activity,
                    (ViewGroup)activity.findViewById(R.id.input_board_holder),
                    handler, changeAppsListener);
            inputMgr.setIsWithMultimediaMethod(false);
            inputMgr.setIsWithCallMethod(false);
//            inputMgr.drawableResId().open = R.drawable.sms_add_btn;
//            inputMgr.drawableResId().close = R.drawable.sms_close_btn;



//            inputMgr.drawableResId().gotoEmotion = R.drawable.timeline_like_btn;
//            inputMgr.drawableResId().gotoEmotion = R.drawable.sms_kaomoji_btn;
//            inputMgr.drawableResId().keyboard = R.drawable.sms_keyboard;
//            inputMgr.drawableResId().voiceNormal = R.drawable.sms_voice_btn;
//            inputMgr.drawableResId().voicePressed = R.drawable.sms_voice_btn_p;

//            inputMgr.setMomentLikeDrawable(R.drawable.timeline_like_btn,R.drawable.timeline_like_btn_a);

//            inputMgr.layoutTextInnerWrapper.setVisibility(View.GONE);

            handler.setInputBoardMangager(inputMgr);
        }
        if (inputMgr == null) {
            return;
        }

//    	inputMgr.setSoftKeyboardVisibility(true);

        //使得输入框获得事件焦点,自动弹起软件盘
        inputMgr.mTxtContent.setFocusable(true);
        inputMgr.mTxtContent.setFocusableInTouchMode(true);
        inputMgr.mTxtContent.requestFocus();

        Handler hanlder = new Handler();
        hanlder.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager)inputMgr.mTxtContent.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(inputMgr.mTxtContent, 0);
            }
        }, 200);

       
        String title;
        if (replyTo == null)
            title = String.format(activity.getString(R.string.moments_comment_hint),
                    TimelineActivity.COMMENT_MOST_WORDS);
        else
            title = String.format(activity.getResources().getString(R.string.moments_reply_hint),
                    replyTo.nickname,
                    TimelineActivity.COMMENT_MOST_WORDS);

        inputMgr.show(InputBoardManager.FLAG_SHOW_TEXT);
        inputMgr.setInputHint(title);
//        inputMgr.extra().putInt(MomentDetailActivity.EXTRA_REPLY_TO_MOMENT_POS, momentPosition);
        inputMgr.extra().putString(MomentDetailActivity.EXTRA_REPLY_TO_MOMENT_ID, momentId);
        inputMgr.extra().putParcelable(MomentDetailActivity.EXTRA_REPLY_TO_REVIEW, replyTo);

        inputMgr.setLayoutForTimelineMoment(moment,onLikeClickListener);




    }

    /**
     * 用户可以对评论做什么？
     *
     * @param momentPosition position in list
     * @param momentId
     * @param replyTo
     * @param menu
     * @param activity
     * @param handler
     */
    public static void doWithReview(
            final int momentPosition, final String momentId, final Review replyTo,
            final BottomButtonBoard menu,
            final Activity activity,
            final InputBoardManager.InputResultHandler handler,
            final InputBoardManager.ChangeToOtherAppsListener chageAppsListener,
            final TimelineActivity.OnMomentReviewDeleteListener momentReviewDelListener,
            final View.OnClickListener onLikeBtnClickListener) {

        // 除了回复评论，还可以对评论的内容进行操作

//        final InputBoardManager inputMgr = handler.getInputBoardMangager();
        inputMgr = handler.getInputBoardMangager();
        if (inputMgr != null)
        	 inputMgr.setSoftKeyboardVisibility(false);

        //回复按钮
        menu.add(activity.getString(R.string.moments_reply), BottomButtonBoard.BUTTON_BLUE,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                    	
                    	//把获得的数据传到输入框
                        replyToMoment_helper(momentPosition, momentId, replyTo, activity, handler,
                                chageAppsListener, onLikeBtnClickListener);

                        menu.dismiss();
                    }
                });

        //自己的评论，自己可以对其进行删除操作
        //if review is made by me or moment is mine, add delete item
        String myUserId = PrefUtil.getInstance(activity).getUid();

        Database db = new Database(activity);
        Moment replyMoment=db.fetchMoment(momentId);
        if (myUserId.equals(replyTo.uid) || (null != replyMoment && replyMoment.owner.userID.equals(myUserId))) {
            menu.add(activity.getString(R.string.contacts_local_delete), BottomButtonBoard.BUTTON_RED,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            menu.dismiss();

                            MessageDialog dialog = new MessageDialog(activity);
                            dialog.setTitle("提示");
                            dialog.setMessage("确定要删除这条回复吗？");
                            dialog.setCancelable(false);
                            dialog.setRightBold(true);
                            dialog.setTextColorBtnRight(activity.getResources().getColor(R.color.red_30));
                            dialog.setOnLeftClickListener("取消", null);
                            dialog.setOnRightClickListener("删除", new MessageDialog.MessageDialogClickListener() {
                                @Override
                                public void onclick(MessageDialog dialog) {
                                    dialog.dismiss();
                                    deleteMomentReview(activity,momentId,replyTo,momentReviewDelListener);
                                }
                            });
                            dialog.show();


//                            deleteMomentReview(activity,momentId,replyTo,momentReviewDelListener);
                        }
                    });
        }


        TextOperationHelper.fillMenu(activity, menu,null, replyTo.text, true);

        menu.show();
    }
}