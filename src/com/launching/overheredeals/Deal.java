//=======================================================================
// ** Copyright © 2011 Kevin Greene **
//=======================================================================
package com.launching.overheredeals;

import android.graphics.Bitmap;
import android.location.Location;
import android.util.Log;

//=======================================================================
public class Deal
{
	private final String TAG = "OHD";
	
	public String id;
	public String status;
	public Location location;
	public String longTitle;
	public String shortTitle;
	public String dealDescription;
	public String dealUrl;
	
	public String smallImageUrl;
	public String largeImageUrl;
	public Bitmap smallImage;
	public Bitmap largeImage;
	
	public String expiresAt;
	public int price;
	public String priceString;
	public int value;
	public String valueString;
	public int discount;
	public String discountString;
	public double discountPercent;
	
	public String merchantName;
	public String merchantHighlights;
	public String merchantUrl;
	
	//-----------------------------------------------------------------------
	public Deal()
	{}
	
	//-----------------------------------------------------------------------
	public void printDeal()
	{
		Log.d(TAG, "------------------------------------");
		if (id != null)
			Log.d(TAG, "id : " + id);
		if (status != null)
			Log.d(TAG, "status : " + status);
		if (location != null)
			Log.d(TAG, "location : lat=" + location.getLatitude() + " lng=" + location.getLongitude());
		if (longTitle != null)
			Log.d(TAG, "longTitle : " + longTitle);
		if (shortTitle != null)
			Log.d(TAG, "shortTitle : " + shortTitle);
		if (dealDescription != null)
			Log.d(TAG, "dealDescription : " + dealDescription);
		if (dealUrl != null)
			Log.d(TAG, "dealUrl : " + dealUrl);
		if (smallImageUrl != null)
			Log.d(TAG, "smallImageUrl : " + smallImageUrl);
		if (largeImageUrl != null)
			Log.d(TAG, "largeImageUrl : " + largeImageUrl);
		if (expiresAt != null)
			Log.d(TAG, "expiresAt : " + expiresAt);
		Log.d(TAG, "price : " + price);
		if (priceString != null)
			Log.d(TAG, "priceString : " + priceString);
		Log.d(TAG, "value : " + value);
		if (valueString != null)
			Log.d(TAG, "valueString : " + valueString);
		Log.d(TAG, "discount : " + discount);
		if (discountString != null)
			Log.d(TAG, "discountString : " + discountString);
		Log.d(TAG, "discountPercent : " + discountPercent);
		if (merchantName != null)
			Log.d(TAG, "merchantName : " + merchantName);
		if (merchantHighlights != null)
			Log.d(TAG, "merchantHighlights : " + merchantHighlights);
		if (merchantUrl != null)
			Log.d(TAG, "merchantUrl : " + merchantUrl);
		Log.d(TAG, "------------------------------------");
	}
}

//=======================================================================