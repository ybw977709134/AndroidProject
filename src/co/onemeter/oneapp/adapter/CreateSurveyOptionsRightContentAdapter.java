package co.onemeter.oneapp.adapter;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import co.onemeter.oneapp.R;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-9-28
 * Time: 下午1:53
 * To change this template use File | Settings | File Templates.
 */
public class CreateSurveyOptionsRightContentAdapter extends BaseAdapter {
    private ArrayList<String> optionsContent;
    private Context contextRef;

    public CreateSurveyOptionsRightContentAdapter(Context context) {
        contextRef=context;
    }

    public void setOptionsContent(ArrayList<String> content) {
        optionsContent=content;
        if(null == optionsContent) {
            optionsContent=new ArrayList<String>();
        }
    }

    @Override
    public int getCount() {
        return optionsContent.size();
    }

    @Override
    public Object getItem(int i) {
        return optionsContent.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(contextRef).inflate(R.layout.create_survey_options_content_item, null);
        }

        EditText tvOptionContent=(EditText) convertView.findViewById(R.id.survey_option_content);
        tvOptionContent.setText(optionsContent.get(position));

        tvOptionContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                optionsContent.set(position,editable.toString());
            }
        });

        return convertView;
    }
}
