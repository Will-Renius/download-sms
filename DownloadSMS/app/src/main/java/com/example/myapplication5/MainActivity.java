package com.example.myapplication5;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Environment;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.opencsv.CSVWriter;

import java.util.*;
import java.io.*;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.provider.ContactsContract;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private String baseDir = android.os.Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

//    Values from Screen Elements
    private String fileName;
    private String statusMessage;
//    Screen Elements
    private EditText fileNameInput;
    private Button submitButton;
    private TextView statusTextView;
    // Request codes. Can be any number > 0.
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int PERMISSIONS_REQUEST_SMS = 101;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 102;
    private static final int PERMISSIONS_REQUEST_ALL = 42;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        fileNameInput = (EditText) findViewById(R.id.fileNameInput);
        submitButton = (Button) findViewById(R.id.submitButton);
        statusTextView = (TextView)findViewById(R.id.statusText);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                fileName = fileNameInput.getText().toString();
                downloadSMS(fileName);
            }
        });
    }

    private void updateStatusMessage(String statusMessage){
        statusTextView.setText(statusMessage);
    }

    /**
     * Download SMS.
     */
    private void downloadSMS(String fileName) {
        updateStatusMessage("");
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)){
            requestPermissions(new String[]{Manifest.permission.READ_SMS,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_ALL);
    //          After this point wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            Toast.makeText(this, "Downloading ...", Toast.LENGTH_SHORT).show();

            //          Android version is lesser than 6.0 or the permission is already granted.
            List<String[]> sentSMS = getSentSMS();
            List<String[]> inboxSMS = getInboxSMS();
            List<String[]> contacts = getContacts();

    //        Write Contacts CSV
            String contacts_filename = fileName + "_" + "contacts.csv";
            String contacts_filePath = baseDir + File.separator + contacts_filename;

            try {
                CSVWriter writer = new CSVWriter(new FileWriter(contacts_filePath));
                writer.writeAll(contacts);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Download Failed", Toast.LENGTH_SHORT).show();
            }

    //        Write Sent CSV
            String sent_filename = fileName + "_sent_sms.csv";
            String sent_filePath = baseDir + File.separator + sent_filename;

            try {
                CSVWriter writer = new CSVWriter(new FileWriter(sent_filePath));
                writer.writeAll(sentSMS);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Download Failed", Toast.LENGTH_SHORT).show();
            }

    //        Write inbox CSV
            String inbox_filename = fileName + "_inbox_sms.csv";
            String inbox_filePath = baseDir + File.separator + inbox_filename;

            try {
                CSVWriter writer = new CSVWriter(new FileWriter(inbox_filePath));
                writer.writeAll(inboxSMS);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Download Failed", Toast.LENGTH_SHORT).show();
            }
            Toast.makeText(this, "Download Succeeded", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(this, "This app will not work without sufficient permissions", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Read the name of all the contacts.
     *
     * @return a list of names.
     */
    private List<String> getContactNames() {
        List<String> contacts = new ArrayList<>();
        // Get the ContentResolver
        ContentResolver cr = getContentResolver();
        // Get the Cursor of all the contacts
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        // Move the cursor to first. Also check whether the cursor is empty or not.
        if (cursor.moveToFirst()) {
            // Iterate through the cursor
            do {
                // Get the contacts name
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                contacts.add(name);
            } while (cursor.moveToNext());
        }
        // Close the curosor
        cursor.close();

        return contacts;
    }

    /**
     * Get columns of sent messages
     *
     * @return a list of comma separated column values.
     */
    private List<String[]> getSentSMS() {
        List<String[]> sent_data = new ArrayList<String[]>();
        // Get the ContentResolver
        Cursor sent_cursor = getContentResolver().query(Uri.parse("content://sms/sent"), new String[]{"thread_id","address", "date", "date_sent", "body"}, null, null, null);
        sent_data.add(new String[] {"thread_id", "address", "date", "date_sent", "body"});

        if (sent_cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                String threadId = sent_cursor.getString(0);
                String address = sent_cursor.getString(1);
                String date = sent_cursor.getString(2);
                String date_sent = sent_cursor.getString(3);
                String body = sent_cursor.getString(4);

                sent_data.add(new String[] {threadId, address, date, date_sent, body});
            } while (sent_cursor.moveToNext());
        } else {
            // empty box, no SMS
        }
        // Close the curosor
        sent_cursor.close();

        return sent_data;
    }

    /**
     * Get columns of sent messages
     *
     * @return a list of comma separated column values.
     */
    private List<String[]> getInboxSMS() {
        List<String[]> inbox_data = new ArrayList<String[]>();

        Cursor inbox_cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), new String[]{"thread_id","body","person", "date"}, null, null, null);
        inbox_data.add(new String[] {"thread_id", "person", "date", "body"});
//
        if (inbox_cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                String thread_id = inbox_cursor.getString(0);
                String body = inbox_cursor.getString(1);
                String person = inbox_cursor.getString(2);
                String date = inbox_cursor.getString(3);

                inbox_data.add(new String[] {thread_id, person, date, body});
            } while (inbox_cursor.moveToNext());
        } else {
            // empty box, no SMS
        }

        // Close the curosor
        inbox_cursor.close();

        return inbox_data;
    }
    private List<String[]> getContacts() {
        List<String[]> contacts_data = new ArrayList<String[]>();
        Cursor contacts_cursor = getContentResolver().query(Uri.parse("content://contacts/people"), new String[]{"_id","display_name"}, null, null, null);
        contacts_data.add(new String[] {"id", "display_name"});
        if (contacts_cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                String id = contacts_cursor.getString(0);
                String name = contacts_cursor.getString(1);
                contacts_data.add(new String[] {id, name});
            } while (contacts_cursor.moveToNext());
        } else {
            // empty box, no SMS
        }
        // Close the curosor
        contacts_cursor.close();

        return contacts_data;
    }

}
