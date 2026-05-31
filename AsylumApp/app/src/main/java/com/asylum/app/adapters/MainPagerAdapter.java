package com.asylum.app.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.asylum.app.fragments.ChatsFragment;
import com.asylum.app.fragments.PostsFragment;
import com.asylum.app.fragments.ProfileFragment;

import java.util.ArrayList;
import java.util.List;

public class MainPagerAdapter extends FragmentStateAdapter {

    private final List<Fragment> fragments = new ArrayList<>();

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        fragments.add(new PostsFragment());
        fragments.add(new ChatsFragment());
        fragments.add(new ProfileFragment());
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments.get(position);
    }

    @Override
    public int getItemCount() {
        return fragments.size();
    }
    
    public Fragment getFragment(int position) {
        return fragments.get(position);
    }
}
