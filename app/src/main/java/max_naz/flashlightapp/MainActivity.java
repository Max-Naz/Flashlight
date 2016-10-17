package max_naz.flashlightapp;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SoundPool.OnLoadCompleteListener {

    private int sound;
    private SoundPool soundPool;
    private Camera camera;
    private Camera.Parameters parameters;
    private Switch aSwitch;
    private CheckBox cbFon;
    private SharedPreferences sharedPref;
    private ImageButton btnExitApp;

    //CONSTANTS
    private final String CHECK_BOX_STATE = "savedCheckBoxState";
    private final String TAG = "Flashlight_Debug";

    //--------------------------------------LIFECYCLE METHODS------------------------------------//

    //ON CREATE
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate Start");

        checkCameraFlash();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            createSoundPoolWithBuilder();
        } else {
            createSoundPoolWithConstructor();
        }

        soundPool.setOnLoadCompleteListener(this);
        sound = soundPool.load(this, R.raw.click, 1);

        aSwitch = (Switch) findViewById(R.id.switsh_on_off);
        aSwitch.setChecked(true);
        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    setFlashLightOn();
                } else {
                    setFlashLightOff();
                }
            }
        });

        cbFon = (CheckBox) findViewById(R.id.check_box_fon);
        loadCheckBoxState();
        cbFon.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (cbFon.isChecked()) {
                    Toast.makeText(MainActivity.this, "В даном режиме камера становится недоступна", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnExitApp = (ImageButton) findViewById(R.id.btnExit);
        btnExitApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


        Log.d(TAG, "onCreate End");
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
            setFlashLightOnWithoutSound();
            aSwitch.setChecked(true);
        } catch (Exception e) {
            Log.d(TAG, "Exception in onResume() " + e.getMessage());
            e.printStackTrace();
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

            aSwitch = (Switch) findViewById(R.id.switsh_on_off);
            aSwitch.setChecked(true);
            aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b) {
                        setFlashLightOn();
                    } else {
                        setFlashLightOff();
                    }
                }
            });

            cbFon = (CheckBox) findViewById(R.id.check_box_fon);
            loadCheckBoxState();

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
        boolean qOpened = false;
        try {
            releaseCameraAndPreview();
            camera = Camera.open();
            qOpened = (camera != null);
        } catch (Exception e) {
            Log.d(TAG, "Exception in cameraOpen() - failed to open Camera " + e.getMessage());
            e.printStackTrace();
        }
        return qOpened;
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
            editor.putBoolean(CHECK_BOX_STATE, cbFon.isChecked());
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
            boolean savedState = sharedPref.getBoolean(CHECK_BOX_STATE, false);
            cbFon.setChecked(savedState);
            Log.d(TAG, "loadCheckBoxState() - data is load");
        } catch (Exception e) {
            Log.d(TAG, "Exception in loadCheckBoxState() - data is NOT load");
            e.printStackTrace();
        }
    }

    //Turn ON Camera flash on the phone (with sound)
    private void setFlashLightOn() {
        Log.d(TAG, "setFlashLightOn()");
        soundPool.play(sound, 1, 1, 0, 0, 1);
        setFlashLightOnWithoutSound();
    }

    //Turn ON Camera flash on the phone (without sound)
    private void setFlashLightOnWithoutSound() {
        Log.d(TAG, "setFlashLightOnWithoutSound()");
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
        try {
            soundPool.play(sound, 1, 1, 0, 0, 1);
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
}