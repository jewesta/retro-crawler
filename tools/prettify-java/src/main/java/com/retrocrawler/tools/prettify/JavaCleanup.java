package com.retrocrawler.tools.prettify;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.OrderImports;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.tree.J.CompilationUnit;
import org.openrewrite.staticanalysis.FinalizeLocalVariables;
import org.openrewrite.staticanalysis.FinalizeMethodArguments;
import org.openrewrite.staticanalysis.FinalizePrivateFields;
import org.openrewrite.staticanalysis.LambdaBlockToExpression;
import org.openrewrite.staticanalysis.MissingOverrideAnnotation;
import org.openrewrite.staticanalysis.NeedBraces;
import org.openrewrite.staticanalysis.RemoveExtraSemicolons;
import org.openrewrite.staticanalysis.ReplaceLambdaWithMethodReference;
import org.openrewrite.staticanalysis.StaticAccessViaInstance;
import org.openrewrite.staticanalysis.UseDiamondOperator;
import org.openrewrite.style.NamedStyles;

final class JavaCleanup extends Recipe {

	private static final NamedStyles IMPORT_STYLES = new NamedStyles(
			UUID.fromString("8e5e9bc5-2df5-48c6-a2da-fb2b46f356bb"), "com.retrocrawler.tools.prettify.ImportLayout",
			"RetroCrawler import layout", "Orders imports consistently without introducing star imports.", Set.of(),
			List.of(importLayout()));

	@Override
	public String getDisplayName() {
		return "RetroCrawler Java cleanup";
	}

	@Override
	public String getDescription() {
		return "Applies conservative Java cleanup before the Eclipse formatter runs.";
	}

	@Override
	public List<Recipe> getRecipeList() {
		return List.of(new ApplyImportStyles(), new OrderImports(true, null), new NeedBraces(),
				new MissingOverrideAnnotation(false), new RemoveExtraSemicolons(), new StaticAccessViaInstance(),
				new LambdaBlockToExpression(), new ReplaceLambdaWithMethodReference(), new UseDiamondOperator(),
				new FinalizePrivateFields(), new FinalizeMethodArguments(), new FinalizeLocalVariables());
	}

	private static ImportLayoutStyle importLayout() {
		return ImportLayoutStyle.builder().classCountToUseStarImport(99).nameCountToUseStarImport(99)
				.importStaticAllOthers().blankLine().importPackage("java.*").blankLine().importPackage("javax.*")
				.blankLine().importPackage("org.*").blankLine().importPackage("com.*").blankLine().importAllOthers()
				.build();
	}

	private static final class ApplyImportStyles extends Recipe {

		@Override
		public String getDisplayName() {
			return "Apply RetroCrawler import styles";
		}

		@Override
		public String getDescription() {
			return "Makes the RetroCrawler import layout available to import cleanup.";
		}

		@Override
		public TreeVisitor<?, ExecutionContext> getVisitor() {
			return new JavaIsoVisitor<>() {

				@Override
				public CompilationUnit visitCompilationUnit(final CompilationUnit compilationUnit,
						final ExecutionContext context) {
					final CompilationUnit visited = super.visitCompilationUnit(compilationUnit, context);
					return visited.withMarkers(visited.getMarkers().addIfAbsent(IMPORT_STYLES));
				}

			};
		}

	}

}
