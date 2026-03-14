package com.picasaredux;

import javax.swing.*;
import java.awt.*;

class App {

    public App() {
        JFrame frame = new JFrame("Picasa Redux");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        frame.add(generateLayout());
        frame.setVisible(true);
    }

    private static Component generateLayout() {
        AlbumSelector topPane = new AlbumSelector();
        VerticalSlider verticalSlider = new VerticalSlider();

        topPane.setupActionListeners(verticalSlider);

        JPanel frame = new JPanel(new BorderLayout());
        frame.add(topPane.getComponent(), BorderLayout.PAGE_START);
        frame.add(verticalSlider.getComponent(), BorderLayout.CENTER);
        return frame;
    }
}
