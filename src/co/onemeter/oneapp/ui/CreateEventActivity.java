package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;

import com.androidquery.AQuery;

import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.EventWebServerIF;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WEvent;
import org.wowtalk.api.WFile;
import org.wowtalk.ui.MessageBox;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CreateEventActivity extends Activity implements OnClickListener {
    public static final String EXTRA_PAGE_TITLE = "page_title";
    public static final String EXTRA_EVENT_CATEGORY = "event_cat";

    private static final int REQ_INPUT_TITLE = 123;
    private static final int REQ_INPUT_CATEGORY = 124;
    private static final int REQ_INPUT_TIME = 125;
    private static final int REQ_INPUT_PLACE = 126;
    private static final int REQ_INPUT_DESC = 127;
    private static final int REQ_INPUT_COINS = 128;
    private static final int REQ_INPUT_CAPACITY = 129;
    private static final int REQ_INPUT_IMAGE = 130;
    private static final int REQ_INPUT_TELE = 131;

    private WEvent wevent = new WEvent();

    private boolean isAllDay = false;
    private boolean allowReview = false;
    private boolean needToUpdateMulti = false;
    private boolean mIsInfo = true;
    private boolean mIsPublic = true;

    private ImageButton btnTitleBack;
    private TextView btnTitleConfirm;
    private ImageVideoInputWidget imageInputWidget;
    private TextView txtTitle;
    private TextView txtLoc;
    private TextView txtStartTime;
    private TextView txtEndTime;
    private TextView txtCapacity;
    private TextView txtCoins;
    private TextView txtTelephone;
    
    private ImageView imgAllDay;
    private ImageView imgAllowReview;
    private ImageView imgNeedToUpdateMulti;
    
    private ImageButton isBtnInfo;
    private ImageButton isBtnPublic;

    private EditText edtContent;
    private MessageBox msgBox;
    
    private LinearLayout lay_starttime;
    private LinearLayout lay_endtime;
    
    private Handler hanlder = new Handler(){
    	public void handleMessage(android.os.Message msg) {
    		if(msg.what == 0){
    			lay_starttime.setEnabled(true);
    			lay_endtime.setEnabled(true);
    		}
    	};
    };

    private void initView(Bundle bundle) {
        AQuery q = new AQuery(this);
        q.find(R.id.layout_capacity).clicked(this);
        q.find(R.id.layout_coins).clicked(this);

        imageInputWidget = (ImageVideoInputWidget) q.find(R.id.image_input_widget).getView();
        imageInputWidget.setup(this, ImageVideoInputWidget.MediaType.Photo, REQ_INPUT_IMAGE);

        btnTitleBack = (ImageButton) findViewById(R.id.title_back);
        btnTitleConfirm = (TextView) findViewById(R.id.title_confirm);

        txtTitle = (TextView) findViewById(R.id.txt_title);
        txtLoc = (TextView) findViewById(R.id.txt_loc);
        txtStartTime = (TextView) findViewById(R.id.txt_start_time);
        txtEndTime = (TextView) findViewById(R.id.txt_end_time);
        edtContent = (EditText) findViewById(R.id.edt_content);
        txtCapacity = (TextView) findViewById(R.id.txt_capacity);
        txtCoins = (TextView) findViewById(R.id.txt_coins);
        txtTelephone = (TextView) findViewById(R.id.txt_telephone);

        imgAllDay = (ImageView) findViewById(R.id.img_allday);
        imgAllowReview = (ImageView) findViewById(R.id.img_allowreview);
        imgNeedToUpdateMulti = (ImageView) findViewById(R.id.img_needtoupdatemulti);
        isBtnInfo = (ImageButton) findViewById(R.id.btn_isinfo);
        isBtnPublic = (ImageButton) findViewById(R.id.btn_ispublic);
        
        lay_starttime = (LinearLayout) findViewById(R.id.layout_starttime);
        lay_endtime = (LinearLayout)findViewById(R.id.layout_endtime);
        
        ScrollView scrollView = (ScrollView) findViewById(R.id.scrollview_cre_eve);
        scrollView.scrollTo(0, 0);

        btnTitleBack.setOnClickListener(this);
        btnTitleConfirm.setOnClickListener(this);

        imgAllDay.setOnClickListener(this);
        imgAllowReview.setOnClickListener(this);
        imgNeedToUpdateMulti.setOnClickListener(this);
        
        isBtnInfo.setOnClickListener(this);
        isBtnPublic.setOnClickListener(this);

        lay_starttime.setOnClickListener(this);
        lay_endtime.setOnClickListener(this);
        findViewById(R.id.layout_title).setOnClickListener(this);
        findViewById(R.id.layout_loc).setOnClickListener(this);
        findViewById(R.id.layout_repeat).setOnClickListener(this);
        findViewById(R.id.layout_openlevel).setOnClickListener(this);
        findViewById(R.id.layout_targetuser).setOnClickListener(this);
        findViewById(R.id.layout_capacity).setOnClickListener(this);
        findViewById(R.id.layout_coins).setOnClickListener(this);
        findViewById(R.id.layout_addaudio).setOnClickListener(this);
        findViewById(R.id.layout_telephone).setOnClickListener(this);

        if (bundle != null) {
            setTitle(bundle.getString(EXTRA_PAGE_TITLE));
        } else {
            setTitle("");
        }
        wevent.is_get_member_info = mIsInfo;
        msgBox = new MessageBox(this);
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        new AQuery(this).find(R.id.title_text).text(title);
    }

    private void createEvent() {
        updateData();
        msgBox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<WEvent, Void, Integer>() {
            Context context;

            @Override
            protected Integer doInBackground(WEvent... wEvents) {
                context = CreateEventActivity.this;
                final WEvent e = wEvents[0];
                final EventWebServerIF eventweb = EventWebServerIF.getInstance(context);

                // create event record
                int errno = eventweb.fAdd(e);

                // upload multi medias
                if (errno == ErrorCode.OK && e.multimedias != null && !e.multimedias.isEmpty()) {
                    for (WFile f : e.multimedias) {
                        f.remoteDir = WEvent.MEDIA_FILE_REMOTE_DIR;
                    }

                    MediaFilesBatchUploader.upload(context,
                            e.multimedias, new MediaFilesBatchUploader.OnMediaFilesUploadedListener() {
                                @Override
                                public void onAllMediaFilesUploaded(List<WFile> files) {
                                    for (WFile f : files) {
                                        eventweb.fUploadMultimedia(
                                                e.id, f.getExt(), f.fileid, f.thumb_fileid, f.duration);
                                    }
                                }

                                @Override
                                public void onMediaFilesUploadFailed(List<WFile> files) {

                                }
                            });
                }
                return errno;
            }

            @Override
            protected void onPostExecute(Integer errno) {
                msgBox.dismissWait();
                if (errno == ErrorCode.OK) {
                    //Log.i("--->>>create", wevent.is_get_member_info+"");
                    wevent.owner_uid = PrefUtil.getInstance(CreateEventActivity.this).getUid();
                    setResult(RESULT_OK,new Intent().putExtra("event",wevent));
                    finish();
                } else {
                    Toast.makeText(context, R.string.operation_failed, Toast.LENGTH_SHORT).show();
                }
            }
        }, wevent);
    }

    @Override
    public void onBackPressed() {
    	super.onBackPressed();
    	if(msgBox != null){
    		msgBox.dismissToast();
    	}
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_event);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        Bundle b = savedInstanceState == null ? getIntent().getExtras() : savedInstanceState;
        if (b != null) {
            wevent.category = b.getString(EXTRA_EVENT_CATEGORY);
        }

        initView(b);

        if (savedInstanceState != null) {
            imageInputWidget.restoreInstanceState(savedInstanceState);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        imageInputWidget.saveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_back:
                finish();
                break;
            case R.id.title_confirm:
            	boolean isContentTrue = !TextUtils.isEmpty(txtTitle.getText()) && !TextUtils.isEmpty(txtLoc.getText()) 
            				&& !TextUtils.isEmpty(txtStartTime.getText())&& !TextUtils.isEmpty(txtEndTime.getText())
            				&& !TextUtils.isEmpty(edtContent.getText().toString()) && !TextUtils.isEmpty(txtCapacity.getText()) 
            				&& !TextUtils.isEmpty(txtCoins.getText()) && !TextUtils.isEmpty(txtTelephone.getText()); 
                if(isContentTrue){
                	createEvent();
                	closeSoftKeyboard();
                }else{
                	if(!msgBox.isWaitShowing()){
                		msgBox.toast(getString(R.string.event_finish_info), 1000);
                	//Toast.makeText(this, getString(R.string.event_finish_info), Toast.LENGTH_SHORT).show();
                	}
                }
                break;
            case R.id.img_allday:
                isAllDay = !isAllDay;
                imgAllDay.setImageResource(isAllDay ? R.drawable.switch_on : R.drawable.switch_off);
                break;
            case R.id.img_allowreview:
                allowReview = !allowReview;
                imgAllowReview.setImageResource(allowReview ? R.drawable.switch_on : R.drawable.switch_off);
                break;
            case R.id.img_needtoupdatemulti:
                needToUpdateMulti = !needToUpdateMulti;
                imgNeedToUpdateMulti.setImageResource(needToUpdateMulti ? R.drawable.switch_on : R.drawable.switch_off);
                break;
            case R.id.layout_title: {
            	Intent intent = new Intent(this, InputPlainTextActivity.class);
                intent.putExtra(InputPlainTextActivity.EXTRA_TITLE, getString(R.string.event_title));
                intent.putExtra(InputPlainTextActivity.EXTRA_VALUE,txtTitle.getText());
                startActivityForResult(intent,REQ_INPUT_TITLE);
                break;
            }
            case R.id.layout_loc:
                startActivityForResult(
                        new Intent(this, InputPlainTextActivity.class)
                                .putExtra(InputPlainTextActivity.EXTRA_TITLE, getString(R.string.event_loc))
                                .putExtra(InputPlainTextActivity.EXTRA_VALUE,txtLoc.getText()),
                        REQ_INPUT_PLACE);
                break;
                
            case R.id.layout_telephone:
            	startActivityForResult(new Intent(this, InputPlainTextActivity.class)
            			.putExtra(InputPlainTextActivity.EXTRA_TITLE, getString(R.string.event_telephone))
            			.putExtra(InputPlainTextActivity.EXTRA_VALUE,txtTelephone.getText())
            			 .putExtra(InputPlainTextActivity.EXTRA_INPUTTYPE, InputType.TYPE_CLASS_NUMBER), 
            			REQ_INPUT_TELE);
            	break;
            case R.id.layout_starttime:
            	lay_starttime.setEnabled(false);
            	hanlder.sendEmptyMessageDelayed(0, 1000);
	            DateTimeInputHelper.inputStartDateTime(this, new DateTimeInputHelper.OnDateTimeSetListener() {
	                 @Override
	                 public void onDateTimeResult(Calendar result) {
	                     wevent.startTime = result.getTime();
	                     if(wevent.endTime != null){
	                    	 if(wevent.startTime.getTime() > wevent.endTime.getTime()){
	                    		 wevent.endTime = wevent.startTime;
	                    		 msgBox.toast(R.string.event_time_end_earlier);
	                    		 updateUI();
	                    	 }
	                     }
	                     if(wevent.startTime.getTime() > System.currentTimeMillis()){
	                        	updateUI();
	                     }else{
	                        Toast.makeText(CreateEventActivity.this, R.string.event_time_start_earlier_now, Toast.LENGTH_SHORT).show();
	                     }
	                     lay_starttime.setEnabled(true);
	                  }
	            });
                break;
            case R.id.layout_endtime:
            	lay_endtime.setEnabled(false);
            	hanlder.sendEmptyMessageDelayed(0, 1000);
                DateTimeInputHelper.inputStartDateTime(this, new DateTimeInputHelper.OnDateTimeSetListener() {
                    @Override
                    public void onDateTimeResult(Calendar result) {
                        wevent.endTime = result.getTime();
                        if(wevent.endTime.getTime() > System.currentTimeMillis()){
                        	if(wevent.startTime != null && wevent.startTime.getTime() < wevent.endTime.getTime()){
                        		updateUI();
                        	}else{
                            	Toast.makeText(CreateEventActivity.this, R.string.event_time_end_earlier, Toast.LENGTH_SHORT).show();
                        	}
                        }else{
                        	Toast.makeText(CreateEventActivity.this, R.string.event_time_start_earlier_now, Toast.LENGTH_SHORT).show();
                        }
                        lay_endtime.setEnabled(true);
                    }
                });
                break;
            case R.id.layout_repeat:
                break;
            case R.id.layout_openlevel:
                break;
            case R.id.layout_targetuser:
                break;
            case R.id.layout_capacity:
                startActivityForResult(
                        new Intent(this, InputPlainTextActivity.class)
                                .putExtra(InputPlainTextActivity.EXTRA_TITLE, getString(R.string.event_capacity))
                                .putExtra(InputPlainTextActivity.EXTRA_ALLOW_EMPTY, true)
                                .putExtra(InputPlainTextActivity.EXTRA_INPUTTYPE, InputType.TYPE_CLASS_NUMBER),
                        REQ_INPUT_CAPACITY);
                break;
            case R.id.layout_coins:
                startActivityForResult(
                        new Intent(this, InputPlainTextActivity.class)
                                .putExtra(InputPlainTextActivity.EXTRA_TITLE, getString(R.string.event_coins))
                                .putExtra(InputPlainTextActivity.EXTRA_ALLOW_EMPTY, true)
                                .putExtra(InputPlainTextActivity.EXTRA_INPUTTYPE, InputType.TYPE_CLASS_NUMBER),
                        REQ_INPUT_COINS);
                break;
            case R.id.layout_addaudio:
                break;
            case R.id.btn_isinfo:
            	changeIsInfo(!mIsInfo);
            	break;
            case R.id.btn_ispublic:
            	changeIsPublic(!mIsPublic);
            	break;
            default:
                break;
        }
    }

    private void closeSoftKeyboard() {
        InputMethodManager mInputMethodManager ;
        mInputMethodManager = (InputMethodManager) this
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        mInputMethodManager .hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }

	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(resultCode, resultCode, data);

        if (REQ_INPUT_IMAGE == requestCode) {
            imageInputWidget.handleActivityResult(requestCode, resultCode, data);
            return;
        }

        if (resultCode != RESULT_OK)
            return;

        switch (requestCode) {
            case REQ_INPUT_TITLE:
                wevent.title = data.getStringExtra(InputPlainTextActivity.EXTRA_VALUE);
                updateUI();
                break;
            case REQ_INPUT_DESC:
                wevent.description = data.getStringExtra(InputPlainTextActivity.EXTRA_VALUE);
                updateUI();
                break;
            case REQ_INPUT_PLACE:
                wevent.address = data.getStringExtra(InputPlainTextActivity.EXTRA_VALUE);
                updateUI();
                break;
            case REQ_INPUT_TELE:
            	wevent.telephone = data.getStringExtra(InputPlainTextActivity.EXTRA_VALUE);
            	txtTelephone.setText(wevent.telephone);
            	break;
            case REQ_INPUT_CAPACITY : {
                String s = data.getStringExtra(InputPlainTextActivity.EXTRA_VALUE);
                wevent.capacity = 0;
                if (!TextUtils.isEmpty(s)) {
                    try {
                        wevent.capacity = Integer.parseInt(s);
                    } catch (Exception e) {
                    }
                }
                updateUI();
                break;
            }
            case REQ_INPUT_COINS : {
                String s = data.getStringExtra(InputPlainTextActivity.EXTRA_VALUE);
                wevent.costGolds = 0;
                if (!TextUtils.isEmpty(s)) {
                    try {
                        wevent.costGolds = Integer.parseInt(s);
                    } catch (Exception e) {
                    }
                }
                updateUI();
                break;
            }
        }
    }

    private void updateUI() {
        if (wevent == null)
            return;

        txtTitle.setText(wevent.title);
        txtLoc.setText(wevent.description);
        txtLoc.setText(wevent.address);

        AQuery q = new AQuery(this);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        if (wevent.startTime != null) {
            q.find(R.id.txt_start_time).text(sdf.format(wevent.startTime));
        }
        if (wevent.endTime != null) {
            q.find(R.id.txt_end_time).text(sdf.format(wevent.endTime));
        }

        q.find(R.id.txt_coins).text(wevent.costGolds > 0 ?
                Integer.toString(wevent.costGolds) :
                getString(R.string.event_join_coin_free));
        q.find(R.id.txt_capacity).text(wevent.capacity > 0 ?
                Integer.toString(wevent.capacity) :
                getString(R.string.event_not_limited));
    }

    private void updateData() {
        if (wevent == null)
            return;

        AQuery q = new AQuery(this);
        wevent.description = q.find(R.id.edt_content).getText().toString();
        wevent.multimedias = new ArrayList<WFile>(imageInputWidget.getItemCount());
        for (int i = 0; i < imageInputWidget.getItemCount(); ++i) {
            wevent.multimedias.add(imageInputWidget.getItem(i));
        }
    }
    
    private void changeIsInfo(boolean isInfo) {
        mIsInfo = isInfo;
    	wevent.is_get_member_info = isInfo;
    	isBtnInfo.setBackgroundResource(isInfo ? R.drawable.icon_switch_on : R.drawable.icon_switch_off);
	}
    
    private void changeIsPublic(boolean isPublic) {
    	mIsPublic = isPublic;
    	wevent.isOpen = mIsPublic;
    	isBtnPublic.setBackgroundResource(mIsPublic ? R.drawable.icon_switch_on : R.drawable.icon_switch_off);
	}

}
