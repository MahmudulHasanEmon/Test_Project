package com.islamiclifestyle.youtubevideos.ui.menu;

import android.view.View;

public interface YouTubePlayerMenu {
    void show(View anchorView);
    void dismiss();

    void addItem(MenuItem menuItem);

    void addItem(android.view.MenuItem menuItem);

    void removeItem(int itemIndex);
    int getItemCount();
}