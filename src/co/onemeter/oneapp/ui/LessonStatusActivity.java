package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;

import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.LessonPerformance;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.MessageDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * 上课状态页面。
 * Created by yl on 23/12/2014.
 */
public class LessonStatusActivity extends Activity implements OnClickListener{

	public static final String FALG = "isTeacher";
	
	private int lessonId;
	private String stuId;
	private boolean isTeacher;
	
	private List<LessonPerformance> lesPersToPost;
	private List<LessonPerformance> stuPersFromNet;
	private String[] preformstrs;
	
	private MessageBox mMsgBox;
	private LessonWebServerIF lessonServer;
	private Database mDBHelper;
	
	private ListView lvPerformances;
	
	private Handler handler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			if(msg.what == ErrorCode.OK){
				stuPersFromNet.addAll(mDBHelper.fetchLessonPerformance(lessonId, stuId));
//				android.util.Log.i("-->>", stuPersFromNet.toString());
				lvPerformances.setAdapter(new LessonStatusAdapter(preformstrs));
				if(stuPersFromNet.isEmpty()){
					findViewById(R.id.btn_parent_confirm).setVisibility(View.VISIBLE);
					if(!isTeacher){
						ContextThemeWrapper themedContext;
						if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
						    themedContext = new ContextThemeWrapper( LessonStatusActivity.this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar );
						}
						else {
						    themedContext = new ContextThemeWrapper( LessonStatusActivity.this, android.R.style.Theme_Light_NoTitleBar );
						}
						new AlertDialog.Builder(themedContext).setTitle("温馨提示").setMessage("老师还没有评分！").setCancelable(false).setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								finish();
							}
						}).create().show();
					}
				}else{
					if(stuPersFromNet.get(0).property_id == 10){
						findViewById(R.id.btn_parent_confirm).setVisibility(View.VISIBLE);
					}
					if(!isTeacher){
						findViewById(R.id.btn_parent_confirm).setVisibility(View.GONE);
					}
				}
				
			}else{
				mMsgBox.toast(R.string.class_class_status_not_comfired);
			}
		};
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lesson_status);
		
		initView();
	}
	
	private void initView(){
		lesPersToPost = new ArrayList<LessonPerformance>();
		stuPersFromNet = new ArrayList<LessonPerformance>();
		
		mMsgBox = new MessageBox(this);
		lessonServer = LessonWebServerIF.getInstance(LessonStatusActivity.this);
		mDBHelper = Database.open(this);
		
		preformstrs = getResources().getStringArray(R.array.lesson_performance_names);
		
		lvPerformances = (ListView) findViewById(R.id.lv_les_status);
		findViewById(R.id.title_back).setOnClickListener(this);
		
		Button btn = (Button) findViewById(R.id.btn_parent_confirm);
		btn.setOnClickListener(this);
		
		Intent intent = getIntent();
		lessonId = intent.getIntExtra(Constants.LESSONID,0);
		stuId = intent.getStringExtra(Constants.STUID);
		isTeacher = intent.getBooleanExtra(FALG, false);
		if(isTeacher){
			btn.setText(R.string.login_retrieve_password_btn);
		}else{
			btn.setVisibility(View.GONE);
		}
		getStuPerformceById();
	}
	

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_parent_confirm:
			if(isTeacher){
				MessageDialog dialog = new MessageDialog(LessonStatusActivity.this);
                dialog.setTitle("提示");
                dialog.setMessage("你确定提交这次课堂点评吗?");
                dialog.setCancelable(false);
                dialog.setRightBold(true);
                dialog.setOnLeftClickListener("取消", null);
                
                dialog.setOnRightClickListener("确定", new MessageDialog.MessageDialogClickListener() {
                    @Override
                    public void onclick(MessageDialog dialog) {
                        dialog.dismiss();
                        submitStuPerformance();
                    }
                }
                );
                dialog.show();
				
			}else{
				
			}
			break;
		case R.id.title_back:
			finish();
			break;
		default:
			break;
		}
	}
	
	private void getStuPerformceById(){
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				int errno = lessonServer.getLessonPerformance(lessonId, stuId);
				handler.sendEmptyMessage(errno);
			}
		}).start();;
	}
	
	private void submitStuPerformance(){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			protected void onPreExecute() {
				mMsgBox.showWait();
			}

			;

			@Override
			protected Integer doInBackground(Void... params) {
				return lessonServer.addOrModifyLessonPerformance(lesPersToPost);
			}

			protected void onPostExecute(Integer result) {
				mMsgBox.dismissWait();
				if (ErrorCode.OK == result) {
					mMsgBox.toast(R.string.class_submit_success);
					finish();
				} else {
					mMsgBox.toast(R.string.class_submit_failed);
				}
			}

			;

		});
	}
	
	class LessonStatusAdapter extends BaseAdapter{
		private String[] pers;
		
		public LessonStatusAdapter(String[] pers){
			this.pers = pers;
		}
		
		@Override
		public int getCount() {
			return pers.length;
		}

		@Override
		public Object getItem(int position) {
			return pers[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if(convertView == null){
				holder = new ViewHolder();
				convertView = getLayoutInflater().inflate(R.layout.listitem_lesson_status, parent, false);
				holder.tv_per = (TextView) convertView.findViewById(R.id.item_les_perform_tv);
				holder.rg_per = (RadioGroup) convertView.findViewById(R.id.item_les_radioGroup);
				holder.radio0 = (RadioButton) convertView.findViewById(R.id.radio0);
				holder.radio1 = (RadioButton) convertView.findViewById(R.id.radio1);
				holder.radio2 = (RadioButton) convertView.findViewById(R.id.radio2);
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}
			holder.tv_per.setText(pers[position]);
			if(!stuPersFromNet.isEmpty()){
				if(stuPersFromNet.get(0).property_id == 10){
					if(!isTeacher){
						holder.radio0.setChecked(false);
						holder.radio1.setChecked(false);
						holder.radio2.setChecked(false);
					}
					final LessonPerformance performance = new LessonPerformance();
					performance.property_value = 1;
					performance.lesson_id = lessonId;
					performance.student_id = stuId;
					performance.property_id = position + 1;
					holder.rg_per.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
						
						@Override
						public void onCheckedChanged(RadioGroup group, int checkedId) {
							switch (checkedId) {
							case R.id.radio0:
								performance.property_value = 1;
								break;
							case R.id.radio1:
								performance.property_value = 2;
								break;
							case R.id.radio2:
								performance.property_value = 3;
								break;
							default:
								performance.property_value = 1;
									break;
							}
						}
					});
					lesPersToPost.add(performance);
				}else{
					if(position < stuPersFromNet.size()){
						int value = stuPersFromNet.get(position).property_value;
						switch (value) {
							case 1:
								holder.radio0.setChecked(true);
								break;
							case 2:
								holder.radio1.setChecked(true);
								break;
							case 3:
								holder.radio2.setChecked(true);
								break;
						}
				}
				holder.rg_per.setEnabled(false);
				holder.radio0.setEnabled(false);
				holder.radio1.setEnabled(false);
				holder.radio2.setEnabled(false);
				}
				
			}else{
				if(!isTeacher){
					holder.radio0.setChecked(false);
					holder.radio1.setChecked(false);
					holder.radio2.setChecked(false);
				}
				final LessonPerformance performance = new LessonPerformance();
				performance.property_value = 1;
				performance.lesson_id = lessonId;
				performance.student_id = stuId;
				performance.property_id = position + 1;
				holder.rg_per.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
					
					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						switch (checkedId) {
						case R.id.radio0:
							performance.property_value = 1;
							break;
						case R.id.radio1:
							performance.property_value = 2;
							break;
						case R.id.radio2:
							performance.property_value = 3;
							break;
						default:
							performance.property_value = 1;
								break;
						}
					}
				});
				lesPersToPost.add(performance);
			}
			if(!isTeacher){
				holder.rg_per.setEnabled(false);
				holder.radio0.setEnabled(false);
				holder.radio1.setEnabled(false);
				holder.radio2.setEnabled(false);
			}
			return convertView;
		}
		
		class ViewHolder{
			TextView tv_per;
			RadioGroup rg_per;
			RadioButton radio0;
			RadioButton radio1;
			RadioButton radio2;
		}
		
	}

}
