package com.tv.avario.home;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;

import com.android.volley.VolleyError;
import com.avario.core.websockets.AvarioWebSocket;
import com.tv.avario.R;
import com.tv.avario.api.APIClient;
import com.tv.avario.core.StateArray;
import com.tv.avario.exception.AvarioException;
import com.tv.avario.fragment.SettingsDialogFragment;
import com.tv.avario.util.Log;
import com.tv.avario.util.PlatformUtil;
import com.tv.avario.widget.ElementsBar;
import com.tv.avario.widget.adapter.ElementAdapter;
import com.tv.avario.widget.adapter.Entity;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by orly on 2/7/18.
 */

public class MainDialogActivity extends BaseActivity {

  private ElementsBar _elementsBar;
  private ImageView   _closeImage;
  private ImageView   _settingsImage;

  private View _viewSelected;

  private static AvarioWebSocket avarioWebSocket;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_dialog);
    initViews();
    getData();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (avarioWebSocket == null) {
      avarioWebSocket = AvarioWebSocket.getInstance();
    }

    if (avarioWebSocket.getWebSocket() == null ||
        !avarioWebSocket.getWebSocket().isOpen()) {
      avarioWebSocket.start();
    }

  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {

    if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER &&
        event.getAction() == KeyEvent.ACTION_UP) {
      if (_viewSelected != null) {
        switch (_viewSelected.getId()) {
        case R.id.image_close:
          finish();
          break;
        case R.id.image_settings:
          showSettingsDialog(settingsListener);
          break;
        }

      }
      return true;

    }
    return super.dispatchKeyEvent(event);
  }

  private void initViews() {
    _elementsBar = (ElementsBar) findViewById(R.id.element_bar);
    _closeImage = (ImageView) findViewById(R.id.image_close);
    _settingsImage = (ImageView) findViewById(R.id.image_settings);
    _elementsBar.getAdapter().setMode(ElementAdapter.MODE_HOME);

    _closeImage.setOnFocusChangeListener(onFocusChangeListener);
    _settingsImage.setOnFocusChangeListener(onFocusChangeListener);

    _closeImage.setOnClickListener(onClickListener);
    _settingsImage.setOnClickListener(onClickListener);

  }

  private View.OnFocusChangeListener onFocusChangeListener = new View.OnFocusChangeListener() {
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
      if (hasFocus) {
        _viewSelected = v;
      }
    }
  };

  private View.OnClickListener onClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View view) {
      switch (view.getId()) {
      case R.id.image_close:
        finish();
        break;
      case R.id.image_settings:
        showSettingsDialog(settingsListener);
        break;
      }
    }
  };

  private void getData() {
    StateArray state = StateArray.getInstance();
    try {
      if (state.getTelevisionItems() == null) {
        loadBootstrap();
      } else {
        updateElements(state, state.getTelevisionItems());
      }
    } catch (AvarioException e) {
      e.printStackTrace();
    }
    _elementsBar.setFocusable(true);
  }

  private void loadBootstrap() {
    APIClient
        .getInstance(getApplicationContext())
        .getBootstrapJSON(new BootstrapListener(), null, true);
  }

  protected void showSettingsDialog(SettingsDialogFragment.Listener listener) {
    FragmentTransaction transaction = this.getFragmentManager().beginTransaction();

    SettingsDialogFragment settingsDialog = new SettingsDialogFragment();
    settingsDialog.setListener(listener);
    settingsDialog.setCancelable(false);
    settingsDialog.show(transaction, "dialog");

  }

  private void updateElements(StateArray state, JSONArray elementsJSON) {
    ElementAdapter adapter = _elementsBar.getAdapter();
    int newSize = elementsJSON.length(),
        oldSize = adapter.size(),
        diff = oldSize - newSize,
        end,
        misses;

    // changes
    end = Math.min(newSize, oldSize);
    misses = 0;

    for (int index = 0, loop = 0; loop < end && index < newSize; index++) {
      try {
        JSONObject entityJSON = state.getEntity(elementsJSON.optString(index));
        Entity entity;

        entity = adapter.get(index);
        entity.id = entityJSON.optString("entity_id");
        entity.data = entityJSON;
        entity.selected = false;

        loop++;
      } catch (NullPointerException exception) {
        loop++;
      } catch (AvarioException exception) {
        misses++;

        PlatformUtil
            .getErrorToast(this, exception)
            .show();
      }
    }

    adapter.notifyItemRangeChanged(0, oldSize - misses);

    // removal
    end = oldSize - diff - misses;

    for (int index = oldSize - 1; index >= end; index--) { adapter.remove(index); }

    adapter.notifyItemRangeRemoved(end, diff);

    // additions
    for (int index = oldSize + misses; index < newSize; index++) {
      try {
        JSONObject entityJSON = state.getEntity(elementsJSON.optString(index));
        Entity element;

        element = new Entity();
        element.id = entityJSON.optString("entity_id");
        element.data = entityJSON;
        element.selected = false;

        adapter.append(element);
      } catch (AvarioException exception) {
        newSize--;


        PlatformUtil
            .getErrorToast(this, exception)
            .show();
      }
    }

    adapter.notifyItemRangeInserted(oldSize, newSize);

    Log.d("Element", adapter.size() + "");


    _elementsBar.setSelected(true);
    // _elementsBar.getChildAt(0).requestFocus();
    _elementsBar.setFocusable(true);
  }

  private class BootstrapListener extends APIClient.BootstrapListener {
    @Override
    public void onResponse(JSONObject response) {
      StateArray
          .getInstance()
          .setData(response);

      getData();
    }

    @Override
    public void onErrorResponse(VolleyError error) {
    }
  }


  private SettingsListener settingsListener = new SettingsListener() {
    @Override
    public void onSettingsChange() {
      getData();
    }

    @Override
    public void onDialogDetached() {
      getData();
    }
  };
}
