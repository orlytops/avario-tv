package com.avario.home.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.avario.home.R;
import com.avario.home.core.StateArray;
import com.avario.home.exception.AvarioException;
import com.avario.home.util.PlatformUtil;
import com.avario.home.widget.Dial;
import com.avario.home.widget.DialButtonBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by aeroheart-c6 on 09/12/2016.
 */
public class DialFragment extends Fragment {
    private Dial dial;
    private DialButtonBar dialbtnBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        View root = inflater.inflate(R.layout.fragment__dial, container, false);

        this.dial = (Dial) root.findViewById(R.id.dial);
        this.dialbtnBar = (DialButtonBar) root.findViewById(R.id.dialbuttons);
        this.dialbtnBar.setDial(this.dial);

        return root;
    }

    /*
     ***********************************************************************************************
     * Getters / Setters
     ***********************************************************************************************
     */
    public void setEnabled(boolean enabled) {
        this.dial.setEnabled(enabled);
        this.dialbtnBar.setEnabled(enabled);

    }

    public void setEntities(JSONArray entityIds) {
        StateArray states = StateArray.getInstance();
        List<JSONObject> entities = new ArrayList<>();

        try {
            for (int index = 0, length = entityIds.length(); index < length; index++)
                entities.add(states.getEntity(entityIds.getString(index)));
        } catch (AvarioException exception) {
            PlatformUtil
                    .getErrorToast(this.getContext(), exception)
                    .show();
        } catch (JSONException ignored) {
        }

        this.dial.setup(entities);
        this.dialbtnBar.setup(entities);

        this.dial.initialize();
    }

    public void setEntities(List<String> entityIds) {
        StateArray states = StateArray.getInstance();
        List<JSONObject> entities = new ArrayList<>();

        try {
            for (String entityId : entityIds)
                entities.add(states.getEntity(entityId));
        } catch (AvarioException exception) {
            PlatformUtil
                    .getErrorToast(this.getContext(), exception)
                    .show();
        }

        this.dial.setup(entities);
        this.dialbtnBar.setup(entities);

        this.dial.initialize();
    }

    public void setMediaEntity(String entityId) {
        StateArray states = StateArray.getInstance();
        List<JSONObject> entities = new ArrayList<>();

        try {
            entities.add(states.getMediaEntity(entityId));
        } catch (AvarioException exception) {
            PlatformUtil
                    .getErrorToast(this.getContext(), exception)
                    .show();
        }

        this.dial.setup(entities, Dial.Category.MEDIA, false);
        this.dialbtnBar.setup(entities, true);

        this.dial.initialize();
    }

    public void setVolumeEntity(String entityId) {
        StateArray states = StateArray.getInstance();
        List<JSONObject> entities = new ArrayList<>();

        try {
            entities.add(states.getMediaEntity(entityId));
        } catch (AvarioException exception) {
            PlatformUtil
                    .getErrorToast(this.getContext(), exception)
                    .show();
        }

        this.dial.setup(entities, Dial.Category.VOLUME, false);
        this.dialbtnBar.setup(entities, true);

        this.dial.initialize();
    }

    /**
     * @param entityId button entity id
     */
    public void click(String entityId) {
        if (entityId != null) {
            dialbtnBar.click(entityId);
        }
    }
}
