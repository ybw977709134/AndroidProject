package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;
import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.bitmapfun.util.ImageCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * For picking a temp group.
 */
public class PickTempGroupActivity extends Activity {

    private class GroupAdapter extends BaseAdapter {

        private Context mContext;
        private HashMap<String, String> mBufferedNameMap = new HashMap<String, String>();
        private HashSet<String> mDownloadingGroups = new HashSet<String>();

        public GroupAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            return groupChats.size();
        }

        @Override
        public Object getItem(int position) {
            return groupChats.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder tempHolder = null;
            final GroupChatRoom group = groupChats.get(position);
            if (convertView == null) {
                tempHolder = new ViewHolder();
                convertView = LayoutInflater.from(PickTempGroupActivity.this).inflate(R.layout.listitem_groupchat, null);
                tempHolder.txtName = (TextView) convertView.findViewById(R.id.txt_name);
                tempHolder.imageView = (ImageView)convertView.findViewById(R.id.img_photo);
                convertView.setTag(tempHolder);
            } else {
                tempHolder = (ViewHolder) convertView.getTag();
            }
            final ViewHolder holder = tempHolder;

            mPhotoDisplayHelper.setLoadingImage(R.drawable.default_group_avatar_90);
            if (mBufferedNameMap.containsKey(group.groupID)) {
                holder.txtName.setText(mBufferedNameMap.get(group.groupID));
                mPhotoDisplayHelper.loadImage(group, holder.imageView);
            } else {
                if (group.isGroupNameChanged) {
                    holder.txtName.setText(group.groupNameOriginal);
                    saveNameBuff(group.groupID, group.groupNameOriginal);
                } else {
                    holder.txtName.setText(R.string.group_chat_title_default);
                }

                ArrayList<GroupMember> groupMembers = new Database(mContext).fetchGroupMembers(group.groupID);
                if (null == groupMembers || groupMembers.isEmpty()) {
                    if (!mDownloadingGroups.contains(group.groupID)) {
                        mDownloadingGroups.add(group.groupID);
                        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Map<String, Object>>() {
                            @Override
                            protected Map<String, Object> doInBackground(Void... params) {
                                Map<String, Object> resultMap = WowTalkWebServerIF.getInstance(mContext)
                                        .fGroupChat_GetMembers(group.groupID);
                                return resultMap;
                            }

                            protected void onPostExecute(Map<String, Object> resultMap) {
                                int resultCode = (Integer) resultMap.get("code");
                                Log.d("PickTempGroupActivity#download members of temp group "
                                        + group.groupID + ", resultCode is " + resultCode);
                                if (ErrorCode.OK == resultCode) {
                                    ArrayList<GroupMember> members = (ArrayList<GroupMember>) resultMap.get("data");
                                    // 未自定义名字的群组需要用成员名拼接群组名
                                    if (!members.isEmpty()) {
                                        if (!group.isGroupNameChanged) {
                                            String myUid = PrefUtil.getInstance(mContext).getUid();
                                            String myNickname = PrefUtil.getInstance(mContext).getMyNickName();
                                            StringBuffer displayName = new StringBuffer();
                                            for (GroupMember member : members) {
                                                if (!TextUtils.isEmpty(myUid) && myUid.equals(member.userID)) {
                                                    continue;
                                                }
                                                displayName.append(TextUtils.isEmpty(member.alias)
                                                        ? member.nickName : member.alias);
                                                displayName.append(" , ");
                                            }
                                            displayName.append(myNickname);
                                            holder.txtName.setText(displayName);
                                            saveNameBuff(group.groupID, displayName.toString());
                                        }
                                        mPhotoDisplayHelper.loadImage(group, holder.imageView);
                                    }
                                }
                                mDownloadingGroups.remove(group.groupID);
                            }

                            ;
                        });
                    }
                } else {
                    if (!group.isGroupNameChanged) {
                        String myUid = PrefUtil.getInstance(mContext).getUid();
                        String myNickname = PrefUtil.getInstance(mContext).getMyNickName();
                        StringBuffer displayName = new StringBuffer();
                        for(GroupMember member : groupMembers) {
                            if (!TextUtils.isEmpty(myUid) && myUid.equals(member.userID)) {
                                continue;
                            }
                            displayName.append(TextUtils.isEmpty(member.alias)
                                    ? member.nickName : member.alias);
                            displayName.append(" , ");
                        }
                        displayName.append(myNickname);
                        holder.txtName.setText(displayName);
                        saveNameBuff(group.groupID, displayName.toString());
                    }
                    mPhotoDisplayHelper.loadImage(group, holder.imageView);
                }
            }

