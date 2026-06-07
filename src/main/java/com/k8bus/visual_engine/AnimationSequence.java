package com.k8bus.visual_engine;

import java.util.List;

public record AnimationSequence(String id, List<AnimationFrame> frames, boolean loop) {

}