//=======================================================================
// ** Copyright © 2011 Kevin Greene **
//=======================================================================
package com.launching.overheredeals;

import java.util.ArrayList;
import com.launching.overheredeals.views.DealView;

import android.content.Context;
import android.graphics.Canvas;
import android.location.Location;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.hardware.Camera;

//=======================================================================
class DealOverlayView extends RelativeLayout
{
	private static String TAG = "OHD";
    private Context context;
    // private Bitmap mooImAPigBitmap;
    private ArrayList<DealView> dealViews = new ArrayList<DealView>();
    private final int smoothing_factor = 7;
    private Camera camera = null;
    
    private final int AZIMUTH = 0;
    private final int PITCH   = 1;
    private final int ROLL    = 2;
    
    // Stores the phone's global location (i.e. lat and lng).
    private Location currentLocation;

    // A 3D rotational vector that stores the phone's rotation in radians (from the perspective of the camera).
    private float[][] phoneOrientations;
    private int currentOrientationIndex = 0;
    private float[] smoothingWindow;
    
    //-----------------------------------------------------------------------
    DealOverlayView(Context initContext)
    {
        super(initContext);
        this.context = initContext;
        
        // Initialize orientations to [0, 0, 0] to prevent null pointer exceptions in startup race conditions.
        phoneOrientations = new float[smoothing_factor][3];
        
        // ViewGroups disable the onDraw event by default for efficiency. We must explicitly enable it.
        // Use onLayout, not onDraw.
        // setWillNotDraw(false);
        
        // This causes all kinds of weird problems. I.e. text turning dark-grey randomly in DealViews.
        // And it still doesn't fix the problem of the text wrapping at the edge of the screen. WTF!
        // setClipChildren(false);
        
        calculateSmoothingWindow();
        
        /*
        // Initialize Test Bitmap.
		mooImAPigBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.moo_im_a_pig); 
		if (mooImAPigBitmap == null)
			Log.e(TAG, "Loading mooImAPigBitmap failed");
	    */
    }

    //-----------------------------------------------------------------------
    private void calculateSmoothingWindow()
    {
        int smoothingSum = 0;
        smoothingWindow = new float[smoothing_factor];
        
        int i;
        int j = 0;
        for (i = 0; i < smoothing_factor / 2; ++i) {
            j++;
            smoothingWindow[i] = j;
            smoothingSum += j;
        }
        
        // If numPoints is odd we need to add in the middle point.
        if (smoothing_factor % 2 > 0) {
            smoothingWindow[i] = j + 1;
            smoothingSum += j + 1;
            i++;
        }
        
        for (; i < smoothing_factor; ++i) {
            smoothingWindow[i] = j;
            smoothingSum += j;
            j--;
        }
        
        // Normalize each point so that all the points sum together to equal 1.
        for (i = 0; i < smoothing_factor; ++i)
            smoothingWindow[i] /= smoothingSum;
    }
    
    //-----------------------------------------------------------------------
    public void setDeals(ArrayList<Deal> deals)
    {
        // Log.d(TAG, "setDeals size: " + deals.size() + " deals");
        synchronized(dealViews) {
            // Remove the current dealViews from the screen, so they won't keep getting drawn after we delete them.
            this.removeAllViews();
            dealViews.clear();
            
            // Add the new views.
            for (int i = 0; i < deals.size(); ++i) {
                DealView newDealView = new DealView(context, deals.get(i));
                newDealView.setVisibility(View.GONE);
                newDealView.setPosition(0, 0);
                dealViews.add(newDealView);
                this.addView(newDealView);
            }
        }
        requestLayout();
    }
    
    //-----------------------------------------------------------------------
    public void setLocation(Location newLocation)
    {
    	currentLocation = newLocation;
    	// updateDealViews();
    }
    
