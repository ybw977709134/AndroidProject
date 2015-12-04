package co.onemeter.oneapp.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import co.onemeter.oneapp.R;
import com.androidquery.AQuery;

import java.util.HashMap;

/**
 * <p>下拉列表，适用于“时间线”和“乐趣活动”页面，作为过滤器。</p>
 * Created by pzy on 10/15/14.
 */
public abstract class MenuBar {

    public interface OnDropdownMenuItemClickListener {
        public void onDropdownMenuShow(int subMenuResId);
        public void onDropdownMenuItemClick(int subMenuResId, int itemIdx);
    }

    private final int[] menuItemResIds;
    private int selectedMenuItemResId;
    // menuItemResId => item index
    HashMap<Integer, Integer> selectedItemIdx = new HashMap<Integer, Integer>();

    private final View view;
    Context context;
    View dialogBackground;
    View anchorView;
    PopupWindow subMenuDlg;
    OnDropdownMenuItemClickListener onFilterChangedListener;
    private int backgroundColor = Color.WHITE;

    private static String sText;

    private static TextView btn_sender;
    private static TextView btn_cat;

    private static TextView btn_filter1;
    private static TextView btn_filter2;



    /**
     * @param dialogBackground 作为对话框下方的屏幕背景，一般为半透明的黑色。
     */
    public MenuBar(
            Context context,
            int layoutResId,
            int[] menuItemResIds,
            View dialogBackground) {
        this.context = context;
        view = LayoutInflater.from(context).inflate(layoutResId, null);
        this.menuItemResIds = menuItemResIds;
        this.dialogBackground = dialogBackground;
        this.anchorView = view;

        if (layoutResId == R.layout.timeline_filter) {
            btn_sender = (TextView) view.findViewById(R.id.btn_sender);
            btn_cat = (TextView) view.findViewById(R.id.btn_cat);
        }

        if (layoutResId == R.layout.event_filter) {
            btn_filter1 = (TextView) view.findViewById(R.id.btn_filter1);
            btn_filter2 = (TextView) view.findViewById(R.id.btn_filter2);
        }


        for (int r : menuItemResIds) {
            selectedItemIdx.put(r, 0);
        }

        AQuery q = new AQuery(view);
        for (int r : menuItemResIds) {
            q.find(r).clicked(clickListener);
        }
        if(menuItemResIds.length == 1){
            sText = q.find(menuItemResIds[0]).getText().toString();
        }
    }

    public static TextView getSender() {
        if (btn_sender != null) {
            return btn_sender;
        }
        return null;
    }

    public static TextView getCat() {
        if (btn_cat != null) {
            return btn_cat;
        }
        return null;
    }


    public static TextView getBtnFilter1() {
        if (btn_filter1 != null) {
            return btn_filter1;
        }
        return null;
    }

    public static TextView getBtnFilter2() {
        if (btn_filter2 != null) {
            return btn_filter2;
        }
        return null;
    }

    protected abstract String[] getSubItems(int itemId);

    public View getView() {
        return view;
    }

    public void setOnFilterChangedListener(OnDropdownMenuItemClickListener l) {
        onFilterChangedListener = l;
    }

    private void setRightDrawable(TextView textView, int drawableResId) {
        Drawable d = context.getResources().getDrawable(drawableResId);
        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        textView.setCompoundDrawables(null, null, d, null);
    }

    private void setCollapsedButtonSyle(TextView button) {
        button.setTextColor(context.getResources().getColor(R.color.text_gray32));
        setRightDrawable(button, R.drawable.icon_events_category_triangle_grey);
        if(menuItemResIds.length == 1){
            button.setText(sText);
        }
    }

    private void setExpandedButtonSyle(TextView button) {
        button.setTextColor(context.getResources().getColor(R.color.blue_main));
        setRightDrawable(button, R.drawable.icon_events_category_triangle_bule);
    }

