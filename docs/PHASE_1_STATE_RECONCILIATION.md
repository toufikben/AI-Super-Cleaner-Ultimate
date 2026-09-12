# تقرير المرحلة 1 — مصدر الحقيقة والتزامن

**التاريخ:** 12 سبتمبر 2026

## المشكلة

كان `StorageScanner` يضيف أو يحدّث الملفات التي تظهر في MediaStore، لكنه لا يزيل سجلات Room التي اختفت من الجهاز بعد حذف أو نقل خارجي. كما أن reconciliation بعد فحص جزئي كان يمكن أن يكون خطرًا إذا اعتبر الاستعلام الفاشل نتيجة فارغة.

## الإصلاحات

تمت إضافة `lastSeenScanToken` إلى `FileMetadataEntity` مع فهرس ومهاجرة Room من الإصدار 5 إلى 6. كل فحص كامل ناجح يضع token موحدًا للملفات التي ظهرت، ثم يزيل فقط السجلات التي لم تظهر في ذلك الفحص.

تمت إضافة DAO operations للـbatch marking و`removeFilesNotSeenInScan`. الملفات التي تغيرت metadata لها تُستبدل، بينما الملفات غير المتغيرة تُحدّث بعلامة الظهور دون إعادة تحليلها. لا يتم تنفيذ الحذف من Room إلا بعد اكتمال مجموعات Images وVideos وAudio بنجاح.

تم منع الاستعلام الفاشل الذي يعيد `null` من الظهور كفحص ناجح؛ يعامل الآن كـ`IOException`، وبذلك لا يتم حذف سجلات قديمة عند فشل MediaStore provider.

تمت إزالة `fallbackToDestructiveMigration()` حتى لا يؤدي غياب migration إلى مسح قاعدة بيانات المستخدم بصمت. توجد migration صريحة `5 -> 6`.

## Check المنفذ

- `testDebugUnitTest`: ناجح.
- `lintDebug`: ناجح.
- compile بعد تعديل Scanner: يجب إعادة تشغيل الفحص النهائي قبل تعليم المرحلة في الخارطة.
- Instrumentation/اختبار MediaStore الحقيقي: غير منفذ لغياب `adb` وجهاز أو Emulator.

## حدود المرحلة

لم تتم في هذه المرحلة إعادة هيكلة جميع محركات MediaScanner وLargeFileScanner وDuplicateScanner؛ ذلك جزء من المرحلة 2. تم تنفيذ الجزء الآمن المرتبط بحدود مصدر الحقيقة وreconciliation فقط.
