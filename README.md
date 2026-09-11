# AI Super Cleaner Ultimate

تطبيق Android حقيقي لفهم التخزين وتنظيفه بأمان، مع أولوية للخصوصية والدقة قبل تحقيق الدخل.

## حالة المشروع

**المرحلة 1 مكتملة:** تأسيس مشروع Android، هوية Material 3، UI/UX، الملاحة، وثيم قابل للتوسع.

**المرحلة 2 مكتملة:** إدارة الصلاحيات السياقية، Privacy Center، وتفضيلات محلية غير حساسة دون `MANAGE_EXTERNAL_STORAGE`.

**المرحلة 3 مكتملة:** ماسح MediaStore حقيقي، قاعدة Room، تقدم تدريجي قابل للإلغاء، ولوحة Home مرتبطة ببيانات الجهاز.

**المرحلة 4 مكتملة:** Smart Cleanup Score ومحرك توصيات قابل للتفسير.

**المرحلة 5 مكتملة:** Exact Duplicate Engine وPerceptual Similarity ومؤشر الضبابية واكتشاف أولي للقطات الشاشة.

**المرحلة 6 مكتملة:** مراجعة يدوية، MediaStore Trash قابلة للاستعادة، Restore، وPermanent Delete.

**المرحلة 7 مكتملة:** Storage Analyzer للملفات الكبيرة والتنزيلات وAPK، metadata للمدة والمسار، وضغط فيديو إلى نسخة جديدة عبر Media3 Transformer.

## محللات الفيديو والملفات

يعرض Analyze الملفات الأكبر من 500 MB، وDownloads وAPK التي يكشفها MediaStore، مع الحجم ونوع الوسيط والمسار النسبي المتاح والمدة عند توفرها. لا يعني الحجم الكبير أن الملف غير ضروري.

يتيح Tools اختيار فيديو من Storage Access Framework وإنشاء compressed copy محلية عبر Media3 Transformer بترميز H.264/AAC. الأصل لا يُستبدل، وفشل codec يعرض رسالة مفهومة.

## الخصوصية

تظل الملفات على الجهاز. لا يستخدم التطبيق `MANAGE_EXTERNAL_STORAGE` ولا يتجاوز قيود Android. تحفظ Room metadata والبصمات ونتائج التحليل دون رفع محتوى الوسائط.

## البناء

```bash
./gradlew assembleDebug lintDebug
```

## خارطة التنفيذ التالية

1. Compression History وتصدير نسخة الفيديو إلى MediaStore.
2. Advanced Scan ثم الأداء والـcache المتقدم.
3. AdMob ثم Billing وPremium بعد إثبات قيمة المنظف الأساسي.