    private void showDialog(PopupWindow dlgToShow, TextView button) {
        Rect rect = locateView(anchorView);
        if (rect != null) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) dialogBackground.getLayoutParams();
            lp.topMargin = rect.height();
            dialogBackground.setLayoutParams(lp);
        }
        dlgToShow.showAtLocation(dialogBackground, Gravity.TOP, 0, rect != null ? rect.bottom : 0);
        dialogBackground.setVisibility(View.VISIBLE);
        dialogBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryDismissAll();
            }
        });
        if (button != null) {
            setExpandedButtonSyle(button);
        }
    }

    public void tryDismissAll() {
        if (subMenuDlg != null) {
            subMenuDlg.dismiss();
        }

        dialogBackground.setVisibility(View.GONE);

        AQuery q = new AQuery(view);
        for (int r : menuItemResIds) {
            setCollapsedButtonSyle(q.find(r).getTextView());
        }
    }

    public boolean isShowingDialog() {
        return (subMenuDlg != null && subMenuDlg.isShowing());
    }

    private static PopupWindow createListDialog(
            Context context,
            String[] items, final int selectedIdx,
            final AdapterView.OnItemClickListener onItemClickListener,
            final int backgroundColor) {
        View dlgView = View.inflate(context, R.layout.menubar_list, null);
        dlgView.setBackgroundColor(backgroundColor);
        final ListView lv = ((ListView)dlgView.findViewById(android.R.id.list));
        lv.setAdapter(
                new ArrayAdapter<String>(context,
                        R.layout.menubar_list_item,
                        android.R.id.text1,
                        items) {
                    @Override
                    public View getView(final int position, View convertView, ViewGroup parent) {
                        View v = super.getView(position, convertView, parent);
                        v.setBackgroundColor(backgroundColor);
                        final TextView tv = (TextView) v.findViewById(android.R.id.text1);
                        Drawable rightDrawable = selectedIdx == position
                                ? getContext().getResources().getDrawable(R.drawable.icon_section_select)
                                : null;
                        if (rightDrawable != null) {
                            rightDrawable.setBounds(0, 0, rightDrawable.getIntrinsicWidth(), rightDrawable.getIntrinsicHeight());
                        }
                        tv.setCompoundDrawables(null, null,  rightDrawable, null);

                        // WORKAROUND: ListView in a Popup WIndow, onItemClick not fired.
                        v.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                /*
                                 *  as every row is still receiving their touches
                                 *  we can use this to manually trigger onItemClick
                                 *  since it doesn't firing in popupWindow.setFocusable(false)
                                 */
                                sText = tv.getText().toString();
                                onItemClickListener.onItemClick(lv, v, position, getItemId(position));

                            }
                        });
                        return v;
                    }
                }
        );
        // lv.setOnItemClickListener(onItemClickListener); // triggered by OnClickListener
        return new PopupWindow(dlgView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                false // too many side-effect if pop window is fousable.
        );
    }

    private PopupWindow createSubMenu(int menuItemResId) {
        selectedMenuItemResId = menuItemResId;
        return createListDialog(context,
                getSubItems(menuItemResId),
                selectedItemIdx.get(menuItemResId),
                onCatListItemClickListener,
                backgroundColor);
    }

    private AdapterView.OnItemClickListener onCatListItemClickListener =
            new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    selectedItemIdx.put(selectedMenuItemResId, position);
                    tryDismissAll();
                    if (onFilterChangedListener != null) {
                        onFilterChangedListener.onDropdownMenuItemClick(selectedMenuItemResId, position);
                    }
                }
            };

    private static Rect locateView(View v)
    {
        int[] loc_int = new int[2];
        if (v == null) return null;
        try
        {
            v.getLocationOnScreen(loc_int);
        } catch (NullPointerException npe)
        {
            //Happens when the view doesn't exist on screen anymore.
            return null;
        }
        Rect location = new Rect();
        location.left = loc_int[0];
        location.top = loc_int[1];
        location.right = location.left + v.getWidth();
        location.bottom = location.top + v.getHeight();
        return location;
    }

    public void setBackgroundColor(int color) {
        backgroundColor = color;
    }

    private final View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isShowingDialog()) {
                tryDismissAll();
            } else {
                if (onFilterChangedListener != null) {
                    onFilterChangedListener.onDropdownMenuShow(v.getId());
                }
                subMenuDlg = createSubMenu(v.getId());
                showDialog(subMenuDlg, v instanceof TextView ? (TextView) v : null);
            }
        }
    };
}
