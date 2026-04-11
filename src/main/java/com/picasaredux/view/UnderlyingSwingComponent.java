package com.picasaredux.view;

import javax.swing.*;

abstract class UnderlyingSwingComponent {
    private JComponent underlying;

    JComponent getComponent() {
        return underlying;
    }

    void setUnderlyingComponent(JComponent jComponent) {
        underlying = jComponent;
    }
}
