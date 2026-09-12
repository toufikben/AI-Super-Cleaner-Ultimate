# تقرير تنفيذ المراحل 2 و3 و4

**التاريخ:** 12 سبتمبر 2026

## المرحلة 2 — StorageScanner وMediaStore

تم تحسين التعامل مع حدود MediaStore وprovider failures. يقرأ التطبيق Images وVideos وAudio عبر MediaStore، ويعامل `null` cursor كفشل I/O بدل اعتباره مجموعة فارغة قد تؤدي إلى حذف سجلات Room. أضيفت صلاحية `READ_MEDIA_VISUAL_USER_SELECTED` المتوافقة مع Android 14، مع بقاء `MANAGE_EXTERNAL_STORAGE` غير موجودة.

لم تُضف قراءة عامة لمستندات أو Archives أو APK عبر صلاحيات واسعة؛ ذلك يحتاج تصميمًا واضحًا باستخدام SAF وتجربة مستخدم منفصلة، ولذلك تبقى إعادة تقسيم StorageScanner ودعم الفئات الإضافية قيد التنفيذ في المرحلة 2.

## المرحلة 3 — Scan Cache

أصبحت مطابقة cache تتحقق من URI والحجم والوقت وMIME والنوع. أضيف `analysisVersion` حتى تُبطل نتائج hash/perceptual/blur/screenshot عند تغيير الخوارزمية. تمت إضافة migration من Room 6 إلى 7 تنظف نتائج التحليل القديمة وتعيدها إلى حالة غير محللة.

## المرحلة 4 — Exact Duplicate Engine

يستخدم المحرك SHA-256 عبر streaming buffer بحجم 64 KiB بعد grouping أولي بالنوع والحجم. لا يُعلن التطابق اعتمادًا على الاسم أو الحجم أو الوقت فقط. يعاد استخدام content hash المحفوظ عندما تكون نسخة التحليل الحالية، ويتم التعامل مع IOException وSecurityException للملفات غير القابلة للقراءة دون إسقاط التحليل كله. تمت إضافة اختبار SHA-256 معروف للمحتوى `abc`، ويستمر دعم cancellation عبر `ensureActive`.

## فحوصات منفذة

- `./gradlew testDebugUnitTest`: ناجح.
- `./gradlew lintDebug`: ناجح.
- `git diff --check`: ناجح.
- تمت إضافة اختبارات Cache metadata وanalysis version وSHA-256.
- لا يوجد AAB منشأ أو متابع في هذه الدفعة حسب سياسة المشروع.

## القيود المتبقية

- لا يوجد `adb` أو جهاز/Emulator، لذلك لم تُنفذ اختبارات MediaStore الحقيقية أو migration على جهاز أو اختبارات DuplicateEngine عبر ContentResolver فعلي.
- المرحلة 2 تبقى قيد التنفيذ بسبب إعادة فصل مكونات scanning ودعم Documents/Downloads/Archives/APK عبر SAF، وليس من الآمن ادعاء اكتمالها.
- ستحتاج المرحلة 4 لاحقًا إلى اختبار integration على مكتبة وسائط فعلية وقياس الذاكرة، دون بناء AAB قبل المرحلة النهائية.
