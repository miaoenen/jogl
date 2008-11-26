/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 */

package com.sun.javafx.newt.kd;

import com.sun.javafx.newt.*;
import com.sun.opengl.impl.*;
import com.sun.opengl.impl.egl.*;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.NativeWindowException;

public class KDWindow extends Window {
    private static final String WINDOW_CLASS_NAME = "NewtWindow";
    // fullscreen size
    //   this will be correct _after_ setting fullscreen on,
    //   but KD has no method to ask for the display size
    private int fs_width=800, fs_height=480;
    // non fullscreen dimensions ..
    private int nfs_width, nfs_height, nfs_x, nfs_y;

    static {
        NativeLibLoader.loadNEWT();

        if (!initIDs()) {
            throw new RuntimeException("Failed to initialize jmethodIDs");
        }
    }

    public KDWindow() {
    }

    public final boolean isTerminalObject() {
        return true;
    }

    protected void createNative(GLCapabilities caps) {
        int eglRenderableType;
        if(GLProfile.isGLES1()) {
            eglRenderableType = EGL.EGL_OPENGL_ES_BIT;
        }
        else if(GLProfile.isGLES2()) {
            eglRenderableType = EGL.EGL_OPENGL_ES2_BIT;
        } else {
            eglRenderableType = EGL.EGL_OPENGL_BIT;
        }
        EGLConfig config = new EGLConfig(getDisplayHandle(), caps);
        visualID = config.getNativeConfigID();
        chosenCaps = config.getCapabilities();

        windowHandle = CreateWindow(getDisplayHandle(), visualID, eglRenderableType); 
        if (windowHandle == 0) {
            throw new RuntimeException("Error creating window: "+windowHandle);
        }
        nativeWindowHandle = RealizeWindow(windowHandle);
        if (nativeWindowHandle == 0) {
            throw new RuntimeException("Error native Window Handle is null");
        }

        windowHandleClose = windowHandle;
    }

    protected void closeNative() {
        if(0!=windowHandleClose) {
            CloseWindow(windowHandleClose);
        }
    }

    public void setVisible(boolean visible) {
        if(this.visible!=visible) {
            this.visible=visible;
            setVisible0(windowHandle, visible);
            clearEventMask();
        }
    }

    public void setSize(int width, int height) {
        setSize0(windowHandle, width, height);
    }

    public void setPosition(int x, int y) {
        // n/a in KD
        System.err.println("setPosition n/a in KD");
    }

    public boolean setFullscreen(boolean fullscreen) {
        if(this.fullscreen!=fullscreen) {
            this.fullscreen=fullscreen;
            if(this.fullscreen) {
                setFullScreen0(windowHandle, true);
            } else {
                setFullScreen0(windowHandle, false);
                setSize0(windowHandle, nfs_width, nfs_height);
            }
        }
        return true;
    }

    public int getDisplayWidth() {
        return fs_width;
    }

    public int getDisplayHeight() {
        return fs_height;
    }

    protected void dispatchMessages(int eventMask) {
        DispatchMessages(windowHandle, eventMask);
    }

    //----------------------------------------------------------------------
    // Internals only
    //

    private static native boolean initIDs();
    private        native long CreateWindow(long displayHandle, long eglConfig, int eglRenderableType);
    private        native long RealizeWindow(long windowHandle);
    private        native int  CloseWindow(long windowHandle);
    private        native void setVisible0(long windowHandle, boolean visible);
    private        native void setSize0(long windowHandle, int width, int height);
    private        native void setFullScreen0(long windowHandle, boolean fullscreen);
    private        native void DispatchMessages(long windowHandle, int eventMask);

    private void sizeChanged(int newWidth, int newHeight) {
        width = newWidth;
        height = newHeight;
        if(!fullscreen) {
            nfs_width=width;
            nfs_height=height;
        } else {
            fs_width = width;
            fs_height = width;
        }
        sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED);
    }

    private void windowClosed() {
    }

    private long   nativeWindowHandle; // THE KD underlying native window handle
    private long   windowHandleClose;
}