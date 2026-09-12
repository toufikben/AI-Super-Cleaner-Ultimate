# تقرير تنفيذ المراحل 5 و6 وتقدم المرحلة 2

**التاريخ:** 12 سبتمبر 2026

## المرحلة 2 — StorageScanner وMediaStore

تم فصل قائمة مصادر MediaStore في `MediaStoreInventory` لتكون مصدرًا واحدًا لنطاق الفحص، مع رسائل تقدم صريحة توضح أن المصدر هو Android MediaStore وأن النطاق Images/Videos/Audio. بقي دعم Documents وArchives وAPK عبر SAF خارج هذه الدفعة، كما بقي اختبار الصلاحيات الفعلي محجوبًا لغياب جهاز.

## المرحلة 5 — Exact Duplicate Engine

تم استخراج `DuplicateGrouping` من محرك Android حتى يمكن اختبار منطق التجميع دون ContentResolver. grouping الأولي يعتمد النوع والحجم كمرشح فقط، بينما exact grouping لا يحدث إلا بعد وجود content hash. أضيفت اختبارات تثبت أن ملفات ذات hash مختلف لا تتجمع وأن الملفات ذات hash متساوٍ تتجمع مع حساب recoverable bytes باستثناء ملف الاحتفاظ.

يستخدم المحرك SHA-256 streaming، ويعيد استخدام hash صالح، ويستمر في التعامل مع unreadable files وإلغاء coroutine. اختبار ContentResolver الفعلي بملفات على MediaStore ما زال متبقيًا بسبب عدم وجود جهاز أو Emulator.

## المرحلة 6 — Similarity وBlur وScreenshot

أضيف `MediaClassification` الذي يعرض blur كـ`Potentially blurry` وليس حقيقة مؤكدة، ويحسب confidence محدودًا، ويشرح أن النتيجة heuristic تحتاج مراجعة. screenshot لا يُصنف إلا عند وجود إشارة اسم أو relative path وعلى ملف image. تم استبعاد الملفات الداخلة في exact duplicate groups من similarity حتى لا تختلط فئتا EXACT_DUPLICATE وSIMILAR.

أضيفت اختبارات للتجميع والتصنيف. ما زال اختبار الصور الحقيقية resize/compression/crop/night/portrait على جهاز أو fixtures فعلية متبقيًا، كما أن similarity يحتاج لاحقًا تحسين buckets وscore لكل عضو بدل O(n²) الكامل.

## الفحوصات

- `./gradlew compileDebugKotlin`: ناجح.
- `./gradlew testDebugUnitTest`: ناجح.
- `./gradlew lintDebug`: ناجح.
- `git diff --check`: ناجح.
- لم يتم إنشاء أو متابعة AAB.

## الحالة الدقيقة

المرحلة 2 والمرحلتان 5 و6 تبقى قيد التنفيذ الجزئي؛ لا يتم تعليمها `[x]` قبل اختبارات MediaStore/الصور الفعلية، أو توثيق قبول القيود في Release Gate.
