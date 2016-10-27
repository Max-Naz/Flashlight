package max_naz.flashlightapp;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;


import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SoundPool.OnLoadCompleteListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    //Variables
    private int sound;
    private SoundPool soundPool;
    private Camera camera;
    private Camera.Parameters parameters;
    private CheckBox cbFon, cbStart, cbSound;
    private SharedPreferences sharedPref;
    private ImageButton btnSettings, btnOnOff, btnExit, btnFrontLed;
    private boolean isPressedBtnOnOff;
    private boolean isPressedSettings = false;
    private boolean isPressedBtnFrontLed = false;
    private AudioManager audioManager;
    private WindowManager.LayoutParams params;
    private RelativeLayout backgroundMain;
    private int brightness = 128;

    //CONSTANTS
    private final String CHECK_BOX_STATE_FON = "savedCheckBoxStateFon";
    private final String CHECK_BOX_STATE_START = "savedCheckBoxStateStart";
    private final String CHECK_BOX_STATE_SOUND = "savedCheckBoxStateSound";
    private final String TAG = "Flashlight_Debug";

    //--------------------------------------LIFECYCLE METHODS------------------------------------//

    //ON CREATE
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        params = getWindow().getAttributes();
        backgroundMain = (RelativeLayout) findViewById(R.id.activity_main);

        Log.d(TAG, "onCreate() Start");
        checkCameraFlash();

        //Work with buttons
        btnSettings = (ImageButton) findViewById(R.id.btn_settings);
        btnOnOff = (ImageButton) findViewById(R.id.btn_on_off);
        btnExit = (ImageButton) findViewById(R.id.btn_exit);
        btnFrontLed = (ImageButton) findViewById(R.id.btn_front_led);

        btnSettings.setOnClickListener(this);
        btnOnOff.setOnClickListener(this);
        btnExit.setOnClickListener(this);
        btnFrontLed.setOnClickListener(this);

        //Work with sound for buttons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            createSoundPoolWithBuilder();
        } else {
            createSoundPoolWithConstructor();
        }
        soundPool.setOnLoadCompleteListener(this);
        sound = soundPool.load(this, R.raw.click, 1);

        //Work with check box
        cbFon = (CheckBox) findViewById(R.id.check_box_fon);
        cbStart = (CheckBox) findViewById(R.id.check_box_start);
        cbSound = (CheckBox) findViewById(R.id.check_box_sound);

        cbFon.setOnCheckedChangeListener(this);
        cbStart.setOnCheckedChangeListener(this);
        cbSound.setOnCheckedChangeListener(this);

        loadCheckBoxState();

        //Check Box Fon
        if (cbFon.isChecked()) {
            cbFon.setTextColor(getResources().getColor(R.color.textOn));
        }

        //Check Box Sound
        if (cbSound.isChecked()) {
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            cbSound.setTextColor(getResources().getColor(R.color.textOn));
        }

        //Check Box Start
        if (cbStart.isChecked()) {
            cbStart.setTextColor(getResources().getColor(R.color.textOn));
        }

        Log.d(TAG, "onCreate() End");
    }

    //ON RESUME
    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume()");
        try {
            if (camera == null) {
                cameraOpen();
            }
            if (cbStart.isChecked()) {
                setFlashLightOn();
                isPressedBtnOnOff = true;
            } else {
                btnOnOff.setImageResource(R.drawable.ic_flashlight_off_90px);
                isPressedBtnOnOff = false;
            }

        } catch (Exception e) {
            Log.d(TAG, "Exception in onResume() " + e.getMessage());
            e.printStackTrace();
        }

        if (isPressedSettings) {
            cbFon.setVisibility(View.VISIBLE);
            cbStart.setVisibility(View.VISIBLE);
            cbSound.setVisibility(View.VISIBLE);
        } else {
            cbFon.setVisibility(View.GONE);
            cbStart.setVisibility(View.GONE);
            cbSound.setVisibility(View.GONE);
        }
    }

    //ON PAUSE
    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG, "onPause()");
        if (!cbFon.isChecked()) {
            releaseCameraAndPreview();
        }
    }

    //ON STOP
    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "onStop()");
        if (!cbFon.isChecked()) {
            releaseCameraAndPreview();
        }
        saveCheckBoxState();
    }

    //ON RESTART
    @Override
    protected void onRestart() {
        super.onRestart();

        Log.d(TAG, "onRestart() Start");

        try {
            releaseCameraAndPreview();
            cameraOpen();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                createSoundPoolWithBuilder();
            } else {
                createSoundPoolWithConstructor();
            }

            soundPool.setOnLoadCompleteListener(this);
            sound = soundPool.load(this, R.raw.click, 1);

            //Check Boxes
            cbFon = (CheckBox) findViewById(R.id.check_box_fon);
            cbStart = (CheckBox) findViewById(R.id.check_box_start);
            cbSound = (CheckBox) findViewById(R.id.check_box_sound);

            loadCheckBoxState();

            //Check Box Fon
            if (cbFon.isChecked()) {
                cbFon.setTextColor(getResources().getColor(R.color.textOn));
            } else {
                cbFon.setTextColor(getResources().getColor(R.color.textOff));
            }

            //Check Box Start
            if (cbStart.isChecked()) {
                cbStart.setTextColor(getResources().getColor(R.color.textOn));
            } else {
                cbStart.setTextColor(getResources().getColor(R.color.textOff));
            }

            //Check Box Start
            if (cbSound.isChecked()) {
                cbSound.setTextColor(getResources().getColor(R.color.textOn));
            } else {
                cbSound.setTextColor(getResources().getColor(R.color.textOff));
            }

            if (isPressedSettings) {
                cbFon.setVisibility(View.VISIBLE);
                cbStart.setVisibility(View.VISIBLE);
                cbSound.setVisibility(View.VISIBLE);
            } else {
                cbFon.setVisibility(View.GONE);
                cbStart.setVisibility(View.GONE);
                cbSound.setVisibility(View.GONE);
            }

            Log.d(TAG, "onRestart() End");
        } catch (Exception e) {
            Log.d(TAG, "Exception in onRestart() " + e.getMessage());
            e.printStackTrace();
        }
    }

    //ON DESTROY
    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy()");
        saveCheckBoxState();
        releaseCameraAndPreview();
    }

    //------------------------------------------METHODS------------------------------------------//

    //OnClick for buttons
    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            //Settings button
            case R.id.btn_settings:
                soundPool.play(sound, 1, 1, 0, 0, 1);
                if (isPressedSettings) {
                    cbFon.setVisibility(View.GONE);
                    cbStart.setVisibility(View.GONE);
                    cbSound.setVisibility(View.GONE);
                    btnSettings.setImageResource(R.drawable.ic_settings_off_55px);
                    isPressedSettings = false;
                } else {
                    cbFon.setVisibility(View.VISIBLE);
                    cbStart.setVisibility(View.VISIBLE);
                    cbSound.setVisibility(View.VISIBLE);
                    btnSettings.setImageResource(R.drawable.ic_settings_on_55px);
                    isPressedSettings = true;
                }
                break;

            //Button On/Off
            case R.id.btn_on_off:
                if (isPressedBtnOnOff) {
                    soundPool.play(sound, 1, 1, 0, 0, 1);
                    if (isPressedBtnFrontLed) {
                        setFrontLedLightOff();
                        isPressedBtnOnOff = false;
                    } else {
                        setFlashLightOff();
                        isPressedBtnOnOff = false;
                    }
                } else {
                    soundPool.play(sound, 1, 1, 0, 0, 1);
                    if (isPressedBtnFrontLed) {
                        setFrontLedLightOn();
                        isPressedBtnOnOff = true;
                    } else {
                        setFlashLightOn();
                        isPressedBtnOnOff = true;
                    }
                }
                break;

            //Button Front Led
            case R.id.btn_front_led:
                soundPool.play(sound, 1, 1, 0, 0, 1);
                if (isPressedBtnFrontLed) {
                    btnFrontLed.setImageResource(R.drawable.ic_front_led_off_34px);
                    isPressedBtnFrontLed = false;
                } else {
                    setFlashLightOff();
                    btnFrontLed.setImageResource(R.drawable.ic_front_led_on_34px);
                    isPressedBtnOnOff = false;
                    isPressedBtnFrontLed = true;
                }
                break;

            //Button Exit App
            case R.id.btn_exit:
                soundPool.play(sound, 1, 1, 0, 0, 1);
                btnExit.setImageResource(R.drawable.ic_exit_to_app_on);
                finish();
                break;
        }
    }

    //Set Front Led On
    private void setFrontLedLightOn() {
        //Get System Brightness
        try {
            brightness = android.provider.Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        //Set Max Brightness
        android.provider.Settings.System.putInt(getContentResolver(),
                android.provider.Settings.System.SCREEN_BRIGHTNESS, 255);
        //params.screenBrightness = 1;
        getWindow().setAttributes(params);
        //Set Elements Screen Invisible and Set Background White. Set Img for Btn On/Off
        btnFrontLed.setVisibility(View.GONE);
        btnExit.setVisibility(View.GONE);
        btnSettings.setVisibility(View.GONE);
        cbSound.setVisibility(View.GONE);
        cbStart.setVisibility(View.GONE);
        cbFon.setVisibility(View.GONE);
        btnOnOff.setImageResource(R.drawable.ic_flashlight_on_off_black_90px);
        backgroundMain.setBackgroundColor(getResources().getColor(R.color.white_background));
    }

    //Set Front Led Off
    private void setFrontLedLightOff() {

        android.provider.Settings.System.putInt(getContentResolver(),
                android.provider.Settings.System.SCREEN_BRIGHTNESS, brightness);

        btnFrontLed.setVisibility(View.VISIBLE);
        btnExit.setVisibility(View.VISIBLE);
        btnSettings.setVisibility(View.VISIBLE);
        if (isPressedSettings) {
            cbFon.setVisibility(View.VISIBLE);
            cbStart.setVisibility(View.VISIBLE);
            cbSound.setVisibility(View.VISIBLE);
        } else {
            cbFon.setVisibility(View.GONE);
            cbStart.setVisibility(View.GONE);
            cbSound.setVisibility(View.GONE);
        }

        btnOnOff.setImageResource(R.drawable.ic_flashlight_off_90px);
        backgroundMain.setBackgroundColor(getResources().getColor(R.color.background_main_activity));
    }

    //Checks Camera Flash on phone and show the alert message if phone have no camera flash
    private void checkCameraFlash() {
        Log.d(TAG, "checkCameraFlash()");
        try {
            boolean isCameraFlash = getApplicationContext().getPackageManager().hasSystemFeature(
                    PackageManager.FEATURE_CAMERA_FLASH);

            if (isCameraFlash) {
                cameraOpen();
            } else {
                showCameraAlert();
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception in checkCameraFlash() " + e.getMessage());
            e.printStackTrace();
        }
    }

    //Create Alert Message
    private void showCameraAlert() {
        Log.d(TAG, "showCameraAlert()");
        new AlertDialog.Builder(this)
                .setTitle(R.string.error_title)
                .setMessage(R.string.error_text)
                .setPositiveButton(R.string.exit_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
    }

    //Open Camera
    private boolean cameraOpen() {
        Log.d(TAG, "cameraOpen()");
        boolean camOpened = false;
        try {
            releaseCameraAndPreview();
            camera = Camera.open();
            camOpened = (camera != null);
        } catch (Exception e) {
            Log.d(TAG, "Exception in cameraOpen() - failed to open Camera " + e.getMessage());
            e.printStackTrace();
        }
        return camOpened;
    }

    //Create Sound Pool for API 21 and higher
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void createSoundPoolWithBuilder() {
        Log.d(TAG, "createSoundPoolWithBuilder");
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .setMaxStreams(1)
                .build();
    }

    // Create Deprecation Sound Pool for API 20 and lower
    @SuppressWarnings("deprecation")
    private void createSoundPoolWithConstructor() {
        Log.d(TAG, "createSoundPoolWithConstructor");
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
    }

    //Save Check Box State
    public void saveCheckBoxState() {
        Log.d(TAG, "saveCheckBoxState()");
        try {
            sharedPref = getPreferences(MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(CHECK_BOX_STATE_FON, cbFon.isChecked());
            editor.commit();
            editor.putBoolean(CHECK_BOX_STATE_START, cbStart.isChecked());
            editor.commit();
            editor.putBoolean(CHECK_BOX_STATE_SOUND, cbSound.isChecked());
            editor.commit();
            Log.d(TAG, "saveCheckBoxState() - data is save");
        } catch (Exception e) {
            Log.d(TAG, "Exception in saveCheckBoxState() - data is NOT save" + e.getMessage());
            e.printStackTrace();
        }
    }

    //Load Check Box State
    public void loadCheckBoxState() {
        Log.d(TAG, "loadCheckBoxState()");
        try {
            sharedPref = getPreferences(MODE_PRIVATE);

            boolean savedStateFon = sharedPref.getBoolean(CHECK_BOX_STATE_FON, false);
            cbFon.setChecked(savedStateFon);

            boolean savedStateStart = sharedPref.getBoolean(CHECK_BOX_STATE_START, false);
            cbStart.setChecked(savedStateStart);

            boolean savedStateSound = sharedPref.getBoolean(CHECK_BOX_STATE_SOUND, false);
            cbSound.setChecked(savedStateSound);

            Log.d(TAG, "loadCheckBoxState() - data is load");
        } catch (Exception e) {
            Log.d(TAG, "Exception in loadCheckBoxState() - data is NOT load");
            e.printStackTrace();
        }
    }

    //Turn ON Camera flash on the phone (without sound)
    private void setFlashLightOn() {
        Log.d(TAG, "setFlashLightOnWithoutSound()");
        btnOnOff.setImageResource(R.drawable.ic_flashlight_on_90px);
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (camera != null) {
                        parameters = camera.getParameters();
                        if (parameters != null) {
                            List supportedFlashModes = parameters.getSupportedFlashModes();
                            if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                            } else if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
                                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                            } else camera = null;
                            if (camera != null) {
                                camera.setParameters(parameters);
                                camera.startPreview();
                                //For Nexus 5
                                try {
                                    camera.setPreviewTexture(new SurfaceTexture(0));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }).start();
        } catch (Exception e) {
            Log.d(TAG, "Exception in setFlashLightOn() " + e.getMessage());
            e.printStackTrace();
        }
    }

    //Turn OFF Camera flash on the phone
    private void setFlashLightOff() {
        Log.d(TAG, "setFlashLightOff()");
        btnOnOff.setImageResource(R.drawable.ic_flashlight_off_90px);
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (camera != null) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        camera.setParameters(parameters);
                        camera.stopPreview();
                    }
                }
            }).start();
        } catch (Exception e) {
            Log.d(TAG, "Exception in setFlashLightOff() " + e.getMessage());
            e.printStackTrace();
        }
    }

    //Release Camera
    private void releaseCameraAndPreview() {
        Log.d(TAG, "releaseCameraAndPreview()");

        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    //On Load Sound Pool
    @Override
    public void onLoadComplete(SoundPool soundPool, int i, int i1) {
    }

    //On Checked Changed Listener
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        switch (buttonView.getId()) {
            //Check Box Fon
            case R.id.check_box_fon:
                if (cbFon.isChecked()) {
                    cbFon.setTextColor(getResources().getColor(R.color.textOn));
                } else {
                    cbFon.setTextColor(getResources().getColor(R.color.textOff));
                }
                break;

            //Check Box Sound
            case R.id.check_box_sound:
                if (cbSound.isChecked()) {
                    audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                    cbSound.setTextColor(getResources().getColor(R.color.textOn));
                } else {
                    audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                    cbSound.setTextColor(getResources().getColor(R.color.textOff));
                }
                break;

            //Check Box Start
            case R.id.check_box_start:
                if (cbStart.isChecked()) {
                    cbStart.setTextColor(getResources().getColor(R.color.textOn));
                } else {
                    cbStart.setTextColor(getResources().getColor(R.color.textOff));
                }
                break;
        }
    }
}