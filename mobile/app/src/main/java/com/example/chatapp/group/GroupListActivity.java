package com.example.chatapp.group;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.R;

public class GroupListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.group_list_title);
        setContentView(R.layout.group_activity_list);

        findViewById(R.id.groupCreateButton).setOnClickListener(view ->
                startActivity(new Intent(this, GroupCreateActivity.class)));
        findViewById(R.id.groupAddFriendButton).setOnClickListener(view ->
                startActivity(new Intent(this, GroupAddFriendActivity.class)));
        findViewById(R.id.groupOpenConversationButton).setOnClickListener(view ->
                startActivity(new Intent(this, GroupConversationActivity.class)));
        findViewById(R.id.groupOpenDetailButton).setOnClickListener(view ->
                startActivity(new Intent(this, GroupDetailActivity.class)));
    }
}
