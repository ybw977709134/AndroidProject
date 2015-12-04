package co.onemeter.oneapp.adapter;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.Log;
import co.onemeter.oneapp.ui.PhotoDisplayHelper;
import co.onemeter.utils.AsyncTaskExecutor;
import org.wowtalk.api.*;
import org.wowtalk.ui.bitmapfun.util.ImageCache;

import java.text.SimpleDateFormat;
import java.util.*;

public class MessagesAdapter extends BaseAdapter {
	private Date today;
	private Date sevenDaysAgo;
	private int mBlockColor;
    protected ArrayList<ChatMessage> log_msg;
	LayoutInflater mInflater;
	private Context mContext;
	protected Database mDbHelper;
    private PhotoDisplayHelper mPhotoDisplayHelper;
    private HashMap<String, String> chatMessageDisplayNameBuf = new HashMap<String, String>();
    protected HashSet<String> mDownloadingTargets = new HashSet<String>();

	public MessagesAdapter(Context aContext, ArrayList<ChatMessage> data) {
		today = new Date();
		today.setHours(0);
		today.setMinutes(0);
		today.setSeconds(0);
		log_msg = data;
		mContext = aContext;
		mDbHelper = Database.open(mContext);
		mBlockColor = aContext.getResources().getColor(R.color.blue);
        mInflater = (LayoutInflater)aContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPhotoDisplayHelper = new PhotoDisplayHelper(mContext);
        mPhotoDisplayHelper.addImageCache(new ImageCache.ImageCacheParams(mContext, "thumb"));

		long DAY_IN_MS = 1000 * 60 * 60 * 24;
		sevenDaysAgo = new Date(today.getTime() - (7 * DAY_IN_MS));
	}

    public void resetDatabase(Context context) {
        mDbHelper = new Database(context);
    }

    public String getChatMessageDisplayName(ChatMessage chatMessage) {
        if(chatMessageDisplayNameBuf.containsKey(chatMessage.chatUserName)) {
            return chatMessageDisplayNameBuf.get(chatMessage.chatUserName);
        } else {
            return chatMessage.displayName;
        }
    }

    public void setDataSource(ArrayList<ChatMessage> data) {
        if(null == data) {
            data=new ArrayList<ChatMessage>();
        }

        log_msg=data;
    }

	public void setBlockColor(int value) {
		mBlockColor = value;
	}
	
	public int getCount() {
		return log_msg.size();
	}

	public Object getItem(int position) {
		return log_msg.get(position);
	}

	public long getItemId(int position) {

		return position;
	}

	/**
	 * clear the buff name of temp chat room.
	 */
    public void clearNameBuff() {
        if (null != chatMessageDisplayNameBuf) {
            chatMessageDisplayNameBuf.clear();
        }
    }

