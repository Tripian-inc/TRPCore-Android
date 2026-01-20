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

    public MarkerView(Context context) {
        super(context);

        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        setLayoutParams(layoutParams);
        setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
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
        LayoutParams poiOrderTvParams = new LayoutParams((int) UtilityKt.dp2Px(20), (int) UtilityKt.dp2Px(20f));
        poiOrderTv.setLayoutParams(poiOrderTvParams);
        poiOrderTv.setGravity(Gravity.CENTER);
        poiOrderTv.setTextSize(12);
        poiOrderTv.setTypeface(poiOrderTv.getTypeface(), Typeface.BOLD);
        poiOrderTv.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_marker_red));

        orderFrameLayout.addView(poiOrderTv);

        poiOrderTv.setTextColor(Color.WHITE);

        addView(imageViewRelativeLyt);
        addView(orderFrameLayout);

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
