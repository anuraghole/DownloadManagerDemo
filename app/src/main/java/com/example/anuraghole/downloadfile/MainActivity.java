package com.example.anuraghole.downloadfile;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    public static final String FILE_PROVIDER = "com.example.anuraghole.downloadfile.provider";

    public static final String FILE_URL = "https://drive.google.com/uc?authuser=0&id=15d0KjuorIE_Bu0XurF8R61nYjYntqZQ8&export=download";
    //public  static final String FILE_URL="https://s3-us-west-2.amazonaws.com/uw-s3-cdn/wp-content/uploads/sites/6/2017/11/04133712/waterfall-750x500.jpg";
    // public  static final String FILE_URL="http://appeteria.com/video.mp4";
    private static final String TAG = MainActivity.class.getSimpleName();
    long queueId;
    static DownloadManager downloadManager;
    Button viewDownload, fileDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fileDownload = findViewById(R.id.fileDownload);
        viewDownload = findViewById(R.id.viewDownloads);
        fileDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                String fileName = URLUtil.guessFileName(FILE_URL, null, MimeTypeMap.getFileExtensionFromUrl(FILE_URL));
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(FILE_URL));

                request.setTitle(fileName);
                request.allowScanningByMediaScanner();
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

                queueId = downloadManager.enqueue(request);

            }
        });
        viewDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(DownloadManager.ACTION_VIEW_DOWNLOADS);
                startActivity(intent);
            }
        });

        if (!isStoragePermissionGranted()) {
        }

        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            return true;
        }
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                DownloadManager.Query requestQuery = new DownloadManager.Query();
                requestQuery.setFilterById(queueId);
                Cursor cursor = downloadManager.query(requestQuery);
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(columnIndex)) {
                        ImageView imageView = findViewById(R.id.videoView);
                        String localUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                        Log.i(TAG, "onReceive: localUri" + localUri);
                        Toast.makeText(context, "download complete", Toast.LENGTH_SHORT).show();
                        installFile(localUri);
                    } else {
                        Toast.makeText(context, "File not Download properly", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };


    public void installFile(String localUri) {
        File toInstall = new File(localUri);
        Log.i("TAG", "onClick to Install: " + toInstall);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                File file=new File(Uri.parse(localUri).getPath());
                Log.i(TAG, "installFile: file "+file);
                Log.i(TAG, "installFile: fileExist "+file.exists());
                Uri apkUri = FileProvider.getUriForFile(this, FILE_PROVIDER,file);
                Log.i(TAG, "installFile: apkURi " + apkUri);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);

            } else {
                Log.i("TAG", "onClick to Install else: " + localUri);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(localUri), "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        } catch (Exception e) {
            Log.i(TAG, "installFile exception: " + e);
        }
    }


}