    private void saveNameBuff(String key, String value, TextView textView) {
        if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
            return;
        }
        chatMessageDisplayNameBuf.put(key, value);
        if(value.contains(",")){
        	textView.setText(R.string.group_chat_title_default);
        }else{
        	textView.setText(value);
        }
        
    }

    protected void fixBuddyDisplay(final ChatMessage msg, final TextView textView, final ImageView photoImageView) {
//        if (msg.displayName != null && !msg.displayName.equals("")) {
//            saveNameBuff(msg.chatUserName, msg.displayName, textView);
//            return;
//        }
        if (chatMessageDisplayNameBuf.containsKey(msg.chatUserName)) {
            textView.setText(chatMessageDisplayNameBuf.get(msg.chatUserName));
            return;
        }
        final Database db = new Database(mContext);
        Buddy buddy = db.buddyWithUserID(msg.chatUserName);
        if (buddy != null && !TextUtils.isEmpty(buddy.alias)) {
            saveNameBuff(msg.chatUserName, buddy.alias, textView);
        } else if (buddy != null && TextUtils.isEmpty(buddy.alias) && !TextUtils.isEmpty(buddy.nickName)) {
        	saveNameBuff(msg.chatUserName, buddy.nickName, textView);
        } else {
            if (!mDownloadingTargets.contains(msg.chatUserName)) {
                mDownloadingTargets.add(msg.chatUserName);
                AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
                    @Override
                    protected Integer doInBackground(Void... params) {
                        return WowTalkWebServerIF.getInstance(mContext)
                                .fGetBuddyWithUID(msg.chatUserName);
                    }

                    protected void onPostExecute(Integer result) {
                        if (result == ErrorCode.OK) {
                            Buddy buddy = db.buddyWithUserID(msg.chatUserName);
                            if (null != buddy) {
                            
                                saveNameBuff(msg.chatUserName, TextUtils.isEmpty(buddy.alias)?buddy.nickName:buddy.alias, textView);
                                // 重新加载头像
                                IHasPhoto entity = buddy;
                                // 公司发通知时，发起人采用的是buddy，此buddy的 username 格式为"公司id_公司id"
                                String companyId = PrefUtil.getInstance(mContext).getCompanyId();
                                if ((companyId + "_" + companyId).equals(buddy.username)) {
                                    // 公司默认头像暂时使用群组默认头像
                                    PhotoDisplayHelper.displayPhoto(mContext, photoImageView,
                                            R.drawable.default_group_avatar_90, entity, true);
                                } else {
                                    PhotoDisplayHelper.displayPhoto(mContext, photoImageView,
                                            R.drawable.default_avatar_90, entity, true);
                                }
                            }
                        }
                        mDownloadingTargets.remove(msg.chatUserName);
                    }

                    ;
                });
            }
        }
    }

    private void fixNamedTmpGroupChatDisplay(final ChatMessage message, final TextView textView, final ImageView photoImageView) {
        final GroupChatRoom g = mDbHelper.fetchGroupChatRoom(message.chatUserName);

        if(null != g && g.isTemporaryGroup && g.isGroupNameChanged) {
            saveNameBuff(message.chatUserName, g.groupNameOriginal, textView);
            ArrayList<GroupMember> groupMembers = mDbHelper.fetchGroupMembers(message.chatUserName);
            // download members, and refresh the head portrait.
            if(groupMembers.isEmpty()) {
                if (!mDownloadingTargets.contains(g.groupID)) {
                    mDownloadingTargets.add(g.groupID);
                    AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer> () {
                        @Override
                        protected Integer doInBackground(Void... params) {
                            Map<String, Object> resultMap = WowTalkWebServerIF.getInstance(mContext)
                                    .fGroupChat_GetMembers(g.groupID);
                            return (Integer)resultMap.get("code");
                        }

                        @Override
                        protected void onPostExecute(Integer result) {
                            if (ErrorCode.OK == result) {
                                mPhotoDisplayHelper.loadImage(g, photoImageView);
                            }
                            mDownloadingTargets.remove(g.groupID);
                        }
                    });
                }
            }
        }
    }
    private void fixUnNamedTmpGroupChatDisplay(final ChatMessage message, final TextView textView, final ImageView photoImageView) {
        final GroupChatRoom g = mDbHelper.fetchGroupChatRoom(message.chatUserName);

        if(null != g && g.isTemporaryGroup && !g.isGroupNameChanged) {
            ArrayList<GroupMember> groupMembers = mDbHelper.fetchGroupMembers(message.chatUserName);
            Log.d("temp group chat in sms_list, the size is " + groupMembers.size() + ", groupId is " + message.chatUserName);
            PrefUtil prefUtil = PrefUtil.getInstance(mContext);
            final String myUid = prefUtil.getUid();
            final String myNickname = prefUtil.getMyNickName();
            if(groupMembers.isEmpty()) {
                if (null != textView) {
                    textView.setText(R.string.group_chat_title_default);
                }
                if (!mDownloadingTargets.contains(g.groupID)) {
                    mDownloadingTargets.add(g.groupID);
                    AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, String> () {
                        @Override
                        protected String doInBackground(Void... params) {
                            Map<String, Object> resultMap = WowTalkWebServerIF.getInstance(mContext)
                                    .fGroupChat_GetMembers(g.groupID);
                            StringBuffer displayName = new StringBuffer();
                            if (ErrorCode.OK == (Integer)resultMap.get("code")) {
                                ArrayList<GroupMember> members = (ArrayList<GroupMember>) resultMap.get("data");
                                if (!members.isEmpty()) {
                                    for(GroupMember member : members) {
                                        if (!TextUtils.isEmpty(myUid) && myUid.equals(member.userID)) {
                                            continue;
                                        }
                                        displayName.append(TextUtils.isEmpty(member.alias)
                                                ? member.nickName : member.alias);
                                        displayName.append(" , ");
                                    }
                                    displayName.append(myNickname);
                                }
                            }
                            return displayName.toString();
                        }

                        @Override
                        protected void onPostExecute(String displayName) {
                            super.onPostExecute(displayName);
                            saveNameBuff(message.chatUserName, displayName, textView);
                            mDownloadingTargets.remove(g.groupID);
                            mPhotoDisplayHelper.loadImage(g, photoImageView);
                        }
                    });
                }
            } else {
                StringBuffer bufferName = new StringBuffer();
                for(int i=0; i<groupMembers.size(); ++i) {
                    GroupMember aMember=groupMembers.get(i);
                    if (!TextUtils.isEmpty(myUid) && myUid.equals(aMember.userID)) {
                        continue;
                    }
                    bufferName.append(TextUtils.isEmpty(aMember.alias) ? aMember.nickName : aMember.alias);
                    bufferName.append(" , ");
                }
                bufferName.append(myNickname);
                saveNameBuff(message.chatUserName, bufferName.toString(), textView);
            }
        } else {
            saveNameBuff(message.chatUserName, message.displayName, textView);
        }
    }

    protected void fixGroupChatDisplayName(GroupChatRoom groupRoom,
            final ChatMessage msg, final TextView textView) {
        if (chatMessageDisplayNameBuf.containsKey(msg.chatUserName)) {
            textView.setText(chatMessageDisplayNameBuf.get(msg.chatUserName));
            return;
        }

        if (null != groupRoom && !groupRoom.isEditable
                && TextUtils.isEmpty(groupRoom.parentGroupId)) {
            saveNameBuff(msg.chatUserName,
                    mContext.getString(R.string.contactsforbiz_root_group_name_display),
                    textView);
            return;
        }
//        if (!TextUtils.isEmpty(msg.displayName)) {
//            saveNameBuff(msg.chatUserName, msg.displayName, textView);
//            return;
//        }

        final Database db = new Database(mContext);
        GroupChatRoom group = db.fetchGroupChatRoom(msg.chatUserName);
        if (group != null) {
            if (!group.isEditable && TextUtils.isEmpty(group.parentGroupId)) {
                saveNameBuff(msg.chatUserName,
                        mContext.getString(R.string.contactsforbiz_root_group_name_display),
                        textView);
            } else {
                saveNameBuff(msg.chatUserName, group.groupNameOriginal, textView);
            }
        } else {
            if (!mDownloadingTargets.contains(msg.chatUserName)) {
                mDownloadingTargets.add(msg.chatUserName);
                AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
                    @Override
                    protected Integer doInBackground(Void... params) {
                        return WowTalkWebServerIF.getInstance(mContext)
                                .fGroupChat_GetGroupDetail(msg.chatUserName);
                    }

                    protected void onPostExecute(Integer result) {
                        if (result == ErrorCode.OK) {
                            GroupChatRoom room = db.fetchGroupChatRoom(msg.chatUserName);
                            if (null != room) {
                                if (!room.isEditable && TextUtils.isEmpty(room.parentGroupId)) {
                                    saveNameBuff(msg.chatUserName,
                                            mContext.getString(R.string.contactsforbiz_root_group_name_display),
                                            textView);
                                } else {
                                    saveNameBuff(msg.chatUserName, room.groupNameOriginal, textView);
                                }
                            }
                        }
                        mDownloadingTargets.remove(msg.chatUserName);
                    };
                });
            }
        }
    }

	public View getView(int position, View convertView, ViewGroup parent) {

		View lView = null;
		if (convertView != null) {
			lView = convertView;
		} else {
			lView = mInflater.inflate(R.layout.message_cell, parent, false);
		}

		if (log_msg == null || log_msg.size() == 0)
			return lView;

		ImageView imgCheckBox = (ImageView) lView
				.findViewById(R.id.listItem_checkbox);

		final TextView txtContact = (TextView) lView
				.findViewById(R.id.message_cell_contact);
		TextView txtContent = (TextView) lView
				.findViewById(R.id.message_cell_content);
		TextView txtDate = (TextView) lView
				.findViewById(R.id.message_cell_date);
		TextView txtUnreadMsg = (TextView) lView
				.findViewById(R.id.unreadMsg);

		final ImageView photoImageView = (ImageView) lView
				.findViewById(R.id.contactPhoto);
		ImageView imageView_tag_tea = (ImageView) lView
				.findViewById(R.id.imageView_tag_tea);

		final ChatMessage message = log_msg.get(position);

		if (message.unreadCount > 0) {
			txtUnreadMsg.setVisibility(View.VISIBLE);
			txtUnreadMsg.setText(String.valueOf(message.unreadCount));
		} else {
			txtUnreadMsg.setVisibility(View.GONE);
			txtUnreadMsg.setText("");
		}

        photoImageView.setBackgroundDrawable(null);
        if(message.isGroupChatMessage) {
            final GroupChatRoom g = mDbHelper.fetchGroupChatRoom(message.chatUserName);
            if (null != g) {
                showGroupChatMessages(photoImageView, txtContact, message, g);
            } else {
                txtContact.setText(R.string.group_chat_title_default);
                photoImageView.setImageResource(R.drawable.default_group_avatar_90);
                // 此群组在本地不存在，需要下载
                if (!mDownloadingTargets.contains(message.chatUserName)) {
                    mDownloadingTargets.add(message.chatUserName);
                    AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
                        @Override
                        protected Integer doInBackground(Void... params) {
                            return WowTalkWebServerIF.getInstance(mContext)
                                    .fGroupChat_GetGroupDetail(message.chatUserName);
                        }

                        protected void onPostExecute(Integer result) {
                            mDownloadingTargets.remove(message.chatUserName);
                            if (ErrorCode.OK == result) {
                                GroupChatRoom groupChatRoom = mDbHelper.fetchGroupChatRoom(message.chatUserName);
                                showGroupChatMessages(photoImageView, txtContact, message, groupChatRoom);
                            }
                        };
                    });
                }
            }
            imageView_tag_tea.setVisibility(View.GONE);

        } else {
            fixBuddyDisplay(message, txtContact, photoImageView);
            Buddy buddy = mDbHelper.buddyWithUserID(message.chatUserName);
            IHasPhoto entity = buddy;
            if(entity != null) {
                // 公司发通知时，发起人采用的是buddy，此buddy的 username 格式为"公司id_公司id"
                String companyId = PrefUtil.getInstance(mContext).getCompanyId();
                if ((companyId + "_" + companyId).equals(buddy.username)) {
                    // 公司默认头像暂时使用群组默认头像
                    PhotoDisplayHelper.displayPhoto(mContext, photoImageView,
                            R.drawable.default_group_avatar_90, entity, true);
                } else {
                    PhotoDisplayHelper.displayPhoto(mContext, photoImageView,
                            R.drawable.default_avatar_90, entity, true);
                }
            }
            else {
                photoImageView.setImageResource(R.drawable.default_avatar_90);
            } 
            //判断消息列表的帐号头像是否为老师
            if (buddy != null && buddy.getAccountType() == 2) {
            	imageView_tag_tea.setVisibility(View.VISIBLE);
            } else {
            	imageView_tag_tea.setVisibility(View.GONE);
            }
        }

		Date dateMsgSentDate = Database
				.chatMessage_UTCStringToDate(message.sentDate);
		String strDate = "";

		if (null != dateMsgSentDate) {
            if (dateMsgSentDate.after(today)) {
                strDate = String.format("%02d:%02d",
                        dateMsgSentDate.getHours(),
                        dateMsgSentDate.getMinutes());
            } else {
                if (dateMsgSentDate.after(sevenDaysAgo)) {
                    strDate = DateUtils.formatDateTime(mContext,
                            dateMsgSentDate.getTime(),
                            DateUtils.FORMAT_SHOW_WEEKDAY);
                    strDate += mContext.getString(R.string.weekday_name);
                } else {
                    strDate = String.format("%d/%d/%d",
                            dateMsgSentDate.getYear() + 1900,
                            dateMsgSentDate.getMonth() + 1,
                            dateMsgSentDate.getDate());
                }

            }
        }
        txtDate.setText(strDate);
