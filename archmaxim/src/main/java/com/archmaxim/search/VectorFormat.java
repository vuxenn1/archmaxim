package com.archmaxim.search;

final class VectorFormat {

    private VectorFormat() {
    }

    // pgvector's text input format: [0.1,0.2,...]
    static String toLiteral(float[] vector) {
        StringBuilder literal = new StringBuilder(vector.length * 12).append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                literal.append(',');
            }
            literal.append(vector[i]);
        }
        return literal.append(']').toString();
    }
}
