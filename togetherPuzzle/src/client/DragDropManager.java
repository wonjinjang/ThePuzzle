//togetherPuzzle/src/client/DragDropManager.java

package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class DragDropManager {
    public static void enableDrag(JLabel label, Runnable dropCallback) {
        final Point startPos = new Point();
        final boolean[] dragging = {false};

        label.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                startPos.setLocation(e.getPoint());
                dragging[0] = true;
            }
            public void mouseReleased(MouseEvent e) {
                if (dragging[0]) {
                    dragging[0] = false;
                    dropCallback.run();
                }
            }
        });

        label.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (dragging[0]) {
                    int x = label.getX() + e.getX() - startPos.x;
                    int y = label.getY() + e.getY() - startPos.y;
                    label.setLocation(x,y);
                }
            }
        });
    }
}
