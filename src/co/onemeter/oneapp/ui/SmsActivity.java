package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.MessagesAdapter;
import co.onemeter.oneapp.utils.ThemeHelper;
import co.onemeter.utils.AsyncTaskExecutor;

import org.wowtalk.api.*;

import java.util.ArrayList;

/**
 * 消息会话记录页面
 */
public class SmsActivity extends Activity implements OnClickListener {

    /**
     * 每页显示的消息条数(每次额外加载的数目)
     */
    public static final int LIMIT_COUNT_PER_PAGE = 20;
    private static final int REQ_PICK_BUDDYS = 123;

    /**
     * 初始化加载，加载LIMIT_COUNT_PER_PAGE条
     */
    private static final int LOAD_INIT = 0;
    /**
     * 切换帐号后加载，加载本地数据
     */
    private static final int LOAD_SWITCH_ACCOUNT = 1;
    /**
     * 登录之后获取的WowtalkWebServerIF#getLatestChatTargets完成
     */
    private static final int LOAD_AFTER_FINISHED_DOWNLOAD = 2;
    /**
     * 有新会话产生或者当前会话有更新，加载发送时间在当前最后一条之后的
     */
    private static final int LOAD_NEW_OR_UPDATE = 3;
    /**
     * 点击下方"加载更多"，加载当前总条数的额外的LIMIT_COUNT_PER_PAGE条
     */
    private static final int LOAD_MORE = 4;
    private static SmsActivity instance;
    private static int sCurrentCount = 0;
    /**
     * 加号（跳转到选择联系人页面）
     **/
    private ImageButton btnTitleEdit;
    /**
     * 标题
     **/
    private static TextView txtSmsTitle;
    /**
     * 会话列表
     **/
    private ListView lvSms;
    /**
     * 没有会话提示
     **/
    private LinearLayout layoutBg;
    /**
     * 消息表情
     **/
    private ArrayList<ChatMessage> log_msg;
    /**
     * 消息会话适配器
     **/
    private MessagesAdapter myAdapter;
    /**
     * web服务请求
     **/
    private WowTalkWebServerIF mWebIF;
    /**
     * 请求接口工具类
     **/
    private PrefUtil mPrefUtil;
    /**
     * 数据库
     **/
    private Database mDb;
//    private MessageBox mMsgBox;
    /**
     * 标题栏布局
     **/
    private RelativeLayout titleBar;
    /**
     * 搜索布局
     **/
    private RelativeLayout searchLayoutRightBtn;
    /**
     * 搜索取消按钮
     **/
    private Button searchCancelBtn;
    /**
     * 搜索框
     **/
    private EditText etSearch;
    /**
     * 搜索清除按钮
     **/
    private ImageButton ibSearchContentClear;
    /**
     * 搜索年级为空的view显示
     **/
    private ImageView searchEmptyGlass;
    /**
     * 加载更多提示
     **/
    private TextView mLoadMoreText;
    private final static int MSG_ID_REQUEST_FILTER_CONTENT = 1;

    private boolean isResumed = false;
    // 是否还有未获取的最近联系人的聊天信息
    /**
     * 更多从服务器获取
     **/
    private boolean mHasMoreInServer = true;
    /**
     * 加载本地
     **/
    private boolean mHasMoreInLocal = true;
    /**
     * 加载更多
     **/
    private boolean mIsLoadingMore = true;

    //    private Handler mHandler= new Handler() {
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case MSG_ID_REQUEST_FILTER_CONTENT:
//                    updateContentView(true);
//                    break;
//                default:
//                    Log.e("unknown msg id "+msg.what);
//            }
//        }
//    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sms_page);
        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        instance = this;
        log_msg = new ArrayList<ChatMessage>();
        myAdapter = new MessagesAdapter(this, log_msg);
        mDb = new Database(this);
        mWebIF = WowTalkWebServerIF.getInstance(this);
        mPrefUtil = PrefUtil.getInstance(this);
//        mMsgBox = new MessageBox(this);

