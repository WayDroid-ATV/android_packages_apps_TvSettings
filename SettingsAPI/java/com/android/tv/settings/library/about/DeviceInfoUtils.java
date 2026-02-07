/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.tv.settings.library.about;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.util.Log;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

public class DeviceInfoUtils extends com.android.settingslib.DeviceInfoUtils {
    private static final String TAG = "DeviceInfoUtils";

    public static String getOpenGLVersion() {
        // Create an EGL Context
        // References:
        // [1] http://wlog.flatlib.jp/archive/1/2013-12-22
        // [2] packages/apps/Camera2/src/com/android/camera/SurfaceTextureRenderer.java

        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLSurface eglSurface = null;
        EGLContext eglContext = null;
        String result = null;

        // Initialize display
        EGLDisplay eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
            Log.w(TAG, "eglGetDisplay failed");
        }
        int[] iparam = new int[2];
        if (!egl.eglInitialize(eglDisplay, iparam)) {
            Log.w(TAG, "eglInitialize failed");
        }

        // Choose config
        EGLConfig[] eglConfigs = new EGLConfig[1];
        final int[] configSpec = { EGL10.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL10.EGL_NONE };
        if (egl.eglChooseConfig(eglDisplay, configSpec, eglConfigs, 1, iparam) && iparam[0] > 0) {
            // create surface
            SurfaceTexture surfaceTexture = new SurfaceTexture(0);
            eglSurface = egl.eglCreateWindowSurface(eglDisplay, eglConfigs[0], surfaceTexture, null);
            if (eglSurface == null || eglSurface == EGL10.EGL_NO_SURFACE) {
                Log.w(TAG, "eglCreateWindowSurface failed");
            } else {
                // Create context
                final int[] attribList = { EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
                eglContext = egl.eglCreateContext(
                        eglDisplay, eglConfigs[0], EGL10.EGL_NO_CONTEXT, attribList);
                if (eglContext == null || eglContext == EGL10.EGL_NO_CONTEXT) {
                    Log.w(TAG, "eglCreateContext failed");
                }

                // Bind context
                if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                    Log.w(TAG, "eglMakeCurrent failed");
                }
            }
        } else {
            Log.w(TAG, "eglChooseConfig failed");
        }

        result = "GL Vendor: " + GLES20.glGetString(GLES20.GL_VENDOR) + "\n" +
                 "GL Renderer: " + GLES20.glGetString(GLES20.GL_RENDERER) + "\n" +
                 "GL Version: " + GLES20.glGetString(GLES20.GL_VERSION);

        if (eglContext != null) {
            egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            egl.eglDestroyContext(eglDisplay, eglContext);
            egl.eglDestroySurface(eglDisplay, eglSurface);
        }

        return result;
    }
}
