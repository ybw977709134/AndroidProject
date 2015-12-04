package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.GlobalSetting;
import org.wowtalk.api.NetworkIFDelegate;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MediaInputHelper;
import org.wowtalk.ui.MessageBox;

import java.io.File;

/**
 *
 */
public class AlbumCoverChangeActivity extends Activity implements OnClickListener {

    private static final int REQ_INPUT_ALBUMCOVER = 101;
    private static final int REQ_INPUT_ALBUMCOVER_PROVIDER = 102;

    private static final int ALBUMCOVER_HEIGHT = 800;
    private static final int ALBUMCOVER_WIDTH = 800;

    /**
     * 上传成功后，在 Activity result 中输出 file id.
     */
    public static final String EXTRA_FILE_ID = "fileid";

    private static MediaInputHelper sMediaInput;
    private WowTalkWebServerIF mWebIF;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_albumcover_change);
        mWebIF = WowTalkWebServerIF.getInstance(this);
        initView();
    }

    private void initView() {
        findViewById(R.id.pick_from_local).setOnClickListener(this);
        findViewById(R.id.take_photo).setOnClickListener(this);
        findViewById(R.id.select_provider_cover).setOnClickListener(this);
        findViewById(R.id.title_back).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppStatusService.setIsMonitoring(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK
                && sMediaInput != null) {
            String[] path = new String[2];
            // 从提供的7张cover中选择
            if (requestCode == REQ_INPUT_ALBUMCOVER_PROVIDER) {
                String filePath = data.getStringExtra(ProviderAlbumCoverActivity.PROVIDER_COVER_FILE_PATH);
                if (!TextUtils.isEmpty(filePath)) {
                    boolean isHandled = sMediaInput.handleImageResult(
                            AlbumCoverChangeActivity.this, filePath,
                            ALBUMCOVER_WIDTH, ALBUMCOVER_HEIGHT, 0, 0, path);
                    new File(filePath).delete();
                    if (isHandled) {
                        uploadAlbumCoverAsync(path[0]);
                    }
                }
            } else if (requestCode == REQ_INPUT_ALBUMCOVER) {
                if (sMediaInput.handleImageResult(this, data,
                        ALBUMCOVER_HEIGHT, ALBUMCOVER_WIDTH, 0, 0, path)) {
                    uploadAlbumCoverAsync(path[0]);
                }
            }
        }
    }

    private void uploadAlbumCoverAsync(String filepath) {
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<String, Integer, Void>() {
            private MessageBox msgbox = new MessageBox(AlbumCoverChangeActivity.this);

            @Override
            protected void onPreExecute() {
                msgbox.showWait();
            }

            @Override
            protected Void doInBackground(final String... params) {
                try {
                    mWebIF.fPostFileToServer(params[0], GlobalSetting.S3_MOMENT_FILE_DIR, false, new NetworkIFDelegate() {
                                @Override
                                public void didFinishNetworkIFCommunication(int i, byte[] bytes) {
                                    String fileId = new String(bytes);
                                    int pos = params[0].lastIndexOf('.');
                                    String ext = pos != -1 ? params[0].substring(pos) : "jpg";
                                    int errno = mWebIF.fSetAlbumCover(fileId, ext);
                                    if (errno == ErrorCode.OK) {
                                        setResult(RESULT_OK, new Intent().putExtra(EXTRA_FILE_ID, fileId));
                                    }
                                }

                                @Override
                                public void didFailNetworkIFCommunication(int i, byte[] bytes) {
                                    setResult(RESULT_CANCELED, null);
                                }

                                @Override
                                public void setProgress(int i, int i1) {
                                }
                            },
                            TimelineActivity.NETWORK_TAG_UPLOADING_ALBUMCOVER);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void arg) {
                msgbox.dismissWait();
                finish();
            }

            @Override
            protected void onCancelled() {
                msgbox.dismissWait();
                finish();
            }
        }, filepath);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.title_back:
            finish();
            break;
        case R.id.pick_from_local:
            sMediaInput.pickPhoto(AlbumCoverChangeActivity.this, REQ_INPUT_ALBUMCOVER);
            break;
        case R.id.take_photo:
            sMediaInput.takePhoto(AlbumCoverChangeActivity.this, REQ_INPUT_ALBUMCOVER);
            break;
        case R.id.select_provider_cover:
            Intent providerCoverIntent = new Intent(AlbumCoverChangeActivity.this,
                    ProviderAlbumCoverActivity.class);
            startActivityForResult(providerCoverIntent, REQ_INPUT_ALBUMCOVER_PROVIDER);
            break;
        default:
            break;
        }
    }

    public static void launchActivity(Activity activity, MediaInputHelper mediaInput, int requestCode) {
        sMediaInput = mediaInput;
        Intent albumCoverChangeIntent = new Intent(activity, AlbumCoverChangeActivity.class);
        activity.startActivityForResult(albumCoverChangeIntent, requestCode);
    }
}
