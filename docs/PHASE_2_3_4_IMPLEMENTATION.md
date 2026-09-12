# تقرير تنفيذ المراحل 2 و3 و4 ومراجعة Room Migrations

**التاريخ:** 12 سبتمبر 2026

## المرحلة 2 — StorageScanner وMediaStore

تم تحسين التعامل مع حدود MediaStore وprovider failures. يقرأ التطبيق Images وVideos وAudio عبر MediaStore، ويعامل `null` cursor كفشل I/O بدل اعتباره مجموعة فارغة قد تؤدي إلى حذف سجلات Room. أضيفت صلاحية `READ_MEDIA_VISUAL_USER_SELECTED` المتوافقة مع Android 14، مع بقاء `MANAGE_EXTERNAL_STORAGE` غير موجودة.

لم تُضف قراءة عامة لمستندات أو Archives أو APK عبر صلاحيات واسعة؛ ذلك يحتاج تصميمًا واضحًا باستخدام SAF وتجربة مستخدم منفصلة، ولذلك تبقى إعادة تقسيم StorageScanner ودعم الفئات الإضافية قيد التنفيذ في المرحلة 2.

## المرحلة 3 — مزامنة MediaStore وRoom

تم تنفيذ reconciliation باستخدام `lastSeenScanToken`. تُحدّث الملفات الجديدة أو المتغيرة، وتُعلّم الملفات غير المتغيرة بالظهور دون إعادة تحليلها، ثم تُزال السجلات غير المرئية فقط بعد اكتمال مجموعات Images وVideos وAudio. إذا فشل provider أو أعاد query قيمة `null`، لا تتم عملية الحذف.

## المرحلة 4 — Scan Cache والتحليل الأساسي

أصبحت مطابقة cache تتحقق من URI والحجم والوقت وMIME والنوع. أضيف `analysisVersion` حتى تُبطل نتائج hash/perceptual/blur/screenshot عند تغيير الخوارزمية. تمت إضافة migration من Room 6 إلى 7 تنظف نتائج التحليل القديمة وتعيدها إلى حالة غير محللة.

## تقدم المرحلة 5 — Exact Duplicate Engine

هذا الجزء ليس إغلاقًا للمرحلة 5 بعد، لكنه أُنجز جزئيًا ضمن الدفعة: يستخدم المحرك SHA-256 عبر streaming buffer بحجم 64 KiB بعد grouping أولي بالنوع والحجم. لا يُعلن التطابق اعتمادًا على الاسم أو الحجم أو الوقت فقط. يعاد استخدام content hash المحفوظ عندما تكون نسخة التحليل الحالية، ويتم التعامل مع IOException وSecurityException للملفات غير القابلة للقراءة دون إسقاط التحليل كله. تمت إضافة اختبار SHA-256 معروف للمحتوى `abc`، ويستمر دعم cancellation عبر `ensureActive`.

## مراجعة Room Migrations

تمت مطابقة الإصدار الحالي `version = 7` مع الكيانات الحالية وتسلسل migrations:

| الانتقال | التغيير | نتيجة المراجعة |
|---|---|---|
| 1 → 2 | إضافة `contentHash` و`perceptualHash` و`blurScore` و`isScreenshot` بقيم افتراضية صحيحة | متوافق ساكنًا |
| 2 → 3 | إنشاء `trash_items` وفهرس وقت النقل | متوافق ساكنًا |
| 3 → 4 | إضافة `durationMillis` و`relativePath` مع defaults | متوافق ساكنًا |
| 4 → 5 | إنشاء `compression_history` مع مفتاح ID متزايد | متوافق ساكنًا |
| 5 → 6 | إضافة `lastSeenScanToken` وفهرسه | متوافق ساكنًا |
| 6 → 7 | إضافة `analysisVersion` ثم إبطال نتائج التحليل القديمة | متوافق ساكنًا ومقصود وظيفيًا |

تم التأكد من أن `Room @Database(version = 7)` يسجل كل الانتقالات من 1 إلى 7، وأن `fallbackToDestructiveMigration()` غير موجود. كما تم التأكد من أن أسماء الفهارس التي تنشئها migrations تطابق أسماء Room المتوقعة للفهارس المعلنة في الكيان الحالي.

## فحوصات منفذة

- `./gradlew compileDebugKotlin`: ناجح.
- `./gradlew testDebugUnitTest`: ناجح.
- `./gradlew lintDebug`: ناجح.
- `git diff --check`: ناجح.
- تمت إضافة اختبارات Cache metadata وanalysis version وSHA-256.
- لا يوجد AAB منشأ أو متابع في هذه الدفعة حسب سياسة المشروع.

## حدود التحقق

المراجعة الساكنة وCompile تؤكد عدم وجود تعارض ظاهر في Room entities أو SQL migrations. لكن اختبار ترقية قاعدة بيانات فعلية من نسخة 1 أو 5 إلى 7 لم يُنفذ على جهاز/Emulator؛ لا يوجد `adb` أو جهاز في البيئة الحالية. لذلك لا أدعي أن migration ميدانيًا مُثبتة بالكامل حتى يُنفذ اختبار Instrumentation باستخدام قاعدة بيانات فعلية.

كما تبقى المرحلة 2 قيد التنفيذ بسبب إعادة فصل مكونات scanning ودعم Documents/Downloads/Archives/APK عبر SAF، وتبقى المرحلة 5 بحاجة إلى integration tests بملفات حقيقية.
