package co.onemeter.oneapp.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Support partial click and background-highlighting.
 */
public class SpannedTextView extends TextView {
    private ArrayList<String> mTextParts;
    private ArrayList<OnClickListener> mClickListeners;
    private ArrayList<MySpan> mMySpans;
    private OnClickListener bgClickListener;
    private int bgColorHighlighed;
    private int mLastHighlightedIdx = -1;

    private SpannableStringBuilder mStr;

    //----------------------------------------------------------------------
    private static class MySpan extends BackgroundColorSpan {

        private TextView mTv;
        private boolean mHighlightState = false;
        private final int mBgColor;
        private final int mBgColorHighlighted;

        public MySpan(TextView tv, int bgColor, int highlightBgColor) {
            super(highlightBgColor);
            mTv = tv;
            mBgColorHighlighted = highlightBgColor;
            mBgColor = bgColor;
        }

        public boolean isHighlighted() {
            return mHighlightState;
        }

        public boolean highlight(boolean state) {
            if (mHighlightState == state)
                return false;

            mHighlightState = state;
            if (mTv != null) {
                mTv.invalidate();
            }

            return true;
        }

        @Override
        public void updateDrawState (TextPaint ds) {
            if (mHighlightState)
                ds.bgColor = mBgColorHighlighted;
            else
                ds.bgColor = mBgColor;
        }
    }

    //----------------------------------------------------------------------

    public SpannedTextView(Context context) {
        super(context);
    }

    public SpannedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SpannedTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     *
     * @param text
     * @param typeFace can be null.
     * @param foreColor can be 0.
     * @param bgColor can be 0.
     * @param bgColorHighlighted can be 0.
     * @param onClickListener can be null.
     */
    public void append(String text, Typeface typeFace,
                       int foreColor, int bgColor, int bgColorHighlighted,
                       final OnClickListener onClickListener) {
        if (text == null)
            return;

        if (mTextParts == null) {
            mTextParts = new ArrayList<String>();
        }
        mTextParts.add(text);

        if (mClickListeners == null) {
            mClickListeners = new ArrayList<OnClickListener>();
        }
        mClickListeners.add(onClickListener);

        if (mMySpans == null) {
            mMySpans = new ArrayList<MySpan>();
        }

        if (mStr == null) {
            mStr = new SpannableStringBuilder();
        }

        int pos = mStr.length();
        int len = text.length();

        mStr.append(text);

        if (bgColor != 0 || bgColorHighlighted != 0) {
            MySpan span = new MySpan(this, bgColor, bgColorHighlighted);
            mStr.setSpan(span, pos, pos + len, 0);
            mMySpans.add(span);
        } else {
            mMySpans.add(null);
        }

        if (foreColor != 0) {
            mStr.setSpan(new ForegroundColorSpan(foreColor), pos, pos + len, 0);
        }

        if (typeFace != null && !typeFace.equals(Typeface.DEFAULT)) {
            mStr.setSpan(new StyleSpan(typeFace.getStyle()), pos, pos + len, 0);
        }

        setText(mStr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // highlight state
        final int HS_UNCHANGED = 0;
        final int HS_HIGHLIGHT = 1;
        final int HS_NORMAL = 2;

        int highlightState = HS_UNCHANGED;
        boolean clicked = false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                highlightState = HS_HIGHLIGHT;
                break;
            case MotionEvent.ACTION_CANCEL:
                highlightState = HS_NORMAL;
                break;
            case MotionEvent.ACTION_UP:
                highlightState = HS_NORMAL;
                clicked = true;
                break;
            default:
                break;
        }

        int spanIdx = -1;

        if (highlightState == HS_HIGHLIGHT || clicked) {
            // need spanIdx
            Layout layout = getLayout();
            int x = (int)event.getX();
            int y = (int)event.getY();
            if (layout!=null){
                int line = layout.getLineForVertical(y);
                int characterOffset = layout.getOffsetForHorizontal(line, x);

                for (int i = 0, n = mTextParts.size(), pos = 0; i < n; ++i) {
                    if (pos <= characterOffset && characterOffset <= pos + mTextParts.get(i).length()) {
                        spanIdx = i;
                        break;
                    }
                    pos += mTextParts.get(i).length();
                }
            }
        }

        if (highlightState == HS_HIGHLIGHT) {
            if (spanIdx != -1 && mMySpans.get(spanIdx) != null) {
                mMySpans.get(spanIdx).highlight(true);
                mLastHighlightedIdx = spanIdx;
            } else {
                mLastHighlightedIdx = -1;
            }
        } else if (highlightState == HS_NORMAL) {
            if (mLastHighlightedIdx != -1 && mMySpans.get(mLastHighlightedIdx) != null) {
                mMySpans.get(mLastHighlightedIdx).highlight(false);
            }
        }

        if (clicked && spanIdx != -1 && mClickListeners.get(spanIdx) != null) {
            mClickListeners.get(spanIdx).onClick(this);
        }

        if (highlightState == HS_HIGHLIGHT) {
            if ((spanIdx != -1 && mClickListeners.get(spanIdx) == null)) {
                setBackgroundColor(bgColorHighlighed);
                mLastHighlightedIdx = spanIdx;
            }
        } else if (highlightState == HS_NORMAL) {
            if ((mLastHighlightedIdx != -1 && mClickListeners.get(mLastHighlightedIdx) == null)) {
                setBackgroundColor(Color.parseColor("#00000000"));
            }
        }

        if (clicked && spanIdx != -1 && mClickListeners.get(spanIdx) == null && bgClickListener != null) {
            bgClickListener.onClick(this);
        }

        Log.e("spanIdx : " + spanIdx);
        return true;
    }

    public void setBgClicked(int bgColorHighlighed, OnClickListener listener) {
        this.bgColorHighlighed = bgColorHighlighed;
        bgClickListener = listener;
    }

    public void clear() {
        mTextParts = null;
        mClickListeners = null;
        mMySpans = null;
        mLastHighlightedIdx = -1;
        mStr = null;
    }
}
