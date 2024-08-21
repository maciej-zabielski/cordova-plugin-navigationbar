/*
 * Copyright (c) 2016 by Vinicius Fagundes. All rights reserved.
 *
 * This file contains Original Code and/or Modifications of Original Code
 * as defined in and that are subject to the Apache License
 * Version 2.0 (the 'License'). You may not use this file except in
 * compliance with the License. Please obtain a copy of the License at
 * http://opensource.org/licenses/Apache-2.0/ and read it before using this
 * file.
 *
 * The Original Code and all software distributed under the License are
 * distributed on an 'AS IS' basis, WITHOUT WARRANTY OF ANY KIND, EITHER
 * EXPRESS OR IMPLIED, AND APPLE HEREBY DISCLAIMS ALL SUCH WARRANTIES,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, QUIET ENJOYMENT OR NON-INFRINGEMENT.
 * Please see the License for the specific language governing rights and
 * limitations under the License.
 *
 */

package com.viniciusfagundes.cordova.plugin.navigationbar;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONException;

public class NavigationBar extends CordovaPlugin {
    private static final String TAG = "NavigationBar";

    @Override
    public void initialize(final CordovaInterface cordova, CordovaWebView webView) {
        LOG.v(TAG, "NavigationBar: initialization");
        super.initialize(cordova, webView);

        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Clear FLAG_FORCE_NOT_FULLSCREEN which is set initially by Cordova.
                Window window = cordova.getActivity().getWindow();
                window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

                // Read 'NavigationBarBackgroundColor' and 'NavigationBarLight' from config.xml, default is #000000.
                setNavigationBarBackgroundColor(preferences.getString("NavigationBarBackgroundColor", "#000000"), preferences.getBoolean("NavigationBarLight", false));
            }
        });
    }

    @Override
    public boolean execute(final String action, final CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        LOG.v(TAG, "Executing action: " + action);
        final Activity activity = this.cordova.getActivity();
        final Window window = activity.getWindow();

        if ("_ready".equals(action)) {
            boolean navigationBarVisible = (window.getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) == 0;
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, navigationBarVisible));
            return true;
        }

        if ("show".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        int uiOptions = window.getDecorView().getSystemUiVisibility();
                        uiOptions &= ~View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
                        uiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

                        window.getDecorView().setSystemUiVisibility(uiOptions);

                        window.getDecorView().setOnFocusChangeListener(null);
                        window.getDecorView().setOnSystemUiVisibilityChangeListener(null);
                    }
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
            });
            return true;
        }

        if ("hide".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        final int uiOptions = window.getDecorView().getSystemUiVisibility()
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

                        window.getDecorView().setSystemUiVisibility(uiOptions);

                        window.getDecorView().setOnFocusChangeListener(new View.OnFocusChangeListener() {
                            @Override
                            public void onFocusChange(View v, boolean hasFocus) {
                                if (hasFocus) {
                                    window.getDecorView().setSystemUiVisibility(uiOptions);
                                }
                            }
                        });

                        window.getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                            @Override
                            public void onSystemUiVisibilityChange(int visibility) {
                                window.getDecorView().setSystemUiVisibility(uiOptions);
                            }
                        });
                    }
                }
            });
            return true;
        }

        if ("backgroundColorByHexString".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        setNavigationBarBackgroundColor(args.getString(0), args.getBoolean(1));
                    } catch (JSONException ignore) {
                        LOG.e(TAG, "Invalid hexString argument, use f.i. '#777777'");
                    }
                }
            });
            return true;
        }

        return false;
    }

    private void setNavigationBarBackgroundColor(final String colorPref, Boolean lightNavigationBar) {
        lightNavigationBar = lightNavigationBar == null ? false : lightNavigationBar;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Window window = cordova.getActivity().getWindow();
            int uiOptions = window.getDecorView().getSystemUiVisibility();

            // Ensure FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS is set
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            // Handle lightNavigationBar setting for Android O (API 26) and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (lightNavigationBar) {
                    uiOptions |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                } else {
                    uiOptions &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }

            // Handle Android 11 (API 30) and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false);
                WindowInsetsController insetsController = window.getInsetsController();
                if (insetsController != null) {
                    if (lightNavigationBar) {
                        insetsController.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
                    } else {
                        insetsController.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
                    }
                }
            }

            // Apply the UI options
            window.getDecorView().setSystemUiVisibility(uiOptions);

            try {
                // Set the navigation bar color
                window.getClass().getDeclaredMethod("setNavigationBarColor", int.class)
                        .invoke(window, Color.parseColor(colorPref));
            } catch (IllegalArgumentException e) {
                LOG.e(TAG, "Invalid hexString argument, use e.g., '#999999'", e);
            } catch (Exception e) {
                LOG.w(TAG, "Method window.setNavigationBarColor not found for SDK level " + Build.VERSION.SDK_INT, e);
            }
        }
    }
}
