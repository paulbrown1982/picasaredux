package com.picasaredux;

import com.picasaredux.view.App;

public class Main {
    public static void main(String[] args) {
        new App();
    }

    static Boolean returnProvidedForTestSuite(Boolean provided) {
        return provided;
    }
}
