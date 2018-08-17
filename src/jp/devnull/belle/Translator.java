package jp.devnull.belle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Translator {
	private static Map<String, Translator> map = new HashMap<String, Translator>();
	String file;
	boolean debug;
	Map<String, String> literal;
	Map<Pattern, String> regexp;

	public static String translate(String langfile, String str) throws Exception {
		Translator translator = map.get(langfile);
		if (translator == null) {
			translator = new Translator(langfile);
			map.put(langfile, translator);
		}
		if ((str == null) || (str.length() == 0)) {
			return str;
		}

		StringBuilder ret = new StringBuilder();
		for(String s : str.split("\n")) {
			if(ret.length()>0) {
				ret.append("\n");
			}
			ret.append(translator.translate(s));
		}
		
		return ret.toString();
	}

	Translator(String langfile) throws Exception {
		literal = new HashMap<String, String>();
		regexp = new HashMap<Pattern, String>();

		file = "ja.txt";
		if (langfile != null && langfile.equals("debug")) {
			debug = true;
		}
		if (langfile != null && (new File(langfile)).isFile()) {
			file = langfile;
		}

		// ファイルを読み込む
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		Pattern pattern = Pattern.compile(".*\\$[0-9].*");
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
		reader.close();
	}

	String translate(String src) {
		if ((src == null) || (src.length() == 0)) {
			return src;
		}

		String dst = literal.get(src);
		if (dst == null) {
			dst = src;
			Iterator<Entry<Pattern, String>> iterator = regexp.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<Pattern, String> entry = iterator.next();
				Pattern pattern = entry.getKey();
				Matcher matcher = pattern.matcher(dst);
				dst = matcher.replaceAll((String) entry.getValue());
			}
		}
		// 翻訳されていない文を標準エラーに出す。
		if (debug && (src.equals(dst)
				&& dst.length() == dst.getBytes().length // 翻訳されていないもののみ
				&& !src.matches("https?://.+")           // URLを無視
				&& !src.matches("\\$?[ 0-9,./:]+")       // 数値を無視
				&& !src.matches("^[-.\\w]+:?$")          // １単語だけの場合を無視
				&& !src.matches("burp\\..*")             // burp.から始まるもの(クラス名？)を無視
				&& !src.matches("lbl.*")                 // lblから始まるもの(ラベル名？)を無視
				&& src.length() > 1                      // １文字を無視
				&& !src.matches("[- A-Z]+s?")            // 大文字のみの単語を無視
				&& !src.matches("\\s+")                  // 空白のみを無視
		)) {
			System.err.println("[" + src + "]");
		}
		return dst;
	}
}
