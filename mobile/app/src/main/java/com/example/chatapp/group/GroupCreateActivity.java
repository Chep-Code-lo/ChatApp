package com.example.chatapp.group;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.core.storage.LocalStore;
import com.example.chatapp.core.ui.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GroupCreateActivity extends BaseActivity {
    private static final String SELECTED_KEY = "group_selected_people";
    private final Set<String> selected = new LinkedHashSet<>();
    private GroupPersonAdapter adapter;
    private EditText nameInput, search;
    private TextInputLayout nameLayout;
    private ChipGroup chips;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.group_activity_create);
        setupToolbar(getString(R.string.group_ui_create));
        if (savedInstanceState != null) {
            ArrayList<String> saved = savedInstanceState.getStringArrayList(SELECTED_KEY);
            if (saved != null) selected.addAll(saved);
        }
        selected.removeIf(id -> store.person(id) == null);
        nameInput = findViewById(R.id.groupNameInput);
        nameLayout = findViewById(R.id.groupNameLayout);
        search = findViewById(R.id.groupCreateSearchInput);
        chips = findViewById(R.id.groupSelectedChips);
        RecyclerView list = findViewById(R.id.groupMembersRecycler);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GroupPersonAdapter(this, selected, Collections.emptySet(), "", this::renderSelection);
        list.setAdapter(adapter);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { nameLayout.setError(null); }
            @Override public void afterTextChanged(Editable s) { }
        });
        findViewById(R.id.groupCreateSubmitButton).setOnClickListener(view -> create());
        renderSelection();
        filter();
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putStringArrayList(SELECTED_KEY, new ArrayList<>(selected));
        super.onSaveInstanceState(outState);
    }

    private void filter() {
        List<LocalStore.Person> people = GroupText.filter(store.people(), search.getText().toString());
        adapter.submit(people);
        findViewById(R.id.groupCreateEmpty).setVisibility(people.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void renderSelection() {
        ((TextView) findViewById(R.id.groupSelectedCount)).setText(getString(R.string.group_ui_selected_count, selected.size()));
        ((MaterialButton) findViewById(R.id.groupCreateSubmitButton)).setText(getString(R.string.group_ui_create_count, selected.size() + 1));
        chips.removeAllViews();
        for (String id : selected) {
            LocalStore.Person person = store.person(id);
            if (person == null) continue;
            Chip chip = new Chip(this);
            chip.setText(person.name);
            chip.setTextAppearance(R.style.Text_Luma_Label13);
            chip.setTextColor(ContextCompat.getColor(this, R.color.luma_accent_text));
            chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.luma_tint)));
            chip.setChipStrokeWidth(0);
            chip.setCloseIconResource(R.drawable.lucide_x);
            chip.setCloseIconVisible(true);
            chip.setCloseIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.luma_accent_text)));
            chip.setCloseIconContentDescription(getString(R.string.group_ui_remove_person, person.name));
            chip.setOnCloseIconClickListener(view -> {
                selected.remove(id);
                renderSelection();
                adapter.notifyDataSetChanged();
            });
            chips.addView(chip);
        }
    }

    private void create() {
        String name = nameInput.getText().toString().trim();
        if (name.length() < 2 || name.length() > 40) {
            nameLayout.setError(getString(R.string.group_ui_invalid_name));
            nameInput.requestFocus();
            return;
        }
        if (selected.size() < 2) {
            Toast.makeText(this, R.string.group_ui_pick_two, Toast.LENGTH_SHORT).show();
            return;
        }
        String chatId = store.createGroup(name, new ArrayList<>(selected));
        if (chatId == null || chatId.isEmpty()) {
            Toast.makeText(this, R.string.group_ui_create_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(this, GroupConversationActivity.class).putExtra("chat_id", chatId));
        finish();
    }
}