//        txtDate.setTextColor(mBlockColor);

        if (TextUtils.isEmpty(message.msgType)) {
            // set content empty on UI.
            txtContent.setText("");
        } else if (message.msgType.equals(ChatMessage.MSGTYPE_NORMAL_TXT_MESSAGE)) {
			txtContent.setText(message.messageContent);
		} 
		else if (message.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_PHOTO)) {
			txtContent.setText(R.string.apns_photo);
		} 
		else if (message.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_VIDEO_NOTE)) {
			txtContent.setText(R.string.apns_video);
		} 
		else if (message.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_VOICE_NOTE)) {
			txtContent.setText(R.string.apns_voice);
		}
        else if (message.msgType.equals(ChatMessage.MSGTYPE_PIC_VOICE)) {
            txtContent.setText(R.string.apns_hybird);
        }
        else if (message.msgType.equals(ChatMessage.MSGTYPE_LOCATION)) {
            txtContent.setText(R.string.apns_loc);
        }
		else if (message.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_STAMP)) {
			txtContent.setText(R.string.apns_stamp);
		} else if (message.msgType.equals(ChatMessage.MSGTYPE_GROUPCHAT_SOMEONE_JOIN_ROOM)){
			txtContent.setText(R.string.msg_someone_join_group);
		} else if (message.msgType.equals(ChatMessage.MSGTYPE_GROUPCHAT_SOMEONE_QUIT_ROOM)){
			txtContent.setText(R.string.msg_someone_leave_group);
		} else if (message.msgType.equals(ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST)){
			txtContent.setText(R.string.msg_someone_request_group);
		} else if (message.msgType.equals(ChatMessage.MSGTYPE_CALL_LOG) || message.msgType.equals(ChatMessage.MSGTYPE_GET_MISSED_CALL) || message.msgType.equals(ChatMessage.MSGTYPE_NORMAL_CALL_REJECTED)) {
            String sentDateString = "";
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getDefault());
            Date sentDate = Database.chatMessage_UTCStringToDate(message.sentDate);
            if (null != sentDate) {
                sentDateString = sdf.format(sentDate);
            }
            txtContent.setText(String.format(mContext.getResources().getString(R.string.sms_message_call), sentDateString));
        } else if (message.msgType.equals(ChatMessage.MSGTYPE_THIRDPARTY_MSG)) {
            String uid = message.getUidAsBuddyRequest();
            if (uid != null) {
                txtContent.setText(R.string.sms_say_hello_to_you);
            } else {
                txtContent.setText(R.string.newer_chatmessage_receive);
            }
		} else if (message.msgType.equals(ChatMessage.MSGTYPE_SYSTEM_PROMPT)) {
		    txtContent.setText(message.messageContent);
        }else {
			txtContent.setText(R.string.newer_chatmessage_receive);
		}

		return lView;
	}

    protected void showGroupChatMessages(ImageView photoImageView, TextView txtContact, ChatMessage message, GroupChatRoom g) {
        //if it is a temp group, set contact as all members' name if the group_name hasn't been set;
        // otherwise, set as the display_name
        if(null != g && g.isTemporaryGroup) {
            if(chatMessageDisplayNameBuf.containsKey(message.chatUserName)) {
                //tmp group get display name too slow
                txtContact.setText(chatMessageDisplayNameBuf.get(message.chatUserName));
            } else if (g.isGroupNameChanged) {
                fixNamedTmpGroupChatDisplay(message, txtContact, photoImageView);
            } else {
                fixUnNamedTmpGroupChatDisplay(message,txtContact, photoImageView);
            }
        } else {
            fixGroupChatDisplayName(g, message, txtContact);
        }

        if (g != null) {
            if (g.isTemporaryGroup) {
                mPhotoDisplayHelper.setLoadingImage(R.drawable.default_group_avatar_90);
                // add the border
                photoImageView.setScaleType(ScaleType.CENTER_CROP);
                mPhotoDisplayHelper.loadImage(g, photoImageView);
            } else {
                PhotoDisplayHelper.displayPhoto(mContext, photoImageView,
                        R.drawable.default_group_avatar_90, g, true);
            }
        } else {
            photoImageView.setImageResource(R.drawable.default_group_avatar_90);
        }
    }
}