package io.github.timoa;

import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.style.NamedStyles;

import java.util.Collections;

import static java.util.Collections.emptySet;
import static org.openrewrite.Tree.randomId;

public class JacksonImportStyle extends NamedStyles {
    public JacksonImportStyle() {
        super(randomId(),
                "io.github.timoa.misc.JacksonImportStyle",
                "custom style",
                "custom style for the Jackson project",
                emptySet(),
                Collections.singletonList(importLayout()));
    }

    private static ImportLayoutStyle importLayout() {
        return ImportLayoutStyle.builder()
                .importAllOthers()
                .blankLine()
                .importPackage("com.fasterxml.jackson.core.*")
                .importPackage("tools.jackson.*")
                .blankLine()
                .importStaticAllOthers()
                .build();
    }

}
