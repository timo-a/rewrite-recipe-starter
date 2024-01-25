/*
 * Copyright 2024 the original author or authors.
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
package io.github.timoa.misc;

import com.google.common.collect.Range;
import com.google.errorprone.refaster.ImportPolicy;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.UseImportPolicy;
import java.time.LocalDate;
import org.openrewrite.java.template.RecipeDescriptor;

import java.math.BigDecimal;

@RecipeDescriptor(
        name = "Use Guava Ranges",
        description = "Simplifies hand crafted range checks."
)
public class UseRanges {

    @RecipeDescriptor(
        name = "Replace `from.compareTo(candidate) <= 0 && candidate.compareTo(to) <= 0` with a guava `Range.closed(from, to).contains(candidate)`",
        description = "Replace a hand crafted range check for membership in a closed interval ( candidate € [from, to] ) with a guava range expression`."
    )
    public static class RangeClosedBD {

        @BeforeTemplate
        boolean simple(BigDecimal from, BigDecimal candidate, BigDecimal to) {

            return from.compareTo(candidate) <= 0
                && candidate.compareTo(to) <= 0;
        }

        @BeforeTemplate
        boolean candidateAsArgument(BigDecimal from, BigDecimal candidate, BigDecimal to) {

            return from.compareTo(candidate) <= 0
                && to.compareTo(candidate) >= 0;
        }

        @BeforeTemplate
        boolean candidateAsBase(BigDecimal from, BigDecimal candidate, BigDecimal to) {

            return candidate.compareTo(from) >= 0
                && candidate.compareTo(to) <= 0;
        }

        @BeforeTemplate
        boolean flipped(BigDecimal from, BigDecimal candidate, BigDecimal to) {

            return candidate.compareTo(to) <= 0
                && from.compareTo(candidate) <= 0;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(BigDecimal from, BigDecimal candidate, BigDecimal to) {
            return Range.closed(from, to).contains(candidate);
        }
    }
    @RecipeDescriptor(
        name = "Replace `from.compareTo(candidate) < 0 && candidate.compareTo(to) < 0` with a guava `Range.open(from, to).contains(candidate)`",
        description = "Replace a hand crafted range check for membership in an open interval ( candidate € (from, to) ) with a guava range expression`."
    )
    public static class RangeOpenBD {

        @BeforeTemplate
        boolean simple(BigDecimal from, BigDecimal candidate, BigDecimal to) {

            return from.compareTo(candidate) < 0
                && candidate.compareTo(to) < 0;
        }

        @BeforeTemplate
        boolean candidateAsArgument(BigDecimal from, BigDecimal candidate, BigDecimal to) {

            return from.compareTo(candidate) < 0
                && to.compareTo(candidate) > 0;
        }

        @BeforeTemplate
        boolean candidateAsBase(BigDecimal from, BigDecimal candidate, BigDecimal to) {

            return candidate.compareTo(from) > 0
                && candidate.compareTo(to) < 0;
        }

        @BeforeTemplate
        boolean flipped(BigDecimal from, BigDecimal candidate, BigDecimal to) {

            return candidate.compareTo(to) < 0
                && from.compareTo(candidate) < 0;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(BigDecimal from, BigDecimal candidate, BigDecimal to) {
            return Range.open(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
        name = "Replace `from.compareTo(candidate) <= 0 && candidate.compareTo(to) < 0` with a guava `Range.closedOpen(from, to).contains(candidate)`",
        description = "Replace a hand crafted range check for membership in an interval that is open to the right ( candidate € [from, to) ) with a guava range expression`."
    )
    public static class RangeClosedOpenBD {

        @BeforeTemplate
        boolean simple(BigDecimal from, BigDecimal candidate, BigDecimal to) {

            return from.compareTo(candidate) <= 0
                && candidate.compareTo(to) < 0;
        }

        @BeforeTemplate
        boolean candidateAsArgument(BigDecimal from, BigDecimal candidate, BigDecimal to) {

            return from.compareTo(candidate) <= 0
                && to.compareTo(candidate) > 0;
        }

        @BeforeTemplate
        boolean candidateAsBase(BigDecimal from, BigDecimal candidate, BigDecimal to) {

            return candidate.compareTo(from) >= 0
                && candidate.compareTo(to) < 0;
        }

        @BeforeTemplate
        boolean flipped(BigDecimal from, BigDecimal candidate, BigDecimal to) {

            return candidate.compareTo(to) < 0
                && from.compareTo(candidate) <= 0;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(BigDecimal from, BigDecimal candidate, BigDecimal to) {
            return Range.closedOpen(from, to).contains(candidate);
        }
    }
    @RecipeDescriptor(
        name = "Replace `from.compareTo(candidate) < 0 && candidate.compareTo(to) <= 0` with a guava `Range.openClosed(from, to).contains(candidate)`",
        description = "Replace a hand crafted range check for membership in an interval that is open to the left ( candidate € (from, to] ) with a guava range expression`."
    )
    public static class RangeOpenClosedBD {

        @BeforeTemplate
        boolean simple(BigDecimal from, BigDecimal candidate, BigDecimal to) {

            return from.compareTo(candidate) < 0
                && candidate.compareTo(to) <= 0;
        }

        @BeforeTemplate
        boolean candidateAsArgument(BigDecimal from, BigDecimal candidate, BigDecimal to) {

            return from.compareTo(candidate) < 0
                && to.compareTo(candidate) >= 0;
        }

        @BeforeTemplate
        boolean candidateAsBase(BigDecimal from, BigDecimal candidate, BigDecimal to) {

            return candidate.compareTo(from) > 0
                && candidate.compareTo(to) <= 0;
        }

        @BeforeTemplate
        boolean flipped(BigDecimal from, BigDecimal candidate, BigDecimal to) {

            return candidate.compareTo(to) <= 0
                && from.compareTo(candidate) < 0;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(BigDecimal from, BigDecimal candidate, BigDecimal to) {
            return Range.openClosed(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
        name = "Replace `from.compareTo(candidate) <= 0 && candidate.compareTo(to) <= 0` with a guava `Range.closed(from, to).contains(candidate)`",
        description = "Replace a hand crafted range check for membership in a closed interval ( candidate € [from, to] ) with a guava range expression`."
    )
    public static class RangeClosedLocalDate {

        @BeforeTemplate
        boolean simple(LocalDate from, LocalDate candidate, LocalDate to) {

            return from.compareTo(candidate) <= 0
                && candidate.compareTo(to) <= 0;
        }

        @BeforeTemplate
        boolean candidateAsArgument(LocalDate from, LocalDate candidate, LocalDate to) {

            return from.compareTo(candidate) <= 0
                && to.compareTo(candidate) >= 0;
        }

        @BeforeTemplate
        boolean candidateAsBase(LocalDate from, LocalDate candidate, LocalDate to) {

            return candidate.compareTo(from) >= 0
                && candidate.compareTo(to) <= 0;
        }

        @BeforeTemplate
        boolean flipped(LocalDate from, LocalDate candidate, LocalDate to) {

            return candidate.compareTo(to) <= 0
                && from.compareTo(candidate) <= 0;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(LocalDate from, LocalDate candidate, LocalDate to) {
            return Range.closed(from, to).contains(candidate);
        }
    }
    @RecipeDescriptor(
        name = "Replace `from.compareTo(candidate) < 0 && candidate.compareTo(to) < 0` with a guava `Range.open(from, to).contains(candidate)`",
        description = "Replace a hand crafted range check for membership in an open interval ( candidate € (from, to) ) with a guava range expression`."
    )
    public static class RangeOpenLocalDate {

        @BeforeTemplate
        boolean simple(LocalDate from, LocalDate candidate, LocalDate to) {

            return from.compareTo(candidate) < 0
                && candidate.compareTo(to) < 0;
        }

        @BeforeTemplate
        boolean candidateAsArgument(LocalDate from, LocalDate candidate, LocalDate to) {

            return from.compareTo(candidate) < 0
                && to.compareTo(candidate) > 0;
        }

        @BeforeTemplate
        boolean candidateAsBase(LocalDate from, LocalDate candidate, LocalDate to) {

            return candidate.compareTo(from) > 0
                && candidate.compareTo(to) < 0;
        }

        @BeforeTemplate
        boolean flipped(LocalDate from, LocalDate candidate, LocalDate to) {

            return candidate.compareTo(to) < 0
                && from.compareTo(candidate) < 0;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(LocalDate from, LocalDate candidate, LocalDate to) {
            return Range.open(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
        name = "Replace `from.compareTo(candidate) <= 0 && candidate.compareTo(to) < 0` with a guava `Range.closedOpen(from, to).contains(candidate)`",
        description = "Replace a hand crafted range check for membership in an interval that is open to the right ( candidate € [from, to) ) with a guava range expression`."
    )
    public static class RangeClosedOpenLocalDate {

        @BeforeTemplate
        boolean simple(LocalDate from, LocalDate candidate, LocalDate to) {

            return from.compareTo(candidate) <= 0
                && candidate.compareTo(to) < 0;
        }

        @BeforeTemplate
        boolean candidateAsArgument(LocalDate from, LocalDate candidate, LocalDate to) {

            return from.compareTo(candidate) <= 0
                && to.compareTo(candidate) > 0;
        }

        @BeforeTemplate
        boolean candidateAsBase(LocalDate from, LocalDate candidate, LocalDate to) {

            return candidate.compareTo(from) >= 0
                && candidate.compareTo(to) < 0;
        }

        @BeforeTemplate
        boolean flipped(LocalDate from, LocalDate candidate, LocalDate to) {

            return candidate.compareTo(to) < 0
                && from.compareTo(candidate) <= 0;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(LocalDate from, LocalDate candidate, LocalDate to) {
            return Range.closedOpen(from, to).contains(candidate);
        }
    }
    @RecipeDescriptor(
        name = "Replace `from.compareTo(candidate) < 0 && candidate.compareTo(to) <= 0` with a guava `Range.openClosed(from, to).contains(candidate)`",
        description = "Replace a hand crafted range check for membership in an interval that is open to the left ( candidate € (from, to] ) with a guava range expression`."
    )
    public static class RangeOpenClosedLocalDate {

        @BeforeTemplate
        boolean simple(LocalDate from, LocalDate candidate, LocalDate to) {

            return from.compareTo(candidate) < 0
                && candidate.compareTo(to) <= 0;
        }

        @BeforeTemplate
        boolean candidateAsArgument(LocalDate from, LocalDate candidate, LocalDate to) {

            return from.compareTo(candidate) < 0
                && to.compareTo(candidate) >= 0;
        }

        @BeforeTemplate
        boolean candidateAsBase(LocalDate from, LocalDate candidate, LocalDate to) {

            return candidate.compareTo(from) > 0
                && candidate.compareTo(to) <= 0;
        }

        @BeforeTemplate
        boolean flipped(LocalDate from, LocalDate candidate, LocalDate to) {

            return candidate.compareTo(to) <= 0
                && from.compareTo(candidate) < 0;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(LocalDate from, LocalDate candidate, LocalDate to) {
            return Range.openClosed(from, to).contains(candidate);
        }
    }



}