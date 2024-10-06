package io.github.timoa.misc;

import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.style.NamedStyles;

import java.util.Arrays;

import static java.util.Collections.emptySet;
import static org.openrewrite.Tree.randomId;

public class JacksonImportStyle extends NamedStyles {
    public JacksonImportStyle() {
        super(randomId(),
                "io.github.timoa.misc.JacksonImportStyle",
                "custom style",
                "custom style for the Jackson project",
                emptySet(),
                Arrays.asList(
                        importLayout(),
                        //needed so continuation indent, e.g. an `extends` on th next line is not modified.
                        new TabsAndIndentsStyle(false, -2, -2, -2, false, null
        )

                ));
    }

    private static ImportLayoutStyle importLayout() {
        return ImportLayoutStyle.builder()
                .importPackage("java.*")
                .blankLine()
                .importAllOthers()
                .blankLine()
                .importPackage("com.fasterxml.jackson.annotation.*")
                .blankLine()
                .importPackage("com.fasterxml.jackson.core.*")
                .importPackage("com.fasterxml.jackson.databind.*")
                .importPackage("tools.jackson.*")//needed for master
                .blankLine()
                .importPackage("com.fasterxml.jackson.other.modules.*")
                .blankLine()
                .importStaticAllOthers()
                .classCountToUseStarImport(Integer.MAX_VALUE)//disable collapsing imports to *
                .nameCountToUseStarImport(Integer.MAX_VALUE)//disable collapsing imports to *
                .build();
    }

}
