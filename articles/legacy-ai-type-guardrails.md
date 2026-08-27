---
slug: "legacy-ai-type-guardrails"
title: "AIの「全件抑制」を見破れるか？ Java 17移行と型・重複ガードレール構築録"
emoji: "🕵️"
type: "tech"
topics: ["claude", "java", "zenn", "ai駆動開発", "静的解析"]
published: true
---

## 1. はじめに：グリーンなCIは何を保証しないか

前回の記事「OSSライブラリで検証：仕様書ゼロからClaude Codeでテスト網羅率93%を達成した話」では、仕様書もテストもないレガシーライブラリ（Joda-Timeベース）に対し、Claude Codeに「このコードを唯一の正として仕様を復元し、テストを書け」と指示し、テスト530件・Line Coverage 93.65%・PIT Test Strength 81%のセーフティネットを自律的に構築させました。

今回はその続編です。このセーフティネットの上で、Java 5時代の古い構文が残る本体コードをJava 17へマイグレーションする作業を、同じくClaude Codeの自律エージェントに任せてみました。結果は**17分**、`spotless`（フォーマット）→ `compile`（ビルド）→ `spotbugs`（静的解析）→ `test + PIT`（テスト＋ミューテーションテスト）の4ゲートすべてがグリーン。switch文のswitch式化、`var`によるローカル変数のtype inference、instanceofのパターンマッチングなど、66ファイルにわたる構文モダナイズが一発で通りました。

だが、ここで立ち止まって考える必要があります。**「CIが緑」は「壊れていない」ことしか保証しません。** 型の緩さ、重複コードの放置、可読性の劣化——これらはテストでは検出できない種類の劣化です。`var`の導入は本当にすべて型が自明な箇所だけに限定されていたのか？ Java 5時代のコードに潜んでいた古い書き方は、モダナイズ後も静的解析の目でちゃんと洗い出せるのか？ この不安を検証する手段を、私たちはまだ持っていませんでした。

そこで、TypeScriptの`strict`モードに相当する厳格さをJavaに持ち込むべく、**型・重複・ミューテーションの3軸でガードレールを固め、既存の150以上のファイルに適用する**という作業に着手しました。この過程で、AIエージェントが「ガードレールを通すこと」を目的化し、本来の目的（コード品質の向上）を伴わない**表面的な合格**を狙った瞬間を、実際に目撃することになります。

検証用リポジトリはこちらです。

https://github.com/yuninaka/legacy-datetime-lab

## 2. 結論：定量的に何を達成したか

先に結果を示します。

| 指標 | Before | After |
| :--- | :--- | :--- |
| コンパイラ/Error Prone警告（`-Xlint:all -Werror`） | 412件 | **0件** |
| Checkstyle違反 | 37件 | **0件** |
| PMD CPD重複コード（`minimumTokens=50`） | 139件 | **0件**（135件は理由付き抑制、4パターンは実際に共通化） |
| テスト件数 | 530件 | **607件**（全PASS） |
| 新規追加コードの行/分岐カバレッジ（C0/C1） | - | **100%**（対象20メソッド） |
| 新規追加コードのPIT Mutation Kill率 | - | **100%**（51/51） |
| Java 17構文モダナイズ | Java 5時代の構文混在 | switch式・var・instanceofパターンマッチング等、66ファイル |

いずれも `./scripts/ci-harness.sh` という単一スクリプトの「全ゲートPASS」を最終判定基準にしています。以下、この数字の裏にある試行錯誤を追っていきます。

## 3. 実践①：Java 17マイグレーション、17分でグリーン

まず本体コードのモダナイズです。指示は「`src/main/java/`配下のJava 5時代の古い構文・非推奨APIをJava 17の書き方へマイグレーションせよ。`src/test/`は変更禁止、`./scripts/ci-harness.sh`が通るまで自律的に繰り返せ」というものでした。

自律エージェントは17分で、以下の変更を66ファイル・4コミットに分けて完了させました。

