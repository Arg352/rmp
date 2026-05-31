package com.asylum.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.asylum.app.adapters.MainPagerAdapter;
import com.asylum.app.api.SocketManager;
import com.asylum.app.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private BottomNavigationView bottomNav;
    private ViewPager2 viewPager;
    private MainPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);

        viewPager = findViewById(R.id.viewPager);
        bottomNav = findViewById(R.id.bottom_navigation);

        pagerAdapter = new MainPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Плавная анимация перехода
        viewPager.setPageTransformer((page, position) -> {
            page.setAlpha(0f);
            page.setVisibility(View.VISIBLE);

            // Start animation for a specific range of positions
            if (position >= -1 && position <= 1) {
                page.setAlpha(1 - Math.abs(position));
            }
        });

        // Синхронизация ViewPager с BottomNav
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0: bottomNav.setSelectedItemId(R.id.navigation_posts); break;
                    case 1: bottomNav.setSelectedItemId(R.id.navigation_chats); break;
                    case 2: bottomNav.setSelectedItemId(R.id.navigation_profile); break;
                }
            }
        });

        // Клик по меню переключает ViewPager
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_posts) viewPager.setCurrentItem(0);
            else if (itemId == R.id.navigation_chats) viewPager.setCurrentItem(1);
            else if (itemId == R.id.navigation_profile) viewPager.setCurrentItem(2);
            return true;
        });

        // Default item
        if (savedInstanceState == null) {
            viewPager.setCurrentItem(1, false); // По умолчанию чаты
        }
    }

    public void navigateToTab(int itemId, Bundle args) {
        if (bottomNav != null) {
            if (itemId == R.id.navigation_posts) viewPager.setCurrentItem(0);
            else if (itemId == R.id.navigation_chats) viewPager.setCurrentItem(1);
            else if (itemId == R.id.navigation_profile) viewPager.setCurrentItem(2);
            
            // Если нужно передать аргументы во фрагмент
            if (args != null) {
                int index = (itemId == R.id.navigation_posts) ? 0 : (itemId == R.id.navigation_chats ? 1 : 2);
                pagerAdapter.createFragment(index).setArguments(args);
            }
        }
    }

    public void logout() {
        SocketManager.getInstance().disconnect();
        sessionManager.logout();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
