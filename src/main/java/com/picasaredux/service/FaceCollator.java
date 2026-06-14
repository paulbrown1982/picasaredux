package com.picasaredux.service;

import com.picasaredux.model.ImageFileInTree;

import java.util.List;

public interface FaceCollator {
    List<List<ImageFileInTree>> cluster(List<ImageFileInTree> images);
}
