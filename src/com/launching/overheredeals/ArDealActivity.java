//=======================================================================
// ** Copyright © 2011 Kevin Greene **
//=======================================================================
package com.launching.overheredeals;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;

//=======================================================================
public class ArDealActivity extends Activity
{
	private final String TAG = "OHD";
	
    private CameraPreview cameraPreview;
    private DealOverlayView dealOverlayView;
    
    private Camera camera;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location currentBestLocation = null;
    private Location currentGrouponLocation = null;
    private SensorManager sensorManager;
    private SensorEventListener sensorEventListener;
    private Sensor accelerometer;
    private Sensor magneticFieldSensor;

    private GrouponDealDownloadTask pendingGrouponDealDownloadTask = null;
    
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    
    //-----------------------------------------------------------------------
    @Override
	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        // Force Landscape layout (this is also currently done in the Manifest).
        // Is it a good idea to do this here and in the Manifest file?
        // Nope, doing this here and in the manifest causes the app to crash immediately for some reason.
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR | 
        // 						ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Make the window full-screen and prevent the screen from 'locking' after a period of no activity.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // Create our CameraPreview and set it as the content of our activity.
        // Make sure this is added first, so everything added afterward will show up on top.
        cameraPreview = new CameraPreview(this);
        addContentView(cameraPreview, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        
        // For debugging.
        // TextView testTextView = new TextView(this);
        // testTextView.setText("Test");
        // RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
        //                                                                      RelativeLayout.LayoutParams.WRAP_CONTENT);
        // params.addRule(RelativeLayout.CENTER_IN_PARENT);
        // testTextView.setLayoutParams(params);
        
        // RelativeLayout testLayout = new RelativeLayout(this);
        // testLayout.addView(testTextView);
        // addContentView(testLayout, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        
        // Add the overlay on top of the camera preview.
        dealOverlayView = new DealOverlayView(this);
        // dealOverlayView.addView(testTextView);   // For debugging.
        addContentView(dealOverlayView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        
        // Create sensor manager to track orientation and rotation changes.
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        
        // Initialize orientation listener.
        sensorEventListener = new SensorEventListener()
        {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent)
            {
            	sensorChanged(sensorEvent);
            }
            
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy)
            {}
        };
        
		// Initialize locationManager.
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

