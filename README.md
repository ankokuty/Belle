Burp Suite 非公式日本語化ツール
====

このツールは、PortSwigger社の製品であるBurp Suiteのインタフェースを、日本語化するツールです。

## 使用方法

本ツールは、Javassistを使用しています。次のリンクからライブラリファイルをダウンロードしておいてください。

[javassist.jar](https://github.com/jboss-javassist/javassist/raw/rel_3_21_0_ga/javassist.jar)

### インストーラーを使ってBurpをインストールした場合

burp_ja.jar と burp_ja.txt　と javassist.jar の３つのファイルをダウンロードし、Burp Suiteがインストールされているフォルダにコピーします。
インストール時に変更していなければ、``C:\Program Files\BurpSuiteFree`` や``C:\Program Files\BurpSuitePro``にあると思います。

BurpSuiteFree.vmoptions か BurpSuitePro.vmoptions ファイルをエディタで開き、１行追記します。

```bash
# Enter one VM parameter per line
# For example, to adjust the maximum memory usage to 512 MB, uncomment the following line:
# -Xmx512m
# To include another file, uncomment the following line:
# -include-options [path to other .vmoption file]
-Xmx12151m
# 以下の行を追記
-javaagent:burp_ja.jar
```

通常通り、スタートメニューのショートカット等から起動します。

### Burp Suiteのjarファイルを任意の場所にインストールした場合

burp_ja.jar と burp_ja.txt　と javassist.jar の３つのファイルをダウンロードし、Burpのjarファイル(`burpsuite_free_v1.7.06.jar`等)と同じフォルダにコピーします。

Burpのjarファイルがあるフォルダに移動し、(-jar オプションより前に) -javaagentコマンドラインオプションを指定して起動します。

```
java -javaagent:burp_ja.jar -Xmx1024m -jar burpsuite_free_v1.7.06.jar
```

### その他

本ツールは、カレントディレクトリの burp\_ja.txt を読み込みます。Windowsのショートカットなどから起動する場合は burp\_ja.txtがあるフォルダを作業フォルダーに指定、シェルスクリプトやバッチファイルなどから起動する場合はカレントディレクトリを変更した上でjavaコマンドを実行してください。

## 注意事項

このツールは、私個人が勝手に開発したもので、PortSwigger社は一切関係ありません。日本語訳の間違いはもちろんのこと、本ツールを使用したことによる不具合等についてPortSwiggerに問い合わせないようお願いします。

このツールは内部でJava実行環境のバイトコードを変更します。Oracle社のJava実行環境で使用した場合、[バイナリ・コードライセンス](http://www.oracle.com/technetwork/java/javase/terms/license/index.html)に違反する可能性があります。ライセンスを確認の上、[OpenJDK](http://openjdk.java.net/)等その他のJava実行環境での実行を推奨します。

このツールは単純に、辞書ファイルで指定した文字列にマッチする文字列を見つけると、対応する日本語訳に変換しています。
影響範囲をGUI表示部分に限定しているつもりですが、送受信したHTTPメッセージも意図せず変換してしまっている可能性があります。
Webアプリケーションテストへの影響を確認の上、自己責任で使用してください。

PortSwigger社から英語メッセージ一覧をもらったわけではなく、使用中に英語表記であることを目視した箇所を適宜翻訳しているため、おそらく網羅されていません。
未翻訳の箇所や間違いを見つけた場合は、ご連絡ください。

## References

- [Burp Suite](https://portswigger.net/burp/)
- [Burp Suite Japan](https://twitter.com/burpsuitejapan)
- [Burp Suite Japanユーザグループ　サイボウズLive](https://cybozulive.com/join/request/applyNotLogin?key=R602BpyfCD)

## Author

[@\_bun\_](https://twitter.com/_bun_)
