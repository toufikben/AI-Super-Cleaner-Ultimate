# AI Super Cleaner Ultimate

تطبيق Android حقيقي لفهم التخزين وتنظيفه بأمان، مع أولوية للخصوصية والدقة قبل تحقيق الدخل.

## حالة المشروع

**المراحل 1–13 مكتملة:** تأسيس Android وUI/UX، الخصوصية والصلاحيات، الماسح الحقيقي وRoom، Smart Cleanup Score، محركات التكرار والتشابه، التنظيف الآمن وTrash، ضغط النسخ وسجلها، Quick Clean وAdvanced Smart Scan، Cache، bounded concurrency، AdMob، Rewarded Ads، Google Play Billing، Premium، Paywall، UMP، وسياسة الخصوصية وData Safety.

**المرحلة 14 مكتملة:** Unit tests، Instrumentation test source، Release QA script، ProGuard، Release build، وGitHub Actions pipeline.

## QA والبناء

```bash
QA_ALLOW_TEST_ADS=1 ./scripts/qa_release.sh
./gradlew assembleRelease --no-daemon
```

تم التحقق محلياً من Unit tests وassembleDebug وlintDebug وassembleRelease. لا يوجد جهاز أو Emulator متصل في بيئة البناء الحالية، ولذلك لم تُنفذ Instrumentation tests فعلياً؛ مصدر الاختبار موجود في `app/src/androidTest`.

ينتج Release حالياً APK غير موقع حتى تُمرر متغيرات keystore المحمية:

```text
ANDROID_KEYSTORE_PATH
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

لا تحفظ هذه القيم في GitHub أو الملفات المصدرية. راجع [تقرير QA](docs/PHASE_14_QA_REPORT.md) قبل النشر.

## قبل النشر

استبدل Test Ad IDs، أنشئ UMP messages، اربط Privacy Policy URL، أنشئ منتجات Play Billing، اختبر License Testers، نفذ Instrumentation على API 26+، وراجع Data Safety وPlay Console Internal Testing.
