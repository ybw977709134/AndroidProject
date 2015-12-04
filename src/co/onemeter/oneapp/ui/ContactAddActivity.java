package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import co.onemeter.oneapp.utils.ThemeHelper;

import com.zxing.activity.CaptureActivity;

import org.wowtalk.ui.MessageBox;

import co.onemeter.oneapp.R;

public class ContactAddActivity extends Activity implements OnClickListener{
	
	private ImageButton btnTitleClose;
	
	private TextView txtSearch;
    private TextView txtScanQrCode;
	private TextView txtImport;
	private TextView txtCreateGroup;
	private TextView txtOfficial;
    private TextView txtExternal;
    private TextView txtNearby;
    private MessageBox mMsgBox;
	
	private void initView() {
		btnTitleClose = (ImageButton) findViewById(R.id.title_close);
		txtSearch = (TextView) findViewById(R.id.add_search);
        txtScanQrCode = (TextView) findViewById(R.id.scan_qr_code);
		txtImport = (TextView) findViewById(R.id.add_import);
		txtCreateGroup = (TextView) findViewById(R.id.add_create_group);
		txtOfficial = (TextView) findViewById(R.id.add_offical);
        txtExternal = (TextView) findViewById(R.id.add_external);
        txtNearby = (TextView) findViewById(R.id.add_nearby);
        txtOfficial = (TextView) findViewById(R.id.add_offcial_account);
		
		btnTitleClose.setOnClickListener(this);
		txtSearch.setOnClickListener(this);
        txtScanQrCode.setOnClickListener(this);
		txtImport.setOnClickListener(this);
		txtCreateGroup.setOnClickListener(this);
		txtOfficial.setOnClickListener(this);
        txtExternal.setOnClickListener(this);
        txtNearby.setOnClickListener(this);
        findViewById(R.id.add_tell_friends).setOnClickListener(this);
        findViewById(R.id.add_offcial_account).setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_close:
                finish();
                break;
            case R.id.add_search:
                Intent searchIntent = new Intent(ContactAddActivity.this, ContactSearchActivity.class);
                startActivity(searchIntent);
                break;
            case R.id.scan_qr_code:
                Intent scanIntent = new Intent(ContactAddActivity.this, ScanQRCodeActivity.class);
                ThemeHelper.putExtraCurrThemeResId(scanIntent, this);
                scanIntent.putExtra(CaptureActivity.ACTIVITY_ARG_WITH_LAYOUT,true);
                scanIntent.putExtra(CaptureActivity.ACTIVITY_ARG_LAYOUT_ID,R.layout.scan_qr_code_layout);
                scanIntent.putExtra(CaptureActivity.ACTIVITY_ARG_FINISH_AFTER_DECODE,false);
                startActivity(scanIntent);
                break;
            case R.id.add_import:
                Intent localIntent = new Intent(ContactAddActivity.this, LocalContactsActivity.class);
                startActivity(localIntent);
                break;
            case R.id.add_create_group:
                Intent createIntent = new Intent(ContactAddActivity.this, CreateGroupActivity.class);
                startActivity(createIntent);
                break;
            case R.id.add_offical:
                Intent publicIntent = new Intent(ContactAddActivity.this, PublicSearchActivity.class);
                startActivity(publicIntent);
                break;
            case R.id.add_external:
                Intent externalIntent = new Intent(ContactAddActivity.this, ExternalSearchActivity.class);
                startActivity(externalIntent);
                break;
            case R.id.add_nearby:
            	Intent nearbyIntent = new Intent(ContactAddActivity.this, NearbyActivity.class);
            	startActivity(nearbyIntent);
            	break;
            case R.id.add_offcial_account:
                Intent publicIntent2 = new Intent(ContactAddActivity.this, PublicSearchActivity.class);
            	startActivity(publicIntent2);
            	break;
            case R.id.add_tell_friends:
            	tellFriend(v);
            	break;
            default:
                mMsgBox.toast(R.string.not_implemented);
                break;
        }
    }

	private void tellFriend(View parentView) {
        final BottomButtonBoard bottomBoard = new BottomButtonBoard(this, parentView);
        // Send Email
        bottomBoard.add(getString(R.string.setting_send_email), BottomButtonBoard.BUTTON_BLUE,
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent emailIntent = new Intent(Intent.ACTION_SEND);
                        // use this line for testing in the emulator
//                        emailIntent.setType("text/plain");
                        // use from live device
                        emailIntent.setType("message/rfc822");
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.setting_tell_friend_email_subject));
                        emailIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.setting_tell_friend_email_content));
                        startActivity(Intent.createChooser(emailIntent, null));

                        bottomBoard.dismiss();
                    }
                });
        // Send SMS
        bottomBoard.add(getString(R.string.setting_send_sms), BottomButtonBoard.BUTTON_BLUE,
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Uri smsUri = Uri.parse("smsto:");
                        Intent intent = new Intent(Intent.ACTION_SENDTO, smsUri);
                        intent.putExtra("sms_body", getResources().getString(R.string.setting_tell_friend_sms_content));
                        startActivity(intent);
                        bottomBoard.dismiss();
                    }
                });
        //Cancel
        bottomBoard.addCancelBtn(getString(R.string.close));
        bottomBoard.show();
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contact_add_page);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mMsgBox = new MessageBox(this);

		initView();
	}

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
