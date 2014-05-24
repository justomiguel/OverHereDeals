//=======================================================================
// ** Copyright © 2011 Kevin Greene **
//=======================================================================
package com.launching.overheredeals;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

//=======================================================================
class CameraPreview extends SurfaceView implements SurfaceHolder.Callback
{
	private static String TAG = "OHD";
    private SurfaceHolder mHolder;
    private Camera camera;
    private List<Size> mSupportedPreviewSizes;
    
    //-----------------------------------------------------------------------
    CameraPreview(Context context)
    {
        super(context);
        
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    //-----------------------------------------------------------------------
    public void setCamera(Camera newCamera)
    {
        camera = newCamera;
        
        if (camera != null)
        {
            mSupportedPreviewSizes = camera.getParameters().getSupportedPreviewSizes();
            
            try
            {
            	camera.setPreviewDisplay(mHolder);
            }
            catch (IOException e)
            {
            	Log.e(TAG, "IOException caused by setPreviewDisplay() in setCamera()", e);
            }
            
            requestLayout();
        }
    }
    
    //-----------------------------------------------------------------------
    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h)
    {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes)
        {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff)
            {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null)
        {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes)
            {
                if (Math.abs(size.height - targetHeight) < minDiff)
                {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        
        return optimalSize;
    }
    
    //-----------------------------------------------------------------------
    public void surfaceCreated(SurfaceHolder holder)
    {
        // The Surface has been created, tell the camera where to draw.
        try
        {
        	if (camera != null)
        		camera.setPreviewDisplay(holder);
        }
        catch (IOException e)
        {
        	Log.e(TAG, "IOException caused by setPreviewDisplay()", e);
        }
    }

    //-----------------------------------------------------------------------
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        // Surface will be destroyed when we return, so stop the preview.
    	if (camera != null)
    		camera.stopPreview();
    }

    //-----------------------------------------------------------------------
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h)
    {
    	Log.d(TAG, "Setting camera parameters: w = " + w + " h = " + h);
    	
    	if (camera != null)
    	{
	        // Now that the size is known, set up the camera parameters and begin the preview.
    		Size previewSize = getOptimalPreviewSize(mSupportedPreviewSizes, w, h);
    		
	        Camera.Parameters parameters = camera.getParameters();
	        parameters.setPreviewSize(previewSize.width, previewSize.height);
	        camera.setParameters(parameters);
	        camera.startPreview();
    	}
    }
}

//=======================================================================
