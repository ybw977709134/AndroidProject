package co.onemeter.oneapp.ui;

import org.wowtalk.api.WowTalkVoipIF;
import co.onemeter.oneapp.R;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;

/**
 *添加视频的按钮
 */
public class AddVideoButton extends Button implements OnClickListener {

	private boolean mIsON;
	public  String mCalleeName;
	private Drawable stateChecked;
	private Drawable stateUnChecked;
	
	public AddVideoButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		stateChecked = getResources().getDrawable(attrs.getAttributeResourceValue(null, "checked", -1));
		stateUnChecked = getResources().getDrawable(attrs.getAttributeResourceValue(null, "unchecked", -1));
		setBackgroundColor(Color.TRANSPARENT);
		setBackgroundDrawable(mIsON ? stateChecked : stateUnChecked);
		setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if (mIsON) {
			setOff();
		} else {
			startTheInvite();
		}
	}

	/**
	 *
	 */
	public void acceptVideoAndReinvite() {
		mIsON = true;
		WowTalkVoipIF.fAcceptVideoCallRequest(true);
		if (IncallActivity.instance() != null) {
			IncallActivity.instance().showDisplayMsg(R.string.require_for_video_call);
			setBackgroundDrawable(mIsON ? stateChecked : stateUnChecked);
		}
	}
	
	public void setOff() {
		mIsON = false;
		if (IncallActivity.instance() != null) {
			IncallActivity.instance().showDisplayMsg(0);
		}
		setBackgroundDrawable(mIsON ? stateChecked : stateUnChecked);
	}
	
	private void startTheInvite() {
		mIsON = true;
		setBackgroundDrawable(mIsON ? stateChecked : stateUnChecked);
		IncallActivity.instance().showDisplayMsg(R.string.require_for_video_call);
		WowTalkVoipIF.fStartVideoCallInvite();
	}
	
	public boolean isON() {
		return mIsON;
	}
	
	public boolean reset() {
		mIsON = false;
		setBackgroundDrawable(mIsON ? stateChecked : stateUnChecked);
		setBackgroundDrawable(mIsON ? stateChecked : stateUnChecked);
		return mIsON;
	}
	
}
