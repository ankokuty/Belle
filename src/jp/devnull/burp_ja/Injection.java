package jp.devnull.burp_ja;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

public class Injection {
	static ClassPool classPool;
	static ArrayList<Translate> translates;

	static class Translate {
		String en;
		String ja;

		/**
		 * @param en
		 * @param ja
		 */
		public Translate(String[] s) {
			super();
			en = s[0];
			ja = s[1];
		}
	}

	public static void premain(String agentArgs, Instrumentation instrumentation) throws Exception{
		// ファイルから翻訳辞書を読み込む
		translates = new ArrayList<Translate>();
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream("burp_ja.txt"), "UTF-8"));
		String line;
		while ((line = reader.readLine()) != null) {
			try {
				translates.add(new Translate(line.split("\t", 2)));
			} catch (Exception e) {
				// 無視する
			}
		}
		reader.close();

		classPool = ClassPool.getDefault();
		instrumentation.addTransformer(new ClassFileTransformer() {
			String makeCommand(int n) {
				StringBuilder command = new StringBuilder();
				command.append("{");
				command.append(String.format("if($%d!=null && $%d.length()>0){", n, n));
				// テーブルの中身など一部は翻訳しない
				command.append(
						"if (("
						+ "javax.swing.table.DefaultTableCellRenderer.class.isAssignableFrom($0.getClass())"
						+ "  && !sun.swing.table.DefaultTableCellHeaderRenderer.class.isAssignableFrom($0.getClass())"
						+ ") || javax.swing.DefaultListCellRenderer.class.isAssignableFrom($0.getClass())"
						+ "  || $0.getClass().getName().equals(\"javax.swing.plaf.synth.SynthComboBoxUI$SynthComboBoxRenderer\")) {} else {");

				for (Translate t : translates) {
					command.append(String.format("$%d=$%d.replaceAll(\"(?m)^"
							+ t.en.replace("\"", "\\\"").replace("%","%%")+ "$\",\""
							+ t.ja.replace("\"", "\\\"").replace("%","%%") + "\");", n, n));
				}
				command.append(String.format("$%d=$%d.replace(\"\\[Pro version only\\]\",\"[プロ版のみ]\");", n, n));
				/*
				// 翻訳されていない文を標準エラーに出す。
				command.append(String.format(
					"if("
					+ "($%d.getBytes().length) == $%d.length()" // 翻訳されていないもののみ
					+ " && !$%d.matches(\"https?://.+\")"       // URLを無視
					+ " && !$%d.matches(\"[0-9,]+\")"           // 数値を無視
					+ " && !$%d.matches(\"burp\\..*\")"         // burp.から始まるもの(クラス名？)を無視
					+ " && $%d.length()>1"                      // １文字を無視
					+ " && !$%d.matches(\"[A-Z]+s?\")"          // 大文字のみの単語を無視
					+ "){System.err.println($%d);}", n,n,n,n,n,n,n,n));
				*/
				command.append("}}}");
				return command.toString();
			}

			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
					ProtectionDomain protectionDomain, byte[] classfileBuffer)
					throws IllegalClassFormatException {
				try {
					if (className.equals("java/awt/Frame") || className.equals("java/awt/Dialog")) {
						CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
						CtMethod ctMethod = ctClass.getDeclaredMethod("setTitle");
						ctMethod.insertBefore(makeCommand(1));
						return ctClass.toBytecode();
					} else if (className.equals("javax/swing/JLabel")
							|| className.equals("javax/swing/AbstractButton")
							|| className.equals("javax/swing/text/JTextComponent)")) {
						CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
						CtMethod ctMethod = ctClass.getDeclaredMethod("setText");
						ctMethod.insertBefore(makeCommand(1));
						return ctClass.toBytecode();
					} else if (className.equals("javax/swing/JTabbedPane")) {
						CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
						CtMethod ctMethod = ctClass.getDeclaredMethod("addTab");
						ctMethod.insertBefore(makeCommand(1));
						ctMethod = ctClass.getDeclaredMethod("insertTab");
						ctMethod.insertBefore(makeCommand(1));
						return ctClass.toBytecode();
					} else if (className.equals("javax/swing/text/AbstractDocument")) {
						CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
						CtMethod ctMethod = ctClass.getDeclaredMethod("insertString");
						ctMethod.insertBefore(makeCommand(2));
						return ctClass.toBytecode();
					} else if (className.equals("javax/swing/JComponent")) {
						CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
						CtMethod ctMethod = ctClass.getDeclaredMethod("setToolTipText");
						ctMethod.insertBefore(makeCommand(1));
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