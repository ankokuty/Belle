Belle (Burp Suite 非公式日本語化ツール)
====

このツールは、PortSwigger社の製品であるBurp Suiteのインタフェースを、日本語化するツールです。

![mainwindow](screenshots/mainwindow.png)  
![manualsimulator](screenshots/manualsimulator.png)  

## 使用方法

[https://github.com/ankokuty/Belle/releases](https://github.com/ankokuty/Belle/releases)より最新の`belle.zip`をダウンロードし、`javassist.jar`、`belle.jar`、`user.vmoptions`の3つのファイルをBurp Suiteのインストールディレクトリに展開してください。

### インストーラーを使ってBurp Suiteをインストールした場合

インストール時に変更していなければ、`%LOCALAPPDATA%\Programs\BurpSuiteCommunity`や`%LOCALAPPDATA%\Programs\BurpSuitePro`にあると思います。

起動は従来通り、Burp Suiteのショートカットをダブルクリックするなどして起動してください。


### コマンドラインから起動する場合

(-jar オプションより前に) -javaagentコマンドラインオプションを指定して起動してください。

```
java -javaagent:belle.jar -Xmx1024m -jar burpsuite_community.jar
```

### その他

以前のバージョンでは、カレントディレクトリの`ja.txt`を読み込んでいましたが、現在は内部に取り込んでいるため、このファイルは不要です。

以前のバージョンでは、`BurpSuitePro.vmoptions`を編集していましたが、Burp Suite本体が`user.vmoptions`を読み込む仕様になったため、現在では不要です。

以前は、[Javassist](https://github.com/jboss-javassist/javassist/)を自分でダウンロードしてもらうようにしていましたが、リリースパッケージに内包するようにしました。


## 注意事項

このツールは、私個人が勝手に開発したもので、PortSwigger社は一切関係ありません。日本語訳の間違いはもちろんのこと、本ツールを使用したことによる不具合等についてPortSwiggerに問い合わせないようお願いします。

このツールは内部でJava実行環境のバイトコードを変更します。Oracle社のJava実行環境で使用した場合、[バイナリ・コードライセンス](http://www.oracle.com/technetwork/java/javase/terms/license/index.html)に違反する可能性があります。ライセンスを確認の上、[OpenJDK](http://openjdk.java.net/)等その他のJava実行環境での実行を推奨します。Professional版 2.0.15beta以降、Community版 2.1以降のインストーラーには、OpenJDKのJava実行環境(JRE)が内包されています。

Inspector使用時に、選択した文字列が意図せず翻訳されてしまう既知のバグがあります。

このツールは単純に、辞書ファイルで指定した文字列にマッチする文字列を見つけると、対応する日本語訳に変換しています。
影響範囲をGUI表示部分に限定しているつもりですが、送受信したHTTPメッセージも意図せず変換してしまっている可能性があります。
Webアプリケーションテストへの影響を確認の上、自己責任で使用してください。

PortSwigger社から英語メッセージ一覧をもらったわけではなく、使用中に英語表記であることを目視した箇所を適宜翻訳しているため、おそらく網羅されていません。
未翻訳の箇所や間違いを見つけた場合は、ご連絡ください。

## References

- [挙動の解説(MBSD Blog)](https://www.mbsd.jp/blog/20190701.html)
- [Burp Suite](https://portswigger.net/burp/)
- [Burp Suite Japanユーザグループ](https://groups.google.com/d/forum/burp-suite-japan)
- [Burp Suite Japan](https://twitter.com/burpsuitejapan)

## Author

[@ankokuty](https://twitter.com/ankokuty)
