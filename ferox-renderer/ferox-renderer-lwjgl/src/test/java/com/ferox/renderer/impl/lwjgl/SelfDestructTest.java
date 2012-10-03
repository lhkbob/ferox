package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.DisplayMode;
import com.ferox.renderer.DisplayMode.PixelFormat;
import com.ferox.renderer.Framework;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.Task;

public class SelfDestructTest {
    public static void main(String[] args) throws Exception {
        final Framework f = LwjglFramework.create();
        System.out.println("framework created");
        final OnscreenSurface surface = f.createSurface(new OnscreenSurfaceOptions().setFullscreenMode(new DisplayMode(1024,
                                                                                                                       768,
                                                                                                                       PixelFormat.RGB_24BIT))
                                                                                    .setUndecorated(true)
                                                                                    .setResizable(false)
                                                                                    .setWidth(500)
                                                                                    .setHeight(500));

        System.out.println("surface created");
        Thread.sleep(5000);

        String result = f.queue(new Task<String>() {
            @Override
            public String run(HardwareAccessLayer access) {
                System.out.println("activating surface");
                access.setActiveSurface(surface);
                System.out.println("destroying framework");
                f.destroy();
                return "finished";
            }
        }).get();

        System.out.println(result);
    }
}
