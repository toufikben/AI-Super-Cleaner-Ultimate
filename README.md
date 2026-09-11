# AI Super Cleaner Ultimate

تطبيق Android حقيقي لفهم التخزين وتنظيفه بأمان، مع أولوية للخصوصية والدقة قبل تحقيق الدخل.

## حالة المشروع

**المراحل 1–10 مكتملة:** تأسيس Android وUI/UX، الخصوصية والصلاحيات، الماسح الحقيقي وRoom، Smart Cleanup Score، محركات التكرار والتشابه، التنظيف الآمن وTrash، ضغط النسخ وسجلها، Quick Clean وAdvanced Smart Scan، Cache، bounded concurrency، واختبارات Unit.

**المرحلة 11 مكتملة:** AdMob خلف مدير مستقل، Rewarded Ad اختياري، interstitial cooldown، وفشل آمن لا يعطل الوظائف الأساسية.

## سياسة الإعلانات

يمكن للمستخدم اختيار Watch a short ad to unlock one Advanced Scan، لكن Advanced Smart Scan الأساسي لا يعتمد على الإعلان، والحذف لا يعتمد عليه إطلاقاً. لا يظهر إعلان داخل اختيار الملفات أو تأكيد النقل إلى Trash أو الحذف الدائم. يوجد cooldown للإعلانات البينية ولا توجد سلسلة إعلانات بعد كل إجراء.

يستخدم المشروع حالياً Google test App ID وtest ad unit IDs فقط. قبل الإصدار يجب استبدالها بمعرفات الإنتاج، إضافة UMP/consent flow، تحديث Privacy Policy وData Safety، وضبط frequency caps من AdMob console.

## الخصوصية

تظل الملفات على الجهاز. لا يستخدم التطبيق `MANAGE_EXTERNAL_STORAGE` ولا يتجاوز قيود Android. لا تُرفع الصور أو الفيديوهات إلى خادم في مسار التنظيف أو الضغط العادي.

## البناء

```bash
./gradlew assembleDebug lintDebug
```

## خارطة التنفيذ التالية

1. Google UMP والموافقة والـPrivacy/Data Safety قبل الإنتاج.
2. Google Play Billing وPremium وPaywall.
3. اختبارات Instrumentation وBenchmark النهائية.
