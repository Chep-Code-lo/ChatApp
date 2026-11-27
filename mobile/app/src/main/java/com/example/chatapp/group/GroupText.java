package com.example.chatapp.group;

import com.example.chatapp.core.storage.LocalStore;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Search helpers shared by the group's native contact screens. */
final class GroupText {
    private GroupText() { }

    static String normalized(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replace('đ', 'd').trim();
    }

    static String phoneDigits(String value) {
        String digits = value == null ? "" : value.replaceAll("[^0-9]", "");
        if (digits.startsWith("84") && digits.length() >= 11) {
            return "0" + digits.substring(2);
        }
        return digits;
    }

    static List<LocalStore.Person> filter(List<LocalStore.Person> people, String query) {
        String text = normalized(query);
        String digits = phoneDigits(query);
        boolean isPhoneQuery = query != null && query.matches("[+()\\s.0-9-]+") && !digits.isEmpty();
        List<LocalStore.Person> results = new ArrayList<>();
        for (LocalStore.Person person : people) {
            if (text.isEmpty()
                    || normalized(person.name + " " + person.handle).contains(text)
                    || (isPhoneQuery && phoneDigits(person.phone).contains(digits))) {
                results.add(person);
            }
        }
        return results;
    }
}
