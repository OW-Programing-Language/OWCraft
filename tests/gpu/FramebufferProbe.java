package org.owcraft.test;

import dev.oreo.RuntimeExtension;
import java.nio.ByteBuffer;
import java.util.List;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL33.*;

/**
 * Test-only GPU probe for the real OW-Engine/OW-Graphics terrain path.
 * Loads via ServiceLoader ONLY for the smoke test; never used by the game.
 */
public final class FramebufferProbe implements RuntimeExtension {
    @Override public boolean supports(String operation) {
        return "craft.test.framebuffer".equals(operation);
    }

    @Override public Object invoke(String operation, List<Object> args) {
        if (!supports(operation) || !args.isEmpty())
            throw new IllegalArgumentException("Invalid framebuffer sample request");
        glFinish();
        glReadBuffer(GL_BACK);
        int colored = 0;
        int green = 0;
        // Grid samples avoid depending on a single camera/terrain pixel.
        for (int y = 35; y <= 205; y += 17) {
            for (int x = 30; x <= 290; x += 20) {
                ByteBuffer rgba = BufferUtils.createByteBuffer(4);
                glReadPixels(x, y, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, rgba);
                int r = rgba.get(0) & 255;
                int g = rgba.get(1) & 255;
                int b = rgba.get(2) & 255;
                if (Math.abs(r - 12) + Math.abs(g - 18) + Math.abs(b - 25) > 80)
                    colored++;
                if (g > r + 13 && g > b + 12 && g > 48)
                    green++;
            }
        }
        if (glGetError() != GL_NO_ERROR)
            throw new IllegalStateException("OpenGL framebuffer produced errors");
        return List.of(colored, green);
    }
}
