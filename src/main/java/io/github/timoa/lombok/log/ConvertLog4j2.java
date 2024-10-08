/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.timoa.lombok.log;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;

@Value
@EqualsAndHashCode(callSuper = false)
public class ConvertLog4j2 extends ConvertLogRecipe {

    @Override
    public String getDisplayName() {
        return getDisplayName("@Log4j2");
    }

    @Override
    public String getDescription() {
        //language=markdown
        return getDescription("@Log4j2", "org.apache.logging.log4j.Logger");
    }

    @Option(displayName = "Name of the log field",
            description = FIELD_NAME_DESCRIPTION,
            example = "LOGGER",
            required = false)
    @Nullable
    String fieldName;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Log4j2Visitor(fieldName);
    }

    public static class Log4j2Visitor extends LogVisitor {

        Log4j2Visitor(String fieldName_) {
            super(fieldName_);
        }

        @Override
        protected void switchImports() {
            maybeAddImport("lombok.extern.log4j.Log4j2");
            maybeRemoveImport("org.apache.logging.log4j.Logger");
            maybeRemoveImport("org.apache.logging.log4j.LogManager");
        }

        @Override
        protected JavaTemplate getLombokTemplate() {
            return getLombokTemplate("Log4j2", "lombok.extern.log4j.Log4j2");
        }

        @Override
        protected String expectedLoggerPath() {
            return "org.apache.logging.log4j.Logger";
        }

        @Override
        protected boolean methodPath(String path) {
            return "org.apache.logging.log4j.LogManager.getLogger".equals(path);
        }
    }
}
