package com.example.chatapp.group;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.R;

public class GroupAddFriendActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.group_add_friend_title);
        setContentView(R.layout.group_activity_add_friend);

        findViewById(R.id.groupSearchFriendButton).setOnClickListener(view ->
                Toast.makeText(this, R.string.group_mock_search_message, Toast.LENGTH_SHORT).show());
        findViewById(R.id.groupSendFriendRequestButton).setOnClickListener(view ->
                Toast.makeText(this, R.string.group_mock_friend_request_message, Toast.LENGTH_SHORT).show());
    }
}
