package com.scholix.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigation(bottomNavigationView);




        // Setup account (person icon) popup menu
        setupAccountMenu();
    }

    protected abstract int getLayoutResourceId();

    protected void setupBottomNavigation(BottomNavigationView bottomNavigationView) {




        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.nav_home:
                        if (!BaseActivity.this.getClass().equals(HomeActivity.class)) {
                            startActivity(new Intent(BaseActivity.this, HomeActivity.class));
                            finish();
                        } else {
//                            Toast.makeText(BaseActivity.this, "Already in Home", Toast.LENGTH_SHORT).show();
                        }
                        break;

                    case R.id.nav_schedule:
                        if (!BaseActivity.this.getClass().equals(ScheduleActivity.class)) {
                            startActivity(new Intent(BaseActivity.this, ScheduleActivity.class));
                            finish();
                        } else {
//                            Toast.makeText(BaseActivity.this, "Already in Schedule", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case R.id.nav_grades:
                        if (!BaseActivity.this.getClass().equals(GradesActivity.class)) {
//                            Toast.makeText(getApplicationContext(), "Opening Grades...", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(BaseActivity.this, GradesActivity.class);
                            startActivity(intent);
                        } else {
//                            Toast.makeText(getApplicationContext(), "Already in Grades", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case R.id.nav_messages:
                        if (!BaseActivity.this.getClass().equals(MessagesActivity.class)) {
//                            Toast.makeText(getApplicationContext(), "Opening Grades...", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(BaseActivity.this, MessagesActivity.class);
                            startActivity(intent);
                        } else {
//                            Toast.makeText(getApplicationContext(), "Already in Grades", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });
        // Highlight the current selected page based on Activity class
        if (this instanceof HomeActivity) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } else if (this instanceof ScheduleActivity) {
            bottomNavigationView.setSelectedItemId(R.id.nav_schedule);
        } else if (this instanceof GradesActivity) {
            bottomNavigationView.setSelectedItemId(R.id.nav_grades);
        } else if (this instanceof MessagesActivity) {
            bottomNavigationView.setSelectedItemId(R.id.nav_messages);
        }
        else if (this instanceof SettingsActivity) {
            bottomNavigationView.getMenu().setGroupCheckable(0, false, false);
        }
    }

    // Setup the account menu by setting a click listener on the person icon
    private void setupAccountMenu() {
        ImageButton accountButton = findViewById(R.id.account_button);
        if (accountButton != null) {
            accountButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAccountPopup(v);
                }
            });
        }
    }

    // Show the popup menu with icons using reflection hacks
    private void showAccountPopup(View anchor) {
        // Create PopupMenu normally (using ContextThemeWrapper didn't work in this case)
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());

        // Reflection hack to force icons to show in the PopupMenu
        try {
            Field[] fields = popupMenu.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popupMenu);
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                    Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Additional reflection hack to set the custom rounded background
        try {
            Field mPopupField = popupMenu.getClass().getDeclaredField("mPopup");
            mPopupField.setAccessible(true);
            Object menuPopupHelper = mPopupField.get(popupMenu);
            // Retrieve the internal ListView of the PopupMenu
            Method getListView = menuPopupHelper.getClass().getMethod("getListView");
            Object listViewObj = getListView.invoke(menuPopupHelper);
            if (listViewObj instanceof android.widget.ListView) {
                android.widget.ListView listView = (android.widget.ListView) listViewObj;
                // Set your custom drawable (rounded_popup.xml) as background
                listView.setBackgroundResource(R.drawable.rounded_menu);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_username:
//                        Toast.makeText(BaseActivity.this, "Username clicked", Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.menu_settings:
//                        Toast.makeText(BaseActivity.this, "Settings clicked", Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.menu_logout:
//                        Toast.makeText(BaseActivity.this, "Logout clicked", Toast.LENGTH_SHORT).show();
                        // Optionally, log out and navigate to LoginActivity
                        // startActivity(new Intent(BaseActivity.this, LoginActivity.class));
                        // finish();
                        return true;
                    default:
                        return false;
                }
            }
        });

        popupMenu.show();
    }


}
