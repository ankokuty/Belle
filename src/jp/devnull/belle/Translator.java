package jp.devnull.belle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JTextArea;
import javax.swing.plaf.synth.SynthComboBoxUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import javax.swing.tree.DefaultTreeCellRenderer;

public class Translator {
	private static Map<String, Translator> map = new HashMap<String, Translator>();
	boolean debug;
	Map<String, String> literal;
	Map<Pattern, String> regexp;

	public static String translate(Object src, String lang, String str) throws Exception {
		// テーブルの中身など一部は翻訳しない
		// FIXME Inspectorで選択テキストが翻訳されてしまうので、なんとかしたい
		if ((src instanceof PlainDocument && !src.getClass().equals(PlainDocument.class))
				|| (src instanceof JTextComponent && ((JTextComponent) src).isEditable())
				|| src instanceof JTextArea
				|| src instanceof DefaultStyledDocument
				|| src instanceof DefaultTreeCellRenderer
				|| src instanceof DefaultTableCellRenderer
				|| src.getClass().equals(SynthComboBoxUI.class)
		) {
			return str;
		}
		return translate(lang, str);
	}

	public static String translate(String lang, String str) throws Exception {
		Translator translator = map.get(lang);
		if (translator == null) {
			translator = new Translator(lang);
			map.put(lang, translator);
		}
		if ((str == null) || (str.length() == 0)) {
			return str;
		}

		StringBuilder ret = new StringBuilder();
		for (String s : str.split("\n")) {
			if (ret.length() > 0) {
				ret.append("\n");
			}
			ret.append(translator.translate(s));
		}

		return ret.toString();
	}

	Translator(String lang) throws Exception {
		literal = new HashMap<String, String>();
		regexp = new HashMap<Pattern, String>();

		if (lang.equals("debug")) {
			debug = true;
		}

		String filename = lang + ".txt";
		// 内部にある場合は内部を優先する
		InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
		if (is == null) {
			File file = new File(filename);
			try {
				is = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				System.err.println("Could not load language file: " + filename);
				throw e;
			}
		}

		// ファイルを読み込む
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
			Pattern pattern = Pattern.compile(".*\\$[0-9]+.*");
			String line;
			while ((line = reader.readLine()) != null) {
				String[] inputs = line.split("\t", 2);
				literal.put(inputs[0], inputs[1]);
				// 正規表現を使っている場合はコンパイルしておく
				if (pattern.matcher(inputs[1]).matches()) {
					String re = "(?m)^" + inputs[0] + "$";
					try {
						regexp.put(Pattern.compile(re), inputs[1].replace("\"", "\\\""));
					} catch (PatternSyntaxException ignore) {
					}
				}
			}
		}
	}

	String translate(String src) {
		if ((src == null) || (src.length() == 0)) {
			return src;
		}
		src = src.replace("“", "\"").replace("”", "\"").replace("‘", "'").replace("’", "'");

		String dst = literal.get(src);
		if (dst == null) {
			dst = src;
			Iterator<Entry<Pattern, String>> iterator = regexp.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<Pattern, String> entry = iterator.next();
				Pattern pattern = entry.getKey();
				Matcher matcher = pattern.matcher(dst);
				while(matcher.matches()) {
					dst = matcher.replaceAll(entry.getValue());
					matcher = pattern.matcher(dst);
				}
			}
		}
		// 翻訳されていない文を標準エラーに出す。
		if (debug && (src.equals(dst) && dst.length() == dst.getBytes().length // 翻訳されていないもののみ
				&& !src.matches("https?://.+") // URLを無視
				&& !src.matches("\\$?[ 0-9,./:]+") // 数値を無視
				&& !src.matches("^[-.\\w]+:?$") // １単語だけの場合を無視
				&& !src.matches("(burp|javax)\\..*") // burp.やjavax.で始まるもの(クラス名？)を無視
				&& !src.matches("lbl.*") // lblから始まるもの(ラベル名？)を無視
				&& src.length() > 1 // １文字を無視
				&& !src.matches("[-/ A-Z0-9]+s?") // 大文字と数値のみの単語を無視
				&& !src.matches("\\s+") // 空白のみを無視
				&& !src.matches("[A-Z]:\\\\.*") // Windowsのファイルパスを無視
		)) {
			System.err.println("[" + src + "]");
		}
		return dst;
	}
}
