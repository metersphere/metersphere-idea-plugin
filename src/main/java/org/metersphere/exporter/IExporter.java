package org.metersphere.exporter;

import com.intellij.psi.PsiElement;

public interface IExporter {
    boolean export(PsiElement psiElement);
}
