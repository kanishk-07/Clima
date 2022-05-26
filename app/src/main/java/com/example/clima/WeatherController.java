package com.example.clima;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.PrecomputedText;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.loopj.android.http.*;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

// https://openweathermap.org/current#geo
// this link explains how to make API calls to OWM server.

public class WeatherController extends AppCompatActivity {

    final String WEATHER_URL = "http://api.openweathermap.org/data/2.5/weather";
    final String APP_ID = "<place your API_KEY here>";
    //The APP_ID is API_KEY to my account on open_weather_map.org
    final long MIN_TIME = 5000;
    final float MIN_DISTANCE = 1000;
    final int REQUEST_CODE_FOR_GPS_LOCATION = 123;

    final String LOCATION_PROVIDER = LocationManager.GPS_PROVIDER;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private TextView cityLabel;
    private ImageView weatherImage;
    private TextView temperatureLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_controller_layout);
        cityLabel = (TextView) findViewById(R.id.locationTV);
        weatherImage = (ImageView) findViewById(R.id.weatherSymbolIV);
        temperatureLabel = (TextView) findViewById(R.id.tempTV);
        ImageButton changeCityButton = (ImageButton) findViewById(R.id.changeCityButton);

        changeCityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentToChangeCityLayout = new Intent(WeatherController.this, ChangeCityController.class);
                intentToChangeCityLayout.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intentToChangeCityLayout);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent fromChangeCityToController = getIntent();
        String city = fromChangeCityToController.getStringExtra("NewCity");
        if(city==null)
            getWeatherForCurrentLocation();
        else {
            getWeatherForNewCity(city);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(locationManager!=null)
            locationManager.removeUpdates(locationListener);
    }

    private void getWeatherForNewCity(String city) {
        RequestParams params = new RequestParams();
        params.put("q", city);
        params.put("appId", APP_ID);
        requestQueryFromOpenWeatherMap(params);
    }

    private void getWeatherForCurrentLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                String latitude = String.valueOf(location.getLatitude());
                String longitude = String.valueOf(location.getLatitude());
                // now use James Smith networking library
                // from the documentation we get to know exactly what key & values we must pass.
                // James Smith provides a RequestParams Object to plce all those in form of a dictionay.
                RequestParams params = new RequestParams();
                params.put("lat", latitude);
                params.put("lon", longitude);
                params.put("appid", APP_ID);
                requestQueryFromOpenWeatherMap(params);
            }

            @Override
            public void onProviderDisabled(String Provider) {
                Log.d("Clima", "GPS turned oFF");
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_FOR_GPS_LOCATION);
            return;
        }
        locationManager.requestLocationUpdates(LOCATION_PROVIDER, MIN_TIME, MIN_DISTANCE, locationListener);
        // The location Manager triggers when both (min time && min distance) have changed
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==REQUEST_CODE_FOR_GPS_LOCATION) {
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                Log.d("Clima", "GPS Location Permission Granted");
                getWeatherForCurrentLocation();
            }
            else {
                Log.d("Clima", "GPS Location Permission Denied");
            }
        }
    }

    private void requestQueryFromOpenWeatherMap(RequestParams params) {
        AsyncHttpClient clientToMakeRequest = new AsyncHttpClient();
        clientToMakeRequest.get(WEATHER_URL, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] header, JSONObject response) {
                Log.d("Clima", "OWM response success: "+response.toString());
                WeatherDataModel weatherData = WeatherDataModel.fromJsonToWeatherDataModel(response);
                updateUI(weatherData);
            }
            @Override
            public void onFailure(int statusCode, Header[] header, Throwable e, JSONObject response) {
                Log.d("Clima", "OWM response failed: "+e.toString()+" Status Code: "+String.valueOf(statusCode));
                Toast.makeText(WeatherController.this, "Request Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(WeatherDataModel weatherData) {
        temperatureLabel.setText(weatherData.getTemperature());
        cityLabel.setText(weatherData.getCityName());
        int resourceID = getResources().getIdentifier(weatherData.getIconName(), "drawable", getPackageName());
        weatherImage.setImageResource(resourceID);
    }
}