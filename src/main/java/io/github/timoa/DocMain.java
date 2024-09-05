package io.github.timoa;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import io.github.timoa.docgeneration.DeclarativeRecipe;
import io.github.timoa.lombok.SummarizeDataParameterized;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import org.apache.commons.lang3.Validate;
import org.openrewrite.Recipe;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.HashMap;
import java.util.Map;

import nl.jworks.markdown_to_asciidoc.Converter;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class DocMain {

    private static final String TEMPLATE_DIR;
    private static final String CONFIG_DIR;
    private static final String COMMON_FILE;
    private static final String DECLARATIVE_RECIPES;
    static {
        String ROOT = System.getenv("MY_PATH");
        TEMPLATE_DIR = ROOT + "/src/docs/adoc/modules/recipes/pages";
        CONFIG_DIR   = ROOT + "/src/docs/config";
        COMMON_FILE  = CONFIG_DIR + "/common.ftl";
        DECLARATIVE_RECIPES = ROOT + "/src/main/resources/META-INF/rewrite";
    }

    private static final Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);

    public static void main(String[] args) throws IOException, TemplateException, ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        cfg.setDirectoryForTemplateLoading(new File(TEMPLATE_DIR));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        BiPredicate<Path, BasicFileAttributes> matcher = (p, bfa) -> bfa.isRegularFile()
                && p.getFileName().toString().matches(".*\\.stub")
                && !p.getFileName().toString().startsWith("a_");//marker prefix for custom pages

        List<Path> collect = Files.find(Paths.get(TEMPLATE_DIR), 999, matcher).collect(Collectors.toList());

        Map<String, Path> coordinatesToPaths = new HashMap<>();

        for (Path path : collect) {
            List<String> stubLines = Files.readAllLines(path);
            Info nameAndDescription = extractNameAndDescription(stubLines);

            Path freeMarkerPath = getNextPath(path, ".ftl");
            addCommonPart(freeMarkerPath, stubLines);

            Path targetPath = getNextPath(path, ".adoc");
            Path templatePath = Paths.get(DocMain.TEMPLATE_DIR).relativize(freeMarkerPath.toAbsolutePath());
            applyTemplate(templatePath, targetPath, nameAndDescription);
            coordinatesToPaths.put(nameAndDescription.coordinates, Paths.get(DocMain.TEMPLATE_DIR).relativize(targetPath));
        }

        /* custom templating */
        // log to lombok manual
        //read lines from slf4j result [tabs] ==== .... ====
        List<String> tabSource = Files.readAllLines(Paths.get(TEMPLATE_DIR + "/lombok/log/ConvertSlf4j.adoc"));
        List<String> slice1 = tabSource.subList(tabSource.indexOf("[tabs]"), tabSource.size());
        int index2 = 2 + slice1.subList(2, slice1.size()).indexOf("====");
        List<String> targetLines = slice1.subList(0, index2 + 1);

        HashMap<String, Object> datamodel = new HashMap<>();
        datamodel.put("applytabs", targetLines);

        Path templatePath = Paths.get(TEMPLATE_DIR + "/lombok/log/__LogManual.ftl");
        Template template = cfg.getTemplate(templatePath.toAbsolutePath().toString().substring(DocMain.TEMPLATE_DIR.length()));

        // File output
        Path targetPath = Paths.get(TEMPLATE_DIR + "/lombok/log/a_LogManual.adoc");//somehow leading underscores are not recognized by antora
        Writer file = new FileWriter(targetPath.toFile());
        template.process(datamodel, file);
        file.flush();
        file.close();

        //declarative recipes
        //todo maintain target path in a comment line in the declarative recipe
        Path[][] sourceTargetPairs = {
            { Paths.get("lombok", "log", "ConvertAnyLog.yml"),
              Paths.get("lombok", "log", "ConvertAnyLog.adoc")},
            { Paths.get("lombok", "SummarizeData.yml"),
              Paths.get("lombok", "SummarizeData.adoc")},
            { Paths.get("lombok", "SummarizeDataNegligently.yml"),
              Paths.get("lombok", "SummarizeDataNegligently.adoc")},
        };

        coordinatesToPaths.put("io.github.timoa.lombok.SummarizeData", Paths.get("lombok","SummarizeData.adoc"));
        coordinatesToPaths.put("io.github.timoa.lombok.SummarizeDataNegligently", Paths.get("lombok", "SummarizeDataNegligently.adoc"));

        for (Path[] pair : sourceTargetPairs) {
            Path source = pair[0];
            Path target = pair[1];
            cfg.setDirectoryForTemplateLoading(new File(CONFIG_DIR));
            Info info = fetchInfoFromYaml(source.toString()); // e.g. lombok/log/ConvertAnyLog.yml

            for (Info.SubRecipe subRecipe: info.definition.subRecipes) {
                String coordinates = subRecipe.getCoordinates();

                subRecipe.setCoordinates(String.format("xref:%s[%s]", coordinatesToPaths.get(coordinates), coordinates));
            }
            Path freeMarkerPath = Paths.get(COMMON_FILE).getFileName();
            targetPath = Paths.get(TEMPLATE_DIR).resolve(target);
            applyTemplate(freeMarkerPath, targetPath, info);
        }
    }

    private static Path getNextPath(Path path, String suffix) {
        return Paths.get(
                path.getParent().toString(),
                path.getFileName().toString().replace(".stub", suffix));
    }

    private static void applyTemplate(Path templatePath, Path targetPath, Info info) throws IOException, TemplateException {

        String recipeCoordinates = info.coordinates;

        String recipeUrl = "https://github.com/timo-a/rewrite-recipe-starter/blob/main/src/main/java/" + recipeCoordinates.replace('.', '/') + ".java";
        String recipeIssue = "https://github.com/timo-a/rewrite-recipe-starter/issues";
        String recipeSonartype = "https://central.sonatype.com/artifact/io.github.timo-a/rewrite-recipe-starter-test";

        Map<String, Object> map = new HashMap<>();
        map.put("mavenPluginVersion", fetchMavenPluginVersion("openrewrite/rewrite-maven-plugin"));
        map.put("gradlePluginVersion", fetchMavenPluginVersion("openrewrite/rewrite-gradle-plugin"));
        map.put("displayName", Converter.convertMarkdownToAsciiDoc(info.getDisplayName()));
        map.put("description", Converter.convertMarkdownToAsciiDoc(info.getDescription()));

        Map<String, Object> recipeMap = new HashMap<>();
        recipeMap.put("coordinates", recipeCoordinates);
        recipeMap.put("groupId", "io.github.timo-a");
        recipeMap.put("artifactId", "rewrite-recipe-starter");
        recipeMap.put("version", fetchMavenPluginVersion("timo-a/rewrite-recipe-starter"));
        recipeMap.put("url", recipeUrl);
        recipeMap.put("issue", recipeIssue);
        recipeMap.put("sonartype", recipeSonartype);
        if (info.getDefinition() != null) {
            List<Map<String, Object>> subrecipeList = info.definition.subRecipes
                    .stream()
                    .map(sr -> {
                        Map<String, Object> subrecipeMap = new HashMap<>();
                        subrecipeMap.put("name", sr.getCoordinates());
                        sr.parameters.entrySet().forEach(
                                entry -> {
                                    if (entry.getValue() instanceof List) {
                                        List<String> stringList = (List<String>) entry.getValue();
                                        entry.setValue("`" + String.join("`, `", stringList) + "`");
                                    } else if(entry.getValue() instanceof String) {
                                        entry.setValue("`" + entry.getValue() + "`");
                                    }
                                }
                        );
                        subrecipeMap.put("parameters", sr.parameters);
                        return subrecipeMap;})
                    .collect(Collectors.toList());
            recipeMap.put("subrecipeList", subrecipeList);
            recipeMap.put("yamlString", info.definition.yaml);
        }
        map.put("recipe", recipeMap);

        Template template = cfg.getTemplate(templatePath.toString());

        // File output
        Writer file = new FileWriter(targetPath.toFile());
        template.process(map, file);
        file.flush();
        file.close();
    }

    private static Info extractNameAndDescription(List<String> stubLines) throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {

        Validate.isTrue(stubLines.size() >= 2, "Stub file should have at least 2 lines, but has %s" , stubLines.size());
        String firstLine = stubLines.get(0).trim();
        Validate.matchesPattern(firstLine, ":recipe-coordinates: ([a-z]+\\.)+[a-zA-Z0-9]+");
        String recipeCoordinates = firstLine.substring(":recipe-coordinates: ".length());

        String secondLine = stubLines.get(1).trim();
        Validate.matchesPattern(secondLine, ":dummy-parameter-values:\\s*((\\w+ )*\\w+)?");
        String values = secondLine.substring(":dummy-parameter-values:".length()).trim();
        String[] dummyParameters = values.isEmpty()
                ? new String[0]
                : values.split("\\s");

        return fetchInfoFromClass(recipeCoordinates, dummyParameters);
    }

    @Value
    static class Info {
        String coordinates;
        String displayName;
        String description;
        Definition definition;
        @Value
        static class Definition {
            List<SubRecipe> subRecipes;
            String yaml;
        }
        @AllArgsConstructor
        static class SubRecipe {
            @Setter
            @Getter
            String coordinates;
            Map<String, Object> parameters;
        }
    }

    private static Info fetchInfoFromClass(String coordinates, String[] params) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        Class<?>[] parameterTypes = new Class[params.length];
        for (int i = 0; i < params.length; i++) {
            parameterTypes[i] = String.class;
        }

        Class<?> clazz = Class.forName(coordinates);
        Recipe recipe;
        //instantiate Recipe class to get name and description
        if ("io.github.timoa.lombok.SummarizeDataParameterized".equals(coordinates)) {
            //todo find a way to represent lists in the `dummy-parameter-values` line in the stub
            recipe = new SummarizeDataParameterized(Collections.emptyList());
        } else {
            recipe = (Recipe) clazz
                    .getDeclaredConstructor(parameterTypes)
                    .newInstance((Object[]) params);
        }

        return new Info(coordinates, recipe.getDisplayName(), recipe.getDescription(), null);
    }

    private static Info fetchInfoFromYaml(String filename) throws IOException {

        Yaml yaml = new Yaml(new Constructor(DeclarativeRecipe.class, new LoaderOptions()));
        Path resourcesPath = Paths.get(DECLARATIVE_RECIPES, filename);
        InputStream inputStream = Files.newInputStream(resourcesPath.toFile().toPath());
        DeclarativeRecipe declarativeRecipe = yaml.load(inputStream);

        //read as string
        byte[] encoded = Files.readAllBytes(resourcesPath);
        String yamlString = new String(encoded, StandardCharsets.UTF_8);
        int i = yamlString.indexOf("\n---\ntype");
        String yamlStringTruncated = yamlString.substring(i + 1);

        return new Info(declarativeRecipe.getName(), declarativeRecipe.getDisplayName(), declarativeRecipe.getDescription(),
                new Info.Definition(
                        Arrays
                                .stream(declarativeRecipe.getRecipeList())
                                .map(e -> e instanceof String ? castSubrecipe((String)e)
                                                              : castSubrecipe((Map<String, Map<String, Object>>) e))
                                .collect(Collectors.toList()),
                        yamlStringTruncated));
    }

    private static Info.SubRecipe castSubrecipe(String entry) {
        return new Info.SubRecipe(entry, new HashMap<>());
    }

    private static Info.SubRecipe castSubrecipe(Map<String, Map<String, Object>> entry) {
        Map.Entry<String, Map<String, Object>> singleEntry = entry.entrySet().iterator().next();
        String name = singleEntry.getKey();
        Map<String, Object> parameters = singleEntry.getValue();
        parameters.entrySet().forEach(e -> {
            if (e.getValue() == null) {//null is not parsed as a string but as `null`, leads to error
                e.setValue("null");
            }
        });

        return new Info.SubRecipe(name, parameters);
    }

    private static void addCommonPart(Path targetPath, List<String> stubLines) throws IOException {

        List<String> commonLines = Files.readAllLines(Paths.get(COMMON_FILE));
        List<String> targetLines = Stream.concat(
                commonLines.stream(),
                stubLines.stream().skip(2)//skip first two lines as they only include process info
        ).collect(Collectors.toList());

        Files.write(targetPath, targetLines, Charset.defaultCharset());
    }



    private static String fetchMavenPluginVersion(String project) throws IOException {
        String url = "https://github.com/" + project + "/releases/latest";

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(false);

        int status = connection.getResponseCode();
        assert status == HttpURLConnection.HTTP_MOVED_TEMP;

        String location = connection.getHeaderField("location");
        String version = location.substring(location.lastIndexOf('v') + 1);
        return version;
    }
}