        initView();
        lvSms.setAdapter(myAdapter);
        updateContentView(true);
        setMoreText(mIsLoadingMore);
        // 不是从登录界面进入的，则获取本地的数据
        if (!GlobalValue.IS_BOOT_FROM_LOGIN) {
            // 老用户(version=75)没有latest_chat_target这个表（需要兼容）
            loadDatas(++loadingId, LOAD_INIT);
        }
        mHasMoreInServer = mPrefUtil.hasMoreLatestChatTargetInServer();

        Database.addDBTableChangeListener(Database.DUMMY_TBL_SWITCH_ACCOUNT, mAccountSwitchObserver);
        Database.addDBTableChangeListener(Database.TBL_LATEST_CHAT_TARGET, mLatestChatTargetObserver);
        Database.addDBTableChangeListener(Database.DUMMY_TBL_LATEST_CHAT_TARGET_FINISHED, mLatestChatTargetFinishedObserver);
        Database.addDBTableChangeListener(Database.DUMMY_TBL_CHAT_MESSAGES_READED, mChatMessagesReadedObserver);
        Database.addDBTableChangeListener(Database.DUMMY_TBL_NOTICE_MAN_CHANGED, mNoticeManChangedObserver);
//        Database.addDBTableChangeListener(Database.TBL_MESSAGES,chatmessageObserverSms);
//        Database.addDBTableChangeListener(Database.TBL_FINISH_LOAD_MEMBERS, mTempGroupNameObserver);
        registerReceiver(mOutgoMsgReceiver, new IntentFilter(GlobalSetting.OUTGO_MESSAGE_INTENT));
        setVolumeControlStream(SoundPoolManager.USED_AUDIO_STREAM_TYPE);
    }


    /**
     * 搜索框输入监听器
     */
    private TextWatcher mSearchWacher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (isResumed) {
                if (s.length() == 0) {
                    ibSearchContentClear.setVisibility(View.GONE);
                    searchEmptyGlass.setVisibility(View.VISIBLE);
                } else {
                    ibSearchContentClear.setVisibility(View.VISIBLE);
                    searchEmptyGlass.setVisibility(View.GONE);
                }
                updateContentView(true);
            }
