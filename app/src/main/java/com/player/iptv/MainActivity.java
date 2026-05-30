package com.player.iptv;

import android.content.Intent;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.player.iptv.data.CredentialRepository;
import com.player.iptv.utils.DialogUtils;
import com.player.iptv.view.CategoriesFragment;
import com.player.iptv.view.FavoritosFragment;
import com.player.iptv.view.HistoricoFragment;
import com.player.iptv.view.HomeFragment;
import com.player.iptv.view.LiveChannelsFragment;
import com.player.iptv.view.MovieDetailFragment;
import com.player.iptv.view.MoviesFragment;
import com.player.iptv.view.SeriesDetailFragment;
import com.player.iptv.view.SeriesFragment;
import com.player.iptv.view.SettingsFragment;
import com.player.iptv.view.SyncFragment;

public class MainActivity extends AppCompatActivity {

    private boolean isMenuExpanded = false;
    private View sidebar;
    private int activeMenuItemId = -1;

    private final int[] menuItemIds = {
        R.id.menuInicio,
        R.id.menuAoVivo,
        R.id.menuFilmes,
        R.id.menuSeries,
        R.id.menuFavoritos,
        R.id.menuListas,
        R.id.menuHistorico,
        R.id.menuCategorias,
        R.id.menuConfiguracoes
    };

    private final int[] sidebarFocusables = {
        R.id.menuInicio,
        R.id.menuAoVivo,
        R.id.menuFilmes,
        R.id.menuSeries,
        R.id.menuFavoritos,
        R.id.menuListas,
        R.id.menuHistorico,
        R.id.menuCategorias,
        R.id.menuConfiguracoes,
        R.id.btnPremiumTop
    };

    private final int[] viewsToToggle = {
        R.id.tvAppTitle,
        R.id.tvMenuInicio,
        R.id.tvMenuAoVivo,
        R.id.tvMenuFilmes,
        R.id.tvMenuSeries,
        R.id.tvMenuFavoritos,
        R.id.tvMenuListas,
        R.id.tvMenuHistorico,
        R.id.tvMenuCategorias,
        R.id.tvMenuConfiguracoes,
        R.id.premiumCard
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sidebar = findViewById(R.id.sidebar);

        setupSidebarFocus();
        setupMenuListeners();

        if (savedInstanceState == null) {
            if (getIntent().getBooleanExtra("start_sync", false)) {
                showSyncFragment();
            } else {
                setActiveMenu(R.id.menuInicio);
                loadFragment(new HomeFragment());
                scheduleSync();
            }
        }

//        findViewById(R.id.menuPlayer).setOnClickListener(v -> {
//            Intent intent = new Intent(this, PlayerLiveActivity.class);
//            startActivity(intent);
//        });
    }

    private void setupSidebarFocus() {
        View.OnFocusChangeListener expandOnFocus = (v, hasFocus) -> {
            if (hasFocus && !isMenuExpanded) {
                expandMenu();
                if (activeMenuItemId != -1) {
                    View activeItem = findViewById(activeMenuItemId);
                    if (activeItem != null) {
                        activeItem.post(() -> activeItem.requestFocus());
                    }
                }
            }
        };

        for (int id : sidebarFocusables) {
            View item = findViewById(id);
            if (item != null) {
                item.setOnFocusChangeListener(expandOnFocus);
            }
        }

        getWindow().getDecorView().getViewTreeObserver()
            .addOnGlobalFocusChangeListener((oldFocus, newFocus) -> {
                if (newFocus == null || !isChildOfSidebar(newFocus)) {
                    if (isMenuExpanded) {
                        collapseMenu();
                    }
                }
            });
    }

    private boolean isChildOfSidebar(View view) {
        while (view != null) {
            if (view == sidebar) return true;
            if (view.getParent() instanceof View) {
                view = (View) view.getParent();
            } else {
                break;
            }
        }
        return false;
    }

    private void expandMenu() {
        Transition t = new AutoTransition();
        t.setDuration(200);
        TransitionManager.beginDelayedTransition((ViewGroup) sidebar.getParent(), t);

        isMenuExpanded = true;
        ViewGroup.LayoutParams params = sidebar.getLayoutParams();
        params.width = dpToPx(150);
        sidebar.setLayoutParams(params);

        for (int id : viewsToToggle) {
            View view = findViewById(id);
            if (view != null) {
                view.setVisibility(View.VISIBLE);
            }
        }
    }