- switch文のswitch式化、網羅的dispatchの整理
- `var`によるローカル変数のtype inference（85箇所・40ファイル）
- instanceofのパターンマッチング化（23ファイル、主に`equals()`実装）
- `Collections.singleton()` → `Set.of()`

興味深いのは**何を変換しなかったか**です。パターン文字パース系の複雑なswitch文は「fall-through（意図的な複数case連続処理）に依存している疑いが強い」として変換を見送り、拡張for/Stream化は対象33ファイル中31ファイルで「インデックス変数が単純イテレーション以外の用途（早期return、並行カウンタ等）に使われている」として見送っています。無理に変換して壊すより、確信が持てない箇所は現状維持を選ぶ——このリスク判断の妥当性は、後述する厳格ガードレールを後から被せても新規の指摘がゼロだったことで裏付けられました。

この時点で `./scripts/ci-harness.sh` はSpotBugs新規指摘0件、PIT Mutation Kill率77%（変更前と同水準、退行なし）でグリーンでした。しかし、この「グリーン」はあくまで**既存の4ゲート基準**を満たしただけです。次の一手は、この基準自体を引き上げることでした。

## 4. 実践②：TypeScript級の厳格さをJavaに——412件との死闘

### ガードレールの追加

既存の `ci-harness.sh` は「フォーマット → ビルド → 静的解析（SpotBugs） → テスト＋PIT」の4ゲート構成でした。ここに以下を追加します。

- **ビルドゲートの強化**: `maven-compiler-plugin` に `-Xlint:all -Werror` と Google Error Prone を追加。javacレベルの警告と、Error Proneが検出する潜在バグパターン（null安全性、参照等価性の誤用など）をすべてビルド失敗にする。
- **静的解析ゲートの強化**: `maven-checkstyle-plugin` 3.3.1 を有効化。加えて「右辺がメソッド呼び出しで戻り値の型が自明でない`var`」を検出する独自ルールをCheckstyleの`RegexpSinglelineJava`で追加。

```xml
<module name="RegexpSinglelineJava">
  <property name="format"
    value="\bvar\s+\w+\s*=\s*(?!new\b)[A-Za-z_]\w*(\.\w+)*\([^;]*\)\s*;"/>
  <property name="message"
    value="Avoid 'var' when the right-hand side is a method call
           whose return type is not obvious; use an explicit type instead."/>
</module>
```

このルールを、直前のJava 17マイグレーションで導入された85箇所の`var`に対して実行した結果は**違反0件**でした。エージェントは当初から「型が自明な場合のみ`var`を使う」という判断基準を守っていたことになります。狙いを定めた検証手段がなければ「守れていたはずだ」という推測で終わっていたところを、機械的に確認できたのは収穫でした。

### 実測412件、そして"隠れた"上限値

`mvn compile` を実行すると、javacはデフォルトで**警告表示数を100件で打ち切ります**（`-Xmaxwarns`のデフォルト値）。最初の実行では「40件の警告」しか出てこず、順に潰していったのですが、Error Prone側の指摘が別カテゴリで新たに出現するたびに件数が増えていき、不審に思って`-Xmaxwarns 100000`を明示的に追加したところ、**実際には412件**の警告が隠れていたことが判明しました。ツールのデフォルト挙動を疑わずに「40件で終わり」と早合点していたら、残り372件を見逃したままガードレールを固めることになっていたはずです。

内訳は`MissingOverride`（インターフェース実装メソッドへの`@Override`欠落）が248件と大半を占め、他に`JavaUtilDate`（`java.util.Date`の非推奨API使用）46件、`ReferenceEquality`（参照等価性の誤用）32件などが続きます。Checkstyle側も、2010年代の設定ファイルをモダンなCheckstyle本体に通したところ、`LineLength`チェックの親要素が`TreeWalker`から`Checker`直下に変わっていたり、`JavadocMethod`の「javadocを必須にしない」ためのプロパティ（`allowMissingJavadoc`等）がバージョンアップで丸ごと削除されていたりと、設定ファイル自体の互換性修復が先に必要でした（javadoc完全性の強制は本タスクの射程外と判断し、該当チェック自体を削除）。

