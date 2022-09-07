package org.metersphere.parse.utils;

import java.util.Arrays;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * reference YApix
 * @see <a href="https://github.com/jetplugins/yapix/blob/main/src/main/java/io/yapix/parse/util/PsiUtils.java">PsiUtils</a>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PsiUtils {

	/**
	 * get all members psiField (not contains static filed)
	 * @param t the PsiClass
	 * @return PsiField[]
	 */
	public static PsiField[] getMemberFields(PsiClass t) {
		PsiField[] fields = t.getAllFields();
		return Arrays.stream(fields).filter(PsiUtils::isNeedField).toArray(PsiField[]::new);
	}

	public static boolean isNeedField(PsiField field) {
		return !field.hasModifier(JvmModifier.STATIC);
	}

	/**
	 * find the actual type of the generic type
	 * Note: the definitionTypeParameters must same length as the actualTypeArguments
	 * @param psiType psiType
	 * @param definitionTypeParameters the type parameters of the definition class
	 * @param actualTypeParameters the actual type parameters of the runtime
	 * @return Actual Runtime PsiType
	 */
	public static PsiType getGenericType(PsiType psiType, PsiTypeParameter[] definitionTypeParameters, PsiType[] actualTypeParameters) {
		// like T
		String defineType = psiType.getCanonicalText();
		for (PsiTypeParameter definitionTypeParameter : definitionTypeParameters) {
			if (defineType.equals(definitionTypeParameter.getName())) {
				return actualTypeParameters[definitionTypeParameter.getIndex()];
			}
		}
		return null;
	}

}
