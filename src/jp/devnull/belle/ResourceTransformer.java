package jp.devnull.belle;

import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class ResourceTransformer extends ExprEditor {
	String lang;

	public ResourceTransformer(String lang) {
		this.lang = lang.equals("debug") ? "ja" : lang;
	}

	@Override
	public void edit(MethodCall m) throws CannotCompileException {
		if (m.getMethodName().equals("getResourceAsStream")) {
			// リソースの読み込み元を変更する
			m.replace("{$1=$1.replace(\"resources/Documentation/burp\", \"burp-resources-" + lang
					+ "/Documentation/burp\");$_=$proceed($$);}");
		}
	}
}
