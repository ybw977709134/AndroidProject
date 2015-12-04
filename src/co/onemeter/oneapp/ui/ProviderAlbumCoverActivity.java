package co.onemeter.oneapp.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;


public class ProviderAlbumCoverActivity extends Activity {

    public static final String PROVIDER_COVER_FILE_PATH = "provider_cover_file_path";
    private static final int HANDLER_COPY_OVER = 1;

    private MessageBox mMsgBox;

    private ListView mCoverListView;
    private ProviderAlbumCoverAdapter mCoverAdapter;

    private ArrayList<Integer> mCoverResIds;

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case HANDLER_COPY_OVER:
                mMsgBox.dismissWait();
                Intent dataIntent = new Intent();
                dataIntent.putExtra(PROVIDER_COVER_FILE_PATH, String.valueOf(msg.obj));
                setResult(RESULT_OK, dataIntent);
                finish();
                break;
            default:
                break;
            }
        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_albumcover);

        mMsgBox = new MessageBox(this);

        initView();
    }

    private void initView() {
        mCoverListView = (ListView) findViewById(R.id.provider_cover_listview);
        mCoverAdapter = new ProviderAlbumCoverAdapter(this);
        mCoverListView.setAdapter(mCoverAdapter);

        mCoverListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                mMsgBox.showWait();
                copyResDrawable(mCoverResIds.get(position));
            }
        });
        findViewById(R.id.title_back).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppStatusService.setIsMonitoring(true);
    }

    private void copyResDrawable(final int resId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String filePath = "";
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
                File tempFile = new File(Environment.getExternalStorageDirectory(), String.valueOf(System.currentTimeMillis()));
                FileOutputStream  fos = null;
                boolean isSuccess = false;
                try {
                    fos = new FileOutputStream(tempFile);//写入的文件路径
                    bitmap.compress(CompressFormat.PNG, 100, fos);
                    fos.flush();
                    fos.close();
                    isSuccess = true;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
                if (isSuccess) {
                    filePath = tempFile.getAbsolutePath();
                } else {
                    tempFile.delete();
                }
                Message msg = mHandler.obtainMessage(HANDLER_COPY_OVER);
                msg.obj = filePath;
                mHandler.sendMessage(msg);
            }
        }).start();
    }

    private class ProviderAlbumCoverAdapter extends BaseAdapter {
        private Context mContext;

        public ProviderAlbumCoverAdapter(Context context) {
            mContext = context;
            mCoverResIds = new ArrayList<Integer>();
            mCoverResIds.add(R.drawable.cover_01);
            mCoverResIds.add(R.drawable.cover_02);
            mCoverResIds.add(R.drawable.cover_03);
            mCoverResIds.add(R.drawable.cover_04);
            mCoverResIds.add(R.drawable.cover_05);
            mCoverResIds.add(R.drawable.cover_06);
            mCoverResIds.add(R.drawable.cover_07);
        }

        @Override
        public int getCount() {
            return mCoverResIds.size();
        }

        @Override
        public Object getItem(int position) {
            return mCoverResIds.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (null == convertView) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_provider_cover, null);
                holder.imageView = (ImageView) convertView.findViewById(R.id.image_view);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final int resId = mCoverResIds.get(position);
            PhotoDisplayHelper.displayPhoto(mContext, holder.imageView, getResources().getDrawable(resId), null, false);

            return convertView;
        }

    }

    static class ViewHolder {
        ImageView imageView;
    }

}
