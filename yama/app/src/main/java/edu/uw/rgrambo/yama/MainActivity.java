package edu.uw.rgrambo.yama;

import android.app.Activity;
import android.app.LoaderManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import layout.MyPreferenceFragment;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int URL_LOADER = 0;

    BroadcastReceiver broadcastReceiver;

    ExpandableListAdapter expandableListAdapter;
    ExpandableListView expandableListView;

    private BroadcastReceiver SmsReceiver;
    public HashMap<String, List<String>> storedMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getLoaderManager().initLoader(URL_LOADER, null, this);

        storedMessages = new HashMap<>();

        if (SmsReceiver == null) {
            // Set up broadcastReceiver
            SmsReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (storedMessages == null) {
                        storedMessages = new HashMap<>();
                    }
                    Bundle bundle = intent.getExtras();
                    SmsMessage[] messages = null;
                    if (bundle != null) {
                        Object[] objects = (Object[]) bundle.get("pdus");
                        messages = new SmsMessage[objects.length];
                        for (int i = 0; i < messages.length; i++) {
                            messages[i] = SmsMessage.createFromPdu((byte[]) objects[i]);

                            String address = messages[i].getOriginatingAddress();
                            String message = messages[i].getMessageBody();
                            String date = getDate(messages[i].getTimestampMillis(), "MM/dd/yyyy hh:mm:ss");

                            if (!storedMessages.containsKey(address)) {
                                storedMessages.put(address, new ArrayList<String>());
                            }

                            storedMessages.get(address).add(date + ": " + message);

                            // Notify User
                            showNotification(address, message);

                            // Auto-Reply
                            SharedPreferences sharedPreferences = PreferenceManager
                                    .getDefaultSharedPreferences(getBaseContext());
                            Boolean replyEnabled = sharedPreferences
                                    .getBoolean("auto_reply_switch", false);
                            String replyMessage = sharedPreferences
                                    .getString("auto_reply_message", "");

                            if (replyEnabled && !replyMessage.isEmpty()) {
                                Send(address, replyMessage);
                            }
                        }
                    }

                    refreshData();
                }
            };

            registerReceiver(SmsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
        }

        // Set onClick events on image buttons
        ImageButton dotsButton = (ImageButton) findViewById(R.id.dotsButton);
        dotsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().beginTransaction()
                                .replace(android.R.id.content, new MyPreferenceFragment())
                                .addToBackStack(null).commit();
            }
        });

        ImageButton plusButton = (ImageButton) findViewById(R.id.plusButton);
        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, ComposeMessage.class);
                startActivity(i);
            }
        });



        // Grab a reference to the expandable list view
        expandableListView = (ExpandableListView) findViewById(R.id.receivedListView);

        // Refresh the headers and the list data
        refreshData();
    }

    private void refreshData() {
        // Create the expandable list adapter using the headers and list data
        expandableListAdapter = new ExpandableListAdapter(this,
                new ArrayList<String>(storedMessages.keySet()), storedMessages);

        // Set the expandable list view's adapter
        expandableListView.setAdapter(expandableListAdapter);
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0 ){
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
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
                    Toast.makeText(getBaseContext(), "Auto-Replied",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getBaseContext(), "Failed to Auto Reply",
                            Toast.LENGTH_SHORT).show();
                }
            }
        };

        registerReceiver(broadcastReceiver, new IntentFilter(SENT));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, pendingIntent, null);
    }

    @Override
    protected void onDestroy()
    {
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException e) {
            // Do nothing if its already unregistered
        }
        try {
            unregisterReceiver(SmsReceiver);
        } catch (IllegalArgumentException e) {
            // Do nothing if its already unregistered
        }
        super.onDestroy();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case URL_LOADER:
                // Returns a new CursorLoader
                return new CursorLoader(getBaseContext(),
                        Telephony.Sms.Inbox.CONTENT_URI,
                        new String[] {Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.DATE, Telephony.Sms.BODY },
                        null,
                        null,
                        null);
            default:
                // An invalid id was passed in
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        while (data.moveToNext()) {

            String address = data.getString(1);
            String date = getDate(Long.parseLong(data.getString(2)), "MM/dd/yyyy hh:mm:ss");
            String message = data.getString(3);

            if (!storedMessages.containsKey(address)) {
                storedMessages.put(address, new ArrayList<String>());
            }

            storedMessages.get(address).add(date + ": " + message);
        }
        data.close();

        refreshData();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private static String getDate(long milliSeconds, String dateFormat)
    {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }

    // Written with help of
    // http://alvinalexander.com/android/how-to-create-android-notifications-notificationmanager-examples
    public void showNotification(String address, String message) {
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setTicker("Message from " + address)
                .setSmallIcon(R.drawable.message)
                .setContentTitle("Message from " + address)
                .setContentText(message)
                .setContentIntent(pi)
                .setAutoCancel(true)
                        .build();

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(0, notification);
    }
}
