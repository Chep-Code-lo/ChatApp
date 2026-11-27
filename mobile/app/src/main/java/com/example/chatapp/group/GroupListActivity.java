package com.example.chatapp.group;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.core.storage.LocalStore;

import java.util.ArrayList;
import java.util.List;

public class GroupListActivity extends GroupBaseActivity {
    private GroupCardAdapter adapter;
    private EditText search;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.group_activity_list);
        setupToolbar(getString(R.string.group_ui_toolbar));
        search = findViewById(R.id.groupListSearchInput);
        RecyclerView list = findViewById(R.id.groupListRecycler);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GroupCardAdapter(this, store,
                id -> open(GroupConversationActivity.class, id),
                id -> open(GroupDetailActivity.class, id));
        list.setAdapter(adapter);
        findViewById(R.id.groupCreateButton).setOnClickListener(view -> startActivity(new Intent(this, GroupCreateActivity.class)));
        findViewById(R.id.groupAddFriendButton).setOnClickListener(view -> startActivity(new Intent(this, GroupAddFriendActivity.class)));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { refresh(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        if (adapter != null) refresh();
    }

    private void open(Class<?> screen, String chatId) {
        startActivity(new Intent(this, screen).putExtra("chat_id", chatId));
    }

    private void refresh() {
        String query = GroupText.normalized(search.getText().toString());
        List<LocalStore.Chat> groups = new ArrayList<>();
        for (LocalStore.Chat chat : store.chats()) {
            if (chat.isGroup && GroupText.normalized(chat.title).contains(query)) groups.add(chat);
        }
        adapter.submit(groups);
        ((TextView) findViewById(R.id.groupListCount)).setText(getString(R.string.group_ui_group_count, groups.size()));
        TextView empty = findViewById(R.id.groupListEmpty);
        empty.setText(query.isEmpty() ? R.string.group_ui_empty_groups : R.string.group_ui_no_results);
        empty.setVisibility(groups.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
