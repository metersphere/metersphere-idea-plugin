package org.metersphere.exporter;

import com.intellij.psi.PsiElement;

import java.io.IOException;

public interface IExporter {
    boolean export(PsiElement psiElement) throws IOException;
}
