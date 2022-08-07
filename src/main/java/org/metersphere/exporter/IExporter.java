package org.metersphere.exporter;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;

import java.io.IOException;
import java.util.List;

public interface IExporter {
    boolean export(List<PsiJavaFile> javaFiles) throws IOException;

}
