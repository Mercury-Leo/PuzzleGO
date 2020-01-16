package com.mercuryf_ofiro.puzzlego;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.ArrayList;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.WRITE_CONTACTS;

public class ContactsSMS extends AppCompatActivity {

    private static final String TELEPHON_NUMBER_FIELD_NAME = "address";
    private static final String MESSAGE_BODY_FIELD_NAME = "body";
    ListView contact_list;
    ArrayList numberArray;
    ArrayList mobileArray;
    private final int PERMISSIONS_REQUEST_READ_CONTACTS = 1;
    boolean  mContactsPermissionGranted = false;
    private final int PERMISSIONS_REQUEST_WRITE_CONTACTS = 1;
    boolean  mContactsWritePermissionGranted = false;
    String PRIZE_PREF = "prize_pref";
    String name, time, moves;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts_sms);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Share Puzzle");

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            mContactsPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{READ_CONTACTS},
                    PERMISSIONS_REQUEST_READ_CONTACTS);
        }
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                WRITE_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            mContactsWritePermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{WRITE_CONTACTS},
                    PERMISSIONS_REQUEST_WRITE_CONTACTS);
        }
        //Gets via sp the name, time and moves of the puzzle
        SharedPreferences prize_pref = getSharedPreferences(PRIZE_PREF, MODE_PRIVATE);
        name = prize_pref.getString("name", "no name");
        time = prize_pref.getString("time", "no time");
        moves = prize_pref.getString("moves", "no moves");
        numberArray = new ArrayList();
        mobileArray = new ArrayList();
        mobileArray = getAllContacts();
        contact_list = findViewById(R.id.list_contacts);

        ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, mobileArray);
        contact_list.setAdapter(adapter);
        contact_list.setOnItemClickListener((parent, view, position, id) -> {
            Log.d("debug", "The pos is: " + numberArray.get(position).toString());
            Intent sms_intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("sms:"+numberArray.get(position).toString()));
            addMessageToSent(numberArray.get(position).toString(), MessageBuilder(), sms_intent);
        });

    }

    //Gets all the contacts of the device
    private ArrayList getAllContacts(){
        ArrayList<String> nameList = new ArrayList<>();
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);
        if ((cur != null ? cur.getCount() : 0) > 0) {
            while (cur != null && cur.moveToNext()) {
                String id = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));
                nameList.add(name);

                if (cur.getInt(cur.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                        numberArray.add(phoneNo);

                    }
                    pCur.close();
                }
            }
        }
        if (cur != null) {
            cur.close();
        }
        return nameList;
    }

    //Builds a message with the puzzle details
    private String MessageBuilder(){
        String start = "I have solved the " + name + " puzzle using the PuzzleGO app!";
        String middle = "It only took me " + time + " seconds to finish it!";
        String finish = "I completed it in " + moves + " moves!";
        return start + "\n" + middle + "\n" + finish;
    }

    //Sends the message to sms.
    private void addMessageToSent(String telNumber, String messageBody, Intent i) {
        ContentValues sentSms = new ContentValues();
        sentSms.put(TELEPHON_NUMBER_FIELD_NAME, telNumber);
        sentSms.put(MESSAGE_BODY_FIELD_NAME, messageBody);
        i.putExtra("sms_body", messageBody);
        startActivity(i);
    }


}
