package io.metersphere.exporter;

import com.intellij.psi.PsiJavaFile;

import java.util.List;

public interface IExporter {
    boolean export(List<PsiJavaFile> javaFiles) throws Throwable;

}