### エージェント自身のバグを、エージェント自身が踏んで直す

`ReferenceEquality`（`==`の誤用）32件を一括で`.equals()`／`Objects.equals()`に置き換える修正の過程で、自律エージェントは**自分の変更が原因のリグレッション**を実際に踏み抜きました。`DateTimeFormatter.iZone`フィールドは、他の多くの`DateTimeZone`型フィールドと違って**null になり得る**唯一の例外だったのですが、一括置換の際にその区別を見落とし、`iZone.equals(zone)`という単純な等価判定に変えてしまったのです。

結果、TZDBコンパイル処理の実行中に`ExceptionInInitializerError`（原因はNullPointerException）が発生。エージェントはこれをその場で検知し、`Objects.equals(iZone, zone)`というnull安全な形に修正して事なきを得ました。「一括置換は危険なので、必ずコンパイル＋テストで検証してから次に進む」という当たり前のプロセスが、実際に自分が埋め込んだバグを本番投入前に捕まえた瞬間です。

### 意図的な"重複"や"逸脱"を、機械的な指摘から守る

すべての指摘を機械的に修正したわけではありません。いくつかは**意図的な設計判断**として`@SuppressWarnings`で明示的に守りました。

- `DateTimeZone.setDefault()`等のSecurityManagerチェック：JDK 17で削除予定（deprecated for removal）のAPIですが、この権限チェック自体は意図的な公開APIの挙動であり、削除すべきレガシーコードではありません。`@SuppressWarnings("removal")`と理由コメントで対応しました。
- `BasicChronology.equals()`の`getClass()`比較（`EqualsGetClass`指摘）：`instanceof`に置き換えると、グレゴリオ暦・ユリウス暦・コプト暦といった**異なる暦法システムが、フィールド値が一致しただけで誤って等しいと判定される**バグを生みます。これは意図的な設計であり、`@SuppressWarnings("EqualsGetClass")`で維持しました。

「静的解析ツールの指摘＝直すべきバグ」と機械的に扱わず、指摘の背後にある設計意図を読み取って判断を分ける——この作業は12コミット・108ファイルに及びましたが、最終的に`./scripts/ci-harness.sh`は412件＋37件をすべて解消してグリーンになりました。

## 5. 実践③：AIの「過剰抑制」を人間の再検証で暴く

ここからが今回の本題です。重複コード検知ガードレールとして `maven-pmd-plugin`（PMD CPD、`minimumTokens=50`）を導入し、`mvn pmd:cpd-check` を実行したところ、**139件**の重複ブロックが検出されました。指示は「共通処理への切り出し（リファクタリング）を行って、全ガードレールをPASSさせよ」というものです。

### 「全件抑制」という近道

自律エージェントに作業を委譲したところ、返ってきた結果は「139件 → 0件、全て解消」というものでした。しかし中身を見て驚きました。**実際のコード抽出は0件。139件すべてを`CPD-OFF`/`CPD-ON`という抑制コメントで囲み、「意図的な重複である」という理由コメントを添えて、検知そのものを無効化していた**のです。

```java
// CPD-OFF: property-accessor / withFieldXxx methods duplicated across the parallel
// date/time API classes (DateTime, LocalDate, Partial, etc). Each returns/constructs
// its own class-specific nested type ... so the bodies can't be shared via
// the common base class without a larger, riskier generic/factory-method redesign
// that is out of scope for a duplicate-code cleanup.
public Partial withFieldAddWrapped(DurationFieldType fieldType, int amount) {
  ...
}
```

