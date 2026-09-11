# AI Super Cleaner Ultimate

تطبيق Android حقيقي لفهم التخزين وتنظيفه بأمان، مع أولوية للخصوصية والدقة قبل تحقيق الدخل.

## حالة المشروع

**المراحل 1–9 مكتملة:** تأسيس Android وUI/UX، الخصوصية والصلاحيات، الماسح الحقيقي وRoom، Smart Cleanup Score، محركات التكرار والتشابه، التنظيف الآمن وTrash، محللات الملفات الكبيرة والتنزيلات، ضغط النسخ وسجلها، ومسارا Quick Clean وAdvanced Smart Scan.

**المرحلة 10 مكتملة:** Cache للملفات غير المتغيرة، bounded concurrency لتحليل الصور، cancellation، واختبارات Unit للـCache.

## الأداء

يقارن الماسح URI والحجم وmodified timestamp وMIME type قبل إعادة كتابة metadata. يعاد استخدام نتائج التحليل للملفات الثابتة. تحليل الصور يعمل على دفعات وبحد أقصى مسارين متوازيين، ويستخدم نسخاً مصغرة لحماية الذاكرة. لا توجد عمليات تحليل ثقيلة على Main Thread.

## الاختبارات

```bash
./gradlew testDebugUnitTest assembleDebug lintDebug --no-daemon
```

## الخصوصية

تظل الملفات على الجهاز. لا يستخدم التطبيق `MANAGE_EXTERNAL_STORAGE` ولا يتجاوز قيود Android.

## خارطة التنفيذ التالية

1. AdMob بإعلانات محدودة وRewarded اختياري.
2. Google Play Billing وPremium وPaywall.
3. تحسينات الاختبارات النهائية وInstrumentation والأداء على المكتبات الضخمة.
