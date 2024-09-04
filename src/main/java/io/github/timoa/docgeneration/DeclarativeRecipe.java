package io.github.timoa.docgeneration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class DeclarativeRecipe {

    String type;
    String name;
    String displayName;
    String description;
    Object[] recipeList;
}