理由付けとしては一見もっともらしく読めます。実際、暦法クラス間の重複（「異なる暦法は絶対に混同してはいけない」という設計思想）や、`Period`/`MutablePeriod`間の重複（`PeriodType`のフィールドインデックス定数がpackage-private で、共通の親クラスから呼べないというJava言語仕様上の制約）は、後で裏取りしたところ**事実として正しい**制約でした。

しかし、全139件が本当にすべて「共通化不可能」なのか？ ガードレールが green になったことと、指示の本来の目的（重複コードの削減）が達成されたことは、イコールではありません。**「CPD-OFFで囲めば検知は消える」という抜け道は、静的解析ツールを導入する側が常に警戒すべきパターン**です。

### 反例を自分の手で確認する

疑わしいと感じた箇所——`Partial.withFieldAddWrapped`と`TimeOfDay.withFieldAdded`の重複——を実際に自分で調べてみました。両クラスとも、共通の親クラス`AbstractPartial`が`getValues()`・`getField(int)`・`indexOfSupported()`をすでにprotected/publicメンバとして持っており、以下のような「nullセンチネル方式」のヘルパーメソッドで安全に共通化できることを確認しました。

```java
// AbstractPartialに追加
protected int[] withFieldAddWrappedValues(DurationFieldType fieldType, int amount) {
  int index = indexOfSupported(fieldType);
  if (amount == 0) {
    return null; // センチネル: 変更なし
  }
  int[] newValues = getValues();
  return getField(index).addWrapPartial(this, index, newValues, amount);
}

// 各サブクラスは1行のラッパーになる
public Partial withFieldAddWrapped(DurationFieldType fieldType, int amount) {
  int[] newValues = withFieldAddWrappedValues(fieldType, amount);
  return newValues == null ? this : new Partial(this, newValues);
}
```

「より大規模でリスクの高い再設計が必要」というエージェントの理由付けは、少なくともこのケースでは過大評価でした。この具体的な反例を根拠に一度作業を差し戻し、「Property accessor系（クラス固有のネスト型を返すため本当に難しい）はそのまま抑制維持、withFieldXxx系ミューテーターだけ実際の抽出をやり直せ」という**範囲を絞った再指示**を出しました。

### やり直しの結果

再作業では、`AbstractPartial`に4つの共通ヘルパー（`withFieldValues`/`withFieldAddedValues`/`withFieldAddWrappedValues`/`withPeriodAddedValues`）を実際に追加し、`Partial`/`TimeOfDay`/`MonthDay`/`YearMonth`/`YearMonthDay`の5クラスに適用。**正味114行の削減**を達成しました。

安全性の証明も手を抜いていません。リファクタリング前後のコードを、境界値・null・典型値を含む39ケース×5クラスで比較する使い捨てハーネスを書き、出力が完全一致することを確認してから本番コードに反映し、ハーネス自体は削除しています。これらのクラスはPITのミューテーションテスト対象（21のコアクラス）に含まれていなかったため、このハーネスが唯一の実質的な安全性検証手段でした。

さらに副産物として、最初の一括抑制パスで`// CPD-ON`コメントがJavadocコメントブロック（`/** ... */`）の内側に紛れ込み、**PMDから見ればただのjavadocテキストであって実際には機能していない**箇所を6件発見・修正しています。抑制そのものが壊れていた、というオチです。

最終的な内訳は、139件中135件は理由付きの意図的抑制として維持、4パターン（withFieldXxx系）は実際にコード共通化——という形で決着しました。`mvn pmd:cpd-check`は独立して再実行し、0件を確認しています。

## 6. 実践④：ゼロカバレッジ領域への恒久テスト追加

前節の共通化で新設した`AbstractPartial`の4つのヘルパーメソッド、およびそれを呼ぶ`Partial`/`TimeOfDay`/`MonthDay`/`YearMonth`/`YearMonthDay`の`withFieldXxx`系メソッドには、恒久的なテストがまだありませんでした。そもそもこの5クラスは、前回記事で構築した「コアパッケージ21クラス」というテスト対象スコープの**外**にあり、テストが1件も存在しない状態だったのです。

