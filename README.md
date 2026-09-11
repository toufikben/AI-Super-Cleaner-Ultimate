# AI Super Cleaner Ultimate

تطبيق Android حقيقي لفهم التخزين وتنظيفه بأمان، مع أولوية للخصوصية والدقة قبل تحقيق الدخل.

## حالة المشروع

**المراحل 1–7 مكتملة:** تأسيس Android وUI/UX، الخصوصية والصلاحيات، الماسح الحقيقي وRoom، Smart Cleanup Score، محركات التكرار والتشابه، التنظيف الآمن وTrash، وتحليل الملفات الكبيرة والتنزيلات وضغط الفيديو الأولي.

**المرحلة 8 مكتملة:** Compression History، تصدير النسخ إلى MediaStore، ضغط الصور، وعدم استبدال الملفات الأصلية.

## الضغط والتصدير

يتيح Tools اختيار فيديو أو صورة وإنشاء نسخة جديدة. يستخدم الفيديو Media3 Transformer بترميز H.264/AAC، وتستخدم الصور BitmapFactory مع downsampling وpresets Small وBalanced وHigh Quality. تُصدر النسخ عبر MediaStore باستخدام `IS_PENDING` على Android Q+، فلا تظهر النسخة قبل اكتمال الكتابة.

يسجل `compression_history` URI الأصلي والناتج، نوع الوسيط، preset، الحجم قبل وبعد، الحالة، والوقت. لا يستبدل التطبيق الأصل ولا يحذفه بصمت.

## الخصوصية

تظل الملفات على الجهاز. لا يستخدم التطبيق `MANAGE_EXTERNAL_STORAGE` ولا يتجاوز قيود Android. لا تُرفع الصور أو الفيديوهات إلى خادم في مسار التنظيف أو الضغط العادي.

## البناء

```bash
./gradlew assembleDebug lintDebug
```

## خارطة التنفيذ التالية

1. ربط أحجام URI المختارة بسجل Room وتحسين presets.
2. Quick Clean وAdvanced Smart Scan.
3. تحسين الأداء والـcache واختبارات المكتبات الكبيرة.
4. AdMob ثم Billing وPremium بعد إثبات قيمة المنظف الأساسي.