    //-----------------------------------------------------------------------
    public void setOrientation(float[] newOrientation)
    {        
    	if (newOrientation == null || newOrientation.length != 3) {
    	    Log.e(TAG, "Invalid newOrientation");
    	    return;
    	}
    	
        // Log.d(TAG, "New Orientation [" + newOrientation[0] * 180 / Math.PI + ", " + 
        //                                  newOrientation[1] * 180 / Math.PI + ", " + 
        //                                  newOrientation[2] * 180 / Math.PI + "]");
    	
    	int previousOrientationIndex;
    	if (currentOrientationIndex == 0)
    	    previousOrientationIndex = smoothing_factor - 1;
    	else
    	    previousOrientationIndex = currentOrientationIndex - 1;
    	    
		phoneOrientations[currentOrientationIndex][0] = newOrientation[0];
		phoneOrientations[currentOrientationIndex][1] = newOrientation[1];
		phoneOrientations[currentOrientationIndex][2] = newOrientation[2];
		
		// This will handle the "jumping" from 0 to 360, so the signal is continuous.
		// It needs to be continuous for the case when around 0. You get a set such as: [4, 2, 356, 350, 358, 3].
		// Take the moving average of that set and you'll see what I mean ;)
		while (phoneOrientations[previousOrientationIndex][0] - phoneOrientations[currentOrientationIndex][0] > Math.PI * 1.5)
		    phoneOrientations[currentOrientationIndex][0] += Math.PI * 2;
        while (phoneOrientations[previousOrientationIndex][0] - phoneOrientations[currentOrientationIndex][0] < -(Math.PI * 1.5))
            phoneOrientations[currentOrientationIndex][0] -= Math.PI * 2;
		
		currentOrientationIndex++;
		currentOrientationIndex %= smoothing_factor;

        // TODO Hide the GPS loading message since we now have location information. 
        // if (gpsInvalidTextBox.isVisible())
        //     gpsInvalidTextBox.hide();
		
		requestLayout();    		
    }

    //-----------------------------------------------------------------------
    private float[] getCurrentSmoothedPhoneOrientation()
    {
        float[] smoothedOrientation = new float[3];
        
        // Make sure each component is initialized to 0. I'm pretty sure this isn't necessary but whatever.
        for (int i = 0; i < 3; ++i)
            smoothedOrientation[i] = 0;
        
        // Convolve the signal with the smoothingWindow to smooth it.
        for (int i = 0; i < smoothing_factor; ++i) {
            int orientationIndex = (i + currentOrientationIndex) % smoothing_factor; 
            for (int j = 0; j < 3; ++j)
                smoothedOrientation[j] += phoneOrientations[orientationIndex][j] * smoothingWindow[i];
                // smoothedOrientation[j] += phoneOrientations[orientationIndex][j];
        }

        return smoothedOrientation;
    }
    
    //-----------------------------------------------------------------------
    public void setCamera(Camera newCamera)
    {
        this.camera = newCamera;
        requestLayout();
    }

