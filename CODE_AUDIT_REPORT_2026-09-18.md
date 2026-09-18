# تقرير المراجعة الشاملة — AI Super Cleaner Ultimate

**التاريخ:** 18 سبتمبر 2026

**النطاق:** مراجعة مستقلة متعددة المحاور للكود، الأمان والخصوصية، اللغات وواجهة الإعدادات، الميزات المعلنة والمخفية، والبناء والاختبارات.

**حالة التغييرات:** لم يتم تعديل أي ملف مصدر، ولم يتم إنشاء commit أو push نتيجة هذه المراجعة. هذا التقرير يعرض النتائج والاقتراحات فقط بانتظار موافقة صاحب المشروع.

## الملخص التنفيذي

المشروع يُبنى بنجاح حاليًا، لكن المراجعة كشفت مخاطر وظيفية وأمنية وتدويلية لا تظهر في اختبار البناء وحده. أعلى الأولويات هي: عدم إخفاء فشل الحذف، عدم الادعاء بمحو آمن كامل على أجهزة Flash، حماية الخزنة وملفات المعاينة المؤقتة، توحيد جدولة WorkManager، إصلاح تقرير Wi‑Fi الثابت، وإزالة النصوص العربية المضمّنة مباشرة في شاشات Compose. كما أن تغطية الاختبارات الحالية لا تكفي لتأكيد سلامة دورة الحياة أو WorkManager أو الخزنة أو جميع اللغات.

## الأولوية P0 — يجب إصلاحها قبل إصدار موثوق

| المجال | المشكلة | الموضع | الأثر | الإصلاح المقترح |
|---|---|---|---|---|
| الحذف والتكرارات | تُحذف سجلات Room وحالة الواجهة حتى عند فشل حذف الملفات | `DuplicatesViewModel.kt:151-168`, `DuplicateRepository.kt:43` | يختفي ملف لم يُحذف، وتظهر نتيجة مضللة | إرجاع `successfulPaths` و`failedPaths` من `CleanupResult`، وتحديث Room/UI للناجح فقط مع اختبار فشل جزئي |
| تاريخ التنظيف | large-files والتكرارات تُسجل كمصدر `JUNK`، وقد يُحسب الحجم قبل نجاح الحذف | `CleanupManager.kt:48-70` | تاريخ وإحصاءات غير صحيحة | تمرير `HistoryEntry.Source` صراحة، واحتساب المساحة بعد حذف قابل للتحقق |
| Shredder | النسخة المؤقتة تُمحى، بينما المصدر الأصلي الذي اختاره المستخدم لا يُحذف | `ShredderViewModel.kt:27-68`, `FileUtils.kt:14-34` | ادعاء “Permanently erase” غير صحيح وظيفيًا | تنفيذ حذف المصدر عبر SAF/MediaStore عند السماح، أو تغيير الوصف فورًا إلى صياغة دقيقة، مع اختبار المصدر |
| PIN الخزنة | SHA-256 سريع بلا KDF أو rate limit أو backoff | `VaultAuthManager.kt:41-54` | قابلية أعلى للتخمين عند استخراج البيانات | PBKDF2-HMAC أو Argon2id، salt عشوائي، مقارنة ثابتة، حد محاولات وقفل مؤقت |
| معاينة الخزنة | plaintext مؤقت في cache باسم يتضمن الاسم الأصلي وقد يبقى بعد التعطل | `VaultManager.kt:31-33,83-114`, `VaultViewModel.kt:98-108` | تسرب محتمل لملفات حساسة وبقاء نسخ مؤقتة | اسم عشوائي، cache خاص، `finally` شامل، حذف عند الإلغاء/onCleared، وإلغاء Jobs القديمة |

## الأولوية P1 — إصلاحات مهمة قبل الإنتاج

### المنطق ودورة الحياة

1. **سباق معاينة Vault:** Job تنظيف قديم يستمر بعد إغلاق أو تغيير المعاينة. يجب حفظ `previewCleanupJob` وإلغاؤه قبل المعاينة الجديدة والإغلاق.
2. **فاصل Auto Clean غير محترم:** `SettingsViewModel` يجدول 24 ساعة دائمًا بدل `autoCleanIntervalHours` المحدد في Scheduler.
3. **جدولة Storage Alert غير موحدة:** الجدولة تحدث داخل Scheduler فقط، وقد تُجدول رغم تعطيل الإشعارات. يجب جعل startup وتغييرات الإعداد تمر عبر orchestrator واحد.
4. **مسح البيانات غير كامل:** `clearAllData` يتجاهل فشل Room ويستدعي callback دائمًا، ولا يمسح DataStore أو Vault أو temp أو WorkManager. يجب إرجاع نتيجة صريحة ومسح المكونات بطريقة منسقة.
5. **إعادة Worker بلا سياسة:** `AutoCleanWorker` و`StorageAlertWorker` يعيدان `retry` لكل استثناء. يجب إضافة backoff وتمييز الأخطاء الدائمة عن المؤقتة.
6. **استعلامات Room N+1:** `DuplicateRepository` يستعلم عن عناصر كل مجموعة بشكل منفصل. يجب استخدام علاقة Room أو batch query.
7. **Quick Clean Shortcut غير موصول:** `shortcuts.xml` يعلن `QUICK_CLEAN` لكن `MainActivity` لا يعالج `onCreate` أو `onNewIntent` لهذا action.
8. **تزامن عمليات scan/delete/import:** لا توجد حماية موحدة من النقر السريع أو تحديث حالة قديمة. يجب استخدام Mutex أو state machine أو epoch token.
9. **ضعف اختبارات lifecycle:** لا توجد تغطية كافية لإعادة إنشاء Activity أو process death أو permission callbacks أو رجوع المستخدم بين الشاشات.

