package com.cuet.wrongroutechecker;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MainActivity extends FragmentActivity implements
		GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener,
		com.google.android.gms.location.LocationListener {

	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

	private Location mLastLocation;

	// Google client to interact with Google API
	private GoogleApiClient mGoogleApiClient;

	private LocationRequest mLocationRequest;

	// Location updates intervals in second
	private static int UPDATE_INTERVAL = 5000; // 1000 = 1 second
	private static int FATEST_INTERVAL = 2000; // 1000 = 1 second
	@SuppressWarnings("unused")
	private static int DISPLACEMENT = 1; // 10 meters

	GoogleMap map;
	Marker sourceMarker, destinationMarker;
	Double srcLat, srcLng, desLat, desLng;
	int destinationMarkerCheck = 0;
	List<LatLng> polyList;
	
	TextView locationStatus;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		locationStatus = (TextView) findViewById(R.id.locationStatus);

		if (!isMapAvailable()) {
			Toast.makeText(getApplicationContext(),
					"Sorry google play service is not installed",
					Toast.LENGTH_LONG).show();
		} else {
			map = ((SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map)).getMap();
			if (map == null)
				Toast.makeText(getApplicationContext(), "Map can't be created",
						Toast.LENGTH_LONG).show();
			else {
				map.setMyLocationEnabled(true);
			}

		}

		// First we need to check availability of play services
		if (checkPlayServices()) {

			// Building the GoogleApi client
			buildGoogleApiClient();
			displayMyLocation();
		}

		map.setOnMapClickListener(new OnMapClickListener() {

			@Override
			public void onMapClick(LatLng desPoint) {
				// TODO Auto-generated method stub
				if (destinationMarkerCheck == 0) {
					try {
						LatLng desLatLag = desPoint;
						destinationMarker = map.addMarker(new MarkerOptions()
								.position(desLatLag).title("Destination")
								.snippet("Click here"));
						destinationMarker.setIcon(BitmapDescriptorFactory
								.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
						destinationMarker.showInfoWindow();
						map.moveCamera(CameraUpdateFactory.newLatLng(desLatLag));
						// map.animateCamera(CameraUpdateFactory.zoomTo(12.0f));
						destinationMarkerCheck = 1;
						desLat = desPoint.latitude;
						desLng = desPoint.longitude;
						String URL = makeURL(srcLat, srcLng, desLat, desLng);
						new connectAsyncTask(URL).execute();
					} catch (Exception e) {

					}
				} else {
					Toast.makeText(getApplicationContext(),
							"Destination already present ", Toast.LENGTH_SHORT)
							.show();
				}
			}
		});
	}

	public boolean isMapAvailable() {
		int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (result == ConnectionResult.SUCCESS) {
			return true;

		} else if (GooglePlayServicesUtil.isUserRecoverableError(result)) {
			Dialog d = GooglePlayServicesUtil.getErrorDialog(result, this, 1);
			d.show();
		}

		else {
			Toast.makeText(getApplicationContext(),
					"Google Play Service is not installed", Toast.LENGTH_LONG)
					.show();
			finish();
		}
		return false;
	}

	/**
	 * Creating google api client object
	 * */
	protected synchronized void buildGoogleApiClient() {
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API).build();
	}

	/**
	 * Method to verify google play services on the device
	 * */
	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, this,
						PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Toast.makeText(getApplicationContext(),
						"This device is not supported. ", Toast.LENGTH_LONG)
						.show();
				finish();
			}
			return false;
		}
		return true;
	}

	protected void createLocationRequest() {
		mLocationRequest = new LocationRequest();
		mLocationRequest.setInterval(UPDATE_INTERVAL);
		mLocationRequest.setFastestInterval(FATEST_INTERVAL);
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		// mLocationRequest.setSmallestDisplacement(DISPLACEMENT); // 1 meters
	}

	protected void startLocationUpdates() {

		LocationServices.FusedLocationApi.requestLocationUpdates(
				mGoogleApiClient, mLocationRequest, this);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void displayMyLocation() {

		if (sourceMarker != null) {
			sourceMarker.remove();

		}

		mLastLocation = LocationServices.FusedLocationApi
				.getLastLocation(mGoogleApiClient);

		if (mLastLocation != null) {
			try {
				double latitude = mLastLocation.getLatitude();
				double longitude = mLastLocation.getLongitude();
				LatLng myLatLag = new LatLng(latitude, longitude);
				sourceMarker = map.addMarker(new MarkerOptions()
						.position(myLatLag).title("My position")
						.snippet("Click here"));
				sourceMarker.setIcon(BitmapDescriptorFactory
						.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
				sourceMarker.showInfoWindow();
				map.moveCamera(CameraUpdateFactory.newLatLng(myLatLag));
				// map.animateCamera(CameraUpdateFactory.zoomTo(Camera.));
				srcLat = latitude;
				srcLng = longitude;
				//checking sourceMarker is on polyline
				if (destinationMarker != null) {
					boolean isOnline=PolyUtil.isLocationOnPath(myLatLag, polyList, true,1); // 1 meter tolerance
					if(isOnline){
						locationStatus.setText("On Line");
					}else{
						locationStatus.setText("Not On Line");
					}
				}
			} catch (Exception e) {

			}

		} else {

		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (mGoogleApiClient != null) {
			mGoogleApiClient.connect();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		checkPlayServices();
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnected(Bundle arg0) {
		// TODO Auto-generated method stub
		// Once connected with google api, get the location
		Toast.makeText(getApplicationContext(), "On connected called", Toast.LENGTH_SHORT).show();
		displayMyLocation();
		createLocationRequest();
		startLocationUpdates();
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		// TODO Auto-generated method stub
		mGoogleApiClient.connect();
	}

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub

		mLastLocation = location;

		Toast.makeText(
				getApplicationContext(),
				"Location changed!" + "Latitude = " + location.getLatitude()
						+ " Longitude = " + location.getLongitude(),
				Toast.LENGTH_SHORT).show();

		// Displaying the new location on UI
		displayMyLocation();

	}

	public String makeURL(double sourcelat, double sourcelog, double destlat,
			double destlog) {
		StringBuilder urlString = new StringBuilder();
		urlString.append("http://maps.googleapis.com/maps/api/directions/json");
		urlString.append("?origin=");// from
		urlString.append(Double.toString(sourcelat));
		urlString.append(",");
		urlString.append(Double.toString(sourcelog));
		urlString.append("&destination=");// to
		urlString.append(Double.toString(destlat));
		urlString.append(",");
		urlString.append(Double.toString(destlog));
		urlString.append("&sensor=false&mode=driving&alternatives=true");
		return urlString.toString();
	}

	public void polyLineDraw(String result) {

		try {
			// Transform the string into a json object
			final JSONObject json = new JSONObject(result);
			JSONArray routeArray = json.getJSONArray("routes");
			JSONObject routes = routeArray.getJSONObject(0);
			JSONObject overviewPolylines = routes
					.getJSONObject("overview_polyline");
			String encodedString = overviewPolylines.getString("points");
			List<LatLng> list = decodePoly(encodedString);
			polyList = list;

			for (int z = 0; z < list.size() - 1; z++) {
				LatLng src = list.get(z);
				LatLng dest = list.get(z + 1);
				@SuppressWarnings("unused")
				Polyline line = map.addPolyline(new PolylineOptions()
						.add(new LatLng(src.latitude, src.longitude),
								new LatLng(dest.latitude, dest.longitude))
						.width(2).color(Color.BLUE).geodesic(true));
			}

		} catch (JSONException e) {

		}
	}

	private List<LatLng> decodePoly(String encoded) {

		List<LatLng> poly = new ArrayList<LatLng>();
		int index = 0, len = encoded.length();
		int lat = 0, lng = 0;

		while (index < len) {
			int b, shift = 0, result = 0;
			do {
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lat += dlat;

			shift = 0;
			result = 0;
			do {
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lng += dlng;

			LatLng p = new LatLng((((double) lat / 1E5)),
					(((double) lng / 1E5)));
			poly.add(p);
		}

		return poly;
	}

	private class connectAsyncTask extends AsyncTask<Void, Void, String> {
		private ProgressDialog progressDialog;
		String url;

		connectAsyncTask(String urlPass) {
			url = urlPass;
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			progressDialog = new ProgressDialog(MainActivity.this);
			progressDialog.setMessage("Fetching route, Please wait...");
			progressDialog.setIndeterminate(true);
			progressDialog.show();
		}

		@Override
		protected String doInBackground(Void... params) {
			JSONParser jParser = new JSONParser();
			String json = jParser.getJSONFromUrl(url);
			return json;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			progressDialog.hide();
			if (result != null) {
				polyLineDraw(result);
			}
		}
	}
}
