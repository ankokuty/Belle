package jp.devnull.belle;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.Modifier;

public class Injection {
	static ClassPool classPool;
	static Map<String, InjectionMethod> injectionMethods;

	public static void premain(String agentArgs, Instrumentation instrumentation) throws Exception {
		if (agentArgs == null) {
			agentArgs = "ja";
		}
		final String lang = agentArgs;
		classPool = ClassPool.getDefault();

		injectionMethods = new HashMap<>();
		injectionMethods.put("java/awt/Frame",                    new InjectionMethod("setTitle",         1));
		injectionMethods.put("java/awt/Dialog",                   new InjectionMethod("setTitle",         1));
		injectionMethods.put("javax/swing/JLabel",                new InjectionMethod("setText",          1));
		injectionMethods.put("javax/swing/AbstractButton",        new InjectionMethod("setText",          1));
		injectionMethods.put("javax/swing/text/JTextComponent",   new InjectionMethod("setText",          1));
		injectionMethods.put("javax/swing/text/AbstractDocument", new InjectionMethod("insertString",     2));
		injectionMethods.put("javax/swing/JComponent",            new InjectionMethod("setToolTipText",   1));
		injectionMethods.put("javax/swing/JComboBox",             new InjectionMethod("addItem",          1));
		injectionMethods.put("javax/swing/JOptionPane",           new InjectionMethod("showOptionDialog", 2));
		injectionMethods.put("javax/swing/JEditorPane",            new InjectionMethod("setText",          1));
		
		instrumentation.addTransformer(new ClassFileTransformer() {
			CtBehavior insertTranslateCommand(CtBehavior ctMethod, int n) throws Exception {
				StringBuilder src = new StringBuilder();
				src.append("{");
				src.append("ClassLoader classLoader = ClassLoader.getSystemClassLoader();");
				src.append("Class translator = classLoader.loadClass(\"jp.devnull.belle.Translator\");");

				if((ctMethod.getModifiers() & Modifier.STATIC) != 0) {
					src.append("java.lang.reflect.Method method = translator.getDeclaredMethod(\"translate\", new Class[]{String.class, String.class});");
					src.append(String.format("if($%d instanceof String){$%d = (String)method.invoke(null, new Object[]{\"" + lang + "\", $%d});}", n, n, n));
				} else {
					src.append("java.lang.reflect.Method method = translator.getDeclaredMethod(\"translate\", new Class[]{Object.class, String.class, String.class});");
					src.append(String.format("if($%d instanceof String){$%d = (String)method.invoke(null, new Object[]{$0, \"" + lang + "\", $%d});}", n, n, n));
				}
				src.append("}");
				ctMethod.insertBefore(src.toString());
				return ctMethod;
			}

			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
					ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
				try {
					CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
					InjectionMethod method = injectionMethods.get(className);
					if(method!=null) {
						CtBehavior ctMethod = ctClass.getDeclaredMethod(method.methodName);
						insertTranslateCommand(ctMethod, method.place);
						return ctClass.toBytecode();
					} else if (className.equals("javax/swing/JTabbedPane")) {
						CtBehavior ctMethod = ctClass.getDeclaredMethod("addTab");
						insertTranslateCommand(ctMethod, 1);
						ctMethod = ctClass.getDeclaredMethod("insertTab");
						insertTranslateCommand(ctMethod, 1);
						return ctClass.toBytecode();
					} else if (className.equals("javax/swing/JDialog")) {
						CtBehavior ctMethod = ctClass.getDeclaredConstructor(new CtClass[] {
								classPool.get("java.awt.Frame"), classPool.get("java.lang.String"), CtClass.booleanType });
						insertTranslateCommand(ctMethod, 2);
						return ctClass.toBytecode();
//					} else if(className.matches("java/lang/Class")) {
//						ctClass.instrument(new ResourceTransformer(lang));
//						return ctClass.toBytecode();
					} else {
						return null;
					}
				} catch (Exception ex) {
					IllegalClassFormatException e = new IllegalClassFormatException();
					e.initCause(ex);
					throw e;
				}
			}
		}, true);

		for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
			if (clazz.getName().equals("java.lang.Class")) {
				instrumentation.retransformClasses(new Class[] { clazz });
			}
		}
	}
}