package com.tv.avario.widget.adapter;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tv.avario.R;
import com.tv.avario.exception.AvarioException;
import com.tv.avario.util.AssetUtil;
import com.tv.avario.util.EntityUtil;
import com.tv.avario.util.Log;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by aeroheart-c6 on 21/12/2016.
 */
public class ElementAdapter extends RecyclerView.Adapter<ElementAdapter.ViewHolder> {

  public interface OnItemClickListener {
    void onItemClick(int position);
  }

  public static final String TAG            = "Avario/ElementAdapter";
  public static final String BIND_SELECTION = "selection";

  public static final int MODE_HOME    = 1;
  public static final int MODE_CLIMATE = 2;

  private List<Entity> elements;
  private int          itemLayoutId;
  private int          itemWidth;
  private int          mode;

  private OnItemClickListener onItemClickListener;

  private int focusPosition = 0;


  public ElementAdapter(Context context, int pageSize) {
    this.itemLayoutId = R.layout.elementbar__item;
    this.elements = new ArrayList<>();
    this.mode = ElementAdapter.MODE_HOME;

    this.setPageSize(context, pageSize);
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    View view;

    view = inflater.inflate(this.itemLayoutId, parent, false);
    view.setLayoutParams(new RecyclerView.LayoutParams(
        this.itemWidth,
        LinearLayout.LayoutParams.MATCH_PARENT
    ));

    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(ViewHolder holder, final int position, List<Object> payloads) {
    if (payloads.isEmpty()) {
      super.onBindViewHolder(holder, position, payloads);
      return;
    }

    for (Object payload : payloads) {
      if (payload instanceof String && payload.equals(ElementAdapter.BIND_SELECTION)) {
        this.bindSelection(holder, this.elements.get(position));
      }
    }
  }

  @Override
  public void onBindViewHolder(ViewHolder holder, final int position) {
    Entity element = this.elements.get(position);

    this.bindSelection(holder, element);
    if (this.mode == ElementAdapter.MODE_HOME) {
      holder.textTV.setText(element.data.optString("name"));
    } else {
      holder.textTV.setText(element.data.optString(
          "name_climate",
          element.data.optString("name")
      ));
    }

    holder.textTV.setTextColor(
        holder.itemView.getContext().getResources().getColor(R.color.black__60));

        /*try {
            AssetUtil.toDrawable(holder.itemView.getContext(),
                    EntityUtil.getStateIconUrl(holder.itemView.getContext(), element.data),
                    new AssetUtil.ImageViewCallback(holder.iconIV));
        } catch (AvarioException e) {
            e.printStackTrace();
        }*/

    try {
      AssetUtil.toDrawable(
          holder.itemView.getContext(),
          EntityUtil.getStateIconUrl(holder.itemView.getContext(), element.data),
          new AssetUtil.ImageViewCallback(holder.iconIV)
      );
    } catch (AvarioException ignored) {
    }


    holder.itemView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {

        Log.d(TAG, "================ onclick ==================");

        if (onItemClickListener != null) {
          onItemClickListener.onItemClick(position);
        }
      }
    });

    holder.itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
          focusPosition = position;
        }
      }
    });

  }

  public void setOnItemClickListener(
      OnItemClickListener onItemClickListener) {
    this.onItemClickListener = onItemClickListener;
  }

  private void bindSelection(ViewHolder holder, Entity entity) {
    holder.itemView.setSelected(entity.selected);
    holder.itemView.setActivated(EntityUtil.isEntityOn(entity.data));
  }

  public void clearSelected(Entity exception) {
    for (Entity element : this.elements) {
      if (exception != null && element == exception) { continue; }

      element.selected = false;
    }
  }

  private void setPageSize(Context context, int pageSize) {
    int screenWidth = context.getResources().getDisplayMetrics().widthPixels,
        width;

    width = screenWidth / pageSize;
    width = width + (width / 2 / (pageSize - 1));
    this.itemWidth = width;
  }

  public void setMode(int mode) {
    if (mode != ElementAdapter.MODE_HOME &&
        mode != ElementAdapter.MODE_CLIMATE) { mode = ElementAdapter.MODE_HOME; }

    this.mode = mode;
  }

  public int getFocusPosition() {
    return focusPosition;
  }

  @Override
  public int getItemCount() {
    return this.elements.size();
  }

  public int size() {
    return this.elements.size();
  }

  public void clear() {
    this.elements.clear();
  }

  public void append(Entity element) {
    this.elements.add(element);
  }

  public void remove(int index) {
    this.elements.remove(index);
  }

  public Entity get(int index) {
    try {
      return this.elements.get(index);
    } catch (IndexOutOfBoundsException exception) {
      return null;
    }
  }

  /*
   ***********************************************************************************************
   * Inner Classes - SubTypes
   ***********************************************************************************************
   */
  static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
      View.OnFocusChangeListener {
    private TextView  textTV;
    private ImageView iconIV;

    private ViewHolder(View view) {
      super(view);

      this.textTV = (TextView) view.findViewById(R.id.text);
      this.iconIV = (ImageView) view.findViewById(R.id.icon);
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
      Log.d(TAG,
          "================ onFocus ================== " + hasFocus + " " + getAdapterPosition());
    }
  }
}