    //-----------------------------------------------------------------------
    @Override
    protected void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4)
    {
        // Log.d(TAG, "onLayout");
        updateDealViews();
        super.onLayout(arg0, arg1, arg2, arg3, arg4);
    }
    
    //-----------------------------------------------------------------------
    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
    }
    
    //-----------------------------------------------------------------------
    private void updateDealViews()
    {
        if (camera == null)
            return;
        if (currentLocation == null)
            return;
        if (dealViews == null || dealViews.size() == 0)
            return;
        
        float[] phoneOrientation = getCurrentSmoothedPhoneOrientation();
        
        synchronized(dealViews) {
            for (int i = 0; i < dealViews.size(); ++i)
            {
                DealView dealView = dealViews.get(i);
                
                if (dealView.deal.location == null) {
                    dealView.setVisibility(View.GONE);
                    continue;
                }
                
                // Return value of bearintTo() is defined in degrees east of true north in the range [-180, 180].
                // We want it in the range [0, 360]. Also, convert to radians.
                double dealBearing = currentLocation.bearingTo(dealView.deal.location) * Math.PI / 180;
                if (dealBearing < 0)
                    dealBearing += Math.PI * 2;
                
                // The azimuth angle is store in the range [-180, 180].
                // We want it in the range [0, 360]. It's already in radians.
                double phoneAzimuth = phoneOrientation[AZIMUTH];
                while (phoneAzimuth < 0)
                    phoneAzimuth += Math.PI * 2;
                while (phoneAzimuth >= Math.PI * 2)
                    phoneAzimuth -= Math.PI * 2;
                
                // horizontalTheta is the horizontal angle difference between the direction we are facing at 
                // our current location (from GPS) and the location of the deal.
                double horizontalTheta = phoneAzimuth - dealBearing;
                double horizontalThetaDegrees = horizontalTheta * 180 / Math.PI;
                
                // verticalTheta is the vertical angle difference between the direction we are facing and the deal's altitude.
                // Currently the deals are all assumed to be always at the exact same altitude as the phone.
                double verticalTheta = phoneOrientation[PITCH];
                double verticalThetaDegrees = verticalTheta * 180 / Math.PI;
                
                // Only show the deal if it might be visible.
                if (Math.abs(horizontalTheta) > Math.PI / 4 &&
                    Math.PI * 2 - Math.abs(horizontalTheta) > Math.PI / 4) {
                    if (dealView.getVisibility() != View.GONE)
                        dealView.setVisibility(View.GONE);
                    continue;
                }
                
                // Calculate the absolute pixel position to draw each deal according to the following ratio. Solve for horizontalPixelOffset:
                //
                //          horizontalTheta             horizontalPixelOffset
                //     --------------------------  =  -------------------------
                //      horizontalViewAngle / 2           screenWidth / 2
                //
                // Get the x and y components on the screen by multiplying that result with cos(ROLL) and sin(ROLL) appropriately.
                // Calculate verticalPixelOffset similarly using verticalTheta, verticalViewAngle, and screenHeight.
                
                Camera.Parameters camParams = camera.getParameters();
                float horizontalViewAngle = camParams.getHorizontalViewAngle();
                float verticalViewAngle = camParams.getVerticalViewAngle();
                
                int screenWidth = this.getWidth();
                int screenHeight = this.getHeight();
                
                double horizontalThetaXComp = -horizontalThetaDegrees * Math.cos(-phoneOrientation[ROLL] - Math.PI / 2)
                                                                      / (horizontalViewAngle / 2) * screenWidth / 2;
                double horizontalThetaYComp = -horizontalThetaDegrees * Math.sin(-phoneOrientation[ROLL] - Math.PI / 2)
                                                                      / (verticalViewAngle / 2) * screenHeight / 2;
                double verticalThetaXComp   = verticalThetaDegrees    * Math.sin(-phoneOrientation[ROLL] - Math.PI / 2)
                                                                      / (horizontalViewAngle / 2) * screenWidth / 2;
                double verticalThetaYComp   = -verticalThetaDegrees   * Math.cos(-phoneOrientation[ROLL] - Math.PI / 2)
                                                                      / (verticalViewAngle / 2) * screenHeight / 2;
                
                int xPixelOffsetFromCenter = (int) (horizontalThetaXComp + verticalThetaXComp);
                int yPixelOffsetFromCenter = (int) (horizontalThetaYComp + verticalThetaYComp);
    
                int xPixelOffsetAbsolute = screenWidth  / 2 + xPixelOffsetFromCenter;
                int yPixelOffsetAbsolute = screenHeight / 2 + yPixelOffsetFromCenter;
                
                /*
                int xCenter = this.getWidth() / 2;
                int yCenter = this.getHeight() / 2;
                
                int bitmapWidthOffset = mooImAPigBitmap.getWidth() / 2;
                int bitmapHeightOffset = mooImAPigBitmap.getHeight() / 2;
                
                xPixelOffset -= bitmapWidthOffset * Math.cos(-phoneOrientation[ROLL] - Math.PI / 2);
                yPixelOffset += bitmapHeightOffset * Math.sin(-phoneOrientation[ROLL] - Math.PI / 2);
                
                // This is the key to getting the correct viewing perspective. Really? Why?
                // matrix.preTranslate(-xCenter, -yCenter);
                // matrix.postTranslate(xCenter, yCenter); 
                
                // Rotate the bitmap.
                // Create a matrix for the manipulation.
                Matrix matrix = new Matrix();
                
                // Resize the bit map?
                // matrix.postScale(1, 1);
                
                // Rotate the Bitmap so it's always oriented with the top of the image toward the sky.
                matrix.postRotate((float)((-phoneOrientation[ROLL] * 180 / Math.PI) - 90) % 360);
         
                // Recreate the new Bitmap.
                Bitmap rotatedMooImAPigBitmap = Bitmap.createBitmap(mooImAPigBitmap, 0, 0,
                                                           mooImAPigBitmap.getWidth(), 
                                                           mooImAPigBitmap.getHeight(), 
                                                           matrix, true);
                
                // Draw deals to screen.
                Paint paint = new Paint();
        
                canvas.drawBitmap(rotatedMooImAPigBitmap, 
                                  (float)(xCenter + xPixelOffset), 
                                  (float)(yCenter + yPixelOffset), paint);
                */
                
                // Update the dealView's angle and position, then make the dealView visible if it's not already.
                dealView.setRotation(((-phoneOrientation[ROLL]) - Math.PI / 2) % (Math.PI * 2));
                dealView.setPosition(xPixelOffsetAbsolute, yPixelOffsetAbsolute);
                if (dealView.getVisibility() != View.VISIBLE)
                    dealView.setVisibility(View.VISIBLE);
                dealView.invalidate();
            }
        }
    }
}

//=======================================================================
