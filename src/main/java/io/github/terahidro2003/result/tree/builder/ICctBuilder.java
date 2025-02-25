package io.github.terahidro2003.result.tree.builder;

public interface ICctBuilder {
    static CctBuilder builder() {
        return new CctBuilder();
    }
}
