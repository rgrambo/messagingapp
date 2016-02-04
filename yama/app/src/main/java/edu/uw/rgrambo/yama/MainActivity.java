package edu.uw.rgrambo.yama;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import layout.MyPreferenceFragment;

public class MainActivity extends AppCompatActivity {

    ExpandableListAdapter expandableListAdapter;
    ExpandableListView expandableListView;

    private BroadcastReceiver SmsReceiver;
    public HashMap<String, List<String>> storedMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

                            if (!storedMessages.containsKey(address)) {
                                storedMessages.put(address, new ArrayList<String>());
                            }

                            storedMessages.get(address).add(message);
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
}
