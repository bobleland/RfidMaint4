package com.pottssoftware.rfidmaint4;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.ugrokit.api.Ugi;
import com.ugrokit.api.UgiEpc;
import com.ugrokit.api.UgiFooterView;
import com.ugrokit.api.UgiInventory;
import com.ugrokit.api.UgiInventoryDelegate;
import com.ugrokit.api.UgiRfMicron;
import com.ugrokit.api.UgiRfidConfiguration;
import com.ugrokit.api.UgiTag;
import com.ugrokit.api.UgiTagCell;
import com.ugrokit.api.UgiTitleView;
import com.ugrokit.api.UgiUiActivity;
import com.ugrokit.api.UgiUiUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends UgiUiActivity implements
        UgiInventoryDelegate,
        UgiInventoryDelegate.InventoryHistoryIntervalListener,
        UgiInventoryDelegate.InventoryDidStopListener,
        //UgiInventoryDelegate.InventoryDidStartListener,
        //UgiInventoryDelegate.InventoryTagChangedListener,
        //UgiInventoryDelegate.InventoryFilterListener,
        //UgiInventoryDelegate.InventoryFilterLowLevelListener,
        UgiInventoryDelegate.InventoryTagFoundListener,
        UgiInventoryDelegate.InventoryTagSubsequentFindsListener {

    private static final int SPECIAL_FUNCTION_NONE = 0;
    private static final int SPECIAL_FUNCTION_READ_USER_MEMORY = 1;
    private static final int SPECIAL_FUNCTION_READ_TID_MEMORY = 2;
    private static final int SPECIAL_FUNCTION_READ_RF_MICRON_MAGNUS_SENSOR_CODE = 3;
    private static final int SPECIAL_FUNCTION_READ_RF_MICRON_MAGNUS_TEMPERATURE = 4;

    private int specialFunction = SPECIAL_FUNCTION_NONE;

    private static final UgiRfMicron.MagnusModels RF_MICRON_MAGNUS_MODEL = UgiRfMicron.MagnusModels.Model402;
    private static final UgiRfMicron.RssiLimitTypes RF_MICRON_MAGNUS_LIMIT_TYPE = UgiRfMicron.RssiLimitTypes.LessThanOrEqual;
    private static final int RF_MICRON_MAGNUS_LIMIT_THRESHOLD = 31;

    //////////////////////////////

    private UgiRfidConfiguration rfidConfiguration;

    private OurListAdapter listAdapter;

    private List<UgiTag> displayedTags = new ArrayList<>();
    private Map<UgiTag, StringBuilder> detailedData = new HashMap<>();
    private String wEPC="";

    /** Called when the activity is first created. */
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        this.setDisplayDialogIfDisconnected(true);
        UgiTitleView titleView = (UgiTitleView)findViewById(R.id.title_view);
        this.configureTitleViewNavigation(titleView);
        titleView.setBatteryStatusIndicatorDisplayVersionInfoOnTouch(true);
        titleView.setUseBackgroundBasedOnUiColor(true);
        titleView.setDisplayWaveAnimationWhileScanning(true);
        titleView.setTheTitle(getResources().getString(R.string.main_title));

        rfidConfiguration = UgiRfidConfiguration.LOCATE_DISTANCE;
        ListView tagListView = (ListView) findViewById(R.id.tagList);
        listAdapter = new OurListAdapter(this);
        tagListView.setAdapter(listAdapter);
        tagListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final UgiInventory inventory = Ugi.getSingleton().getActiveInventory();
                if ((inventory!= null)) {
                    if (!inventory.isPaused()) inventory.pauseInventory();
                    final UgiTag tag = displayedTags.get(position);
                    UgiUiUtil.showMenu(MainActivity.this, null, new Runnable() {
                                @Override
                                public void run() {
                                    inventory.resumeInventory();
                                }
                            },
                            new UgiUiUtil.MenuTitleAndHandler("commission (write EPC)", new Runnable() {
                                @Override
                                public void run() {
                                    doCommission(tag);
                                }
                            }),
                            new UgiUiUtil.MenuTitleAndHandler("read user memory", new Runnable() {
                                @Override
                                public void run() {
                                    doReadUserMemory(tag);
                                }
                            }),
                            new UgiUiUtil.MenuTitleAndHandler("write user memory", new Runnable() {
                                @Override
                                public void run() {
                                    doWriteUserMemory(tag);
                                }
                            }),
                            new UgiUiUtil.MenuTitleAndHandler("read then write user memory", new Runnable() {
                                @Override
                                public void run() {
                                    doReadThenWriteUserMemory(tag);
                                }
                            }),
                            new UgiUiUtil.MenuTitleAndHandler("scan for this tag only", new Runnable() {
                                @Override
                                public void run() {
                                    doLocate(tag);
                                }
                            })
                    );
                } else {
                    UgiUiUtil.showOk(MainActivity.this,
                            "not scanning", "Touch a tag while scanning (or paused) to act on the tag");
                }
            }
        });
        updateUI();
    }

    // If you do not want UgiUiActivity (the superclass) to automatically rotate the screen
    // for devices with the audio jack on the top, comment in this bit of code
  /*
  @Override public boolean ugiShouldHandleRotation() {
    return false;
  }
  */

    ///////////////////////////////////////////////////////////////////////////////////////
    // UI
    ///////////////////////////////////////////////////////////////////////////////////////


    private UgiFooterView getFooter() {
        return (UgiFooterView)findViewById(R.id.footer);
    }

    private void updateUI() {
        final UgiInventory inventory = Ugi.getSingleton().getActiveInventory();
        findViewById(R.id.actions_button).setEnabled(inventory == null);

        UgiFooterView footer = getFooter();
        if (inventory != null) {
            //
            // Scanning
            //
            if (inventory.isPaused()) {
                footer.setLeft(getResources().getString(R.string.FooterResume), new Runnable() {
                    @Override
                    public void run() {
                        inventory.resumeInventory();
                        updateUI();
                    }
                });
            } else {
                footer.setLeft(getResources().getString(R.string.FooterPause), new Runnable() {
                    @Override
                    public void run() {
                        inventory.pauseInventory();
                        updateUI();
                    }
                });
            }
            footer.setCenter(getResources().getString(R.string.FooterStop), new Runnable() {
                @Override
                public void run() {
                    stopScanning();
                }
            });
            footer.setRight(null, null);
        } else {
            //
            // Not scanning
            //
            footer.setLeft(getResources().getString(R.string.FooterInfo), new Runnable() {
                @Override
                public void run() {
                    UgiUiUtil.showVersionAlert(MainActivity.this, null, false);
                }
            });
            footer.setCenter(getResources().getString(R.string.FooterStart), new Runnable() {
                @Override
                public void run() {
                    startScanning();
                }
            });
            footer.setRight(getResources().getString(R.string.FooterConfigure), new Runnable() {
                @Override
                public void run() {
                    doConfigure();
                }
            });
        }
    }

    private void updateCountAndTime() {
        TextView tv = (TextView) findViewById(R.id.count_value);
        tv.setText(displayedTags.size() + "");

        UgiInventory inventory = Ugi.getSingleton().getActiveInventory();
        if (inventory != null) {
            int seconds = (int) ((System.currentTimeMillis() - inventory.getStartTime().getTime()) / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            tv = (TextView) findViewById(R.id.time_value);
            tv.setText(String.format("%02d:%02d", minutes, seconds));
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // Inventory
    ///////////////////////////////////////////////////////////////////////////////////////

    private Handler updateTimerHandler = null;

    private void startScanning() {
        displayedTags.clear();
        detailedData.clear();
        updateTable();

        UgiRfidConfiguration config;
        if (specialFunction == SPECIAL_FUNCTION_READ_RF_MICRON_MAGNUS_SENSOR_CODE) {
            config = UgiRfMicron.configToReadMagnusSensorValue(
                    UgiRfidConfiguration.LOCATE_DISTANCE,
                    RF_MICRON_MAGNUS_MODEL,
                    RF_MICRON_MAGNUS_LIMIT_TYPE,
                    RF_MICRON_MAGNUS_LIMIT_THRESHOLD);
        } else if (specialFunction == SPECIAL_FUNCTION_READ_RF_MICRON_MAGNUS_TEMPERATURE) {
            config = UgiRfMicron.configToReadMagnusTemperature(UgiRfidConfiguration.LOCATE_DISTANCE);
        } else {
            if (specialFunction == SPECIAL_FUNCTION_READ_USER_MEMORY) {
                config = new UgiRfidConfiguration.Builder(this.rfidConfiguration)
                        .withMinUserBytes(64)
                        .withMaxUserBytes(64).build();
            } else if (specialFunction == SPECIAL_FUNCTION_READ_TID_MEMORY) {
                config = new UgiRfidConfiguration.Builder(this.rfidConfiguration)
                        .withMinTidBytes(24)
                        .withMaxTidBytes(24).build();
            } else {
                config = this.rfidConfiguration;
            }
        }
        Ugi.getSingleton().startInventory(this, config);
        updateUI();
        updateCountAndTime();
        if (!rfidConfiguration.wantsReportSubsequentFinds()) {
            updateTimerHandler = new Handler();
            updateTimerHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (updateTimerHandler != null) {
                        updateCountAndTime();
                        updateTimerHandler.postDelayed(this, 1000);
                    }
                }
            }, 1000); // 1 second delay (takes millis)
        }
    }

    private void stopScanning() {
        updateTimerHandler = null;
        UgiUiUtil.stopInventoryWithCompletionShowWaiting(this, new UgiInventory.StopInventoryCompletion() {
            @Override
            public void exec() {
                updateUI();
            }
        });
    }

    @Override
    public void disconnectedDialogCancelled() {
        stopScanning();
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // Inventory Delegate
    ///////////////////////////////////////////////////////////////////////////////////////

    @Override public void inventoryDidStop(int result) {
        if ((result != UGI_INVENTORY_COMPLETED_LOST_CONNECTION) && (result != UGI_INVENTORY_COMPLETED_OK)) {
            //
            // Inventory error
            //
            UgiUiUtil.showInventoryError(this, result);
        }
        updateTimerHandler = null;
        updateUI();
    }

    @Override public void inventoryHistoryInterval() {
        updateTable();
    }

    @Override public void inventoryTagFound(UgiTag tag, UgiInventory.DetailedPerReadData[] detailedPerReadData) {
        displayedTags.add(tag);
        detailedData.put(tag, new StringBuilder());
        handlePerReads(tag, detailedPerReadData);
        updateTable();
    }

    @Override public void inventoryTagSubsequentFinds(UgiTag tag, int count,
                                                      UgiInventory.DetailedPerReadData[] detailedPerReadData) {
        handlePerReads(tag, detailedPerReadData);
    }

    private void handlePerReads(UgiTag tag,
                                UgiInventory.DetailedPerReadData[] detailedPerReadData) {
        if (specialFunction == SPECIAL_FUNCTION_READ_RF_MICRON_MAGNUS_SENSOR_CODE) {
            for (UgiInventory.DetailedPerReadData p : detailedPerReadData) {
                //
                // get sensor code and add it to the string we display
                //
                int sensorCode = UgiRfMicron.getMagnusSensorCode(p);
                StringBuilder s = detailedData.get(tag);
                if (s.length() > 0) s.append(" ");
                s.append(sensorCode);
                if (RF_MICRON_MAGNUS_LIMIT_TYPE != UgiRfMicron.RssiLimitTypes.None) {
                    //
                    // get on-chip RSSI and add it to the string we display
                    //
                    int onChipRssi = UgiRfMicron.getMagnusOnChipRssi(p);
                    s.append("/");
                    s.append(onChipRssi);
                }
            }
        } else if (specialFunction == SPECIAL_FUNCTION_READ_RF_MICRON_MAGNUS_TEMPERATURE) {
            for (UgiInventory.DetailedPerReadData p : detailedPerReadData) {
                //
                // Get the temperature and add it to string we display
                //
                double temperatureC = UgiRfMicron.getMagnusTemperature(tag, p);
                StringBuilder s = detailedData.get(tag);
                if (s.length() > 0) s.append(" ");
                s.append(temperatureC);
            }
        }
    }

  /*
  @Override public void inventoryDidStart() {
    Log.i(TAG, "inventoryDidStart");
  }

  @Override public void inventoryTagChanged(UgiTag tag, boolean firstFind) {
    Log.i(TAG, "inventoryTagChanged: firstFind = " + firstFind + ": " + tag);
  }

  @Override public boolean inventoryFilter(UgiEpc epc) {
    Log.i(TAG, "inventoryFilter: " + epc);
    byte[] epcBytes = epc.toBytes();
    return (epcBytes[epcBytes.length-1] & 1) == 1 ? true : false;  // filter out tags with odd EPCs
  }

  @Override public boolean inventoryFilterLowLevel(byte[] epc) {
    Log.i(TAG, "inventoryFilterLowLevel: " + Util.byteArrayToString(epc));
    return (epc[epc.length-1] & 1) == 1 ? true : false;  // filter out tags with odd EPCs
  }
  */

    ///////////////////////////////////////////////////////////////////////////////////////
    // List View
    ///////////////////////////////////////////////////////////////////////////////////////

    private void updateTable() {
        listAdapter.notifyDataSetChanged();
        updateCountAndTime();
    }

    private class OurListAdapter extends BaseAdapter {

        private MainActivity mainActivity;

        public OurListAdapter(MainActivity a) {
            super();
            mainActivity = a;
        }

        @Override public boolean hasStableIds() {
            return true;
        }

        @Override public int getCount() {
            return displayedTags.size();
        }

        @Override public Object getItem(int position) {
            return position < displayedTags.size() ? displayedTags.get(position) : null;
        }

        @Override public long getItemId(int position) {
            return position < displayedTags.size() ? displayedTags.get(position).getEpc().hashCode() : 0;
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            UgiTagCell listItem = convertView != null ? (UgiTagCell)convertView : new UgiTagCell(getContext(), null);
            if (position < displayedTags.size()) {
                UgiTag tag = displayedTags.get(position);
                listItem.setDisplayTag(tag);
                listItem.setThemeColor(mainActivity.getThemeColor());
                listItem.setTitle(tag.getEpc().toString());
                String detailText = null;
                if (specialFunction == SPECIAL_FUNCTION_READ_USER_MEMORY) {
                    detailText = "user: " + byteArrayToString(tag.getUserBytes());
                } else if (specialFunction == SPECIAL_FUNCTION_READ_TID_MEMORY) {
                    detailText = "tid: " + byteArrayToString(tag.getTidBytes());
                } else {
                    StringBuilder s = detailedData.get(tag);
                    if (s.length() > 0) {
                        detailText = s.toString();
                    }
                }
                listItem.setDetail(detailText);
                listItem.updateHistoryView();
            }
            return listItem;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // Tag actions
    ///////////////////////////////////////////////////////////////////////////////////////

    // All tag actions called with inventory paused

    private void doCommission(UgiTag tag) {
        final UgiEpc oldEpc = tag.getEpc();
        UgiUiUtil.showTextInput(this,
                "commission tag", "EPC:", "commission", oldEpc.toString(),
                UgiUiUtil.DEFAULT_INPUT_TYPE,
                new UgiUiUtil.ShowTextInputCompletion() {
                    @Override
                    public void exec(String value) {
                        UgiEpc newEpc = new UgiEpc(value);
                        Ugi.getSingleton().getActiveInventory().resumeInventory();
                        updateUI();
                        UgiUiUtil.showWaiting(MainActivity.this, "commissioning");
                        Ugi.getSingleton().getActiveInventory().programTag(oldEpc, newEpc, UgiInventory.NO_PASSWORD,
                                new UgiInventory.TagAccessCompletion() {
                                    @Override
                                    public void exec(UgiTag tag, UgiInventory.TagAccessReturnValues result) {
                                        UgiUiUtil.hideWaiting();
                                        if (result == UgiInventory.TagAccessReturnValues.OK) {
                                            UgiUiUtil.showOk(MainActivity.this, "commission tag", "Successful\nNew EPC: " + tag.getEpc());
                                            updateTable();
                                        } else {
                                            UgiUiUtil.showOk(MainActivity.this, "commission tag",
                                                    UgiUiUtil.getTagAccessErrorMessage(result));
                                        }
                                    }
                                });
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        Ugi.getSingleton().getActiveInventory().resumeInventory();
                        updateUI();
                    }
                }, new UgiUiUtil.ShowTextInputShouldEnableForTextCompletion() {
                    @Override
                    public boolean exec(String value) {
                        return (value.length() == oldEpc.toString().length()) && value.matches("^[0-9a-fA-F]*$");
                    }
                });
    }

    private void doReadUserMemory(UgiTag tag) {
        Ugi.getSingleton().getActiveInventory().resumeInventory();
        updateUI();
        UgiUiUtil.showWaiting(this, "reading user memory");
        System.out.println("doReadUserMemory");
        Ugi.getSingleton().getActiveInventory().readTag(
                tag.getEpc(),
                UgiRfidConfiguration.MemoryBank.User,
                0, 16, 64,
                UgiInventory.NO_PASSWORD,
                new UgiInventory.TagReadCompletion() {
                    @Override
                    public void exec(UgiTag tag, byte[] data, UgiInventory.TagAccessReturnValues result) {
                        System.out.println("doReadUserMemory CALLBACK: " + result);
                        UgiUiUtil.hideWaiting();
                        if (result == UgiInventory.TagAccessReturnValues.OK) {
                            UgiUiUtil.showOk(MainActivity.this, "read user memory",
                                    "Read " + data.length + " bytes: " + byteArrayToString(data),
                                    "", null);
                        } else {
                            UgiUiUtil.showOk(MainActivity.this, "read user memory",
                                    "Error: " + UgiUiUtil.getTagAccessErrorMessage(result));
                        }
                    }
                }
        );
    }

    private void doWriteUserMemory(UgiTag tag) {
        Ugi.getSingleton().getActiveInventory().resumeInventory();
        updateUI();
        final byte[] newData = "Hello World!".getBytes();
        UgiUiUtil.showWaiting(MainActivity.this, "writing user memory");
        Ugi.getSingleton().getActiveInventory().writeTag(tag.getEpc(),
                UgiRfidConfiguration.MemoryBank.User, 0, newData, null,
                UgiInventory.NO_PASSWORD, new UgiInventory.TagAccessCompletion() {
                    @Override
                    public void exec(UgiTag tag, UgiInventory.TagAccessReturnValues result) {
                        UgiUiUtil.hideWaiting();
                        if (result == UgiInventory.TagAccessReturnValues.OK) {
                            UgiUiUtil.showOk(MainActivity.this, "write user memory",
                                    "Write " + newData.length + " bytes: " + byteArrayToString(newData),
                                    "", null);
                        } else {
                            UgiUiUtil.showOk(MainActivity.this, "write user memory",
                                    "Error writing tag: " + UgiUiUtil.getTagAccessErrorMessage(result));
                        }
                    }});
    }

    private void doReadThenWriteUserMemory(UgiTag tag) {
        Ugi.getSingleton().getActiveInventory().resumeInventory();
        updateUI();
        UgiUiUtil.showWaiting(this, "reading user memory");
        Ugi.getSingleton().getActiveInventory().readTag(
                tag.getEpc(),
                UgiRfidConfiguration.MemoryBank.User,
                0, 16, 64,
                UgiInventory.NO_PASSWORD,
                new UgiInventory.TagReadCompletion() {
                    @Override
                    public void exec(UgiTag tag, byte[] data, UgiInventory.TagAccessReturnValues result) {
                        UgiUiUtil.hideWaiting();
                        if (result == UgiInventory.TagAccessReturnValues.OK) {
                            byte[] newData = new byte[data.length];
                            System.arraycopy(data, 1, newData, 0, data.length-1);
                            newData[data.length-1] = data[0];
                            final byte[] _newData = newData;
                            UgiUiUtil.showWaiting(MainActivity.this, "writing user memory");
                            Ugi.getSingleton().getActiveInventory().writeTag(tag.getEpc(),
                                    UgiRfidConfiguration.MemoryBank.User, 0, newData, data,
                                    UgiInventory.NO_PASSWORD, new UgiInventory.TagAccessCompletion() {
                                        @Override
                                        public void exec(UgiTag tag, UgiInventory.TagAccessReturnValues result) {
                                            UgiUiUtil.hideWaiting();
                                            if (result == UgiInventory.TagAccessReturnValues.OK) {
                                                UgiUiUtil.showOk(MainActivity.this, "write user memory",
                                                        "Write " + _newData.length + " bytes: " + byteArrayToString(_newData),
                                                        "", null);
                                            } else {
                                                UgiUiUtil.showOk(MainActivity.this, "write user memory",
                                                        "Error writing tag: " + UgiUiUtil.getTagAccessErrorMessage(result));
                                            }
                                        }});
                        } else {
                            UgiUiUtil.showOk(MainActivity.this, "read user memory",
                                    "Error reading tag: " + UgiUiUtil.getTagAccessErrorMessage(result));
                        }
                    }
                }
        );
    }

    private void doLocate(final UgiTag tag) {
        updateTimerHandler = null;
        UgiUiUtil.stopInventoryWithCompletionShowWaiting(this, new UgiInventory.StopInventoryCompletion() {
            @Override
            public void exec() {
                displayedTags.clear();
                detailedData.clear();
                updateTable();

                UgiRfidConfiguration config = new UgiRfidConfiguration.Builder(UgiRfidConfiguration.LOCATE_DISTANCE)
                        .withSelectBank(UgiRfidConfiguration.MemoryBank.Epc)
                        .withSelectMask(tag.getEpc().toBytes())
                        .withSelectOffset(32)
                        .build();
                Ugi.getSingleton().startInventory(MainActivity.this, config);
                updateUI();
                updateCountAndTime();
                UgiUiUtil.showToast(MainActivity.this, "Restarted inventory", "Searching for only " + tag.getEpc().toString());
            }
        });
    }


    ///////////////////////////////////////////

    private static String byteArrayToString(byte[] ba) {
        if (ba == null) return null;
        StringBuilder sb = new StringBuilder(ba.length*2);
        for (byte b : ba) {
            sb.append(NibbleToChar((b >> 4) & 0xf));
            sb.append(NibbleToChar(b & 0xf));
        }
        return sb.toString();
    }

    private static char NibbleToChar(int nibble) {
        return (char) (nibble + (nibble < 10 ? '0' : 'a'-10));
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // Configure
    ///////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    private static final ArrayList<UgiRfidConfiguration> INVENTORY_TYPE_LIST =
            new ArrayList<UgiRfidConfiguration>() {{
                add(UgiRfidConfiguration.LOCATE_DISTANCE);         // 0
                add(UgiRfidConfiguration.INVENTORY_SHORT_RANGE);   // 1
                add(UgiRfidConfiguration.INVENTORY_DISTANCE);      // 2
                add(UgiRfidConfiguration.LOCATE_SHORT_RANGE);      // 3
                add(UgiRfidConfiguration.LOCATE_VERY_SHORT_RANGE); // 4
                add(UgiRfidConfiguration.SINGLE_FIND);             // 5
            }};

    private void doConfigure() {
        UgiUiUtil.showMenu(MainActivity.this, "configure", null,
                new UgiUiUtil.MenuTitleAndHandler("inventory type", new Runnable() {
                    @Override
                    public void run() {
                        UgiUiUtil.showChoices(MainActivity.this,
                                new String[]{
                                        //"Manual Configuration",
                                        "Locate Distance",
                                        "Inventory Short Range",
                                        "Inventory Distance",
                                        "Locate Short Range",
                                        "Locate Very Short Range",
                                        "Single Find"
                                },
                                INVENTORY_TYPE_LIST.indexOf(rfidConfiguration),
                                "Inventory Type", "set type", true,
                                new UgiUiUtil.ShowChoicesCompletion() {
                                    @Override
                                    public void exec(int choiceIndex, String choice) {
                                        rfidConfiguration = INVENTORY_TYPE_LIST.get(choiceIndex);
                                    }
                                }, null, null);
                    }
                }),
                new UgiUiUtil.MenuTitleAndHandler("special functions", new Runnable() {
                    @Override
                    public void run() {
                        UgiUiUtil.showChoices(MainActivity.this,
                                new String[]{
                                        "None",
                                        "Read User Memory",
                                        "Read TID memory",
                                        "Read RF Micron sensor code",
                                        "Read RF Micron temperature"
                                },
                                specialFunction,
                                "Special Functions", "", true,
                                new UgiUiUtil.ShowChoicesCompletion() {
                                    @Override
                                    public void exec(int choiceIndex, String choice) {
                                        specialFunction = choiceIndex;
                                    }
                                }, null, null);
                    }
                })
        );

    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // Actions
    ///////////////////////////////////////////////////////////////////////////////////////

    public void doActions(View view) {
        Collection<UgiUiUtil.MenuTitleAndHandler> items = new ArrayList<>();
        items.add(new UgiUiUtil.MenuTitleAndHandler("set region", new Runnable() {
            @Override
            public void run() {
                Ugi.getSingleton().invokeSetRegion(false);
            }
        }));
        items.add(new UgiUiUtil.MenuTitleAndHandler("audio reconfiguration", new Runnable() {
            @Override
            public void run() {
                Ugi.getSingleton().invokeAudioReconfiguration();
            }
        }));
        items.add(new UgiUiUtil.MenuTitleAndHandler("set audio jack location", new Runnable() {
            @Override
            public void run() {
                Ugi.getSingleton().invokeAudioJackLocation();
            }
        }));
        items.add(new UgiUiUtil.MenuTitleAndHandler("example: second page", new Runnable() {
            @Override
            public void run() {
                startActivityWithTransition(SecondPageActivity.class);
            }
        }));
        if (Ugi.getSingleton().getActiveInventory() == null) {
            items.add(new UgiUiUtil.MenuTitleAndHandler("example: find one tag", new Runnable() {
                @Override
                public void run() {
                    startActivityWithTransition(FindOneTagActivity.class, FIND_ONE_REQUEST);
                }
            }));
        }
        UgiUiUtil.showMenu(MainActivity.this, null, null, items.toArray(new UgiUiUtil.MenuTitleAndHandler[items.size()]));
    }

    //-------------------------------------------------

    private static final int FIND_ONE_REQUEST = 1;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FIND_ONE_REQUEST:
                if (data != null) {
                    String epcString = data.getAction();
                    handleTagFound(epcString);
                    Bundle bundle = new Bundle();
                    bundle.putString("mode", "add");
                    bundle.putString("epc", epcString);
                    Intent inEd = new Intent(getApplicationContext(),TreesEdit.class);
                    inEd.putExtras(bundle);
                    startActivity(inEd);

                }
                break;
        }
    }

    private void handleTagFound(String epcString) {
        UgiUiUtil.showOk(this, "Tag found", epcString);
    }

}