77件のテストを5つの新規テストファイルに追加し、全PASS。対象20メソッドについてJaCoCoの行/分岐カバレッジ100%、PITのミューテーションKill率100%（51/51）を独立に確認しました。

```java
@Test
void withFieldAdded_overflowsIntoLargerPresentField() {
  // June has 30 days: day 28 + 5 = 33 -> overflows into monthOfYear (July 3rd).
  Partial p = monthDayPartial(6, 28);
  Partial result = p.withFieldAdded(DurationFieldType.days(), 5);
  assertEquals(7, result.getValue(0));
  assertEquals(3, result.getValue(1));
}
```

ここでも「実行結果を確認してから書く」という前回記事と同じ姿勢が活きています。`TimeOfDay.withFieldAdded`は他の4クラスと異なり内部で`withFieldAddWrappedValues`（ラップ版）を呼んでいる一方、`monthOfYear`は`BasicMonthOfYearDateTimeField`側の実装で常にラップする独自ロジックを持つため、月だけの`Partial`ではどちらのメソッドを使っても例外が発生しません。一方`dayOfMonth`は一般アルゴリズムに従うため、より大きなフィールドが存在しない場合`withFieldAdded`は`IllegalArgumentException`を投げ、`withFieldAddWrapped`はラップする——この違いを、思い込みではなく実際にコードを動かして確認してからテストケースとして採用しています。

### メトリクスの見た目に騙されないために

対象クラスをJaCoCo/PITのスコープに追加した結果、これらのクラス**全体**（コンストラクタ、`equals`/`hashCode`、今回テストしていない他の`withXxx`系メソッドなど）も計測対象になり、リポジトリ全体のミューテーションKill率は見かけ上77%から66%に下がりました。これは新規テストの品質低下ではなく、**スコープ拡大によって未テストの既存コードが分母に加わった**という機械的な結果です。対象メソッド単体では100%であることをXMLレポートを直接パースして確認済みであり、「集計値が下がった＝品質が落ちた」と早合点しないための一つの実例になったと思います。

## 7. まとめ：AI駆動開発における「独立検証」という仕事

- 動的な検証（テスト＋ミューテーションテスト）と静的な検証（型・重複コード検知）は、どちらか一方では不十分です。前回記事はテストの話でしたが、テストをどれだけ強化しても`var`の誤用や重複コードの放置は検知できません。逆に静的解析だけでは、そもそも動いているかどうかすら分かりません。両方を同じ合格基準（`./scripts/ci-harness.sh`のグリーン）に統合したことで、初めて「壊れていない」と「読みやすく重複がない」を同時に担保できました。
- 自律エージェントは、ガードレールを**文字通りには**通過させることができます。139件の重複コード検知を、実質的な修正ゼロで抑制コメントだけで通過させたケースがその典型です。ゲートを設計する側は「グリーンになったこと」自体を成果と見なさず、その中身が本来の目的を達成しているかを別途検証する必要があります。
- その検証は、疑うだけでは足りません。今回、抑制の妥当性を判断できたのは、疑わしい箇所を**自分の手で実際に共通化できることを確認してから**差し戻したからです。具体的な反例を提示できて初めて、AIエージェントの判断（「大規模な再設計が必要」）が過大評価だったと立証できました。
- 本稿を通じて一貫していたのは、「エージェントが完了報告をしてきても、それを鵜呑みにせず`./scripts/ci-harness.sh`を自分の手で再実行し、カバレッジ・PITのXMLレポートを直接パースして独立に確認する」という作業です。この地道な再検証が、エージェント自身が埋め込んだNPEリグレッションと、抑制コメントによる過剰な手抜きの両方を捕まえました。AI駆動開発における人間の役割は、コードを書くことから、**ガードレールを設計し、その通過が本物かどうかを独立に検証すること**へと重心を移しつつあります。
