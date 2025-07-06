package com.yuanseen.shuati;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.yuanseen.shuati.databinding.ActivityMainBinding;
import com.yuanseen.shuati.ui.gallery.GalleryFragment;
import com.yuanseen.shuati.ui.home.HomeFragment;
import com.yuanseen.shuati.ui.slideshow.SlideshowFragment;
import com.yuanseen.shuati.ui2.QuizHomeActivity;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private TextView cardContentTextView; // 改为使用内容TextView
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 获取抽屉布局和导航视图
        drawer = binding.drawerLayout;
        navigationView = binding.navView;


        // 设置FAB点击打开抽屉
        binding.fab.setOnClickListener(view -> {
            drawer.openDrawer(GravityCompat.START);
        });

        // 设置导航菜单项点击监听
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                showHomeFragment();
            } else if (itemId == R.id.nav_gallery) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new GalleryFragment(),"gallery")
                        .commit();
            } else if (itemId == R.id.nav_slideshow) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SlideshowFragment())
                        .commit();
            }

            drawer.closeDrawer(GravityCompat.START);
            return true;
        });
// 修改后的延迟初始化代码
        navigationView.postDelayed(() -> {
            try {
                // 查找已经包含在NavigationView中的卡片视图
                // 注意：include标签默认不会为根视图生成id，所以需要给include标签添加id
                View cardView = navigationView.findViewById(R.id.included_card_view);

                if (cardView != null) {
                    cardContentTextView = cardView.findViewById(R.id.tv_card_content);
                    progressBar = cardView.findViewById(R.id.progress_bar);
                    fetchNotice();
                } else {
                    Log.e("MainActivity", "Card view not found in NavigationView");
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Error initializing card view", e);
            }
        }, 100);


        // 默认显示首页
        if (savedInstanceState == null) {
            showHomeFragment();
        }

        initNavHeader();
    }

    private void initNavHeader(){
        // Customize the header view
        View headerView = navigationView.getHeaderView(0);
        TextView line1 = headerView.findViewById(R.id.header_line1);
        TextView line2 = headerView.findViewById(R.id.header_line2);
        TextView line3 = headerView.findViewById(R.id.header_line3);
//        ImageView headerImage = headerView.findViewById(R.id.header_image);
        // Set your custom values
        line1.setText("测试版本");
        line2.setText("联系邮箱:");
        line3.setText("YuanSeen@foxmail.com");

        // You can also set a click listener on the header if needed
        headerView.setOnClickListener(v -> {
            // Handle header click
        });
    }

    // 获取公告信息
    private void fetchNotice() {
        if (cardContentTextView == null || progressBar == null) {
            Log.e("MainActivity", "Views are not initialized");
            return;
        }

        runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            cardContentTextView.setText("加载中...");
        });

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://Mc2.bxvps.cn:24721/api/notices")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("MainActivity", "Network request failed", e);
                runOnUiThread(() -> {
                    if (cardContentTextView != null && progressBar != null) {
                        cardContentTextView.setText("公告加载失败: " + e.getMessage());
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    String responseData = response.body().string();
                    Log.d("MainActivity", "Response data: " + responseData); // 添加日志

                    JSONObject jsonObject = new JSONObject(responseData);
                    final String notice = jsonObject.getString("menunotice");

                    runOnUiThread(() -> {
                        if (cardContentTextView != null && progressBar != null) {
                            cardContentTextView.setText(notice);
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                } catch (Exception e) {
                    Log.e("MainActivity", "Error parsing response", e);
                    runOnUiThread(() -> {
                        if (cardContentTextView != null && progressBar != null) {
                            cardContentTextView.setText("公告解析失败: " + e.getMessage());
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }
    // 显示HomeFragment并更新导航菜单选中状态
    private void showHomeFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
        navigationView.setCheckedItem(R.id.nav_home);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 检查当前显示的Fragment是否是HomeFragment
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (!(currentFragment instanceof HomeFragment)) {
                // 如果不是HomeFragment，则返回HomeFragment
                showHomeFragment();
                return true; // 消费返回键事件
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        // 检查抽屉是否打开
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        // 检查当前显示的Fragment是否是HomeFragment
        else {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (!(currentFragment instanceof HomeFragment)) {
                // 如果不是HomeFragment，则返回HomeFragment
                showHomeFragment();
            } else {
                // 如果是HomeFragment，则执行默认的返回行为
                super.onBackPressed();
            }
        }
    }
}