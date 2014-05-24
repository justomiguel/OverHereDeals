//=======================================================================
// ** Copyright © 2011 Kevin Greene **
//=======================================================================
package com.launching.overheredeals.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.launching.overheredeals.Deal;
import com.launching.overheredeals.DealDetailsActivity;

//=======================================================================
public class DealView extends RelativeLayout implements OnClickListener
{
    private final String TAG = "OHD";
    public Deal deal;
    private RelativeLayout.LayoutParams layoutParams = null;
    private TextView shortTitleTextView = null;
    private Activity dealArActivity;
    private double rotation = 0;
    private double oldRotation = 0;
    
    //-----------------------------------------------------------------------
    public DealView(Context context, Deal deal)
    {
        super(context);
        this.deal = deal;
        dealArActivity = (Activity) context;
        layoutParams = new RelativeLayout.LayoutParams(120, 40);
        this.setMinimumWidth(120);
        // this.layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        // layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        // layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        setLayoutParams(layoutParams);
        setOnClickListener(this);
        setPadding(4, 0, 4, 0);
        setBackgroundColor(0xCC121212);
        
        // Initialize the shortTitleTextView.
        shortTitleTextView = new TextView(context);
        shortTitleTextView.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        shortTitleTextView.setText(deal.shortTitle);
        shortTitleTextView.setTextSize(10);
        shortTitleTextView.setMinWidth(120);
        shortTitleTextView.setMaxLines(3);
        
        addView(shortTitleTextView);
    }

    //-----------------------------------------------------------------------
    // Clock-wise rotation in radians.
    public void setRotation(double radians)
    {
        this.rotation = radians;
    }
    
    //-----------------------------------------------------------------------
    // x and y represent the screen position in pixels of the CENTER of the DealView.
    public void setPosition(int x, int y)
    {
        layoutParams.leftMargin = x - getWidth() / 2;
        layoutParams.topMargin = y - getHeight() / 2;
    }
    
    //-----------------------------------------------------------------------
    @Override
    public void onDraw(Canvas canvas)
    {
        // Log.d(TAG, "Deal.onDraw " + deal.shortTitle);

        /*
        // Rotate the View from it's old rotation to its new rotation.
        float oldRotationDegrees = (float) (oldRotation * 180 / Math.PI);
        float rotationDegrees = (float) (rotation * 180 / Math.PI);
        Animation rotateAnimation = new RotateAnimation(oldRotationDegrees, rotationDegrees, getWidth() / 2, getHeight() / 2);

        // Set the animation's parameters.
        rotateAnimation.setDuration(5);                   // duration in ms
        rotateAnimation.setRepeatCount(0);                // -1 = infinite repeated
        rotateAnimation.setRepeatMode(Animation.REVERSE); // reverses each repeat. Doesn't do anything since repeatCount is 0
        rotateAnimation.setFillAfter(true);               // keep rotation after animation
        rotateAnimation.setStartTime(-1);
        this.clearAnimation();
        this.setAnimation(rotateAnimation);
        
        oldRotation = rotation;
        */
        
        super.onDraw(canvas);
        
        /*  // This only rotates the text in the view. Not the view itself.
        canvas.save();
        // Rotate the view around its center.
        canvas.rotate((float) (rotation * 180 / Math.PI), getWidth() / 2, getHeight() / 2);
        super.onDraw(canvas);
        canvas.restore();
        */
    }

    //-----------------------------------------------------------------------
    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // shortTitleTextView.measure(120, 40);
        // setMeasuredDimension(120, 40);
    }
    
    //-----------------------------------------------------------------------
    @Override
    public void onClick(View arg0)
    {
        Log.d(TAG, "Deal Clicked " + deal.dealDescription);
        this.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        
        // Show the DealDetailsActivity.
        Intent intent = new Intent(dealArActivity.getBaseContext(), DealDetailsActivity.class);
        // intent.putExtra("dealDescription", deal.dealDescription);
        // etc putExtras.
        dealArActivity.startActivity(intent);
    }

    //-----------------------------------------------------------------------
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }
}

//=======================================================================
