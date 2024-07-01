package io.github.timoa;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import lombok.Value;
import org.apache.commons.lang3.Validate;
import org.openrewrite.Recipe;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
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

public class DocMain {

    private static final String TEMPLATE_DIR;
    private static final String COMMON_FILE;
    static {
        String ROOT = System.getenv("MY_PATH");
        TEMPLATE_DIR = ROOT + "/src/docs/adoc/modules/recipes/pages";
        COMMON_FILE  = ROOT + "/src/docs/config/common.adoc";

    }

    private static final Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);

    public static void main(String[] args) throws IOException, TemplateException, ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        cfg.setDirectoryForTemplateLoading(new File(TEMPLATE_DIR));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        BiPredicate<Path, BasicFileAttributes> matcher = (p, bfa) -> bfa.isRegularFile()
                && p.getFileName().toString().matches(".*\\.stub");

        List<Path> collect = Files.find(Paths.get(TEMPLATE_DIR), 999, matcher).collect(Collectors.toList());

        for (Path path : collect) {
            List<String> stubLines = Files.readAllLines(path);
            Info nameAndDescription = extractNameAndDescription(stubLines);

            Path freeMarkerPath = getNextPath(path, ".ftl");
            addCommonPart(freeMarkerPath, stubLines);

            Path targetPath = getNextPath(path, ".adoc");
            applyTemplate(freeMarkerPath, targetPath, nameAndDescription);
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
        map.put("recipe", recipeMap);

        Template template = cfg.getTemplate( templatePath.toAbsolutePath().toString().substring(DocMain.TEMPLATE_DIR.length()));

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
    }

    private static Info fetchInfoFromClass(String coordinates, String[] params) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        Class<?>[] parameterTypes = new Class[params.length];
        for (int i = 0; i < params.length; i++) {
            parameterTypes[i] = String.class;
        }

        Class<?> clazz = Class.forName(coordinates);
        Recipe recipe = (Recipe) clazz
                .getDeclaredConstructor(parameterTypes)
                .newInstance((Object[]) params);

        return new Info(coordinates, recipe.getDisplayName(), recipe.getDescription());
    }

    private static void addCommonPart(Path targetPath, List<String> stubLines) throws IOException {

        List<String> commonLines = Files.readAllLines(Paths.get(COMMON_FILE));
        List<String> targetLines = Stream.concat(commonLines.stream(), stubLines.stream().skip(2)).collect(Collectors.toList());

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
