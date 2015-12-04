package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.adapter.GroupTreeAdapter;
import co.onemeter.oneapp.contacts.model.ContactTreeNode;
import co.onemeter.oneapp.contacts.model.Person;
import co.onemeter.oneapp.utils.ThemeHelper;
import co.onemeter.utils.AsyncTaskExecutor;
import com.androidquery.AQuery;
import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;

import java.util.List;

/**
 * <p>显示“校园里”通讯录。</p>
 * Created by pzy on 11/22/14.
 */
public class SchoolMatesFragment extends Fragment
        implements BottomButtonBoard.OptionsMenuProvider, AdapterView.OnItemClickListener, IDBTableChangeListener {

    private static final int REQ_ADD_CLASS = 1;
    /** 上次从本地数据库加载组织架构的时间，(timestamp in ms)
     *
     * 数据有可能在我们监视时段之外发生了变化， 但是这里又不能总是刷新UI，
     * 因为刷新UI有副作用：树状视图的展开状态丢失了， 所以要维护这个时间戳
     * 以便判断是否的确有更新。
     * */
    private static long lastLoadSchool = 0;

//    Adapter adapter;
    GroupTreeAdapter adapter;
    AQuery aQuery;
    MessageBox msgbox;
    List<GroupChatRoom> schools;
    int errno;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_schoolmates, container, false);
        aQuery = new AQuery(v);
        aQuery.find(R.id.listview).itemClicked(this);

        msgbox = new MessageBox(getActivity());

        schools = new Database(getActivity()).fetchSchools();
        lastLoadSchool = System.currentTimeMillis();
        updateUi();

        // 如果本地没有数据，则刷新。但这种事只做一次，以防服务器也没有数据时，客户端反复刷新
        if (isEmpty()) {
            refresh();
        }

        return v;
    }

    public boolean handleBackPress() {
        return false;
    }

    public void gotoTop() {

    }

    public void refresh() {
        msgbox.showWait();

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
//                schools = WowTalkWebServerIF.getInstance(getActivity()).getMySchools(true);
//                return schools != null && !schools.isEmpty() ? ErrorCode.OK : ErrorCode.OPERATION_FAILED;
            	schools.clear();
            	errno =  WowTalkWebServerIF.getInstance(getActivity()).getMySchoolsErrno(true, schools);
            	return errno;
            }

            @Override
            public void onPostExecute(Integer errno) {
                msgbox.dismissWait();
                if (errno == ErrorCode.OK) {
                    new Database(getActivity()).storeSchools(schools);
                }
                else{
//                	schools = new Database(getActivity()).fetchSchools();
//                  lastLoadSchool = System.currentTimeMillis();
//                	if(schools != null && !schools.isEmpty()){
//                    	msgbox.toast(R.string.timeout_message);
//                	}else if(schools == null && schools.isEmpty()) {
//                		msgbox.toast(R.string.no_school_class);
//                	}
                	msgbox.toast(R.string.timeout_contacts_message, Toast.LENGTH_SHORT);
            	}
                updateUi();
            }
        });
    }

    private void updateUi() {
        if (!isEmpty()) {
            adapter = new GroupTreeAdapter(getActivity(), schools);
            aQuery.find(R.id.listview).adapter(adapter);
            aQuery.find(R.id.schoolmate_emptyview).visibility(View.GONE);
        } else {
            aQuery.find(R.id.schoolmate_emptyview).visibility(View.VISIBLE);
            schools.clear();
//            adapter.notifyDataSetChanged();
            adapter = new GroupTreeAdapter(getActivity(), schools);
            aQuery.find(R.id.listview).adapter(adapter);

        }
    }

    @Override
    public String[] getOptionsMenuItems(Context context) {
        return new String[] {
                context.getString(R.string.add),
                context.getString(R.string.refresh_from_server) };
    }

    @Override
    public int[] getOptionsMenuItemIcons(Context context) {
        return new int[] { R.drawable.nav_refresh_selector, R.drawable.nav_add_selector };
    }

    @Override
    public boolean onOptionsItemSelected(int position) {
        switch (position) {
            case 1: {
                Intent intent = new Intent(getActivity(), AddClassActivity.class);
                ThemeHelper.putExtraCurrThemeResId(intent, getActivity());
                startActivityForResult(intent, REQ_ADD_CLASS);
                return true;
            }
            case 0:
                refresh();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_ADD_CLASS && resultCode == Activity.RESULT_OK) {
            refresh();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Database.addDBTableChangeListener(Database.TBL_GROUP_MEMBER, this);

        if (Database.modified(Database.TBL_GROUP_MEMBER, lastLoadSchool)) {
            schools = new Database(getActivity()).fetchSchools();
            lastLoadSchool = System.currentTimeMillis();
            updateUi();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Database.removeDBTableChangeListener(this);
    }

    public boolean isEmpty() {
        return schools == null || schools.isEmpty();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        try {
            Context context = getActivity();
            if (context == null)
                return;

            Object item = adapterView.getItemAtPosition(i);
            if (item instanceof ContactTreeNode) {
                ContactTreeNode contact = (ContactTreeNode) item;
                if (!contact.isGroup()) {
                    String uid = contact.getGUID();
                    String myUid = PrefUtil.getInstance(context).getUid();
                    if (!TextUtils.equals(uid, myUid)) {
                        Buddy buddy = new Database(context).buddyWithUserID(uid);
                        ContactInfoActivity.launch(context, Person.fromBuddy(buddy),
                                buddy == null ? 0 : buddy.getFriendShipWithMe(),true);
                    } else {
                        startActivity(new Intent(context, MyInfoActivity.class));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDBTableChanged(String tableName) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    schools = new Database(getActivity()).fetchSchools();
                    lastLoadSchool = System.currentTimeMillis();
                    updateUi();
                }
            });
        }
    }
}