    private void collapseMenu() {
        Transition t = new AutoTransition();
        t.setDuration(200);
        TransitionManager.beginDelayedTransition((ViewGroup) sidebar.getParent(), t);

        isMenuExpanded = false;
        ViewGroup.LayoutParams params = sidebar.getLayoutParams();
        params.width = dpToPx(50);
        sidebar.setLayoutParams(params);

        for (int id : viewsToToggle) {
            View view = findViewById(id);
            if (view != null) {
                view.setVisibility(View.GONE);
            }
        }
    }

    private void setupMenuListeners() {
        for (int id : menuItemIds) {
            View item = findViewById(id);
            if (item != null) {
                item.setOnClickListener(v -> {
                    if (v.getId() == activeMenuItemId) return;
                    setActiveMenu(v.getId());
                    loadFragment(createFragmentForMenu(v.getId()));
                });
            }
        }
    }

    /**
     * Navega para um item de menu a partir de outro Fragment (ex: HomeFragment).
     */
    public void navigateTo(int menuItemId) {
        setActiveMenu(menuItemId);
        loadFragment(createFragmentForMenu(menuItemId));
    }

    private void setActiveMenu(int menuItemId) {
        if (activeMenuItemId != -1) {
            setMenuItemStyle(activeMenuItemId, false);
        }
        activeMenuItemId = menuItemId;
        setMenuItemStyle(menuItemId, true);
    }

    private void setMenuItemStyle(int menuItemId, boolean active) {
        ViewGroup item = findViewById(menuItemId);
        if (item == null) return;

        item.setSelected(active);

        if (active) {
            item.setBackgroundResource(R.drawable.bg_menu_item_active);
        } else {
            item.setBackgroundResource(R.drawable.bg_menu_item);
        }

        for (int i = 0; i < item.getChildCount(); i++) {
            View child = item.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                tv.setTextColor(ContextCompat.getColor(this,
                        active ? R.color.text_primary : R.color.text_secondary));
                tv.setTypeface(null,
                        active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            } else if (child instanceof ImageView) {
                ImageView iv = (ImageView) child;
                iv.setColorFilter(ContextCompat.getColor(this,
                        active ? R.color.colorAccent : R.color.text_secondary),
                        android.graphics.PorterDuff.Mode.SRC_IN);
            }
        }
    }

    private Fragment createFragmentForMenu(int menuItemId) {
        if (menuItemId == R.id.menuInicio) {
            return new HomeFragment();
        } else if (menuItemId == R.id.menuAoVivo) {
            return new LiveChannelsFragment();
        } else if (menuItemId == R.id.menuFilmes) {
            return new MoviesFragment();
        } else if (menuItemId == R.id.menuSeries) {
            return new SeriesFragment();
        } else if (menuItemId == R.id.menuFavoritos) {
            return new FavoritosFragment();
        } else if (menuItemId == R.id.menuHistorico) {
            return new HistoricoFragment();
        } else if (menuItemId == R.id.menuCategorias) {
            return new CategoriesFragment();
        } else if (menuItemId == R.id.menuConfiguracoes) {
            return new SettingsFragment();
        }
        return new HomeFragment();
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit();
    }

    private void showSyncFragment() {
        SyncFragment syncFragment = new SyncFragment();
        syncFragment.setSyncListener(new SyncFragment.SyncListener() {
            @Override
            public void onSyncCompleted() {
                runOnUiThread(() -> {
                    setActiveMenu(R.id.menuInicio);
                    loadFragment(new HomeFragment());
                    scheduleSync();
                });
            }

            @Override
            public void onSyncError(String message) {
            }
        });
        loadFragment(syncFragment);
    }

    private void scheduleSync() {
        CredentialRepository repo = new CredentialRepository(this);
        repo.scheduleSync();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public void onBackPressed() {
        DialogUtils.showExitAppDialog(this, () -> finishAffinity());
    }
}