//            mHandler.removeMessages(MSG_ID_REQUEST_FILTER_CONTENT);
//            mHandler.sendEmptyMessageDelayed(MSG_ID_REQUEST_FILTER_CONTENT,100);
        }
    };

    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (View.VISIBLE == titleBar.getVisibility() &&
                    v.getId() == R.id.edt_search &&
                    MotionEvent.ACTION_DOWN == event.getAction()) {
                titleBar.setVisibility(View.GONE);
                searchLayoutRightBtn.setVisibility(View.VISIBLE);
                searchEmptyGlass.setVisibility(View.VISIBLE);
                etSearch.setFocusable(true);
                etSearch.setFocusableInTouchMode(true);
                etSearch.requestFocus();
                ((InputMethodManager) SmsActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
            }
            return false;
        }
    };

    /**
     * webtalk端发送的消息
     */
    private BroadcastReceiver mOutgoMsgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GlobalSetting.OUTGO_MESSAGE_INTENT.equals(intent.getAction())) {
                loadDatas(++loadingId, LOAD_NEW_OR_UPDATE);
            }
        }
    };

    /**
     * 获取当前聊天对象的数量（记录）
     *
     * @return
     */
    public static int getCurrentChatTargets() {
        return sCurrentCount;
    }

    /**
     * 初始化组件
     */
    private void initView() {
        btnTitleEdit = (ImageButton) findViewById(R.id.title_edit);
        txtSmsTitle = (TextView) findViewById(R.id.txtSmsTitle);
        lvSms = (ListView) findViewById(R.id.sms_list);
        layoutBg = (LinearLayout) findViewById(R.id.layout_bg);
        RelativeLayout headView = (RelativeLayout) LayoutInflater.from(SmsActivity.this)
                .inflate(R.layout.sms_page_header, null);
        lvSms.addHeaderView(headView);
        RelativeLayout moreLayout = (RelativeLayout) LayoutInflater.from(SmsActivity.this)
                .inflate(R.layout.listitem_load_more, null);
        lvSms.addFooterView(moreLayout);
        mLoadMoreText = (TextView) findViewById(R.id.text);
        mLoadMoreText.setVisibility(View.GONE);

        titleBar = (RelativeLayout) findViewById(R.id.title_bar);
        searchLayoutRightBtn = (RelativeLayout) findViewById(R.id.search_margin_right);
        searchCancelBtn = (Button) findViewById(R.id.cancel_btn);
        etSearch = (EditText) findViewById(R.id.edt_search);
        ibSearchContentClear = (ImageButton) findViewById(R.id.field_clear);
        searchEmptyGlass = (ImageView) findViewById(R.id.search_glass_img);

        etSearch.addTextChangedListener(mSearchWacher);

        searchCancelBtn.setOnClickListener(this);
        etSearch.setOnTouchListener(mOnTouchListener);
        ibSearchContentClear.setOnClickListener(this);
        searchEmptyGlass.setOnClickListener(this);

        btnTitleEdit.setOnClickListener(this);
        searchEmptyGlass.setVisibility(View.GONE);
    }

    /**
     * 设置消息的点击事件
     *
     * @param usingList
     */
    private void setMsgItemListener(final ArrayList<ChatMessage> usingList) {
        final View parentView = findViewById(R.id.layout);
        /**listview的点击事件**/
        lvSms.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                                    long arg3) {
                // footer_view
                if (position > usingList.size()) {
                    if (mIsLoadingMore) {
                        return;
                    }
                    setMoreText(true);
                    loadDatas(++loadingId, LOAD_MORE);
                    return;
                }
                // 添加了header_view
                ChatMessage msg = usingList.get(position - 1);
                if (msg.isGroupChatMessage()) {
//					GroupChatRoom g = mDb.fetchGroupChatRoom(msg.chatUserName);
                    MessageComposerActivity.launchToChatWithGroup(
                            SmsActivity.this,
                            MessageComposerActivity.class,
                            msg.chatUserName);
                } else {
                    //调用发送消息的方法
                    MessageComposerActivity.launchToChatWithBuddy(
                            SmsActivity.this,
                            MessageComposerActivity.class,
                            msg.chatUserName);
                }
            }
        });

        /**
         * listview 长按事件
         */
        lvSms.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int position, long arg3) {
                // footer_view
                if (position > usingList.size()) {
                    return true;
                }
                closeSoftKeyboard();
                // 添加了header_view
                final ChatMessage msg = usingList.get(position - 1);

                final BottomButtonBoard board = new BottomButtonBoard(SmsActivity.this, parentView);
                String[] texts = new String[]{
//                        getString(R.string.session_delete),
                        getString(R.string.session_delete_chat_target)
//                        getString(R.string.session_clear),
//                        getString(R.string.session_clear_chat_target)
                };
                int[] btnStyles = new int[]{
//                        BottomButtonBoard.BUTTON_BLUE,
                        BottomButtonBoard.BUTTON_BLUE
//                        BottomButtonBoard.BUTTON_RED,
//                        BottomButtonBoard.BUTTON_RED
                };
                OnClickListener[] listeners = new OnClickListener[texts.length];
//                listeners[0] = new OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        mDb.deleteChatMessageWithUser(msg.chatUserName);
//                        board.dismiss();
//                    }
//                };
                listeners[0] = new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDb.deleteLatestChatTarget(msg.chatUserName);
                        mDb.deleteChatMessageWithUser(msg.chatUserName);
                        board.dismiss();
                    }
                };
