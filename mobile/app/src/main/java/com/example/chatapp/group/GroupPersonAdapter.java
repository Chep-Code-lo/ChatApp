package com.example.chatapp.group;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.core.storage.LocalStore;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class GroupPersonAdapter extends RecyclerView.Adapter<GroupPersonAdapter.Holder> {
    interface PersonAction { void run(LocalStore.Person person); }

    private final Context context;
    private final List<LocalStore.Person> people = new ArrayList<>();
    private final Set<String> selected;
    private final Set<String> disabled;
    private final String disabledLabel;
    private final Runnable selectionChanged;
    private PersonAction removeAction;

    GroupPersonAdapter(Context context, Set<String> selected, Set<String> disabled,
                       String disabledLabel, Runnable selectionChanged) {
        this.context = context;
        this.selected = selected;
        this.disabled = disabled;
        this.disabledLabel = disabledLabel;
        this.selectionChanged = selectionChanged;
    }

    void setRemoveAction(PersonAction action) { removeAction = action; }

    void submit(List<LocalStore.Person> next) {
        people.clear();
        people.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.group_item_person, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        LocalStore.Person person = people.get(position);
        boolean blocked = disabled != null && disabled.contains(person.id);
        boolean selectable = selected != null;
        holder.avatar.setImageResource(person.avatarRes != 0 ? person.avatarRes : R.drawable.lucide_user);
        holder.name.setText(person.name);
        String contact = person.phone == null || person.phone.isEmpty()
                ? person.handle : person.phone + " · " + person.handle;
        holder.handle.setText(blocked ? disabledLabel : contact);
        holder.itemView.setAlpha(blocked ? 0.58f : 1f);
        holder.check.setOnCheckedChangeListener(null);
        holder.check.setVisibility(selectable ? View.VISIBLE : View.GONE);
        holder.check.setEnabled(!blocked);
        holder.check.setChecked(selectable && selected.contains(person.id));
        holder.check.setContentDescription(context.getString(R.string.group_ui_select_person, person.name));
        holder.check.setOnCheckedChangeListener((button, checked) -> {
            if (!selectable || blocked) return;
            if (checked) selected.add(person.id); else selected.remove(person.id);
            if (selectionChanged != null) selectionChanged.run();
        });
        holder.itemView.setOnClickListener(selectable && !blocked ? view -> holder.check.toggle() : null);
        holder.itemView.setClickable(selectable && !blocked);
        holder.remove.setVisibility(!selectable && removeAction != null ? View.VISIBLE : View.GONE);
        holder.remove.setContentDescription(context.getString(R.string.group_ui_remove_member_accessibility, person.name));
        holder.remove.setOnClickListener(view -> { if (removeAction != null) removeAction.run(person); });
    }

    @Override public int getItemCount() { return people.size(); }

    static final class Holder extends RecyclerView.ViewHolder {
        final ShapeableImageView avatar;
        final TextView name;
        final TextView handle;
        final MaterialCheckBox check;
        final ImageButton remove;

        Holder(View view) {
            super(view);
            avatar = view.findViewById(R.id.groupPersonAvatar);
            name = view.findViewById(R.id.groupPersonName);
            handle = view.findViewById(R.id.groupPersonHandle);
            check = view.findViewById(R.id.groupPersonCheck);
            remove = view.findViewById(R.id.groupPersonRemoveButton);
        }
    }
}
