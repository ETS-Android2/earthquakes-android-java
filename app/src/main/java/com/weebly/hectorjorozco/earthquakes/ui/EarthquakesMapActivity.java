package com.weebly.hectorjorozco.earthquakes.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.weebly.hectorjorozco.earthquakes.R;
import com.weebly.hectorjorozco.earthquakes.models.Earthquake;
import com.weebly.hectorjorozco.earthquakes.utils.MapsUtils;
import com.weebly.hectorjorozco.earthquakes.utils.QueryUtils;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;


public class EarthquakesMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String IS_FAB_MENU_OPEN_VALUE_KEY = "IS_FAB_MENU_OPEN_VALUE_KEY";

    private GoogleMap mGoogleMap;
    private SharedPreferences mSharedPreferences;
    private int mGoogleMapType;
    private boolean mIsFabMenuOpen = false;
    private boolean mRotation = false;
    private List<Earthquake> mEarthquakes;
    private boolean mShowMap;
    private BottomNavigationView mBottomNavigationView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_earthquakes_map);

        mEarthquakes = QueryUtils.getEarthquakesList();

        if (mEarthquakes != null) {
            mShowMap = mEarthquakes.size() > 0;
        } else {
            mShowMap = false;
        }

        TextView noDatatextView = findViewById(R.id.activity_earthquakes_map_text_view);
        FrameLayout mMapLayout = findViewById(R.id.activity_earthquakes_map_frame_layout);

        setupBottomNavigationView();

        if (mShowMap) {

            noDatatextView.setVisibility(View.GONE);
            mMapLayout.setVisibility(View.VISIBLE);

            if (savedInstanceState != null) {
                mRotation = true;
                mIsFabMenuOpen = savedInstanceState.getBoolean(IS_FAB_MENU_OPEN_VALUE_KEY);
            }

            // Gets the values saved on Shared Preferences to set them on the map
            mSharedPreferences = getSharedPreferences(
                    getString(R.string.app_shared_preferences_name), 0);
            mGoogleMapType = mSharedPreferences.getInt(getString(
                    R.string.activity_earthquakes_map_google_map_type_shared_preference_key), GoogleMap.MAP_TYPE_NORMAL);

            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.activity_earthquakes_map);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            }

            // Sets up the Google Map FABs
            LinearLayout layoutFabLayer1 = findViewById(R.id.activity_earthquakes_map_fab_layer_1_linear_layout);
            LinearLayout layoutFabLayer2 = findViewById(R.id.activity_earthquakes_map_fab_layer_2_linear_layout);
            LinearLayout layoutFabLayer3 = findViewById(R.id.activity_earthquakes_map_fab_layer_3_linear_layout);
            FloatingActionButton layersFab = findViewById(R.id.activity_earthquakes_map_layers_fab);
            View fabBackgroundLayout = findViewById(R.id.activity_earthquakes_map_fab_background);

            if (mIsFabMenuOpen) {
                MapsUtils.showFabMenu(layoutFabLayer1, layoutFabLayer2, layoutFabLayer3,
                        fabBackgroundLayout, layersFab, this);
            }

            layersFab.setOnClickListener(view -> {
                if (!mIsFabMenuOpen) {
                    mIsFabMenuOpen = true;
                    MapsUtils.showFabMenu(layoutFabLayer1, layoutFabLayer2, layoutFabLayer3,
                            fabBackgroundLayout, layersFab, this);
                } else {
                    mIsFabMenuOpen = false;
                    MapsUtils.hideFabMenu(layoutFabLayer1, layoutFabLayer2, layoutFabLayer3,
                            fabBackgroundLayout, layersFab, this);
                }
            });

            findViewById(R.id.activity_earthquakes_map_fab_1).setOnClickListener(v -> {
                mGoogleMapType = GoogleMap.MAP_TYPE_NORMAL;
                if (mGoogleMap != null && mGoogleMap.getMapType() != mGoogleMapType) {
                    mGoogleMap.setMapType(mGoogleMapType);
                    saveGoogleMapTypeOnSharedPreferences(mGoogleMapType);
                }
            });

            findViewById(R.id.activity_earthquakes_map_fab_2).setOnClickListener(v -> {
                mGoogleMapType = GoogleMap.MAP_TYPE_HYBRID;
                if (mGoogleMap != null && mGoogleMap.getMapType() != mGoogleMapType) {
                    mGoogleMap.setMapType(mGoogleMapType);
                    saveGoogleMapTypeOnSharedPreferences(mGoogleMapType);
                }
            });

            findViewById(R.id.activity_earthquakes_map_fab_3).setOnClickListener(v -> {
                mGoogleMapType = GoogleMap.MAP_TYPE_TERRAIN;
                if (mGoogleMap != null && mGoogleMap.getMapType() != mGoogleMapType) {
                    mGoogleMap.setMapType(mGoogleMapType);
                    saveGoogleMapTypeOnSharedPreferences(mGoogleMapType);
                }
            });

            fabBackgroundLayout.setOnClickListener(view -> {
                mIsFabMenuOpen = false;
                MapsUtils.hideFabMenu(layoutFabLayer1,
                        layoutFabLayer2, layoutFabLayer3, fabBackgroundLayout, layersFab, this);
            });

        } else {
            noDatatextView.setVisibility(View.VISIBLE);
            mMapLayout.setVisibility(View.GONE);
        }
    }


    private void setupBottomNavigationView(){
        mBottomNavigationView = findViewById(R.id.activity_earthquakes_map_bottom_navigation_view);
        mBottomNavigationView.setOnNavigationItemSelectedListener(menuItem -> {
            switch (menuItem.getItemId()){
                case R.id.menu_activity_main_bottom_navigation_view_action_list:
                    onBackPressed();
                    break;
                case R.id.menu_activity_main_bottom_navigation_view_action_map:
                    break;
                case R.id.menu_activity_main_bottom_navigation_view_action_favorites:
                    onBackPressed();
                    startActivity(new Intent(EarthquakesMapActivity.this, FavoritesActivity.class));
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    break;
                case R.id.menu_activity_main_bottom_navigation_view_action_news:
                    break;
            }
            return true;
        });
    }


    private void saveGoogleMapTypeOnSharedPreferences(int googleMapType) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(getString(R.string.activity_earthquakes_map_google_map_type_shared_preference_key),
                googleMapType);
        editor.apply();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mGoogleMap = googleMap;

        Earthquake earthquake;
        LatLng earthquakePosition, firstEarthquakePosition;
        DecimalFormat formatter = new DecimalFormat("0.0");

        for (int i = 0; i < mEarthquakes.size(); i++) {

            earthquake = mEarthquakes.get(i);

            earthquakePosition = new LatLng(earthquake.getLatitude(), earthquake.getLongitude());

            String magnitudeToDisplay = formatter.format(earthquake.getMagnitude());
            magnitudeToDisplay = magnitudeToDisplay.replace(',', '.');
            double roundedMagnitude = Double.parseDouble(magnitudeToDisplay);

            MapsUtils.MarkerAttributes markerAttributes = MapsUtils.getMarkerAttributes(roundedMagnitude);

            googleMap.addMarker(new MarkerOptions()
                    .position(earthquakePosition)
                    .title(MapsUtils.constructEarthquakeTitleForMarker(earthquake, magnitudeToDisplay, this))
                    .snippet(MapsUtils.constructEarthquakeSnippetForMarker(earthquake.getTimeInMilliseconds()))
                    .icon(BitmapDescriptorFactory.fromResource(markerAttributes.getMarkerImageResourceId()))
                    .anchor(0.5f, 0.5f)
                    .alpha(markerAttributes.getAlphaValue())
                    .zIndex(markerAttributes.getZIndex())).setTag(i);
        }

        googleMap.setOnInfoWindowClickListener(marker -> {

            Intent intent = new Intent(EarthquakesMapActivity.this, EarthquakeDetailsActivity.class);
            Bundle bundle = new Bundle();
            bundle.putParcelable(EarthquakeDetailsActivity.EXTRA_EARTHQUAKE, mEarthquakes.get((int) marker.getTag()));
            bundle.putByte(EarthquakeDetailsActivity.EXTRA_CALLER, EarthquakeDetailsActivity.EARTHQUAKE_MAP_ACTIVITY_CALLER);
            intent.putExtra(EarthquakeDetailsActivity.EXTRA_BUNDLE_KEY, bundle);

            startActivity(intent);
            overridePendingTransition(R.anim.slide_up, R.anim.no_animation);
        });

        googleMap.setMapType(mGoogleMapType);
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        // Animate the camera only when the activity is created, not after a rotation
        if (!mRotation) {
            firstEarthquakePosition = new LatLng(mEarthquakes.get(0).getLatitude(), mEarthquakes.get(0).getLongitude());
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(firstEarthquakePosition));
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_earthquakes_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        if (item.getItemId() == R.id.menu_activity_earthquakes_map_action_help) {

            showHelpSnackBar();
        }
        return super.onOptionsItemSelected(item);
    }


    private void showHelpSnackBar(){
        int stringID, numberOfEarthquakes;
        if (QueryUtils.sMoreThanMaximumNumberOfEarthquakesForMap) {
            stringID = R.string.activity_earthquakes_map_max_number_exceeded_message;
            numberOfEarthquakes = MainActivity.MAX_NUMBER_OF_EARTHQUAKES_FOR_MAP;
        } else {
            stringID = R.string.activity_earthquakes_map_number_of_earthquakes_shown_message;
            numberOfEarthquakes = mEarthquakes.size();
        }
        String message = getString(stringID,
                String.format(Locale.getDefault(), "%,d",
                        numberOfEarthquakes));
        Snackbar.make(findViewById(R.id.activity_earthquakes_map_coordinator_layout),message,
                Snackbar.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBottomNavigationView.setSelectedItemId(R.id.menu_activity_main_bottom_navigation_view_action_map);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mShowMap) {
            outState.putBoolean(IS_FAB_MENU_OPEN_VALUE_KEY, mIsFabMenuOpen);
        }
    }

}