//                listeners[2] = new OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        mDb.deleteAllChatMessages();
//                        board.dismiss();
//                    }
//                };
//                listeners[3] = new OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        mDb.clearLatestChatTargets();
//                        board.dismiss();
//                    }
//                };
                board.add(texts, btnStyles, listeners).show();
                return true;
            }

        });
    }

    /**
     * 选中跳转返回值方法
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (REQ_PICK_BUDDYS == requestCode && RESULT_OK == resultCode) {
            boolean isGroupChat = data.getBooleanExtra("is_group_chat", true);
            if (isGroupChat) {
                String gid = data.getStringExtra("gid");
                if (!TextUtils.isEmpty(gid)) {
                    MessageComposerActivity.launchToChatWithGroup(
                            SmsActivity.this,
                            MessageComposerActivity.class,
                            gid);
                }
            } else {
                String buddyId = data.getStringExtra("buddy_id");
                if (!TextUtils.isEmpty(buddyId)) {
                    MessageComposerActivity.launchToChatWithBuddy(
                            this,
                            MessageComposerActivity.class,
                            buddyId);
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.field_clear:
                //清除搜索框
                etSearch.setText("");
                break;
            case R.id.cancel_btn:
            case R.id.search_glass_img:
                //取消搜索
                ibSearchContentClear.performClick();
                titleBar.setVisibility(View.VISIBLE);
                searchLayoutRightBtn.setVisibility(View.GONE);
                searchEmptyGlass.setVisibility(View.GONE);
                closeSoftKeyboard();
                break;
            case R.id.title_edit:
                //加号 跳转到其他页面
                Intent intent = new Intent();
                intent.setClass(SmsActivity.this, MultiSelectActivity.class);
                ThemeHelper.putExtraCurrThemeResId(intent, this);
                startActivityForResult(intent, REQ_PICK_BUDDYS);
                break;
            default:
                break;
        }
    }

    /**
     * 关闭软键盘
     */
    private void closeSoftKeyboard() {
        InputMethodManager mInputMethodManager;
        mInputMethodManager = (InputMethodManager) this
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        mInputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /**
     *
     */
    private IDBTableChangeListener mAccountSwitchObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            mDb = new Database(SmsActivity.this);
            myAdapter.resetDatabase(SmsActivity.this);
            loadDatas(++loadingId, LOAD_SWITCH_ACCOUNT);
        }
    };

    /**
     * 收到推送消息，会触发此observer;
     * 登录时获取WowtalkWebServerIF#getLatestChatTargets，会触发此observer；
     * 当前消息列表,{@link #getMoreFromServer(int, int)}时，不会触发此observer.
     */
    private IDBTableChangeListener mLatestChatTargetObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            mDb = new Database(SmsActivity.this);
            loadDatas(++loadingId, LOAD_NEW_OR_UPDATE);
        }
    };

    /**
     * 登录/后台切到前台时获取WowtalkWebServerIF#getLatestChatTargets，会触发此observer；
     * 收到推送消息，不会触发此observer;
     * 当前消息列表,{@link #getMoreFromServer(int, int)}时，不会触发此observer.
     */
    private IDBTableChangeListener mLatestChatTargetFinishedObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            mDb = new Database(SmsActivity.this);
            mHasMoreInServer = mPrefUtil.hasMoreLatestChatTargetInServer();
            loadDatas(++loadingId, LOAD_AFTER_FINISHED_DOWNLOAD);
        }
    };

    private IDBTableChangeListener mChatMessagesReadedObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            mDb = new Database(SmsActivity.this);
            loadDatas(++loadingId, LOAD_NEW_OR_UPDATE);
        }
    };

    private IDBTableChangeListener mNoticeManChangedObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            mDb = new Database(SmsActivity.this);
            loadDatas(++loadingId, LOAD_NEW_OR_UPDATE);
        }
    };

    /**
     * 临时会话的群组名称，在成员下载完成后，要根据成员重新计算
     */
