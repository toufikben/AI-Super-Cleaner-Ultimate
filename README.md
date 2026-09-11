# AI Super Cleaner Ultimate

تطبيق Android حقيقي لفهم التخزين وتنظيفه بأمان، مع أولوية للخصوصية والدقة قبل تحقيق الدخل.

## حالة المشروع

**المراحل 1–8 مكتملة:** تأسيس Android وUI/UX، الخصوصية والصلاحيات، الماسح الحقيقي وRoom، Smart Cleanup Score، محركات التكرار والتشابه، التنظيف الآمن وTrash، تحليل الملفات الكبيرة والتنزيلات، وضغط الصور والفيديو مع تصدير وسجل محلي.

**المرحلة 9 مكتملة:** فصل Quick Clean المحافظ عن Advanced Smart Scan.

## مسارات المسح والتنظيف

Quick Clean يعرض فقط مرشحي Exact Duplicate الذين يملكون content hash. لا يدرج الملفات الكبيرة أو الصور الضبابية أو التشابه البصري افتراضياً، ولا يحدد أو يحذف أي ملف تلقائياً. بعد اختيار المستخدم يمر المسار عبر التأكيد وTrash.

Advanced Smart Scan يشغل المسح الحقيقي ومحركات SHA-256 وPerceptual Analysis وRecommendation Engine، ثم يعيد Smart Cleanup Score والتوصيات المفسرة. لا يوجد حذف أثناء التحليل. هذا المسار جاهز للربط لاحقاً بـ Premium أو Rewarded Ad اختياري، لكن لا يُجعل الإعلان شرطاً للحذف.

## الخصوصية

تظل الملفات على الجهاز. لا يستخدم التطبيق `MANAGE_EXTERNAL_STORAGE` ولا يتجاوز قيود Android.

## البناء

```bash
./gradlew assembleDebug lintDebug
```

## خارطة التنفيذ التالية

1. تحسين الأداء والـcache وbounded concurrency للمكتبات الكبيرة.
2. اختبارات Unit وInstrumentation وذاكرة منخفضة.
3. AdMob ثم Billing وPremium بعد تثبيت الأداء والاختبارات.
