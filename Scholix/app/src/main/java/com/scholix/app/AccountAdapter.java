package com.scholix.app;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {

    private List<Account> accountList;
    private final DeleteListener deleteListener;
    private final SaveListener saveListener;
    private final String[] sources = {"Classroom", "Bar Ilan", "Webtop"};

    public interface DeleteListener {
        void onDelete(int position);
    }

    public interface SaveListener {
        void onSave();
    }

    public AccountAdapter(List<Account> accountList, DeleteListener deleteListener, SaveListener saveListener) {
        this.accountList = accountList;
        this.deleteListener = deleteListener;
        this.saveListener = saveListener;
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_account, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        Account account = accountList.get(position);

        holder.username.setText(account.getUsername());
        holder.password.setText(account.getPassword());

        // Source Spinner setup
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(holder.itemView.getContext(),
                android.R.layout.simple_spinner_item, sources);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.sourceSpinner.setAdapter(spinnerAdapter);

        // Set source spinner selection
        int sourceIndex = 0;
        for (int i = 0; i < sources.length; i++) {
            if (sources[i].equals(account.getSource())) {
                sourceIndex = i;
                break;
            }
        }
        holder.sourceSpinner.setSelection(sourceIndex);

        // Year Spinner setup
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(holder.itemView.getContext(),
                android.R.layout.simple_spinner_item, new String[]{"1", "2", "3"});
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.yearSpinner.setAdapter(yearAdapter);

        if (account.isEditing()) {
            // Edit Mode
            holder.username.setEnabled(true);
            holder.password.setEnabled(true);
            holder.password.setVisibility(View.VISIBLE);
            holder.saveBtn.setVisibility(View.VISIBLE);
            holder.editBtn.setVisibility(View.GONE);
            holder.deleteBtn.setVisibility(View.GONE);
            holder.sourceSpinner.setVisibility(View.VISIBLE);
            holder.password.setInputType(InputType.TYPE_CLASS_TEXT);

            // Show Year Spinner if Bar Ilan
            if (account.getSource().equals("Bar Ilan")) {
                holder.yearSpinner.setVisibility(View.VISIBLE);
                holder.yearSpinner.setSelection(account.getYear() - 1); // set current year
            } else {
                holder.yearSpinner.setVisibility(View.GONE);
            }

        } else {
            // View Mode
            holder.username.setEnabled(false);
            holder.password.setEnabled(false);
            holder.password.setVisibility(View.GONE);
            holder.saveBtn.setVisibility(View.GONE);
            holder.editBtn.setVisibility(View.VISIBLE);
            holder.deleteBtn.setVisibility(View.VISIBLE);
            holder.sourceSpinner.setVisibility(View.GONE);
            holder.yearSpinner.setVisibility(View.GONE);
        }

        // Edit button clicked → switch to edit mode
        holder.editBtn.setOnClickListener(v -> {
            account.setEditing(true);
            notifyItemChanged(position);

            RecyclerView recyclerView = (RecyclerView) holder.itemView.getParent();
            recyclerView.post(() -> recyclerView.smoothScrollToPosition(position));

            NestedScrollView scrollView = ((PlatformsActivity) holder.itemView.getContext()).findViewById(R.id.nested_scroll);
            scrollView.postDelayed(() -> {
                scrollView.smoothScrollTo(0, holder.itemView.getTop() + 1000);
            }, 100);
        });

        // Save button clicked
        holder.saveBtn.setOnClickListener(v -> {
            String updatedUsername = holder.username.getText().toString();
            String updatedPassword = holder.password.getText().toString();
            String updatedSource = holder.sourceSpinner.getSelectedItem().toString();

            if (updatedUsername.isEmpty() || updatedPassword.isEmpty()) {
                Toast.makeText(holder.itemView.getContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (updatedSource.equals("Webtop")) {
                // Validate Webtop login
                new Thread(() -> {
                    try {
                        LoginManager loginManager = new LoginManager();
                        LoginManager.LoginResult result = loginManager.validateLogin(updatedUsername, updatedPassword);

                        ((PlatformsActivity) holder.itemView.getContext()).runOnUiThread(() -> {
                            if (result.success) {
                                account.setUsername(updatedUsername);
                                account.setPassword(updatedPassword);
                                account.setSource(updatedSource);
                                account.setEditing(false);
                                notifyItemChanged(position);
                                saveListener.onSave();
                                Toast.makeText(holder.itemView.getContext(), "Webtop Account Updated", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(holder.itemView.getContext(), "Webtop Login Failed: " + result.message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        ((PlatformsActivity) holder.itemView.getContext()).runOnUiThread(() ->
                                Toast.makeText(holder.itemView.getContext(), "Login Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                    }
                }).start();
            } else {
                // Non-Webtop → Save immediately
                account.setUsername(updatedUsername);
                account.setPassword(updatedPassword);
                account.setSource(updatedSource);
                // Save Year if Bar Ilan
                if (updatedSource.equals("Bar Ilan")) {
                    int selectedYear = Integer.parseInt(holder.yearSpinner.getSelectedItem().toString());
                    account.setYear(selectedYear);
                }
                account.setEditing(false);
                notifyItemChanged(position);
                saveListener.onSave();
                Toast.makeText(holder.itemView.getContext(), "Account Updated", Toast.LENGTH_SHORT).show();
            }

            // Close keyboard
            InputMethodManager imm = (InputMethodManager) holder.itemView.getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(holder.itemView.getWindowToken(), 0);
        });

        // Delete button
        holder.deleteBtn.setOnClickListener(v -> deleteListener.onDelete(position));
    }

    @Override
    public int getItemCount() {
        return accountList.size();
    }

    public static class AccountViewHolder extends RecyclerView.ViewHolder {
        EditText username, password;
        Spinner sourceSpinner, yearSpinner;
        Button editBtn, deleteBtn, saveBtn;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.account_username);
            password = itemView.findViewById(R.id.account_password);
            sourceSpinner = itemView.findViewById(R.id.account_source_spinner);
            yearSpinner = itemView.findViewById(R.id.account_year_spinner);
            editBtn = itemView.findViewById(R.id.edit_button);
            deleteBtn = itemView.findViewById(R.id.delete_button);
            saveBtn = itemView.findViewById(R.id.save_button);
        }
    }
}