### الأمان والخصوصية

1. **ملفات الخزنة المؤقتة:** `file_paths.xml` واسع نسبيًا، ويجب تضييقه إن لم يكن FileProvider ضروريًا.
2. **Biometric Weak:** الكود يقبل `BIOMETRIC_WEAK`؛ يجب توثيق ذلك أو تقييد الاستخدام لـ `BIOMETRIC_STRONG` مع PIN fallback.
3. **القفل التلقائي:** لا يظهر timeout موثوق للخزنة عند background. يجب القفل في lifecycle وبعد idle timeout.
4. **ادعاء المحو العسكري/DoD:** overwrite متعدد المرور لا يضمن محو Flash/F2FS أو النسخ/السحابة. يجب إزالة ادعاءات “military” و“DoD” أو جعلها تحذيرًا تقنيًا دقيقًا.
5. **النسخ الاحتياطي:** `allowBackup=true`، وبعض DataStore مثل `rules_prefs` و`permissions_prefs` و`ad_frequency` غير مستثنى صراحة. يجب تعطيل النسخ أو استثناء البيانات الحساسة واختبار Android 12+.
6. **صلاحيات زائدة:** يجب مراجعة `CHANGE_WIFI_STATE` و`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` والموقع، واستخدام أقل نطاق Media/Photo Picker/SAF حيث يمكن.
7. **Wi‑Fi غير دقيق:** `WifiSecurityScanner` يضع WPA2 ثابتًا، ولا يميز OPEN/WEP/WPA3/Unknown. يجب قراءة capabilities الفعلية وعرض Unknown عند عدم توفرها.
8. **Billing:** الاعتماد على Boolean محلي لحالة Premium يحتاج إعادة تحقق من BillingClient في كل تشغيل، مع معالجة expiry/cancel/acknowledgement.
9. **رسائل SDK الخام:** لا يفضل عرض `debugMessage` و`error.message` كما هي للمستخدم؛ يجب تحويلها لرسائل عامة.
10. **AdMob/UMP:** لا يوجد endpoint للمطور أو Firebase Analytics ظاهر في الكود، لكن SDKs الخارجية تعالج بياناتها كما هو مذكور في السياسة. يجب ألا تستخدم مواد المتجر عبارة “لا جمع بيانات” بلا تقييد.

## اللغات وواجهة الإعدادات

### ما تم التحقق منه

- جميع ملفات `values*` تحتوي 285 مفتاحًا.
- لا توجد مفاتيح ناقصة أو إضافية بين اللغات.
- `LocaleManager` و`locales_config.xml` متوافقان مع اللغات المتاحة.
- `pt-rBR` اسم مورد Android الصحيح، ويقابله `pt-BR` في locale code.
- Android RTL مفعّل عبر `android:supportsRtl=true`.

### المشكلة المهمة

توجد نصوص عربية ثابتة خارج موارد strings، ولذلك تغيير اللغة لا يطبق فعليًا على جميع الشاشات. المواضع المؤكدة:

- `BatteryScreen.kt`: حالات البطارية والشحن والحرارة والصحة.
- `DashboardScreen.kt`: “من/مستخدم/متاح”.
- `JunkScreen.kt`: رسائل الصلاحيات والعناصر المحددة.
- `ShredderScreen.kt`: صيغة عدد الملفات.
- `ThemeScreen.kt`: نصوص المعاينة وأسماء الأزرار.
- `SchedulerScreen.kt`: الفواصل الزمنية.
- `RulesScreen.kt`.
- `rules/components/ConditionEditor.kt`.
- `rules/components/RuleEditorScreen.kt`.

كما أن `SettingsScreen.kt` يعرض رقم الإصدار `1.0.0` ثابتًا بدل `BuildConfig.VERSION_NAME`، وهناك نصوص إنجليزية/أسماء ثابتة غير قابلة للترجمة في بعض المواضع.

### الاختبارات المقترحة

