package com.picasaredux;


import javax.swing.*;
import java.io.IOException;

class AlbumSelector extends UnderlyingSwingComponent {

    final JPanel panel;
    final JTextField jtf;
    final JButton go;

    public AlbumSelector() {
        jtf = new JTextField();
        go = new JButton("Select album folder");

        panel = new JPanel();
        panel.add(jtf);
        panel.add(go);

        setUnderlyingComponent(panel);
    }

    public void setupActionListeners(VerticalSlider verticalSlider) {
        verticalSlider.hide();

        go.addActionListener(_ -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new java.io.File(".."));
            chooser.setDialogTitle("Pick an album folder");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);

            JRootPane jrp = panel.getRootPane();

            int result = chooser.showOpenDialog(jrp);
            if (result == JFileChooser.APPROVE_OPTION) {
                try {
                    String albumFolder = chooser.getSelectedFile().getCanonicalPath();
                    jtf.setText(albumFolder);
                    jtf.setColumns(albumFolder.length());

                    verticalSlider.setAlbum(albumFolder);
                    verticalSlider.show();

                    jrp.revalidate();
                    jrp.repaint();
                } catch (IOException ioe) {
                    System.err.println("Cannot load album: " + ioe.getMessage());
                }
            }
        });
    }
}
