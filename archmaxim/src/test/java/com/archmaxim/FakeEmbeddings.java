package com.archmaxim;

import com.archmaxim.search.TextNormalizer;

/**
 * Deterministic stand-in for BGE-M3: hashes normalized words into a unit vector,
 * so texts that share words end up close together.
 */
final class FakeEmbeddings {

    static final int DIMENSION = 1024;

    private FakeEmbeddings() {
    }

    static float[] of(String text) {
        float[] vector = new float[DIMENSION];
        for (String token : TextNormalizer.tokens(text)) {
            vector[Math.floorMod(token.hashCode(), DIMENSION)] += 1;
        }
        double norm = 0;
        for (float value : vector) {
            norm += value * value;
        }
        if (norm == 0) {
            vector[0] = 1;
            return vector;
        }
        float scale = (float) (1 / Math.sqrt(norm));
        for (int i = 0; i < vector.length; i++) {
            vector[i] *= scale;
        }
        return vector;
    }
}
