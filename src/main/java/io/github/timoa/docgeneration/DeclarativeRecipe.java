package io.github.timoa.docgeneration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Value;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class DeclarativeRecipe {

    String type;
    String name;
    String displayName;
    String description;
    List<String> recipeList;
}
