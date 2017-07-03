package com.pottssoftware.rfidmaint4;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.ugrokit.api.*;

public class FindOneTagActivity extends UgiUiActivity implements UgiInventoryDelegate, UgiInventoryDelegate.InventoryTagFoundListener {

  private UgiTag tagFound = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_find_one_tag);
    Ugi.getSingleton().startInventory(this, UgiRfidConfiguration.INVENTORY_DISTANCE);
    this.setDisplayDialogIfDisconnected(true);
    UgiTitleView titleView = (UgiTitleView)findViewById(R.id.title_view);
    this.configureTitleViewNavigation(titleView);
    titleView.setUseBackgroundBasedOnUiColor(true);
    titleView.setDisplayWaveAnimationWhileScanning(true);
    titleView.setTheTitle("find one tag");
  }

  @Override
  public void disconnectedDialogCancelled() {
    stopScanningThenGoBack();
  }

  //////////////////

  @Override public void inventoryTagFound(UgiTag tag,
                                          UgiInventory.DetailedPerReadData[] detailedPerReadData) {
    //Ugi.log("inventoryTagFound: " + tag);

    if (tagFound == null) {
      tagFound = tag;
      TextView tv = (TextView) findViewById(R.id.textView);
      tv.setText(tag.getEpc().toString());
      stopScanningThenGoBack();
    }
  }

  public void doCancel(View view) {
    stopScanningThenGoBack();
  }

  private void stopScanningThenGoBack() {
    UgiUiUtil.stopInventoryWithCompletionShowWaiting(this, new UgiInventory.StopInventoryCompletion() {
      @Override
      public void exec() {
        goBack(Activity.RESULT_OK, tagFound != null ? new Intent(tagFound.getEpc().toString()) : null);
      }
    });
  }

}
