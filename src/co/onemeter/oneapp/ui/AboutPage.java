package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import org.wowtalk.api.PrefUtil;
import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;

/**
 *关于页面
 */
public class AboutPage extends Activity implements OnClickListener, View.OnLongClickListener {
    private static final String SUPPORT_EMAIL_ADD = "support@onemeter.co";

    private ImageButton btnTitleBack;
    private TextView textView_about_back;
    private TextView txtVersion;
    private MessageBox mMsgBox;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_page);
        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);
        initView();
    }

    private void initView() {
        btnTitleBack = (ImageButton) findViewById(R.id.title_back);
        textView_about_back = (TextView) findViewById(R.id.textView_about_back);
        txtVersion = (TextView) findViewById(R.id.txt_version);
        txtVersion.setText(getVersion(false));
        txtVersion.setLongClickable(true);
        txtVersion.setOnLongClickListener(this);
        btnTitleBack.setOnClickListener(this);
        textView_about_back.setOnClickListener(this);
        findViewById(R.id.app_support_layout).setOnClickListener(this);
    }

    /**
     *
     * @param verbose output more info.
     * @return version text or empty string.
     */
    public String getVersion(boolean verbose) {
        try {
            boolean isDebuggable =  ( 0 != ( getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE ) );
            PackageInfo pkgInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            PrefUtil prefUtil = PrefUtil.getInstance(this);
            String webDomain = prefUtil.getWebDomain();
            return (verbose ? pkgInfo.packageName + ",\r\n" : "")
                    + pkgInfo.versionName
                    + (verbose ? ", " + pkgInfo.versionCode : "")
                    + (isDebuggable ? "-[debug]" : "")
                    + (verbose ? ",\r\n" + webDomain : "");

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.title_back:
            case R.id.textView_about_back:
                finish();
                break;
            case R.id.app_support_layout:
//                Uri uri = Uri.parse("mailto:" + SUPPORT_EMAIL_ADD);
//                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, uri);
//                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.aboutpage_feedback_subject));
//                startActivity(emailIntent);
                AppStatusService.setIsMonitoring(false);

                StringBuffer uriSBuffer = new StringBuffer();
                uriSBuffer.append("mailto:").append(SUPPORT_EMAIL_ADD)
                    .append("?subject=").append(getResources().getString(R.string.aboutpage_feedback_subject));
                Uri uri = Uri.parse(uriSBuffer.toString());
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, uri);
                startActivity(Intent.createChooser(emailIntent, null));
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.txt_version:
                startActivity(new Intent(this, com.example.panzy.sockettester.MainActivity.class));
                break;
        }
        return false;
    }


    @Override
    protected void onResume() {
        super.onResume();
        AppStatusService.setIsMonitoring(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