		// Define a listener that responds to location updates.
		locationListener = new LocationListener()
		{
			// Called when a new location is found by the network location provider.
		    public void onLocationChanged(Location location)
		    {
				locationChanged(location);
		    }

		    public void onStatusChanged(String provider, int status, Bundle extras)
		    {}

		    public void onProviderEnabled(String provider)
		    {}

		    public void onProviderDisabled(String provider)
		    {}
		};
    }
    
    //-----------------------------------------------------------------------
    @Override
    protected void onResume()
    {
        super.onResume();
        
        // Open the default i.e. the first rear facing camera.
        // camera should always be null when we get here, so this check should always be true.
        // Sometimes open fails, so we need to tell the user what happened and exit.
        if (camera == null) {
            try {
                camera = Camera.open();
                if (camera == null)
                    throw new Exception();
            }
            catch(Exception e) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Cannot open Camera. Make sure no other apps are using your beautiful shiny camera and try again.")
                       .setCancelable(false)
                       .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                                ArDealActivity.this.finish();
                           }
                       });
                builder.create();
            }
        }

        cameraPreview.setCamera(camera);
        dealOverlayView.setCamera(camera);
        
        // Start receiving updates when the phone orientation changes.
        if (sensorManager != null && accelerometer != null && magneticFieldSensor != null)
        {
        	sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        	sensorManager.registerListener(sensorEventListener, magneticFieldSensor, SensorManager.SENSOR_DELAY_UI);
        }
	    
		// Register the listener with the Location Manager to receive location updates.
	    // Can use network (less accurate location but fast), GPS (more accurate location but slow), or both.
	    if (locationManager != null && locationListener != null)
	    {
	    	locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
	    	locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
	    }
    }

    //-----------------------------------------------------------------------
    @Override
    protected void onPause()
    {
        super.onPause();
        
        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (camera != null)
        {
        	cameraPreview.setCamera(null);
        	dealOverlayView.setCamera(null);
            camera.release();
            camera = null;
        }
        
        // Stop updates for sensor changes when the activity goes inactive to save battery.
        sensorManager.unregisterListener(sensorEventListener);
        
        // Stop getting location updates when not in use to save battery.
    	locationManager.removeUpdates(locationListener);
    	
    	// Cancel any pending Groupon Deal requests.
    	if (pendingGrouponDealDownloadTask != null)
    	    pendingGrouponDealDownloadTask.cancel(true);
    	pendingGrouponDealDownloadTask = null;
    }
    
    //-----------------------------------------------------------------------
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        // Ignore orientation/keyboard change.
        super.onConfigurationChanged(newConfig);
    }

    //-----------------------------------------------------------------------
    private void locationChanged(Location location)
    {
    	if (isBetterLocation(location, currentBestLocation)) {
        	// Log.d(TAG, "New best Location received: lat = " + (int)location.getLatitude() + 
    	    //            " lon = " + (int)location.getLongitude() + " accuracy = " + location.getAccuracy());
        	
    		// Save the new best location.
    		currentBestLocation = location;
    		
            // If our new location is significantly different, or if it has been a while, 
            // then we need to get an updated set of deals from Groupon.
            // This will also rate-limit how many requests we send to Groupon.
            if (isSignificantlyDifferentLocation(location, currentGrouponLocation)) {
                // Save the location for next time.
                currentGrouponLocation = location;
                
        	    // Cancel any pending Groupon Deal request.
                if (pendingGrouponDealDownloadTask != null)
                    pendingGrouponDealDownloadTask.cancel(true);
                pendingGrouponDealDownloadTask = null;
                
                // Send the request.
                pendingGrouponDealDownloadTask = new GrouponDealDownloadTask();
                pendingGrouponDealDownloadTask.execute(location);
            }
            
            // Set the location for the dealView so it knows how to draw the deals in 3D space.
            dealOverlayView.setLocation(location);
    	}
    }

    //-----------------------------------------------------------------------
    private float[] magnitudeValues;
    private float[] accelerometerValues;
    
    private void sensorChanged(SensorEvent sensorEvent)
    {
    	switch (sensorEvent.sensor.getType())
    	{
          case Sensor.TYPE_MAGNETIC_FIELD:
        	magnitudeValues = sensorEvent.values.clone();
            break;
          case Sensor.TYPE_ACCELEROMETER:
        	accelerometerValues = sensorEvent.values.clone();
            break;
          default:
        	Log.e(TAG, "Invalid Sensor in sensorChanged");
        	break;
        }

    	// If we've received all the necessary information and a new orientation.
        if (magnitudeValues != null && accelerometerValues != null)
        {
            float[] rotationMatrix = new float[16];
            float[] inclinationMatrix = new float[16];

            SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, 
            								accelerometerValues, magnitudeValues);

            float[] phoneOrientation = new float[3];
            float[] remappedRotationMatrix = new float[16];
            SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, 
            									SensorManager.AXIS_Z, remappedRotationMatrix);
            
            SensorManager.getOrientation(remappedRotationMatrix, phoneOrientation);
            dealOverlayView.setOrientation(phoneOrientation);
        }
    }
    
    //-----------------------------------------------------------------------
    // Determines whether newLocation reading is better than the oldLocation fix.
    protected boolean isBetterLocation(Location newLocation, Location oldLocation)
    {
    	// A new location is always better than no location.
        if (oldLocation == null)
            return true;

        // Check whether the new location fix is newer or older.
        long timeDelta = newLocation.getTime() - oldLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved.
        if (isSignificantlyNewer)
            return true;
        // If the new location is more than two minutes older, it must be worse.
        else if (isSignificantlyOlder)
            return false;

        // Check whether the new location fix is more or less accurate.
        int accuracyDelta = (int) (newLocation.getAccuracy() - oldLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider.
        boolean isFromSameProvider = isSameProvider(newLocation.getProvider(), oldLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy.
        if (isMoreAccurate)
            return true;
        else if (isNewer && !isLessAccurate)
            return true;
        else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider)
            return true;

        return false;
    }
    
    //-----------------------------------------------------------------------
    // Returns true if newLocation is more than 100.0 meters from oldLocation
    // or if it has been more than 2 minutes since the oldLocation was received.
    protected boolean isSignificantlyDifferentLocation(Location newLocation, Location oldLocation)
    {
        // A new location is always better than no location.
        if (oldLocation == null)
            return true;

        // Check whether the new location fix is newer or older.
        long timeDelta = newLocation.getTime() - oldLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        
        double distance = oldLocation.distanceTo(newLocation);
        boolean isSignificantlyDifferent = distance > 100.0;
        
        // If it's been more than two minutes since the current location or if the difference is greater 
        // than 100 meters, use the new location because the user has likely moved.
        // That or we now have a much more accurate location, and the old one was crap.
        if (isSignificantlyNewer || isSignificantlyDifferent)
            return true;

        return false;
    }
    
    //-----------------------------------------------------------------------
    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2)
    {
        if (provider1 == null)
          return provider2 == null;
        return provider1.equals(provider2);
    }

    //=======================================================================
    private class GrouponDealDownloadTask extends AsyncTask<Location, Void, ArrayList<Deal>>
    {
        // The radius from the user's current location to grab deals for (in miles).
        private int radius = 10;
        private final String groupon_client_id = "cd2047437d68888dd07451ce0ad90503a2bcaeea";
        
        /* // Error codes.
        public static final String INVALID_DIVISION = "Invalid Division";
        public static final String RATE_EXCEEDED = "Rate Exceeded";
        public static final String UNAUTHORIZED_REQUEST = "Unauthorized Request";
        public static final String FORBIDDEN = "Forbidden";
        public static final String NOT_FOUND = "Not Found";
        public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
        public static final String SERVICE_UNAVAILABLE = "Service Unavailable";
        public static final String UNDEFINED_RESPONSE = "Undefined Response";
        */
        
        //-----------------------------------------------------------------------
        public GrouponDealDownloadTask()
        {}

        //-----------------------------------------------------------------------
        // Runs in a separate thread. Return value is passed to onPostExecute().
        @Override
        protected ArrayList<Deal> doInBackground(Location... locations)
        {            
            assert(locations.length == 1);
            Location location = locations[0];
            
            try {
                // Create HttpRequest and format the GET request.
                String httpRequestUrl = "http://api.groupon.com/v2/deals.json?client_id=" + groupon_client_id +
                                        "&lat=" + location.getLatitude() +"&lng=" + location.getLongitude() +
                                        "&radius=" + radius;
                
                HttpClient hc = new DefaultHttpClient();
                HttpGet get = new HttpGet(httpRequestUrl);
                
                Log.d(TAG, "Sending Groupon Request: lat=" + location.getLatitude() + " lng=" + 
                      location.getLongitude() + " radius=" + radius);
                
                // Send Http request and wait for response (blocking).
                HttpResponse httpResponse = hc.execute(get);
                
                // If we were cancelled while waiting for the request to finish, just quit now and die.
                if (isCancelled())
                    return null;
                
                if (httpResponse == null) {
                    Log.e(TAG, "Failed to download deals from Groupon");
                    return null;
                }
                
                // Check if message succeeded.
                String response;
                if (grouponResponseIsSuccess(httpResponse)) {
                    try {
                        response = EntityUtils.toString(httpResponse.getEntity());
                    }
                    catch (ParseException e) {
                        Log.e(TAG, "ParseException in Groupon Request");
                        e.printStackTrace();
                        return null;
                    }
                    catch (IOException e) {
                        Log.e(TAG, "IOException in Groupon Request");
                        e.printStackTrace();
                        return null;
                    }
                }
                else if (grouponResponseIsInvalidDivision(httpResponse)) {
                    // TODO Show a message to the user telling him to move to a fucking city or something (i.e. his area is not supported).
                    printGrouponResponseError(httpResponse);
                    return null;
                }
                else {
                    printGrouponResponseError(httpResponse);
                    return null;
                }
                
                // Request succeeded and response string is valid at this point.
                
                // Do the JSON parsing.
                ArrayList<Deal> dealList = parseDealsFromJsonResponse(response);
                
                // A parsing error occurred. An error message was likely printed in parseDealsFromJsonResponse().
                if (dealList == null)
                    return null;
                
                // For debugging.
                Log.d(TAG, "Received " + dealList.size() + " deals");            
                // for (int i = 0; i < dealList.size(); ++i)
                //     dealList.get(i).printDeal();
                
                // Check again if we have been cancelled.
                if (isCancelled())
                    return null;
                
                return dealList;
            }
            catch(IOException e) {
                Log.e(TAG, "Exception in Group DealAsyncTask()", e);
                e.printStackTrace();
            }
            
            return null;
        }
        
        //-----------------------------------------------------------------------
        @Override
        protected void onCancelled()
        {
            // Do nothing if we were cancelled.
        }
        
        //-----------------------------------------------------------------------
        // Runs in the UI thread.
        @Override
        protected void onPostExecute(ArrayList<Deal> dealList)
        {
            // Clear pending task so the next one can be created.
            pendingGrouponDealDownloadTask = null;
            
            // Tell the DealOverlayView about the new deals so it draws them to the screen.
            if (dealList != null)
                dealOverlayView.setDeals(dealList);
            
            /* 
            // TODO Handle too many/not enough deals. Increase/decrease radius and send new Groupon request.
            if (dealList.size() == 0) {
                // Increase radius.
                // If new radius isn't too big, send new request to Groupon.
                // Else, display message saying no deals were found =(.
                return;
            }
            if (dealList.size() < min_num_deals) {
                // Set the deals so at least the few deals we have are displayed for now.
                dealOverlayView.setDeals(dealList);
                
                // Increase radius.
                // If new radius isn't too big, send new request to Groupon.
                
                return;
            }
            if (dealList.size() > max_num_deals) {
                // Set the deals so at least as many deals as will fit are displayed for now.
                // I think it is better not to show it yet. Wait until we have a stable number of deals,
                // so it doesn't look like deals are disappearing.
                // dealOverlayView.setDeals(dealList);
            
                // Decrease radius.
                // If new radius isn't too small, send new request to Groupon.
                // Else, show deals and a notice that some may not be being shown because there are too many.
                    dealOverlayView.setDeals(dealList);
            }
            */
        }
        
        //-----------------------------------------------------------------------
        private ArrayList<Deal> parseDealsFromJsonResponse(String response)
        {
            ArrayList<Deal> dealList = new ArrayList<Deal>();
            JSONObject responseJsonObject;
            JSONArray dealsJsonArray = new JSONArray();
            
            // Parse JSON message and create the ArrayList of deals.
            // Using Groupon's API v2. Documentation here: https://sites.google.com/site/grouponapiv2/api-resources/deals#index-request
            try {
                responseJsonObject = new JSONObject(response);
                dealsJsonArray = responseJsonObject.getJSONArray("deals");
            }
            catch (JSONException e1) {
                Log.e(TAG, "Error creating deals JSON array");
                e1.printStackTrace();
                return null;
            }
            
            for (int i = 0; i < dealsJsonArray.length(); ++i) {
                try {
                    Deal deal = new Deal();
                    JSONObject currentJsonDeal = dealsJsonArray.getJSONObject(i);
                    
                    deal.id = currentJsonDeal.getString("id");
                    deal.longTitle = currentJsonDeal.getString("title");
                    deal.shortTitle = currentJsonDeal.getString("announcementTitle");
                    deal.dealUrl = currentJsonDeal.getString("dealUrl");
                    deal.status = currentJsonDeal.getString("status");
                    deal.smallImageUrl = currentJsonDeal.getString("smallImageUrl");
                    deal.largeImageUrl = currentJsonDeal.getString("largeImageUrl");
                    
                    deal.merchantName = currentJsonDeal.getJSONObject("merchant").getString("name");
                    deal.merchantUrl = currentJsonDeal.getJSONObject("merchant").getString("websiteUrl");
                    deal.merchantHighlights = currentJsonDeal.getString("highlightsHtml");

                    // There is always only one object in the "options" array. Idk why it's an array.
                    JSONObject options = currentJsonDeal.getJSONArray("options").getJSONObject(0);
                    deal.expiresAt = options.getString("expiresAt");
                    deal.price = options.getJSONObject("price").getInt("amount");
                    deal.priceString = options.getJSONObject("price").getString("formattedAmount");
                    deal.value = options.getJSONObject("value").getInt("amount");
                    deal.valueString = options.getJSONObject("value").getString("formattedAmount");
                    deal.discount = options.getJSONObject("discount").getInt("amount");
                    deal.discountString = options.getJSONObject("discount").getString("formattedAmount");
                    deal.discountPercent = options.getDouble("discountPercent");
                    deal.dealDescription = options.getJSONArray("details").getJSONObject(0).getString("description");
                    
                    // Parse the redemptionLocation that is closest to the phone's current location. 
                    Location tmpLocation = new Location("Groupon");
                    int closestLocationIndex = 0;
                    double closestDistance = 1000000000;
                    JSONArray redemptionLocations = options.getJSONArray("redemptionLocations");
                    
                    if (redemptionLocations.length() > 0) {
                        for (int j = 0; j < redemptionLocations.length(); ++j) {
                            tmpLocation.setLatitude(redemptionLocations.getJSONObject(j).getDouble("lat"));
                            tmpLocation.setLongitude(redemptionLocations.getJSONObject(j).getDouble("lng"));
                            double distance = currentBestLocation.distanceTo(tmpLocation);
                            
                            // If this deal is the closest so far, save it as the closest.
                            if (distance < closestDistance) {
                                closestLocationIndex = j;
                                closestDistance = distance;
                            }
                        }
                        
                        // Set deal.location to the closest location we found.
                        tmpLocation.setLatitude(redemptionLocations.getJSONObject(closestLocationIndex).getDouble("lat"));
                        tmpLocation.setLongitude(redemptionLocations.getJSONObject(closestLocationIndex).getDouble("lng"));
                        deal.location = tmpLocation;
                    }
                    
                    dealList.add(deal);
                }
                catch (JSONException e) {
                    Log.e(TAG, "Error Parsing JSON response");
                    e.printStackTrace();
                }
            }

            return dealList;
        }

        //-----------------------------------------------------------------------
        private boolean grouponResponseIsSuccess(HttpResponse httpResponse)
        {
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
                return true;
            return false;
        }

        //-----------------------------------------------------------------------
        private boolean grouponResponseIsInvalidDivision(HttpResponse httpResponse)
        {
            if (httpResponse.getStatusLine().getStatusCode() == 400)
                return true;
            return false;
        }
        
        //-----------------------------------------------------------------------
        private void printGrouponResponseError(HttpResponse httpResponse)
        {
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
                Log.e(TAG, "Groupon Response: 200 Success");
            else if (httpResponse.getStatusLine().getStatusCode() == 400)
                Log.e(TAG, "Groupon Response: 400 Bad Request / Invalid Division");
            else if (httpResponse.getStatusLine().getStatusCode() == 401)
                Log.e(TAG, "Groupon Response: 401 Unauthorized Request");
            else if (httpResponse.getStatusLine().getStatusCode() == 403)
                Log.e(TAG, "Groupon Response: 403 Forbidden");
            else if (httpResponse.getStatusLine().getStatusCode() == 404)
                Log.e(TAG, "Groupon Response: 404 Not Found");
            else if (httpResponse.getStatusLine().getStatusCode() == 420)
                Log.e(TAG, "Groupon Response: 420 Rate Exceeded");
            else if (httpResponse.getStatusLine().getStatusCode() == 500)
                Log.e(TAG, "Groupon Response: 500 Internal Server Error");
            else if (httpResponse.getStatusLine().getStatusCode() == 503)
                Log.e(TAG, "Groupon Response: 503 Service Unavailable");
            else
                Log.e(TAG, "Groupon Response: Undefined Response");
        }
    }
}

//=======================================================================