1. إضافة اختبار يقارن keys وplaceholders بين الإنجليزية وكل locale.
2. اختبار كل لغة من شاشة Settings → Language ثم إعادة إنشاء Activity والتحقق من استمرارها.
3. فتح Dashboard وBattery وJunk وRules وScheduler وShredder وTheme بكل لغة، والتأكد من عدم ظهور العربية الثابتة.
4. اختبار RTL للعربية والأردية بصريًا واتجاه الأيقونات والأسهم والأرقام.
5. اختبار `pt-BR` منفصلًا للتأكد من عدم سقوطه إلى `pt` أو الإنجليزية.

## الميزات المعلنة مقابل التنفيذ

| الميزة | الحالة | الملاحظة |
|---|---|---|
| Junk cleanup | موجودة | الحذف الجزئي والتاريخ يحتاجان إصلاحًا |
| Duplicates | موجودة | فشل الحذف يخفي العناصر |
| Large files | موجودة | مصدر التاريخ يحتاج تصحيحًا |
| Privacy Scanner | موجودة | heuristic محدود، و`lastUsedDays` دائمًا null |
| Vault | موجودة | مخاطر PIN والـplaintext المؤقت |
| Shredder | موجودة جزئيًا | يمحو النسخة المؤقتة لا المصدر الأصلي |
| Wi‑Fi Security | موجودة جزئيًا | نوع الأمان ثابت WPA2 |
| RAM Booster | موجودة لكن الادعاء مبالغ فيه | تعتمد على `System.gc()` ولا تضمن تحسينًا |
| Battery | موجودة | توجد نصوص عربية ثابتة |
| Rules | موجودة | export/import يبدو غير مكشوف بوضوح في UI |
| Scheduler | موجودة | تعارض الفواصل ودورة الحياة |
| Storage alerts | موجودة | تحتاج orchestrator وstartup/reboot tests |
| History | موجودة | قد تعرض مصادر/أحجامًا غير صحيحة |
| Widget | موجود | غير موثق بوضوح في وصف المتجر |
| Quick Clean shortcut | معلنة في XML لكن غير معالجة | يجب وصلها أو إزالتها |
| Rewarded ads | موجودة في AdManager لكن لا يظهر caller | dead/unexposed feature محتمل |
| Privacy Policy link | المورد موجود لكن رابط Settings غير واضح | يجب إضافة رابط فعلي واختباره |
| Trash/restore | الوصف يذكره، التنفيذ غير واضح | نفّذها أو احذف ذكرها |
| Premium claims | بعض الادعاءات غير مثبتة | خصوصًا 3× faster وUnlimited vault وNo tracking |

## البناء والاختبارات

- `testDebugUnitTest` ينجح حاليًا.
- البناء وLint ينجحان، لكن Lint لديه نحو 122 warning.
- توجد ملفات Kotlin كثيرة مقابل عدد قليل من الاختبارات.
- لا توجد تغطية كافية لـ Room وWorkManager وVault وPermissions وCompose navigation وR8 runtime.
- CI لا يشغل بصورة صريحة `testReleaseUnitTest` و`lintRelease` واختبارات جهاز قبل اعتماد artifact.
- Release المحلي غير موقع؛ التوقيع يجب أن يتم في CI أو بيئة المفاتيح الرسمية.
- `verify_release.sh` يفحص APK أفضل من AAB ولا يفرض bundletool/jarsigner على AAB.
- `MANAGE_EXTERNAL_STORAGE` قد ينجح تقنيًا في البناء ثم يحتاج قبول Google Play عبر Declaration Form وInternal Testing.

## خطة الإصلاح المقترحة بعد الموافقة

### المرحلة 1 — سلامة البيانات والأمان

إصلاح نتائج الحذف، تاريخ العمليات، Shredder، PIN/KDF، ملفات Vault المؤقتة، القفل التلقائي، والنسخ الاحتياطي.

### المرحلة 2 — جدولة ودورة حياة

توحيد WorkManager، إصلاح الفواصل، boot/reboot behavior، الإشعارات، الإلغاء، race conditions، ومسح البيانات الكامل.

### المرحلة 3 — اللغات

تحويل كل النصوص الثابتة إلى resources، إضافة placeholders، تحديث اللغات الـ11، وربط رقم الإصدار بـ BuildConfig.

### المرحلة 4 — الميزات والتوثيق

وصل Quick Clean أو إزالته، إصلاح Wi‑Fi، إضافة رابط سياسة الخصوصية، مراجعة Trash وWidget وRewarded Ads، وتصحيح ادعاءات Premium والوصف.

### المرحلة 5 — الاختبارات والإصدار

إضافة unit/Compose/WorkManager/security tests، تشغيل release lint/tests، تحسين فحوص AAB، معالجة التحذيرات المهمة، ثم بناء نسخة Release موقعة.

## قرار التنفيذ

لا توجد تغييرات مصدرية نُفذت نتيجة هذه المراجعة. يلزم اعتماد صاحب المشروع لخطة الإصلاح أو تحديد مرحلة/مشكلات معينة قبل بدء أي تعديل أو commit أو push.
