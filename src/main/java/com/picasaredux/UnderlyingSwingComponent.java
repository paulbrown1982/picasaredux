package com.picasaredux;

import javax.swing.*;

public abstract class UnderlyingSwingComponent {
    private JComponent underlying;

    protected JComponent getComponent() {
        return underlying;
    }

    protected void setUnderlyingComponent(JComponent jComponent) {
        underlying = jComponent;
    }
}