            return convertView;
        }

        private void saveNameBuff(String groupID, String groupNameOriginal) {
            mBufferedNameMap.put(groupID, groupNameOriginal);
        }

        private class ViewHolder {
            TextView txtName;
            ImageView imageView;
        }
    }

    private static PickTempGroupActivity instance;
	private ImageButton btnTitleBack;
	private TextView mNoneTempGroupText;
	private ListView lvGroupChat;
	private Database dbHelper;
	private GroupAdapter groupAdapter;
	private ArrayList<GroupChatRoom> groupChats = new ArrayList<GroupChatRoom>();
    private PhotoDisplayHelper mPhotoDisplayHelper;
    private WowTalkWebServerIF mWeb;
    private MessageBox mMsgBox;
    private HashMap<String, PhotoDisplayHelper.Params> mPhotoDisplayParams
            = new HashMap<String, PhotoDisplayHelper.Params>();

    public static PickTempGroupActivity instance() {
        if (instance == null)
            return new PickTempGroupActivity();
        return instance;
    }

    private IDBTableChangeListener mFinishLoadMembersObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("PickTempGroupActivity, load group members finished, trigger the mFinishLoadMembersObserver.");
                    mMsgBox.dismissWait();

                    getTemporaryGroupFromLocal();
                    if (null == groupAdapter) {
                        groupAdapter = new GroupAdapter(PickTempGroupActivity.this);
                        lvGroupChat.setAdapter(groupAdapter);
                    } else {
                        groupAdapter.notifyDataSetChanged();
                    }
                    if (groupChats.isEmpty()){
                        mNoneTempGroupText.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    };

	private void initView() {
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		lvGroupChat = (ListView) findViewById(R.id.list_group);
		mNoneTempGroupText = (TextView) findViewById(R.id.groupchat_notexist);
		btnTitleBack.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
			
		});
		lvGroupChat.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
				MessageComposerActivity.launchToChatWithGroup(
						PickTempGroupActivity.this,
						MessageComposerActivity.class,
						groupChats.get(position));
			}
			
		});
	}
	
	
	private void getTemporaryGroupFromLocal() {
		groupChats = dbHelper.fetchTempGroupChatRooms();
	}

	/**
	 * 是否需要每次进入此页面都要从服务器下载所有群组，暂时先不这么做
	 */
	private void getTemporaryGroupFromServer() {
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                int errno = mWeb.fGroupChat_GetMyGroups();
                PrefUtil.getInstance(PickTempGroupActivity.this).setLocalGroupListLastModified();
                return errno;
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result == ErrorCode.OK) {
                    getTemporaryGroupFromLocal();
                    groupAdapter = new GroupAdapter(PickTempGroupActivity.this);
                    lvGroupChat.setAdapter(groupAdapter);
                    // whether there are groups.
                    if (groupChats.isEmpty()) {
                        mNoneTempGroupText.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        instance = this;
		setContentView(R.layout.groupchat_list);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mWeb = WowTalkWebServerIF.getInstance(PickTempGroupActivity.this);
        mMsgBox = new MessageBox(PickTempGroupActivity.this);
		dbHelper = new Database(this);
        mPhotoDisplayHelper = new PhotoDisplayHelper(this);
        mPhotoDisplayHelper.addImageCache(new ImageCache.ImageCacheParams(this, "thumb"));
		initView();
	}

    @Override
    protected void onResume() {
        super.onResume();

        getTemporaryGroupFromLocal();
        if (null == groupAdapter) {
            groupAdapter = new GroupAdapter(PickTempGroupActivity.this);
            lvGroupChat.setAdapter(groupAdapter);
//                getTemporaryGroupFromServer();
        } else {
            groupAdapter.notifyDataSetChanged();
        }
        if (groupChats.isEmpty()){
            mNoneTempGroupText.setVisibility(View.VISIBLE);
        }

        if (!PrefUtil.getInstance(PickTempGroupActivity.this).isGroupMembersUptodatePerfectly()) {
            mMsgBox.showWait();
        }

        Database.addDBTableChangeListener(Database.DUMMY_TBL_FINISH_LOAD_MEMBERS,mFinishLoadMembersObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Database.removeDBTableChangeListener(mFinishLoadMembersObserver);
    }
}
