/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.renderer.impl.lwjgl;

import java.awt.*;
import java.awt.image.ImageObserver;
import java.text.AttributedCharacterIterator;

public final class NullGraphics extends Graphics {
    @Override
    public void translate(int x, int y) {
    }

    @Override
    public void setXORMode(Color c1) {
    }

    @Override
    public void setPaintMode() {
    }

    @Override
    public void setFont(Font font) {
    }

    @Override
    public void setColor(Color c) {
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
    }

    @Override
    public void setClip(Shape clip) {
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        return null;
    }

    @Override
    public Font getFont() {
        return null;
    }

    @Override
    public Color getColor() {
        return null;
    }

    @Override
    public Rectangle getClipBounds() {
        return null;
    }

    @Override
    public Shape getClip() {
        return null;
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
    }

    @Override
    public void drawString(String str, int x, int y) {
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2,
                             int sy2, Color bgcolor, ImageObserver observer) {
        return false;
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2,
                             int sy2, ImageObserver observer) {
        return false;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor,
                             ImageObserver observer) {
        return false;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        return false;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        return false;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        return false;
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public Graphics create() {
        return null;
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
    }
}