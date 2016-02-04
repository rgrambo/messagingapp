package edu.uw.rgrambo.yama;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import layout.MyPreferenceFragment;

public class ComposeMessage extends AppCompatActivity {

    BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose_message);

        // Set onClick events on buttons
        Button cancelButton = (Button) findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Set onClick events on image buttons
        Button sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = ((EditText)findViewById(R.id.phoneNumberEditText))
                        .getText().toString();
                String message = ((EditText)findViewById(R.id.messageEditText))
                        .getText().toString();

                if (phoneNumber.length() > 0 && message.length() > 0) {
                    Send(phoneNumber, message);
                } else {
                    Toast.makeText(getBaseContext(), "Must enter a phone number and a message",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set onClick event on image button
        ImageButton searchButton = (ImageButton) findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Uri uriContact = ContactsContract.Contacts.CONTENT_URI;
                Intent intentPickContact = new Intent(Intent.ACTION_PICK, uriContact);
                startActivityForResult(intentPickContact, 1);
            }
        });
    }

    // Wrote function with help of
    // http://android-er.blogspot.com/2012/11/get-phone-number-from-contacts-database.html
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK) {
            if (requestCode == 1) {
                Uri returnUri = data.getData();
                Cursor cursor = getContentResolver().query(returnUri, null, null, null, null);

                if (cursor.moveToNext()) {
                    String contactID = cursor.getString(
                            cursor.getColumnIndex(ContactsContract.Contacts._ID));

                    String hasPhoneNumber = cursor.getString(
                            cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

                    if (hasPhoneNumber.equals("1")) {
                        // If contact has a phone number, query for it
                        Cursor cursorNumber = getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactID,
                                null,
                                null);

                        // Grab the first phone number
                        if (cursorNumber.moveToNext()) {
                            String phoneNumber = cursorNumber.getString(cursorNumber.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER));

                            ((EditText)findViewById(R.id.phoneNumberEditText))
                                    .setText(phoneNumber);
                        }
                    }


                } else {
                    Toast.makeText(getApplicationContext(), "NO data!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "No Phone Number", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Function made with help of https://mobiforge.com/design-development/sms-messaging-android
    private void Send(String phoneNumber, String message) {
        String SENT = "SMS_SENT";

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (getResultCode() == Activity.RESULT_OK) {
                    Toast.makeText(getBaseContext(), "Successfully Sent",
                            Toast.LENGTH_SHORT).show();
                    // Clear the inputs
                    ((EditText)findViewById(R.id.phoneNumberEditText))
                            .setText("");
                    ((EditText)findViewById(R.id.messageEditText))
                            .setText("");
                } else {
                    Toast.makeText(getBaseContext(), "Failed to Send",
                            Toast.LENGTH_SHORT).show();
                }
            }
        };

        registerReceiver(broadcastReceiver, new IntentFilter(SENT));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, pendingIntent, null);
    }

    @Override
    protected void onStop()
    {
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException e) {
            // Do nothing if its already unregistered
        }
        super.onStop();
    }
}
