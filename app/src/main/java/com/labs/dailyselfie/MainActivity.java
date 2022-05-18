package com.labs.dailyselfie;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class MainActivity extends Activity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final long INTERVAL_TIMES = 15 * 1000L ;

    private SelfieRecordAdapter mAdapter;
    private String mCurrentSelfieName;
    private String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(LOG_TAG, "getExternalStorageState() = " + Environment.getExternalStorageState());

        ListView selfieList = (ListView) findViewById(R.id.selfie_list);

        mAdapter = new SelfieRecordAdapter(getApplicationContext());
        selfieList.setAdapter(mAdapter);
        selfieList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            SelfieRecord selfieRecord = (SelfieRecord) mAdapter.getItem(position);
            Intent intent = new Intent(MainActivity.this, DetailActivity.class);
            intent.putExtra(Intent.EXTRA_TEXT, selfieRecord.getPath());
            startActivity(intent);
            }
        });
        createSelfieAlarm();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_camera) {
            dispatchTakePictureIntent();
            return true;
        }
        if (id == R.id.action_delete_selected) {
            deleteSelectedSelfies();
            return true;
        }
        if (id == R.id.action_delete_all) {
            deleteAllSelfies();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        mCurrentSelfieName = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File imageFile = File.createTempFile(
                mCurrentSelfieName,
                ".jpg",
                getExternalFilesDir(null));

        mCurrentPhotoPath = imageFile.getAbsolutePath();
        return imageFile;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            }
            catch (IOException ex) {
            }

            if (photoFile != null) {
                Intent chooser = Intent.createChooser(takePictureIntent, "Chọn App để mở Camera");
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    Uri uri = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + ".provider",photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                    startActivityForResult(chooser, REQUEST_IMAGE_CAPTURE);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            File photoFile = new File(mCurrentPhotoPath);
            File selfieFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), mCurrentSelfieName + ".jpg");
            photoFile.renameTo(selfieFile);

            SelfieRecord selfieRecord = new SelfieRecord(Uri.fromFile(selfieFile).getPath(), mCurrentSelfieName);
            Log.d(LOG_TAG, selfieRecord.getPath() + " - " + selfieRecord.getDisplayName());
            mAdapter.add(selfieRecord);
//            createSelfieAlarm();
        }
        else {
            File photoFile = new File(mCurrentPhotoPath);
            photoFile.delete();
        }


    }

    private void deleteSelectedSelfies() {
        ArrayList<SelfieRecord> selectedSelfies = mAdapter.getSelectedRecords();
        for (SelfieRecord selfieRecord : selectedSelfies) {
            File selfieFile = new File(selfieRecord.getPath());
            selfieFile.delete();
        }
        mAdapter.clearSelected();
    }

    private void deleteAllSelfies() {
        for (SelfieRecord selfieRecord : mAdapter.getAllRecords()) {
            File selfieFile = new File(selfieRecord.getPath());
            selfieFile.delete();
        }
        mAdapter.clearAll();
    }

    private void createSelfieAlarm() {
        try {
            Intent intent = new Intent(this, SelfieNotificationReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + INTERVAL_TIMES,
                    INTERVAL_TIMES,
                    pendingIntent);
        } catch (Exception e) {
            Log.d("ALARM", e.getMessage().toString());
        }
    }
}
