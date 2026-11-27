package com.example.chatapp.group;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.core.storage.LocalStore;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GroupAddFriendActivity extends GroupBaseActivity {
    private static final String SELECTED_KEY = "group_friend_selection";
    private final Set<String> selected = new LinkedHashSet<>();
    private final Set<String> disabled = new LinkedHashSet<>();
    private String chatId;
    private GroupPersonAdapter adapter;
    private EditText search;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.group_activity_add_friend);
        chatId = getIntent().getStringExtra("chat_id");
        boolean memberMode = chatId != null;
        setupToolbar(getString(memberMode ? R.string.group_ui_add_member_title : R.string.group_ui_find_title));
        if (savedInstanceState != null) {
            ArrayList<String> saved = savedInstanceState.getStringArrayList(SELECTED_KEY);
            if (saved != null) selected.addAll(saved);
        }
        search = findViewById(R.id.groupSearchFriendInput);
        RecyclerView list = findViewById(R.id.groupFriendRecycler);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GroupPersonAdapter(this, selected, disabled,
                getString(memberMode ? R.string.group_ui_already_member : R.string.group_ui_pending), this::renderAction);
        list.setAdapter(adapter);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        search.setOnEditorActionListener((view, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEARCH) { searchNow(); return true; }
            return false;
        });
        findViewById(R.id.groupSearchFriendButton).setOnClickListener(view -> searchNow());
        findViewById(R.id.groupSendFriendRequestButton).setOnClickListener(view -> apply());
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        if (adapter != null) refresh();
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putStringArrayList(SELECTED_KEY, new ArrayList<>(selected));
        super.onSaveInstanceState(outState);
    }

    private void refresh() {
        disabled.clear();
        if (chatId != null) {
            LocalStore.Chat group = store.chat(chatId);
            if (group == null || !group.isGroup) {
                Toast.makeText(this, R.string.group_ui_unavailable, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            disabled.addAll(group.memberIds);
            TextView subtitle = findViewById(R.id.groupFriendSubtitle);
            subtitle.setText(getString(R.string.group_ui_add_member_subtitle, group.title));
            subtitle.setVisibility(View.VISIBLE);
        } else {
            for (LocalStore.Person person : store.people()) {
                if (store.isFriendRequestPending(person.id)) disabled.add(person.id);
            }
        }
        selected.removeAll(disabled);
        selected.removeIf(id -> store.person(id) == null);
        renderAction();
        filter();
    }

    private void filter() {
        List<LocalStore.Person> results = GroupText.filter(store.people(), search.getText().toString());
        adapter.submit(results);
        ((TextView) findViewById(R.id.groupFriendResultCount)).setText(getString(R.string.group_ui_contact_count, results.size()));
        TextView empty = findViewById(R.id.groupFriendEmpty);
        empty.setText(search.getText().toString().trim().isEmpty()
                ? R.string.group_ui_no_contacts : R.string.group_ui_no_contacts_found);
        empty.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void renderAction() {
        MaterialButton button = findViewById(R.id.groupSendFriendRequestButton);
        button.setText(getString(chatId == null ? R.string.group_ui_request_count : R.string.group_ui_add_member_count, selected.size()));
        button.setEnabled(!selected.isEmpty());
    }

    private void searchNow() {
        filter();
        InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(search.getWindowToken(), 0);
    }

    private void apply() {
        if (selected.isEmpty()) {
            Toast.makeText(this, R.string.group_ui_choose_people, Toast.LENGTH_SHORT).show();
            return;
        }
        int count = selected.size();
        if (chatId != null) {
            LocalStore.Chat group = store.chat(chatId);
            if (group == null || !group.isGroup) { refresh(); return; }
            store.addMembers(chatId, new ArrayList<>(selected));
            Toast.makeText(this, getString(R.string.group_ui_members_added, count), Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, R.string.group_ui_request_unavailable, Toast.LENGTH_SHORT).show();
        }
    }
}
