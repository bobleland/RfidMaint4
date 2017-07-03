package com.pottssoftware.rfidmaint4;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;

import com.ugrokit.api.UgiFooterView;
import com.ugrokit.api.UgiTitleView;
import com.ugrokit.api.UgiUiActivity;
import com.ugrokit.api.UgiUiUtil;

public class SecondPageActivity extends UgiUiActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_second_page);

    this.setThemeColor(Color.parseColor("#F58026"));
    UgiTitleView titleView = (UgiTitleView)findViewById(R.id.title_view);
    this.configureTitleViewNavigation(titleView);
    titleView.setBatteryStatusIndicatorDisplayVersionInfoOnTouch(true);
    titleView.setTheTitle("second page");
    titleView.setRightButton(R.drawable.btn_second_page_right, UgiUiUtil.NULL_COLOR,
                             0, UgiUiUtil.NULL_COLOR,
                             new Runnable() {
      @Override
      public void run() {
        System.out.println("right");
      }
    });
    UgiFooterView footer = (UgiFooterView)findViewById(R.id.footer);
    footer.setCenter("back", new Runnable() {
      @Override
      public void run() {
        goBack(Activity.RESULT_OK);
      }
    });
  }

}
