package com.ferox.scene.task.ffp;

import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.ferox.math.Vector4;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.HardwareAccessLayer;

public class ColorState implements State {
    public static final Vector4 DEFAULT_DIFFUSE = new Vector4(0.8, 0.8, 0.8, 1.0);
    public static final Vector4 DEFAULT_SPECULAR = new Vector4(0.0, 0.0, 0.0, 1.0);
    public static final Vector4 DEFAULT_EMITTED = new Vector4(0.0, 0.0, 0.0, 1.0);
    public static final Vector4 DEFAULT_AMBIENT = new Vector4(0.2, 0.2, 0.2, 1.0);

    private final Vector4 diffuse = new Vector4();
    private final Vector4 specular = new Vector4();
    private final Vector4 emitted = new Vector4();

    private double shininess;

    public void set(@Const ColorRGB diffuse, @Const ColorRGB specular,
                    @Const ColorRGB emitted, double alpha, double shininess) {
        if (diffuse == null) {
            this.diffuse.set(DEFAULT_DIFFUSE).w = alpha;
        } else {
            this.diffuse.set(diffuse.red(), diffuse.green(), diffuse.blue(), alpha);
        }

        if (specular == null) {
            this.specular.set(DEFAULT_SPECULAR).w = alpha;
        } else {
            this.specular.set(specular.red(), specular.green(), specular.blue(), alpha);
        }

        if (emitted == null) {
            this.emitted.set(DEFAULT_EMITTED).w = alpha;
        } else {
            this.emitted.set(emitted.red(), emitted.green(), emitted.blue(), alpha);
        }

        this.shininess = shininess;
    }

    @Override
    public void visitNode(StateNode currentNode, AppliedEffects effects,
                          HardwareAccessLayer access) {
        FixedFunctionRenderer r = access.getCurrentContext().getFixedFunctionRenderer();

        r.setMaterial(DEFAULT_AMBIENT, diffuse, specular, emitted);
        r.setMaterialShininess(shininess);

        currentNode.visitChildren(effects, access);
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(shininess);

        int hash = 17 * (int) (bits ^ (bits >>> 32));
        hash = 31 * hash + diffuse.hashCode();
        hash = 31 * hash + emitted.hashCode();
        hash = 31 * hash + specular.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ColorState)) {
            return false;
        }

        ColorState ts = (ColorState) o;
        return ts.diffuse.equals(diffuse) && ts.emitted.equals(emitted) && ts.specular.equals(specular) && shininess == shininess;
    }
}
