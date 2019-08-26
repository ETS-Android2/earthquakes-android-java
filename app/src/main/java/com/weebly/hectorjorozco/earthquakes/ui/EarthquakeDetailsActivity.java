package com.weebly.hectorjorozco.earthquakes.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.weebly.hectorjorozco.earthquakes.R;
import com.weebly.hectorjorozco.earthquakes.models.Earthquake;
import com.weebly.hectorjorozco.earthquakes.utils.MapsUtils;
import com.weebly.hectorjorozco.earthquakes.utils.QueryUtils;

import java.text.DecimalFormat;

import static android.view.View.GONE;


public class EarthquakeDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_EARTHQUAKE = "EXTRA_EARTHQUAKE_KEY";
    public static final String EXTRA_BUNDLE_KEY = "EXTRA_BUNDLE_KEY";
    private static final String IS_FAB_MENU_OPEN_VALUE_KEY = "IS_FAB_MENU_OPEN_VALUE_KEY";

    private Earthquake mEarthquake;

    private WebView mUsgsMapWebView;
    private FrameLayout mGoogleMapFrameLayout, mUsgsMapFrameLayout;
    private TextView mUsgsMapNoInternetTextView;
    private RadioGroup mMapTypeRadioGroup;
    private GoogleMap mGoogleMap;
    private SharedPreferences mSharedPreferences;
    private LatLng mEarthquakePosition;
    private int mGoogleMapType;
    private int mMagnitudeColor;
    private int mMagnitudeBackgroundColor;
    private boolean mIsGoogleMap;
    private boolean mUsgsMapLoaded = false;
    private boolean mGoogleMapLoaded = false;
    private boolean mGoogleMapRadioButtonClicked = false;
    private boolean mOnBackPressed = false;
    private boolean mRotation = false;
    private boolean mIsFabMenuOpen = false;

    private String[] romanNumerals = new String[]
            {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_earthquake_details);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Bundle bundle = getIntent().getBundleExtra(EXTRA_BUNDLE_KEY);
        if (bundle!=null && bundle.containsKey(EXTRA_EARTHQUAKE)) {
            mEarthquake = bundle.getParcelable(EXTRA_EARTHQUAKE);
        }

        if (savedInstanceState != null) {
            mRotation = true;
            mIsFabMenuOpen = savedInstanceState.getBoolean(IS_FAB_MENU_OPEN_VALUE_KEY);
        }

        // Gets the values saved on Shared Preferences to set them on the map
        mSharedPreferences = getSharedPreferences(
                getString(R.string.app_shared_preferences_name), 0);
        mGoogleMapType = mSharedPreferences.getInt(getString(
                R.string.google_map_type_shared_preference_key), GoogleMap.MAP_TYPE_NORMAL);
        mIsGoogleMap = mSharedPreferences.getBoolean(getString(
                R.string.earthquake_details_map_type_shared_preference_key), true);

        // Sets up the views that will be animated on entry and exit for Android versions 21 or up
        TextView magnitudeTextView = findViewById(R.id.activity_earthquake_details_magnitude_text_view);
        TextView locationOffsetTextView = findViewById(R.id.activity_earthquake_details_location_offset_text_view);
        TextView locationPrimaryTextView = findViewById(R.id.activity_earthquake_details_location_primary_text_view);
        TextView dateAndTimeTextView = findViewById(R.id.activity_earthquake_details_date_and_time_text_view);

        QueryUtils.EarthquakeColors earthquakeColors = QueryUtils.setupEarthquakeInformationOnViews(
                this, mEarthquake, magnitudeTextView, locationOffsetTextView,
                locationPrimaryTextView, dateAndTimeTextView, null);
        mMagnitudeColor = earthquakeColors.getMagnitudeColor();
        mMagnitudeBackgroundColor = earthquakeColors.getMagnitudeBackgroundColor();

        // If Android version is 21 or up set a transition for the elements in the top of the activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            magnitudeTextView.setTransitionName(
                    getString(R.string.activity_earthquake_details_magnitude_text_view_transition));
            locationOffsetTextView.setTransitionName(
                    getString(R.string.activity_earthquake_details_location_offset_text_view_transition));
            locationPrimaryTextView.setTransitionName(
                    getString(R.string.activity_earthquake_details_location_primary_text_view_transition));
            dateAndTimeTextView.setTransitionName(
                    getString(R.string.activity_earthquake_details_date_text_view_transition));

            getWindow().setSharedElementEnterTransition(
                    TransitionInflater.from(this).inflateTransition(R.transition.move));

            Transition sharedElementEnterTransition = getWindow().getSharedElementEnterTransition();
            sharedElementEnterTransition.addListener(new Transition.TransitionListener() {
                @Override
                public void onTransitionStart(Transition transition) {
                }

                @Override
                public void onTransitionEnd(Transition transition) {
                    if (!mOnBackPressed) {
                        setupEarthquakeDetails();
                    }
                }

                @Override
                public void onTransitionCancel(Transition transition) {
                }

                @Override
                public void onTransitionPause(Transition transition) {
                }

                @Override
                public void onTransitionResume(Transition transition) {
                }
            });
        } else {
            setupEarthquakeDetails();
        }

        // After a rotation set up the earthquake details again because "onTransitionEnd" will
        // not be called.
        if (mRotation) {
            setupEarthquakeDetails();
        }
    }


    /**
     * Sets up all the views that are not animated on this Activity.
     */
    private void setupEarthquakeDetails() {

        // Used to assign vector drawables to the Google Map layers fab in API 19
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        setupDetailsOnTopOfMap();

        setupMapsViews();

        setupGoogleMapFabs();

    }


    @SuppressLint("ClickableViewAccessibility")
    private void setupDetailsOnTopOfMap() {

        findViewById(R.id.activity_earthquake_details_on_top_of_map_linear_layout).setVisibility(View.VISIBLE);

        mMapTypeRadioGroup = findViewById(R.id.activity_earthquake_details_map_type_radio_group);
        mMapTypeRadioGroup.setVisibility(View.VISIBLE);

        // Intensity views
        TextView intensityLabelTextView =
                findViewById(R.id.activity_earthquake_details_intensity_label_text_view);
        intensityLabelTextView.setTextColor(mMagnitudeColor);

        LinearLayout intensityValuesLinearLayout = findViewById(R.id.activity_earthquake_details_intensity_values_linear_layout);
        LinearLayout estimatedIntensityLinearLayout =
                findViewById(R.id.activity_earthquake_details_estimated_intensity_linear_layout);
        TextView estimatedLabelTextView =
                findViewById(R.id.activity_earthquake_details_estimated_label_text_view);
        estimatedLabelTextView.setTextColor(mMagnitudeColor);
        TextView estimatedValueTextView =
                findViewById(R.id.activity_earthquake_details_estimated_value_text_view);
        estimatedValueTextView.setTextColor(mMagnitudeColor);
        LinearLayout reportedIntensityLinearLayout =
                findViewById(R.id.activity_earthquake_details_reported_intensity_linear_layout);
        TextView reportedLabelTextView =
                findViewById(R.id.activity_earthquake_details_reported_label_text_view);
        reportedLabelTextView.setTextColor(mMagnitudeColor);
        TextView reportedValueTextView =
                findViewById(R.id.activity_earthquake_details_reported_value_text_view);
        reportedValueTextView.setTextColor(mMagnitudeColor);

        Log.d("TESTING", "Estimated " + mEarthquake.getMmi() + " - Reported " + mEarthquake.getCdi() + " - Alert " + mEarthquake.getAlert() + " - Tsunami " + mEarthquake.getTsunami() + " - Felt " + mEarthquake.getFelt());

        int roundedMmi = (int) Math.round(mEarthquake.getMmi());
        int roundedCdi = (int) Math.round(mEarthquake.getCdi());
        if (roundedMmi < 1 && roundedCdi <1) {
            intensityLabelTextView.setVisibility(GONE);
            intensityValuesLinearLayout.setVisibility(GONE);
        } else {
            if (roundedMmi<1){
                estimatedIntensityLinearLayout.setVisibility(GONE);
            } else {
                estimatedValueTextView.setText(romanNumerals[roundedMmi]);
            }
            if (roundedCdi<1){
                reportedIntensityLinearLayout.setVisibility(GONE);
            } else {
                reportedValueTextView.setText(romanNumerals[roundedCdi]);
            }
        }

        // Alert Views
        LinearLayout alertLinearLayout = findViewById(R.id.activity_earthquake_details_alert_linear_layout);
        TextView alertLabelTextView =
                findViewById(R.id.activity_earthquake_details_alert_label_text_view);
        alertLabelTextView.setTextColor(mMagnitudeColor);
        TextView alertValueTextView =
                findViewById(R.id.activity_earthquake_details_alert_value_text_view);

        String alertText = mEarthquake.getAlert();
        if (alertText==null){
            alertLinearLayout.setVisibility(GONE);
        } else {
            int alertValueTextColor = 0;
            String alertTextValueText = "";
            switch (alertText){
                case "green":
                    alertTextValueText = getString(R.string.activity_earthquake_details_green_text);
                    alertValueTextColor = getResources().getColor(R.color.colorAlertGreen);
                    break;
                case "yellow":
                    alertTextValueText = getString(R.string.activity_earthquake_details_yellow_text);
                    alertValueTextColor = getResources().getColor(R.color.colorAlertYellow);
                    break;
                case "orange":
                    alertTextValueText = getString(R.string.activity_earthquake_details_orange_text);
                    alertValueTextColor = getResources().getColor(R.color.colorAlertOrange);
                    break;
                case "red":
                    alertTextValueText = getString(R.string.activity_earthquake_details_red_text);
                    alertValueTextColor = getResources().getColor(R.color.colorAlertRed);
                    break;
            }
            alertValueTextView.setText(alertTextValueText);
            alertValueTextView.setTextColor(alertValueTextColor);
        }

        // Tsunami views
        TextView tsunamiTextView =
                findViewById(R.id.activity_earthquake_details_tsunami_text_view);
        tsunamiTextView.setTextColor(mMagnitudeColor);

        if (mEarthquake.getTsunami()==0){
            tsunamiTextView.setVisibility(GONE);
        }

        // Felt views
        TextView feltReportsLabelTextView =
                findViewById(R.id.activity_earthquake_details_felt_reports_label_text_view);
        feltReportsLabelTextView.setTextColor(mMagnitudeColor);
        TextView feltReportsValueTextView =
                findViewById(R.id.activity_earthquake_details_felt_reports_value_text_view);
        feltReportsValueTextView.setTextColor(mMagnitudeColor);

        feltReportsValueTextView.setText(String.valueOf(mEarthquake.getFelt()));

        int[][] states = new int[][]{
                new int[]{android.R.attr.state_enabled}, // enabled
                new int[]{-android.R.attr.state_enabled}, // disabled
                new int[]{-android.R.attr.state_checked}, // unchecked
                new int[]{android.R.attr.state_pressed}  // pressed
        };
        int[] magnitudeBackGroundColors = new int[]{
                mMagnitudeBackgroundColor,
                mMagnitudeBackgroundColor,
                mMagnitudeBackgroundColor,
                mMagnitudeBackgroundColor,
        };
        int[] magnitudeColors = new int[]{
                mMagnitudeColor,
                mMagnitudeColor,
                mMagnitudeColor,
                mMagnitudeColor,
        };
        ColorStateList magnitudeBackGroundColorStateList = new ColorStateList(states, magnitudeBackGroundColors);
        ColorStateList magnitudeColorStateList = new ColorStateList(states, magnitudeColors);

        MaterialButton feltReportsButton = findViewById(R.id.activity_earthquake_details_felt_reports_button);
        feltReportsButton.setTextColor(mMagnitudeColor);
        feltReportsButton.setRippleColor(magnitudeBackGroundColorStateList);
        feltReportsButton.setStrokeColor(magnitudeColorStateList);

        // Location views
        TextView coordinatesAndDepthLabelTextView =
                findViewById(R.id.activity_earthquake_details_coordinates_and_depth_label_text_view);
        coordinatesAndDepthLabelTextView.setTextColor(mMagnitudeColor);

        TextView coordinatesValueTextView =
                findViewById(R.id.activity_earthquake_details_coordinates_value_text_view);

        double latitude, longitude;
        String latitudeLetter, longitudeLetter;

        if (mEarthquake.getLatitude() < 0) {
            latitude = mEarthquake.getLatitude() * -1;
            latitudeLetter = getString(R.string.activity_earthquake_details_south_latitude_letter);
        } else {
            latitude = mEarthquake.getLatitude();
            latitudeLetter = getString(R.string.activity_earthquake_details_north_latitude_letter);
        }

        if (mEarthquake.getLongitude() < 0) {
            longitude = mEarthquake.getLongitude() * -1;
            longitudeLetter = getString(R.string.activity_earthquake_details_west_longitude_letter);
        } else {
            longitude = mEarthquake.getLongitude();
            longitudeLetter = getString(R.string.activity_earthquake_details_east_longitude_letter);
        }

        coordinatesValueTextView.setText(getString(R.string.activity_earthquake_details_coordinates_text,
                latitude, latitudeLetter, longitude, longitudeLetter, mEarthquake.getDepth()));
        coordinatesValueTextView.setTextColor(mMagnitudeColor);
    }


    private void setupMapsViews() {
        mUsgsMapWebView = findViewById(R.id.activity_earthquake_details_usgs_map_web_view);
        mUsgsMapFrameLayout = findViewById(R.id.activity_earthquake_details_usgs_map_frame_layout);
        mGoogleMapFrameLayout = findViewById(R.id.activity_earthquake_details_google_map_frame_layout);
        mUsgsMapNoInternetTextView = findViewById(R.id.activity_earthquake_details_usgs_map_no_internet_text_view);

        if (mIsGoogleMap) {
            mMapTypeRadioGroup.check(R.id.activity_earthquake_details_google_map_type_radio_button);
            showGoogleMap();
        } else {
            mMapTypeRadioGroup.check(R.id.activity_earthquake_details_usgs_map_type_radio_button);
            showUsgsMap();
        }

        mUsgsMapNoInternetTextView.setOnClickListener(v -> showUsgsMap());

        mMapTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.activity_earthquake_details_google_map_type_radio_button) {
                showGoogleMap();
                mIsGoogleMap = true;
                mGoogleMapRadioButtonClicked = true;
            } else {
                showUsgsMap();
                mIsGoogleMap = false;
            }
        });
    }


    private void setupGoogleMapFabs() {
        LinearLayout layoutFabLayer1 =
                findViewById(R.id.activity_earthquake_details_google_map_fab_layer_1_linear_layout);
        LinearLayout layoutFabLayer2 =
                findViewById(R.id.activity_earthquake_details_google_map_fab_layer_2_linear_layout);
        LinearLayout layoutFabLayer3 =
                findViewById(R.id.activity_earthquake_details_google_map_fab_layer_3_linear_layout);
        FloatingActionButton layersFab =
                findViewById(R.id.activity_earthquake_details_google_map_layers_fab);
        FloatingActionButton earthquakeLocationFab =
                findViewById(R.id.activity_earthquake_details_google_map_earthquake_location_fab);

        View fabsBackgroundLayout = findViewById(R.id.activity_earthquake_details_google_map_fabs_background);

        if (mIsFabMenuOpen) {
            MapsUtils.showFabMenu(layoutFabLayer1, layoutFabLayer2, layoutFabLayer3,
                    fabsBackgroundLayout, layersFab, this);
        }

        layersFab.setOnClickListener(view -> {
            if (!mIsFabMenuOpen) {
                mIsFabMenuOpen = true;
                MapsUtils.showFabMenu(layoutFabLayer1, layoutFabLayer2, layoutFabLayer3,
                        fabsBackgroundLayout, layersFab, this);
            } else {
                mIsFabMenuOpen = false;
                MapsUtils.hideFabMenu(layoutFabLayer1, layoutFabLayer2, layoutFabLayer3,
                        fabsBackgroundLayout, layersFab, this);
            }
        });

        earthquakeLocationFab.setOnClickListener(v ->
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(mEarthquakePosition)));

        findViewById(R.id.activity_earthquake_details_google_map_fab_layer_1).setOnClickListener(v -> {
            mGoogleMapType = GoogleMap.MAP_TYPE_NORMAL;
            if (mGoogleMap != null && mGoogleMap.getMapType() != mGoogleMapType) {
                mGoogleMap.setMapType(mGoogleMapType);
            }
        });

        findViewById(R.id.activity_earthquake_details_google_map_fab_layer_2).setOnClickListener(v -> {
            mGoogleMapType = GoogleMap.MAP_TYPE_HYBRID;
            if (mGoogleMap != null && mGoogleMap.getMapType() != mGoogleMapType) {
                mGoogleMap.setMapType(mGoogleMapType);
            }
        });

        findViewById(R.id.activity_earthquake_details_google_map_fab_layer_3).setOnClickListener(v -> {
            mGoogleMapType = GoogleMap.MAP_TYPE_TERRAIN;
            if (mGoogleMap != null && mGoogleMap.getMapType() != mGoogleMapType) {
                mGoogleMap.setMapType(mGoogleMapType);
            }
        });

        fabsBackgroundLayout.setOnClickListener(view -> {
            mIsFabMenuOpen = false;
            MapsUtils.hideFabMenu(layoutFabLayer1,
                    layoutFabLayer2, layoutFabLayer3, fabsBackgroundLayout, layersFab, this);
        });
    }


    @SuppressLint("SetJavaScriptEnabled")
    private void showUsgsMap() {
        mUsgsMapFrameLayout.setVisibility(View.VISIBLE);
        mGoogleMapFrameLayout.setVisibility(GONE);
        if (!QueryUtils.internetConnection(this)) {
            mUsgsMapWebView.setVisibility(GONE);
            mUsgsMapNoInternetTextView.setVisibility(View.VISIBLE);
        } else {
            mUsgsMapNoInternetTextView.setVisibility(GONE);
            mUsgsMapWebView.setVisibility(View.VISIBLE);
            if (!mUsgsMapLoaded) {
                mUsgsMapWebView.getSettings().setJavaScriptEnabled(true);
                mUsgsMapWebView.getSettings().setDomStorageEnabled(true);
                mUsgsMapWebView.setWebChromeClient(new WebChromeClient());
                mUsgsMapWebView.loadUrl(mEarthquake.getUrl() + "/map");
                mUsgsMapLoaded = true;
            }
        }
    }


    private void showGoogleMap() {
        mUsgsMapWebView.setVisibility(GONE);
        mUsgsMapNoInternetTextView.setVisibility(GONE);
        mUsgsMapFrameLayout.setVisibility(GONE);
        mGoogleMapFrameLayout.setVisibility(View.VISIBLE);
        if (!mGoogleMapLoaded) {
            SupportMapFragment earthquakeDetailsMapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.activity_earthquake_details_google_map_fragment);
            if (earthquakeDetailsMapFragment != null) {
                earthquakeDetailsMapFragment.getMapAsync(this);
            }
            mGoogleMapLoaded = true;
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {

        mGoogleMap = googleMap;

        DecimalFormat formatter = new DecimalFormat("0.0");

        mEarthquakePosition = new LatLng(mEarthquake.getLatitude(), mEarthquake.getLongitude());

        String magnitudeToDisplay = formatter.format(mEarthquake.getMagnitude());
        magnitudeToDisplay = magnitudeToDisplay.replace(',', '.');
        double roundedMagnitude = Double.parseDouble(magnitudeToDisplay);

        MapsUtils.MarkerAttributes markerAttributes = MapsUtils.getMarkerAttributes(roundedMagnitude);

        googleMap.addMarker(new MarkerOptions()
                .position(mEarthquakePosition)
                .title(MapsUtils.constructEarthquakeTitleForMarker(mEarthquake, magnitudeToDisplay))
                .snippet(MapsUtils.constructEarthquakeSnippetForMarker(mEarthquake.getTimeInMilliseconds()))
                .icon(BitmapDescriptorFactory.fromResource(markerAttributes.getMarkerImageResourceId()))
                .anchor(0.5f, 0.5f)
                .alpha(markerAttributes.getAlphaValue())
                .zIndex(markerAttributes.getZIndex()));

        googleMap.setMapType(mGoogleMapType);

        googleMap.getUiSettings().setZoomControlsEnabled(true);

        if (mGoogleMapRadioButtonClicked) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mEarthquakePosition, 6.7f));
            mGoogleMapRadioButtonClicked = false;
        } else if (!mRotation) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mEarthquakePosition, 6.7f));
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_earthquake_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case android.R.id.home:
                // Set this flag to true to prevent reloading USGS web view onTransitionEnd callback
                mOnBackPressed = true;
                // Fade out view
                mUsgsMapWebView.animate().alpha(0.0f);
                onBackPressed();
                break;
            case R.id.menu_activity_earthquake_details_action_help:
                Intent intent = new Intent(this, EarthquakesInformationActivity.class);
                startActivity(intent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_FAB_MENU_OPEN_VALUE_KEY, mIsFabMenuOpen);
    }

    /**
     * When the activity is destroyed save the values of the map preferences so
     * they can be restored when the activity is started again.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(getString(R.string.google_map_type_shared_preference_key),
                mGoogleMapType);
        editor.putBoolean(getString(R.string.earthquake_details_map_type_shared_preference_key),
                mIsGoogleMap);
        editor.apply();
    }
}