//    private IDBTableChangeListener mTempGroupNameObserver = new IDBTableChangeListener() {
//        public void onDBTableChanged(String tableName) {
//            Log.d("SmsActivity#mTempGroupNameObserver, reload sms ui.");
//            AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Void>() {
//                @Override
//                protected Void doInBackground(Void... params) {
//                    if (null != myAdapter) {
////                        myAdapter.storeChatMessageDisplayName(log_msg);
//                        myAdapter.clearNameBuff();
//                    }
//                    return null;
//                }
//
//                protected void onPostExecute(Void result) {
//                    updateContentView(true);
//                };
//            });
//        }
//    };
    @Override
    protected void onResume() {
        super.onResume();
//        updateContentView(true);
        isResumed = true;
    }

    private void updateContentView(boolean withIndicator) {
        if (withIndicator) {
            findViewById(R.id.tv_no_chat_message_indicate).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.tv_no_chat_message_indicate).setVisibility(View.GONE);
        }
        Log.w("log msg size " + log_msg.size());
        if (log_msg == null || log_msg.size() == 0) {
            layoutBg.setVisibility(View.VISIBLE);
            setMsgItemListener(new ArrayList<ChatMessage>());
            myAdapter.setDataSource(log_msg);
            myAdapter.notifyDataSetChanged();
        } else {
            layoutBg.setVisibility(View.GONE);
            ArrayList<ChatMessage> filteredMsgList = log_msg;
            String filterString = etSearch.getText().toString().toLowerCase();
            if (!TextUtils.isEmpty(filterString)) {
                Log.w("filter with " + filterString);
                filteredMsgList = new ArrayList<ChatMessage>();
                for (ChatMessage aMessage : log_msg) {
                    String name = myAdapter.getChatMessageDisplayName(aMessage);
                    Log.w("filter match for " + name);
                    if (!TextUtils.isEmpty(name) && name.toLowerCase().contains(filterString)) {
                        filteredMsgList.add(aMessage);
                    }
                }
            }
            setMsgItemListener(filteredMsgList);
            Log.w("filtered msg count " + filteredMsgList.size());
            myAdapter.setDataSource(filteredMsgList);
            myAdapter.notifyDataSetChanged();
        }
    }

    private void setMoreText(boolean isLoadingMore) {
        mIsLoadingMore = isLoadingMore;
        mLoadMoreText.setText(isLoadingMore ? R.string.loading_more_in_progress : R.string.load_more);
        mLoadMoreText.setVisibility(mHasMoreInLocal ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Database.removeDBTableChangeListener(mAccountSwitchObserver);
        Database.removeDBTableChangeListener(mLatestChatTargetObserver);
        Database.removeDBTableChangeListener(mLatestChatTargetFinishedObserver);
        Database.removeDBTableChangeListener(mChatMessagesReadedObserver);
        Database.removeDBTableChangeListener(mNoticeManChangedObserver);
//        Database.removeDBTableChangeListener(chatmessageObserverSms);
//        Database.removeDBTableChangeListener(mTempGroupNameObserver);
        unregisterReceiver(mOutgoMsgReceiver);
    }

//	public static boolean isInstanciated() {
//		return instance != null;
//	}

    /**
     * 消息会话activity实例
     * @return
     */
    public static SmsActivity instance() {
        return instance;
    }

    /**
     *
     * @param title
     */
    public static void setNavTitle(final String title) {
        instance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtSmsTitle.setText(title);
            }
        });
    }

    /**
     * safe to be called from worker thread.
     */
