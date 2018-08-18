package jp.devnull.belle;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;

public class Injection {
	static ClassPool classPool;

	public static void premain(final String agentArgs, Instrumentation instrumentation) throws Exception {
		classPool = ClassPool.getDefault();

		instrumentation.addTransformer(new ClassFileTransformer() {
			CtBehavior insertTranslateCommand(CtBehavior ctMethod, int n) throws Exception {
				StringBuilder inner = new StringBuilder();
				inner.append("{");
				inner.append("ClassLoader classLoader = ClassLoader.getSystemClassLoader();");
				inner.append("Class translator = classLoader.loadClass(\"jp.devnull.belle.Translator\");");
				inner.append("java.lang.reflect.Method method = translator.getDeclaredMethod(\"translate\", new Class[]{String.class, String.class});");
				inner.append(String.format("if($%d instanceof String){$%d = (String)method.invoke(null, new Object[]{\"" + agentArgs + "\", $%d});}", n, n, n));
				inner.append("}");

				StringBuilder outer = new StringBuilder();
				// テーブルの中身など一部は翻訳しない
				outer.append("if ("
						+ "(javax.swing.table.DefaultTableCellRenderer.class.isAssignableFrom($0.getClass())"
						+ "  && !sun.swing.table.DefaultTableCellHeaderRenderer.class.isAssignableFrom($0.getClass()))"
						+ "  || javax.swing.text.DefaultStyledDocument.class.isAssignableFrom($0.getClass())"
						+ "  || javax.swing.tree.DefaultTreeCellRenderer.class.isAssignableFrom($0.getClass())"
						+ "  || $0.getClass().getName().equals(\"javax.swing.plaf.synth.SynthComboBoxUI$SynthComboBoxRenderer\")) {} else");
				outer.append(inner.toString());
				try {
					ctMethod.insertBefore(outer.toString());
				} catch (Exception e) {
					// 静的メソッドの場合$0でコンパイルエラーが出る
					ctMethod.insertBefore(inner.toString());
				}
				return ctMethod;
			}

			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
					ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
				try {
					if (className.equals("java/awt/Frame") || className.equals("java/awt/Dialog")) {
						CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
						CtBehavior ctMethod = ctClass.getDeclaredMethod("setTitle");
						insertTranslateCommand(ctMethod, 1);
						return ctClass.toBytecode();
					} else if (className.equals("javax/swing/JLabel") || className.equals("javax/swing/AbstractButton")
							|| className.equals("javax/swing/text/JTextComponent)")) {
						CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
						CtBehavior ctMethod = ctClass.getDeclaredMethod("setText");
						insertTranslateCommand(ctMethod, 1);
						return ctClass.toBytecode();
					} else if (className.equals("javax/swing/JTabbedPane")) {
						CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
						CtBehavior ctMethod = ctClass.getDeclaredMethod("addTab");
						insertTranslateCommand(ctMethod, 1);
						ctMethod = ctClass.getDeclaredMethod("insertTab");
						insertTranslateCommand(ctMethod, 1);
						return ctClass.toBytecode();
					} else if (className.equals("javax/swing/text/AbstractDocument")) {
						CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
						CtBehavior ctMethod = ctClass.getDeclaredMethod("insertString");
						insertTranslateCommand(ctMethod, 2);
						return ctClass.toBytecode();
					} else if (className.equals("javax/swing/JComponent")) {
						CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
						CtBehavior ctMethod = ctClass.getDeclaredMethod("setToolTipText");
						insertTranslateCommand(ctMethod, 1);
						return ctClass.toBytecode();
					} else if (className.equals("javax/swing/JComboBox")) {
						CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
						CtBehavior ctMethod = ctClass.getDeclaredMethod("addItem");
						insertTranslateCommand(ctMethod, 1);
						return ctClass.toBytecode();
					} else if (className.equals("javax/swing/JOptionPane")) {
						CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
						CtBehavior ctMethod = ctClass.getDeclaredMethod("showOptionDialog");
						insertTranslateCommand(ctMethod, 3);
						return ctClass.toBytecode();
					} else if (className.equals("javax/swing/JDialog")) {
						CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
						CtBehavior ctMethod = ctClass.getDeclaredConstructor(new CtClass[] {
								classPool.get("java.awt.Frame"), classPool.get("String"), CtClass.booleanType });
						insertTranslateCommand(ctMethod, 2);
						return ctClass.toBytecode();
					} else {
						return null;
					}
				} catch (Exception ex) {
					IllegalClassFormatException e = new IllegalClassFormatException();
					e.initCause(ex);
					throw e;
				}
			}
		});
	}
}