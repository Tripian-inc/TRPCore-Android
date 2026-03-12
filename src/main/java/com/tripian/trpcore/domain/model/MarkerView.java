package com.tripian.trpcore.domain.model;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.tripian.trpcore.R;
import com.tripian.trpcore.util.extensions.UtilityKt;

/**
 * Used to create custom map marker views.
 *
 * @author Tripian
 * @version 1.0
 */

public class MarkerView extends RelativeLayout {

    private ImageView iconView;
    private ImageView iconViewBackground;
    private TextView poiOrderTv;
    private boolean isSelected = false;
    private int cityIndex = 0;  // 0 = first city (black), 1+ = secondary cities (primary color)

    public MarkerView(Context context) {
        super(context);

        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        setLayoutParams(layoutParams);
        setBackgroundColor(ContextCompat.getColor(context, R.color.trp_transparent));
        setGravity(Gravity.CENTER);
        setPadding((int) UtilityKt.dp2Px(3),
                (int) UtilityKt.dp2Px(3),
                (int) UtilityKt.dp2Px(3),
                (int) UtilityKt.dp2Px(3));
        setId(View.generateViewId());

        initView(context);
    }

    private void initView(Context context) {

        int padding = (int) UtilityKt.dp2Px(10);
        int size = (int) UtilityKt.dp2Px(40);

        RelativeLayout imageViewRelativeLyt = new RelativeLayout(context);
        LayoutParams imageViewRelativeLytParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        imageViewRelativeLyt.setLayoutParams(imageViewRelativeLytParams);
        imageViewRelativeLytParams.setMargins((int) UtilityKt.dp2Px(8), 0, 0, 0);

        iconView = new ImageView(context);
        LayoutParams leftPoiImageParams = new LayoutParams(size, size);
        iconView.setPadding(padding, padding, padding, padding);
        leftPoiImageParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        iconView.setLayoutParams(leftPoiImageParams);
        iconView.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_marker_black));

        int sizeBackground = (int) UtilityKt.dp2Px(46);
        iconViewBackground = new ImageView(context);
        LayoutParams iconViewParams = new LayoutParams(sizeBackground, sizeBackground);
        iconViewBackground.setPadding(padding, padding, padding, padding);
        iconViewParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        iconViewBackground.setLayoutParams(iconViewParams);
        iconViewBackground.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_map_icon_oval));

        imageViewRelativeLyt.addView(iconViewBackground);
        imageViewRelativeLyt.addView(iconView);

        FrameLayout orderFrameLayout = new FrameLayout(context);
        FrameLayout.LayoutParams orderFrameLayoutParams = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        orderFrameLayout.setLayoutParams(orderFrameLayoutParams);

        poiOrderTv = new TextView(context);
        LayoutParams poiOrderTvParams = new LayoutParams((int) UtilityKt.dp2Px(30), (int) UtilityKt.dp2Px(30f));
        poiOrderTv.setLayoutParams(poiOrderTvParams);
        poiOrderTv.setGravity(Gravity.CENTER);
        poiOrderTv.setTextSize(18);
        poiOrderTv.setTypeface(poiOrderTv.getTypeface(), Typeface.BOLD);
        poiOrderTv.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_marker_red));

        orderFrameLayout.addView(poiOrderTv);

        // Default state: white background with black text
        setSelected(false);

        addView(imageViewRelativeLyt);
        addView(orderFrameLayout);

    }

    /**
     * Sets the city index for the marker.
     * cityIndex 0 = first city (black/white style)
     * cityIndex 1+ = secondary cities (primary color style)
     *
     * @param index the city index (0-based)
     */
    public void setCityIndex(int index) {
        this.cityIndex = index;
        // Re-apply selection state with new city index
        setSelected(isSelected);
    }

    /**
     * Sets the selection state of the marker.
     * For first city (cityIndex == 0):
     *   - Selected: black background with white text
     *   - Unselected: white background with black border and black text
     * For secondary cities (cityIndex > 0):
     *   - Selected: primary color background with white text
     *   - Unselected: white background with primary border and primary text
     *
     * @param selected true if marker is selected, false otherwise
     */
    public void setSelected(boolean selected) {
        this.isSelected = selected;
        if (cityIndex == 0) {
            // First city: black/white style
            if (selected) {
                poiOrderTv.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.bg_marker_red));
                poiOrderTv.setTextColor(Color.WHITE);
            } else {
                poiOrderTv.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.bg_marker_white));
                poiOrderTv.setTextColor(ContextCompat.getColor(getContext(), R.color.trp_black_soft));
            }
        } else {
            // Secondary cities: primary color style
            if (selected) {
                poiOrderTv.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.bg_marker_primary));
                poiOrderTv.setTextColor(Color.WHITE);
            } else {
                poiOrderTv.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.bg_marker_white_primary));
                poiOrderTv.setTextColor(ContextCompat.getColor(getContext(), R.color.trp_primary));
            }
        }
    }

    /**
     * Returns the current selection state of the marker.
     *
     * @return true if marker is selected, false otherwise
     */
    public boolean isMarkerSelected() {
        return isSelected;
    }

    /**
     * After accessing this imageview, the image is set.
     *
     * @return ImageView Returns the imageview that needs to be set for the marker image.
     */
    public ImageView getIconView() {
        return iconView;
    }

    public ImageView getIconViewBackground() {
        return iconViewBackground;
    }

    /**
     * After accessing this textview, the route order is set.
     *
     * @return Returns the textview that needs to be set for the marker text.
     */
    public TextView getPoiOrderTv() {
        return poiOrderTv;
    }
}
