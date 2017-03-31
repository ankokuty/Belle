package jp.devnull.belle;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;

public class Injection {
	static ClassPool classPool;
	public static void premain(final String agentArgs, Instrumentation instrumentation) throws Exception {
		classPool = ClassPool.getDefault();

		instrumentation.addTransformer(new ClassFileTransformer() {
			CtMethod insertTranslateCommand(CtMethod ctMethod, int n) throws Exception{
				StringBuilder command = new StringBuilder();
				command.append("{");
				// テーブルの中身など一部は翻訳しない
				command.append("if ("
						+ "(javax.swing.table.DefaultTableCellRenderer.class.isAssignableFrom($0.getClass())"
						+ "  && !sun.swing.table.DefaultTableCellHeaderRenderer.class.isAssignableFrom($0.getClass())"
						+ ") || javax.swing.DefaultListCellRenderer.class.isAssignableFrom($0.getClass())"
						+ "  || javax.swing.text.DefaultStyledDocument.class.isAssignableFrom($0.getClass())"
						+ "  || javax.swing.tree.DefaultTreeCellRenderer.class.isAssignableFrom($0.getClass())"
						+ "  || $0.getClass().getName().equals(\"javax.swing.plaf.synth.SynthComboBoxUI$SynthComboBoxRenderer\")) {} else {");
				command.append(String.format("if($%d instanceof String){$%d=java.awt.Component.burpTranslate((String)$%d);}", n, n,	n));
				command.append("}}");
				
				ctMethod.insertBefore(command.toString());
				return ctMethod;
			}
			CtClass addTranslateTableMethod(CtClass ctClass) throws Exception {
				// 変換用のテーブルを作成する
				StringBuilder command = new StringBuilder();
				command.append("public static java.util.Map createMap(){");
				command.append("java.util.Map map = new java.util.HashMap();");
				String langfile = "ja.txt";
				if (agentArgs != null && (new File(agentArgs)).isFile()){
					langfile = agentArgs;
				}

				BufferedReader reader = new BufferedReader(
						new InputStreamReader(new FileInputStream(langfile), "UTF-8"));
				String line;
				while ((line = reader.readLine()) != null) {
					try {
						String[] inputs = line.split("\t", 2);
						command.append("map.put(java.util.regex.Pattern.compile(\"(?m)^" + inputs[0].replace("\"", "\\\"")
							+ "$\"), \"" + inputs[1].replace("\"", "\\\"") + "\");");
					} catch (Exception e) {
						// 無視する
					}
				}
				reader.close();
				command.append("return map;");
				command.append("}");
				CtMethod translateTableMethod = CtMethod.make(command.toString(), ctClass);
				ctClass.addMethod(translateTableMethod);
				return ctClass;
			}

			CtClass addTranslatorMethod(CtClass ctClass) throws Exception {
				// 変換を行うメソッドを追加する
				StringBuilder command = new StringBuilder();
				command.append("public static String burpTranslate(String str){");
				command.append("if((str==null) || (str.length()==0)){return str;}");
				command.append("java.util.Iterator iterator = translateMaps.entrySet().iterator();");
				command.append("while(iterator.hasNext()){");
				command.append("java.util.Map.Entry entry = (java.util.Map.Entry)iterator.next();");
				command.append("str = ((java.util.regex.Pattern)entry.getKey()).matcher(str).replaceAll((String)entry.getValue());");
				command.append("}");// while
				// 翻訳されていない文を標準エラーに出す。
				if (agentArgs != null && agentArgs.equals("debug")) {
					command.append("if("
							+ "(str.getBytes().length) == str.length()" // 翻訳されていないもののみ
							+ " && !str.matches(\"https?://.+\")"       // URLを無視
							+ " && !str.matches(\"\\\\$?[0-9,./]+\")"   // 数値を無視
							+ " && !str.matches(\"^[-.\\\\w]+:?$\")"    // １単語だけの場合を無視
							+ " && !str.matches(\"burp\\..*\")"         // burp.から始まるもの(クラス名？)を無視
							+ " && !str.matches(\"lbl.*\")"             // lblから始まるもの(ラベル名？)を無視
							+ " && str.length()>1"                      // １文字を無視
							+ " && !str.matches(\"[A-Z]+s?\")"          // 大文字のみの単語を無視
							+ " && !str.matches(\"\\\\s+\")"            // 空白のみを無視
							+ "){System.err.println(str);}");
				}
				command.append("return str;");
				command.append("}");
				CtMethod translateTableMethod = CtMethod.make(command.toString(), ctClass);
				ctClass.addMethod(translateTableMethod);
				return ctClass;
			}
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
					ProtectionDomain protectionDomain, byte[] classfileBuffer)
					throws IllegalClassFormatException {
				try {
					// java.awt.Componentを汚して訳語変換メソッドを追加
					if (className.equals("java/awt/Component")) {
						CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
						// 変換テーブル作成メソッドを追加
						addTranslateTableMethod(ctClass);
						// 変換テーブルのフィールドを追加
						CtField f = CtField.make("static java.util.Map translateMaps;", ctClass);
						ctClass.addField(f, "createMap()");
						// 変換処理を行うメソッドを追加
						addTranslatorMethod(ctClass);
						return ctClass.toBytecode();
					} else if (className.equals("java/awt/Frame") || className.equals("java/awt/Dialog")) {
						CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
						CtMethod ctMethod = ctClass.getDeclaredMethod("setTitle");
						insertTranslateCommand(ctMethod, 1);
						return ctClass.toBytecode();
					} else if (className.equals("javax/swing/JLabel")
							|| className.equals("javax/swing/AbstractButton")
							|| className.equals("javax/swing/text/JTextComponent)")) {
						CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
						CtMethod ctMethod = ctClass.getDeclaredMethod("setText");
						insertTranslateCommand(ctMethod, 1);
						return ctClass.toBytecode();
					} else if (className.equals("javax/swing/JTabbedPane")) {
						CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
						CtMethod ctMethod = ctClass.getDeclaredMethod("addTab");
						insertTranslateCommand(ctMethod, 1);
						ctMethod = ctClass.getDeclaredMethod("insertTab");
						insertTranslateCommand(ctMethod, 1);
						return ctClass.toBytecode();
					} else if (className.equals("javax/swing/text/AbstractDocument")) {
						CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
						CtMethod ctMethod = ctClass.getDeclaredMethod("insertString");
						insertTranslateCommand(ctMethod, 2);
						return ctClass.toBytecode();
					} else if (className.equals("javax/swing/JComponent")) {
						CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
						CtMethod ctMethod = ctClass.getDeclaredMethod("setToolTipText");
						insertTranslateCommand(ctMethod, 1);
						return ctClass.toBytecode();
					} else if (className.equals("javax/swing/JComboBox")) {
						CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
						CtMethod ctMethod = ctClass.getDeclaredMethod("addItem");
						insertTranslateCommand(ctMethod, 1);
						return ctClass.toBytecode();
					} else if (className.equals("javax/swing/JOptionPane")) {
						CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
						CtMethod ctMethod = ctClass.getDeclaredMethod("showOptionDialog");
						ctMethod.insertBefore("{$3=java.awt.Component.burpTranslate($3);}");
						return ctClass.toBytecode();
					} else if (className.equals("javax/swing/JDialog")) {
						CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
						CtConstructor ctMethod = ctClass.getDeclaredConstructor(new CtClass[]{classPool.get("java.awt.Frame"),classPool.get("String"),CtClass.booleanType});
						ctMethod.insertBefore("{$2=java.awt.Component.burpTranslate($2);}");
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