//	private void refreshTableData() {
//		this.runOnUiThread(new Runnable(){
//			@Override
//			public void run() {
//                updateContentView();
//			}
//		});
//	}
    private long loadingId = 0;

    protected void loadDatas(final long curLoadingId, final int loadType) {
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, ArrayList<ChatMessage>>() {
            @Override
            protected ArrayList<ChatMessage> doInBackground(Void... contexts) {
                ArrayList<ChatMessage> cmList = null;
                if (curLoadingId == loadingId) {
                    cmList = getDatasFromDBOrServer(loadType);
                }
                return cmList;
            }

            @Override
            protected void onPostExecute(ArrayList<ChatMessage> cmList) {
                if (curLoadingId == loadingId) {
                    // 需要刷新tab_bar上的未读信息数，在StartActivity中
                    mDb.triggerUnreadCount();
                    log_msg.clear();
                    if (null != cmList) {
                        log_msg.addAll(cmList);
                    }
                    myAdapter.clearNameBuff();
                    updateContentView(true);
                    if (loadType == LOAD_INIT
                            || loadType == LOAD_AFTER_FINISHED_DOWNLOAD
                            || loadType == LOAD_MORE) {
                        setMoreText(false);
                    }
                }
            }
        });
    }

    /**
     * 从服务器获取更多数据
     * @param offset
     * @param count
     * @param isObserver
     */
    private void getMoreFromServer(int offset, int count, boolean isObserver) {
        mHasMoreInServer = true;
        mPrefUtil.setHasMoreLatestChatTargetInServer(true);
        int resulCode = mWebIF.getLatestChatTargets(offset, count, isObserver);
        if (resulCode == ErrorCode.OK) {
            mHasMoreInServer = mPrefUtil.hasMoreLatestChatTargetInServer();
        }
    }

    /**
     * 从本地数据库／服务器获取chat_targets
     *
     * @param loadType
     * @return
     */
    private ArrayList<ChatMessage> getDatasFromDBOrServer(int loadType) {
        Database dbHelper = new Database(this);
        ArrayList<ChatMessage> messages = new ArrayList<ChatMessage>();
        switch (loadType) {
            case LOAD_INIT:
                int initCount = LIMIT_COUNT_PER_PAGE;
                messages = dbHelper.getLatestedMessages(initCount);
                // 本地没有数据，则从服务器获取（为了兼容老版本，没有latest_chat_target表的问题）
                if (messages.isEmpty()) {
                    getMoreFromServer(0, LIMIT_COUNT_PER_PAGE, false);
                    messages = dbHelper.getLatestedMessages(initCount);
                }
                mHasMoreInLocal = !messages.isEmpty();
                sCurrentCount = messages.size();
                break;
            case LOAD_SWITCH_ACCOUNT:
            case LOAD_AFTER_FINISHED_DOWNLOAD:
                int count = LIMIT_COUNT_PER_PAGE;
                messages = dbHelper.getLatestedMessages(count);
                mHasMoreInLocal = !messages.isEmpty();
                sCurrentCount = messages.size();
                break;
            case LOAD_MORE:
                int newCount = sCurrentCount + LIMIT_COUNT_PER_PAGE;
                if (mHasMoreInServer) {
                    getMoreFromServer(sCurrentCount, LIMIT_COUNT_PER_PAGE, false);
                }
                messages = dbHelper.getLatestedMessages(newCount);
                mHasMoreInLocal = messages.size() > sCurrentCount;
                sCurrentCount = messages.size();
                break;
            case LOAD_NEW_OR_UPDATE:
                String earliestDate = "";
                if (null != log_msg && !log_msg.isEmpty()) {
                    earliestDate = log_msg.get(log_msg.size() - 1).sentDate;
                }
                if (TextUtils.isEmpty(earliestDate)) {
                    messages = dbHelper.getLatestedMessages(sCurrentCount + LIMIT_COUNT_PER_PAGE);
                } else {
                    messages = dbHelper.getLatestedMessages(earliestDate);
                }
                sCurrentCount = messages.size();
                break;
            default:
                break;
        }
        return messages;
    }

//	public void addChatMessage(ChatMessage msg) {
//		msg.fixMessageSenderDisplayName(
//				this, R.string.session_unknown_buddy, R.string.session_unknown_group);
//
//		for (int i = 0; i < log_msg.size(); i++) {
//			ChatMessage tmpMsg = log_msg.get(i);
//			if (tmpMsg.chatUserName.equals(msg.chatUserName)) {
//				log_msg.remove(i);
//				break;
//			}
//		}
//		log_msg.add(0, msg);
//		refreshTableData();
//	}

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_UP) {
            if (handleBackEvent()) {
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private boolean handleBackEvent() {
        if (View.GONE == titleBar.getVisibility()) {
            searchCancelBtn.performClick();
            return true;
        }

        return false;
    }
}
